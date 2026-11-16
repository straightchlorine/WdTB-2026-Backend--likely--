package pl.wsb.likely;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import pl.wsb.likely.market.entity.Wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Checks that the data in data.sql follows the game rules. Some seed numbers are calculated from
 * others (shares, balances, prices, Brier) and typed in by hand, so they are easy to break when
 * editing.
 *
 * <p>Rules that span several tables cannot be written as a CHECK in the database. In the app the
 * services will enforce them (LAB 04). Here we check them on the seed data. Each query returns the
 * rows that break the rule, so a correct seed gives an empty list.
 */
@DataJpaTest
class SeedConsistencyTest {

    // Indexes into the array of shares sold on each side of the market.
    private static final int YES = 0;
    private static final int NO = 1;

    @Autowired private JdbcTemplate jdbc;

    @ParameterizedTest(name = "{0}: {1} rows")
    @CsvSource({"question, 7", "resolution, 3", "trade, 9", "holding, 9", "score_result, 6"})
    void seedHasExpectedRowCount(String table, int expected) {
        assertThat(jdbc.queryForObject("select count(*) from " + table, Integer.class))
                .isEqualTo(expected);
    }

    static Stream<Arguments> rules() {
        return Stream.of(
                arguments(
                        "holding = sum of the player's trades in the question",
                        """
                        select h.id from holding h
                        where h.shares_yes <> coalesce((select sum(t.shares) from trade t
                                where t.wallet_id = h.wallet_id and t.question_id = h.question_id and t.side = 'YES'), 0)
                           or h.shares_no <> coalesce((select sum(t.shares) from trade t
                                where t.wallet_id = h.wallet_id and t.question_id = h.question_id and t.side = 'NO'), 0)
                        """),
                arguments(
                        "every trade has a holding",
                        """
                        select t.id from trade t
                        where not exists (select 1 from holding h
                                where h.wallet_id = t.wallet_id and h.question_id = t.question_id)
                        """),
                arguments(
                        "balance = starting balance - sum(amount) + sum(payout)",
                        """
                        select w.id from wallet w
                        where w.balance <> %s
                                - coalesce((select sum(t.amount) from trade t where t.wallet_id = w.id), 0)
                                + coalesce((select sum(h.payout) from holding h where h.wallet_id = w.id), 0)
                        """
                                .formatted(Wallet.STARTING_BALANCE.toPlainString())),
                arguments(
                        "payout: 1 per winning share, cost refund if cancelled, otherwise none",
                        """
                        select h.id from holding h
                        join question q on q.id = h.question_id
                        left join resolution r on r.question_id = q.id
                        where case q.status
                                when 'RESOLVED' then h.payout is distinct from
                                        case r.outcome when 'YES' then h.shares_yes else h.shares_no end
                                when 'CANCELLED' then h.payout is distinct from (select sum(t.amount) from trade t
                                        where t.wallet_id = h.wallet_id and t.question_id = h.question_id)
                                else h.payout is not null
                              end
                        """),
                arguments(
                        "market loss <= b * ln 2",
                        """
                        select q.id from question q
                        where q.status = 'RESOLVED'
                          and (select sum(h.payout) from holding h where h.question_id = q.id)
                              - (select sum(t.amount) from trade t where t.question_id = q.id) > q.liquidity_b * ln(2)
                        """),
                arguments(
                        "a resolution exists exactly for RESOLVED",
                        """
                        select q.id from question q
                        where (q.status = 'RESOLVED') <> (exists (select 1 from resolution r where r.question_id = q.id))
                        """),
                arguments(
                        "trade between question creation and close",
                        """
                        select t.id from trade t
                        join question q on q.id = t.question_id
                        where t.created_at < q.created_at or t.created_at >= q.close_at
                        """),
                // True only right after start. In the running app the LAB 06 job sets the CLOSED
                // status.
                arguments(
                        "status OPEN/CLOSED matches close_at",
                        """
                        select q.id from question q
                        where (q.status = 'OPEN' and q.close_at <= current_timestamp)
                           or (q.status = 'CLOSED' and q.close_at > current_timestamp)
                        """),
                arguments(
                        "wallet belongs to an existing account",
                        """
                        select w.id from wallet w
                        where not exists (select 1 from user_account u where u.id = w.account_id)
                        """),
                arguments(
                        "every account has a wallet",
                        """
                        select u.id from user_account u
                        where not exists (select 1 from wallet w where w.account_id = u.id)
                        """),
                arguments(
                        "score points to an existing account and a RESOLVED question",
                        """
                        select s.id from score_result s
                        left join question q on q.id = s.question_id
                        where q.status is distinct from 'RESOLVED'
                           or not exists (select 1 from user_account u where u.id = s.account_id)
                        """),
                arguments(
                        "a score exists exactly for players with a net position in a RESOLVED"
                                + " question",
                        """
                        select h.id from holding h
                        join question q on q.id = h.question_id
                        join wallet w on w.id = h.wallet_id
                        where q.status = 'RESOLVED'
                          and (h.shares_yes <> h.shares_no) <> (exists (select 1 from score_result s
                                where s.account_id = w.account_id and s.question_id = h.question_id))
                        """));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rules")
    void seedFollowsRule(String rule, String violatingRowsSql) {
        assertThat(jdbc.queryForList(violatingRowsSql)).as(rule).isEmpty();
    }

    /**
     * Rebuilds the market of each question from zero, trade by trade (order = id), and compares the
     * cost and the price after the trade with the stored values.
     */
    @Test
    @DisplayName("Cost and price of every trade match the rebuilt LMSR market")
    void tradeAmountsAndPricesMatchLmsr() {
        Map<Long, double[]> marketByQuestion = new HashMap<>();
        List<Map<String, Object>> trades =
                jdbc.queryForList(
                        """
                        select t.id, t.question_id, t.side, t.shares, t.amount, t.price_after, q.liquidity_b
                        from trade t join question q on q.id = t.question_id
                        order by t.id
                        """);

        for (Map<String, Object> t : trades) {
            double b = ((BigDecimal) t.get("liquidity_b")).doubleValue();
            BigDecimal shares = (BigDecimal) t.get("shares");
            double[] sharesSold =
                    marketByQuestion.computeIfAbsent(
                            (Long) t.get("question_id"), id -> new double[2]);
            double costBefore = lmsrCost(b, sharesSold[YES], sharesSold[NO]);
            sharesSold["YES".equals(t.get("side").toString()) ? YES : NO] += shares.doubleValue();

            // The cost never exceeds the number of shares (each pays at most 1), but CEILING could
            // push it slightly over.
            BigDecimal expectedAmount =
                    BigDecimal.valueOf(lmsrCost(b, sharesSold[YES], sharesSold[NO]) - costBefore)
                            .setScale(4, RoundingMode.CEILING)
                            .min(shares);
            BigDecimal expectedPrice =
                    BigDecimal.valueOf(lmsrPriceYes(b, sharesSold[YES], sharesSold[NO]))
                            .setScale(6, RoundingMode.HALF_UP);
            assertThat((BigDecimal) t.get("amount"))
                    .as("amount, trade %s", t.get("id"))
                    .isEqualByComparingTo(expectedAmount);
            assertThat((BigDecimal) t.get("price_after"))
                    .as("price_after, trade %s", t.get("id"))
                    .isEqualByComparingTo(expectedPrice);
        }
    }

    @Test
    @DisplayName("Shares in a question equal the sum of shares in its trades")
    void questionSharesEqualSumOfTrades() {
        assertThat(
                        jdbc.queryForList(
                                """
                                select q.id from question q
                                where q.shares_yes <> coalesce((select sum(t.shares) from trade t where t.question_id = q.id and t.side = 'YES'), 0)
                                   or q.shares_no <> coalesce((select sum(t.shares) from trade t where t.question_id = q.id and t.side = 'NO'), 0)
                                """))
                .isEmpty();
    }

    /**
     * The forecast is the price after the player's last trade, clamped to 0.5 on the side of their
     * net position. Brier is (forecast - result)^2, with result 1 for YES and 0 for NO.
     */
    @Test
    @DisplayName("Forecast and Brier score follow the scoring rule")
    void scoresFollowForecastRuleAndBrier() {
        List<Map<String, Object>> scores =
                jdbc.queryForList(
                        """
                        select s.id, s.forecast, s.brier, r.outcome, h.shares_yes, h.shares_no, t.price_after
                        from score_result s
                        join resolution r on r.question_id = s.question_id
                        join wallet w on w.account_id = s.account_id
                        join holding h on h.wallet_id = w.id and h.question_id = s.question_id
                        join trade t on t.id = (select max(id) from trade
                                where wallet_id = w.id and question_id = s.question_id)
                        """);
        assertThat(scores).hasSize(6);

        for (Map<String, Object> s : scores) {
            BigDecimal lastPrice = (BigDecimal) s.get("price_after");
            boolean netYes =
                    ((BigDecimal) s.get("shares_yes")).compareTo((BigDecimal) s.get("shares_no"))
                            > 0;
            BigDecimal half = new BigDecimal("0.5");
            BigDecimal expectedForecast = netYes ? lastPrice.max(half) : lastPrice.min(half);
            BigDecimal outcome =
                    "YES".equals(s.get("outcome").toString()) ? BigDecimal.ONE : BigDecimal.ZERO;
            BigDecimal expectedBrier =
                    expectedForecast.subtract(outcome).pow(2).setScale(6, RoundingMode.HALF_UP);

            assertThat((BigDecimal) s.get("forecast"))
                    .as("forecast, score %s", s.get("id"))
                    .isEqualByComparingTo(expectedForecast);
            assertThat((BigDecimal) s.get("brier"))
                    .as("brier, score %s", s.get("id"))
                    .isEqualByComparingTo(expectedBrier);
        }
    }

    // C(q) = b * ln(exp(qYes/b) + exp(qNo/b)). We subtract the maximum first so exp does not
    // overflow a double.
    private static double lmsrCost(double b, double yes, double no) {
        double a = yes / b;
        double c = no / b;
        double m = Math.max(a, c);
        return b * (m + Math.log(Math.exp(a - m) + Math.exp(c - m)));
    }

    private static double lmsrPriceYes(double b, double yes, double no) {
        return 1 / (1 + Math.exp((no - yes) / b));
    }
}
