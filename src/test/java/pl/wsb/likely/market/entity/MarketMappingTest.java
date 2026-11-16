package pl.wsb.likely.market.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import pl.wsb.likely.account.domain.AccountRole;
import pl.wsb.likely.account.domain.UserAccount;
import pl.wsb.likely.scoring.entity.ScoreResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Each relation is saved and loaded back, and the related entity arrives lazily (as a proxy) until
 * we touch it. {@code clear()} after the save forces a real read from the database instead of
 * returning objects from the EntityManager memory.
 */
@DataJpaTest
class MarketMappingTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Autowired private TestEntityManager em;

    private UserAccount creator;
    private Question question;
    private Wallet wallet;
    private Trade trade;
    private Holding holding;
    private Resolution resolution;

    @BeforeEach
    void persistGraph() {
        creator = em.persist(new UserAccount("author@example.com", "Author", AccountRole.USER));
        question =
                em.persist(
                        new Question(
                                creator,
                                "Test question?",
                                null,
                                T0.plusDays(7),
                                new BigDecimal("100.0000"),
                                T0));
        wallet = em.persist(new Wallet(creator, T0));
        trade =
                em.persist(
                        new Trade(
                                question,
                                wallet,
                                Outcome.YES,
                                new BigDecimal("10.0000"),
                                new BigDecimal("5.1250"),
                                new BigDecimal("0.524979"),
                                T0.plusHours(1)));
        holding = em.persist(new Holding(wallet, question));
        resolution =
                em.persist(new Resolution(question, Outcome.YES, "Test source", T0.plusDays(8)));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("The question creator loads lazily as an account from the database")
    void questionPointsToCreatorAccount() {
        Question loaded = em.find(Question.class, question.getId());

        assertThat(Hibernate.isInitialized(loaded.getCreator())).isFalse();
        assertThat(loaded.getCreator().getEmail()).isEqualTo("author@example.com");
    }

    @Test
    @DisplayName("A new question is open, has an empty market and version 0")
    void newQuestionStartsOpenWithEmptyMarket() {
        Question loaded = em.find(Question.class, question.getId());

        assertThat(loaded.getStatus()).isEqualTo(QuestionStatus.OPEN);
        assertThat(loaded.getMarket().getSharesYes()).isEqualByComparingTo("0");
        assertThat(loaded.getMarket().getSharesNo()).isEqualByComparingTo("0");
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("A trade points lazily to its question and wallet and keeps amount and price")
    void tradeOwnsBothManyToOneSides() {
        Trade loaded = em.find(Trade.class, trade.getId());

        assertThat(Hibernate.isInitialized(loaded.getQuestion())).isFalse();
        assertThat(Hibernate.isInitialized(loaded.getWallet())).isFalse();
        assertThat(loaded.getQuestion().getTitle()).isEqualTo("Test question?");
        assertThat(loaded.getWallet().getAccount().getId()).isEqualTo(creator.getId());
        assertThat(loaded.getAmount()).isEqualByComparingTo("5.1250");
        assertThat(loaded.getPriceAfter()).isEqualByComparingTo("0.524979");
    }

    @Test
    @DisplayName("The trade list of a question is lazy and read-only")
    void questionTradesIsLazyReadOnlyInverseSide() {
        Question loaded = em.find(Question.class, question.getId());

        // getTrades() wraps the collection, so we ask JPA directly whether the trades field is
        // loaded.
        assertThat(
                        em.getEntityManager()
                                .getEntityManagerFactory()
                                .getPersistenceUnitUtil()
                                .isLoaded(loaded, "trades"))
                .isFalse();
        assertThat(loaded.getTrades()).extracting(Trade::getId).containsExactly(trade.getId());
        assertThatThrownBy(() -> loaded.getTrades().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("A holding joins wallet and question and has no payout yet")
    void holdingJoinsWalletAndQuestion() {
        Holding loaded = em.find(Holding.class, holding.getId());

        assertThat(Hibernate.isInitialized(loaded.getWallet())).isFalse();
        assertThat(Hibernate.isInitialized(loaded.getQuestion())).isFalse();
        assertThat(loaded.getWallet().getId()).isEqualTo(wallet.getId());
        assertThat(loaded.getQuestion().getId()).isEqualTo(question.getId());
        assertThat(loaded.getSharesYes()).isEqualByComparingTo("0");
        assertThat(loaded.getPayout()).isNull();
    }

    @Test
    @DisplayName("A resolution loads its question lazily")
    void resolutionLoadsItsQuestionLazily() {
        Resolution loaded = em.find(Resolution.class, resolution.getId());

        assertThat(Hibernate.isInitialized(loaded.getQuestion())).isFalse();
        assertThat(loaded.getQuestion().getTitle()).isEqualTo("Test question?");
        assertThat(loaded.getOutcome()).isEqualTo(Outcome.YES);
    }

    @Test
    @DisplayName("A wallet loads its owner account lazily")
    void walletLoadsOwnerAccountLazily() {
        Wallet loaded = em.find(Wallet.class, wallet.getId());

        assertThat(Hibernate.isInitialized(loaded.getAccount())).isFalse();
        assertThat(loaded.getAccount().getEmail()).isEqualTo("author@example.com");
    }

    @Test
    @DisplayName("A new wallet gets the starting balance")
    void walletStartsWithStartingBalance() {
        Wallet loaded = em.find(Wallet.class, wallet.getId());

        assertThat(loaded.getBalance()).isEqualByComparingTo(Wallet.STARTING_BALANCE);
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("A score loads its player and its question lazily")
    void scoreResultLoadsPlayerAndQuestionLazily() {
        ScoreResult score =
                em.persist(
                        new ScoreResult(
                                creator,
                                question,
                                new BigDecimal("0.524979"),
                                new BigDecimal("0.225645")));
        em.flush();
        em.clear();

        ScoreResult loaded = em.find(ScoreResult.class, score.getId());

        assertThat(Hibernate.isInitialized(loaded.getAccount())).isFalse();
        assertThat(Hibernate.isInitialized(loaded.getQuestion())).isFalse();
        assertThat(loaded.getAccount().getEmail()).isEqualTo("author@example.com");
        assertThat(loaded.getQuestion().getTitle()).isEqualTo("Test question?");
        assertThat(loaded.getBrier()).isEqualByComparingTo("0.225645");
    }
}
