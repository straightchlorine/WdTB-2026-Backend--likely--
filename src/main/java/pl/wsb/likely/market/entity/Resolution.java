package pl.wsb.likely.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * The final result of a question (1:1 with {@link Question}).
 *
 * <p>A row exists iff the question is RESOLVED.
 *
 * <p>A cancelled question has none. The unique {@code question_id} ensures a question cannot be
 * resolved twice on a db level.
 *
 * <p>{@code Question} has no field pointing here, so it isn't aware of its own resolution. The
 * resolution is read from a repository instead.
 */
@Entity
@Table(name = "resolution")
public class Resolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false, unique = true)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 3)
    private Outcome outcome;

    // Either the question creator or an admin.
    @Column(name = "resolved_by_id", nullable = false)
    private Long resolvedById;

    @Column(name = "resolved_at", nullable = false)
    private LocalDateTime resolvedAt;

    protected Resolution() {}

    public Resolution(
            Long questionId, Outcome outcome, Long resolvedById, LocalDateTime resolvedAt) {
        this.questionId = questionId;
        this.outcome = outcome;
        this.resolvedById = resolvedById;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public Long getResolvedById() {
        return resolvedById;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
}
