package pl.wsb.likely.scoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The Brier score of one player in one resolved question. Ranking of players would be built from
 * this rows.
 *
 * <p>Module scoring does not know the entities of modules market and account. It keeps only ids,
 * with no relations and no FK. The Service is planned to check that those ids are valid.
 */
@Entity
@Table(
        name = "score_result",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_score_result_account_question",
                        columnNames = {"account_id", "question_id"}))
@Check(name = "ck_score_result", constraints = "forecast between 0 and 1 and brier between 0 and 1")
public class ScoreResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    // YES probability assigned to the player:
    // the price after their last trade (highest id), clamped to 0.5 on the side of their net
    // position:
    // (net YES: max(0.5, price), otherwise min(0.5, price)).
    // This way a player who bought NO does not get a YES forecast just because the price was high.
    @Column(name = "forecast", nullable = false, precision = 9, scale = 6)
    private BigDecimal forecast;

    // (forecast - result)^2, where result is 1 for YES and 0 for NO. Lower is better.
    @Column(name = "brier", nullable = false, precision = 9, scale = 6)
    private BigDecimal brier;

    @Column(name = "scored_at", nullable = false)
    private LocalDateTime scoredAt;

    protected ScoreResult() {}

    public ScoreResult(
            Long accountId,
            Long questionId,
            BigDecimal forecast,
            BigDecimal brier,
            LocalDateTime scoredAt) {
        this.accountId = accountId;
        this.questionId = questionId;
        this.forecast = forecast;
        this.brier = brier;
        this.scoredAt = scoredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public BigDecimal getForecast() {
        return forecast;
    }

    public BigDecimal getBrier() {
        return brier;
    }

    public LocalDateTime getScoredAt() {
        return scoredAt;
    }
}
