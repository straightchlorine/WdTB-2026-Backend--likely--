package pl.wsb.likely.market.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The {@code @Version} scenario: we read a question or wallet, someone else saves a change in the
 * meantime (we raise the version in SQL), and we then try to save our stale copy. Hibernate
 * compares the versions and rejects the write instead of overwriting the other change.
 */
@DataJpaTest
class OptimisticLockingTest {

    @Autowired private TestEntityManager em;

    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("A stale copy of a question cannot overwrite another change")
    void staleQuestionCannotBeSaved() {
        Question stale =
                em.getEntityManager()
                        .createQuery("select q from Question q", Question.class)
                        .setMaxResults(1)
                        .getSingleResult();

        assertStaleCopyRejected(stale, "question", stale.getId());
    }

    @Test
    @DisplayName("A stale copy of a wallet cannot overwrite another change")
    void staleWalletCannotBeSaved() {
        Wallet stale =
                em.getEntityManager()
                        .createQuery("select w from Wallet w", Wallet.class)
                        .setMaxResults(1)
                        .getSingleResult();

        assertStaleCopyRejected(stale, "wallet", stale.getId());
    }

    private void assertStaleCopyRejected(Object stale, String table, Long id) {
        em.detach(stale);
        jdbc.update("update " + table + " set version = version + 1 where id = ?", id);

        assertThatThrownBy(() -> em.merge(stale)).isInstanceOf(OptimisticLockException.class);
    }
}
