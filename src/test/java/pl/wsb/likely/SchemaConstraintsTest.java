package pl.wsb.likely;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.stream.Stream;

/**
 * Checks that the constraints from the entities really reached the database. Each SQL statement
 * below breaks the data from data.sql, and the database must reject it. We write plain SQL because
 * such writes (data.sql, H2 console) skip the Java validation.
 */
@DataJpaTest
class SchemaConstraintsTest {

    @Autowired private JdbcTemplate jdbc;

    // The second argument is a part of the database error message. Without it the test would also
    // pass when the write fails for another reason than the rule in the case name (e.g. NOT NULL
    // instead of CHECK).
    static Stream<Arguments> invalidWrites() {
        return Stream.of(
                arguments("b = 0", "ck_question", "update question set liquidity_b = 0"),
                arguments(
                        "negative shares_yes in question",
                        "ck_question",
                        "update question set shares_yes = -1"),
                arguments(
                        "negative shares_no in question",
                        "ck_question",
                        "update question set shares_no = -1"),
                arguments(
                        "close before creation",
                        "ck_question",
                        "update question set close_at = dateadd('DAY', -1, created_at)"),
                arguments(
                        "close equal to creation",
                        "ck_question",
                        "update question set close_at = created_at"),
                arguments(
                        "status outside the enum", "MAYBE", "update question set status = 'MAYBE'"),
                arguments(
                        "question without creator",
                        "CREATOR_ID",
                        "update question set creator_id = null"),
                arguments("question without title", "TITLE", "update question set title = null"),
                arguments(
                        "question without creation date",
                        "CREATED_AT",
                        "update question set created_at = null"),
                arguments(
                        "question with unknown creator",
                        "FOREIGN KEY(CREATOR_ID)",
                        "update question set creator_id = -1"),
                arguments(
                        "second resolution for a question",
                        "RESOLUTION(QUESTION_ID",
                        "insert into resolution (question_id, outcome, source, resolved_at)"
                                + " select question_id, 'NO', source, resolved_at from resolution"),
                arguments(
                        "resolution without outcome",
                        "OUTCOME",
                        "update resolution set outcome = null"),
                // WHERE is needed for resolution, wallet, holding and score_result. An update of
                // all rows would fail on uniqueness first, not on the foreign key.
                arguments(
                        "resolution of unknown question",
                        "FOREIGN KEY(QUESTION_ID)",
                        "update resolution set question_id = -1 where id = (select min(id) from"
                                + " resolution)"),
                arguments("negative balance", "ck_wallet", "update wallet set balance = -0.0001"),
                arguments(
                        "second wallet for the same account",
                        "WALLET(ACCOUNT_ID",
                        "insert into wallet (account_id, balance, created_at, version)"
                                + " select account_id, 0, created_at, 0 from wallet"),
                arguments(
                        "wallet without account",
                        "ACCOUNT_ID",
                        "update wallet set account_id = null"),
                arguments(
                        "wallet of unknown account",
                        "FOREIGN KEY(ACCOUNT_ID)",
                        "update wallet set account_id = -1 where id = (select min(id) from"
                                + " wallet)"),
                arguments(
                        "wallet without opening date",
                        "CREATED_AT",
                        "update wallet set created_at = null"),
                arguments(
                        "trade below 0.01 shares",
                        "ck_trade",
                        "update trade set shares = 0.0099, amount = 0.0099"),
                arguments("free trade", "ck_trade", "update trade set amount = 0"),
                arguments(
                        "cost higher than shares",
                        "ck_trade",
                        "update trade set amount = shares + 1"),
                arguments("price above 1", "ck_trade", "update trade set price_after = 1.000001"),
                arguments("price below 0", "ck_trade", "update trade set price_after = -0.000001"),
                arguments(
                        "trade without question",
                        "QUESTION_ID",
                        "update trade set question_id = null"),
                arguments("trade without wallet", "WALLET_ID", "update trade set wallet_id = null"),
                arguments("trade without side", "SIDE", "update trade set side = null"),
                arguments(
                        "trade with unknown question",
                        "FOREIGN KEY(QUESTION_ID)",
                        "update trade set question_id = -1"),
                arguments(
                        "trade with unknown wallet",
                        "FOREIGN KEY(WALLET_ID)",
                        "update trade set wallet_id = -1"),
                arguments(
                        "second holding for the same pair",
                        "uk_holding_wallet_question",
                        "insert into holding (wallet_id, question_id, shares_yes, shares_no) "
                                + "select wallet_id, question_id, 0, 0 from holding"),
                arguments(
                        "negative shares_yes in holding",
                        "ck_holding",
                        "update holding set shares_yes = -1"),
                arguments(
                        "negative shares_no in holding",
                        "ck_holding",
                        "update holding set shares_no = -1"),
                arguments("negative payout", "ck_holding", "update holding set payout = -1"),
                arguments(
                        "holding with unknown wallet",
                        "FOREIGN KEY(WALLET_ID)",
                        "update holding set wallet_id = -1 where id = (select min(id) from"
                                + " holding)"),
                arguments(
                        "holding with unknown question",
                        "FOREIGN KEY(QUESTION_ID)",
                        "update holding set question_id = -1 where id = (select min(id) from"
                                + " holding)"),
                arguments(
                        "second score for a player in a question",
                        "uk_score_result_account_question",
                        "insert into score_result (account_id, question_id, forecast, brier)"
                                + " select account_id, question_id, forecast, brier from"
                                + " score_result"),
                arguments(
                        "forecast above 1",
                        "ck_score_result",
                        "update score_result set forecast = 1.5"),
                arguments(
                        "Brier above 1", "ck_score_result", "update score_result set brier = 1.5"),
                arguments(
                        "Brier below 0",
                        "ck_score_result",
                        "update score_result set brier = -0.000001"),
                arguments(
                        "score without question",
                        "QUESTION_ID",
                        "update score_result set question_id = null"),
                arguments(
                        "score without player",
                        "ACCOUNT_ID",
                        "update score_result set account_id = null"),
                arguments(
                        "score of unknown question",
                        "FOREIGN KEY(QUESTION_ID)",
                        "update score_result set question_id = -1 where id = (select min(id) from"
                                + " score_result)"),
                arguments(
                        "score of unknown player",
                        "FOREIGN KEY(ACCOUNT_ID)",
                        "update score_result set account_id = -1 where id = (select min(id) from"
                                + " score_result)"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidWrites")
    void databaseRejectsInvalidWrite(String rule, String constraint, String sql) {
        assertThatThrownBy(() -> jdbc.update(sql))
                .isInstanceOf(DataIntegrityViolationException.class)
                .message()
                .containsIgnoringCase(constraint);
    }

    // A column with a too small scale would not make any seed write fail: H2 rounds the extra
    // decimals instead (e.g. 0.549834 into numeric(9,4) is stored as 0.5498). The seed data cannot
    // catch a wrong scale, so we check the column definition directly.
    static Stream<Arguments> decimalColumns() {
        return Stream.of(
                arguments("question", "liquidity_b", 19, 4),
                        arguments("question", "shares_yes", 19, 4),
                arguments("question", "shares_no", 19, 4), arguments("wallet", "balance", 19, 4),
                arguments("trade", "shares", 19, 4), arguments("trade", "amount", 19, 4),
                arguments("trade", "price_after", 9, 6), arguments("holding", "shares_yes", 19, 4),
                arguments("holding", "shares_no", 19, 4), arguments("holding", "payout", 19, 4),
                arguments("score_result", "forecast", 9, 6),
                        arguments("score_result", "brier", 9, 6));
    }

    @ParameterizedTest(name = "{0}.{1} = numeric({2},{3})")
    @MethodSource("decimalColumns")
    void decimalColumnHasExpectedPrecisionAndScale(
            String table, String column, int precision, int scale) {
        var actual =
                jdbc.queryForMap(
                        "select numeric_precision p, numeric_scale s from"
                                + " information_schema.columns where lower(table_name) = ? and"
                                + " lower(column_name) = ?",
                        table,
                        column);
        assertThat(((Number) actual.get("P")).intValue()).isEqualTo(precision);
        assertThat(((Number) actual.get("S")).intValue()).isEqualTo(scale);
    }
}
