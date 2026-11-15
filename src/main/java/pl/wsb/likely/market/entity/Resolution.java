package pl.wsb.likely.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import pl.wsb.likely.account.domain.UserAccount;

import java.time.LocalDateTime;

/**
 * The final result of a question (1:1 with {@link Question}).
 *
 * <p>A row exists iff the question is RESOLVED.
 *
 * <p>A cancelled question has none. The unique {@code question_id} ensures a question cannot be
 * resolved twice on a db level.
 *
 * <p>{@code Question} has no field pointing here, so it isn't aware of its own resolution. Adding
 * it there would cause an extra query for every question, since the inverse {@code @OneToOne} side
 * is always loaded eagerly. The resolution is read from a repository instead.
 */
@Entity
@Table(name = "resolution")
public class Resolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", unique = true)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 3)
    private Outcome outcome;

    // The account that resolved the question: its creator or an admin
    // (many resolutions -> one account).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resolved_by_id")
    private UserAccount resolvedBy;

    @Column(name = "resolved_at", nullable = false)
    private LocalDateTime resolvedAt;

    protected Resolution() {}

    public Resolution(
            Question question, Outcome outcome, UserAccount resolvedBy, LocalDateTime resolvedAt) {
        this.question = question;
        this.outcome = outcome;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public Question getQuestion() {
        return question;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public UserAccount getResolvedBy() {
        return resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
}
