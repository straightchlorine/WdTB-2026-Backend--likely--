package pl.wsb.likely.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;

/**
 * A player's position in one question. It joins wallet and question (N:M).
 *
 * <p>An entity is used instead of {@code @ManyToMany}, since the pair has its own data: the shares
 * held on each side and the payout.
 *
 * <p>A single wallet and question pair is one row.
 *
 * <p>The shares (YES/NO) equal the sum of the player's {@code Trade}s in this question. Kept here
 * so settlement does not have to sum the whole history.
 *
 * <p>Shares (YES/NO) would grow with each {@code Trade} row added.
 */
@Entity
@Table(
        name = "holding",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_holding_wallet_question",
                        columnNames = {"wallet_id", "question_id"}))
@Check(name = "ck_holding", constraints = "shares_yes >= 0 and shares_no >= 0 and payout >= 0")
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "shares_yes", nullable = false, precision = 19, scale = 4)
    private BigDecimal sharesYes;

    @Column(name = "shares_no", nullable = false, precision = 19, scale = 4)
    private BigDecimal sharesNo;

    // null until the question is settled.
    // Then the amount paid to the wallet.
    // if RESOLVED: 1 point per share of the winning side (0 for the losing side).
    // if CANCELLED: refund of the player's sum(trade.amount) in this question.
    @Column(name = "payout", precision = 19, scale = 4)
    private BigDecimal payout;

    protected Holding() {}

    public Holding(Long walletId, Long questionId) {
        this.walletId = walletId;
        this.questionId = questionId;
        this.sharesYes = BigDecimal.ZERO;
        this.sharesNo = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public BigDecimal getSharesYes() {
        return sharesYes;
    }

    public BigDecimal getSharesNo() {
        return sharesNo;
    }

    public BigDecimal getPayout() {
        return payout;
    }
}
