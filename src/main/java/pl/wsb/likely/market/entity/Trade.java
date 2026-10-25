package pl.wsb.likely.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One buy trade. Trades are only appended and never changed.
 *
 * <p>The order of trades is given by {@code id}, not {@code createdAt}. Two trades can have the
 * same time, but the id always grows.
 *
 * <p>A player's "last trade" is the one with the highest id. The caller sets the time, as in {@link
 * Question}.
 */
@Entity
@Table(name = "trade")
// The service validates the input first.
// The CHECK catches writes that skip it (data.sql, manual SQL in the console).
// amount <= shares because a share pays at most 1 point, so it cannot cost more.
@Check(
        name = "ck_trade",
        constraints =
                "shares >= 0.01 and amount > 0 and amount <= shares and price_after between 0 and"
                        + " 1")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 3)
    private Outcome side;

    @Column(name = "shares", nullable = false, precision = 19, scale = 4)
    private BigDecimal shares;

    // Cost is calculated once, at buy time.
    //
    // In the planned API the player gives an amount and pays exactly that, and
    // we round the shares down to 4 places.
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // YES price after this trade.
    // The price before is price_after of the previous trade (or 0.5 at the start),
    // so it's not stored.
    // The forecast for the Brier score comes from this column.
    @Column(name = "price_after", nullable = false, precision = 9, scale = 6)
    private BigDecimal priceAfter;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Trade() {}

    public Trade(
            Long questionId,
            Long walletId,
            Outcome side,
            BigDecimal shares,
            BigDecimal amount,
            BigDecimal priceAfter,
            LocalDateTime createdAt) {
        this.questionId = questionId;
        this.walletId = walletId;
        this.side = side;
        this.shares = shares;
        this.amount = amount;
        this.priceAfter = priceAfter;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Long getWalletId() {
        return walletId;
    }

    public Outcome getSide() {
        return side;
    }

    public BigDecimal getShares() {
        return shares;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPriceAfter() {
        return priceAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
