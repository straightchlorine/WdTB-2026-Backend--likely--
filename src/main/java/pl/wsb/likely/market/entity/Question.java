package pl.wsb.likely.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A question about the future, with its own LMSR market.
 *
 * <p>Every trade changes the market state ({@link LmsrState}), so the question has
 * {@code @Version}. Two parallel buys must not price from the same stale state.
 *
 * <p>The caller sets the creation time (unlike in {@code UserAccount}), so tests and seed data can
 * choose it.
 */
@Entity
@Table(name = "question")
// b > 0, since the LMSR formula divides by b.
// Shares are never negative.
// A question cannot close before it is created.
@Check(
        name = "ck_question",
        constraints =
                "liquidity_b > 0 and shares_yes >= 0 and shares_no >= 0 and close_at > created_at")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Id of the account that created the question. Plain id, so modules refer to one another
    // by id only.
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private QuestionStatus status;

    @Column(name = "close_at", nullable = false)
    private LocalDateTime closeAt;

    @Embedded private LmsrState market;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Question() {}

    public Question(
            Long creatorId,
            String title,
            String description,
            LocalDateTime closeAt,
            BigDecimal liquidityB,
            LocalDateTime createdAt) {
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.status = QuestionStatus.OPEN;
        this.closeAt = closeAt;
        this.market = new LmsrState(liquidityB);
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCloseAt() {
        return closeAt;
    }

    public LmsrState getMarket() {
        return market;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }
}
