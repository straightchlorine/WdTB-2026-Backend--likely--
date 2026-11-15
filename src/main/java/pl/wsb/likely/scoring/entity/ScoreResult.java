package pl.wsb.likely.scoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import pl.wsb.likely.account.domain.UserAccount;
import pl.wsb.likely.market.entity.Question;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The Brier score of one player in one resolved question. Ranking of players would be built from
 * these rows.
 *
 * <p>Many scores point to one player (account) and many to one question; a player has at most one
 * score per question.
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private UserAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

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
            UserAccount account,
            Question question,
            BigDecimal forecast,
            BigDecimal brier,
            LocalDateTime scoredAt) {
        this.account = account;
        this.question = question;
        this.forecast = forecast;
        this.brier = brier;
        this.scoredAt = scoredAt;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getAccount() {
        return account;
    }

    public Question getQuestion() {
        return question;
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
