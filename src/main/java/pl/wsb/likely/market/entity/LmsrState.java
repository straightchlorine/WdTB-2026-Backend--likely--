package pl.wsb.likely.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * Market state of a single question: the liquidity {@code liquidityB} and the total shares sold on
 * each side.
 *
 * <p>The price is not stored - it follows from these three numbers. The columns live in the {@code
 * question} table.
 *
 * <p>{@code @Embeddable} is used, since:
 *
 * <ul>
 *   <li>All three numbers describe the market for a single question; they make sense only together.
 *   <li>Market state doesn't exist without its question, so a separate table would only add a
 *       foreign key and a join on every read, with nothing to gain. Embedded, the columns stay in
 *       {@code question} and are grouped in this class.
 *   <li>The market logic in later labs gets a single object instead of three numbers, so it is
 *       easier to read.
 * </ul>
 */
@Embeddable
public class LmsrState {

    // Never changes after creation. A different b during play would change past prices.
    @Column(name = "liquidity_b", nullable = false, precision = 19, scale = 4)
    private BigDecimal liquidityB;

    @Column(name = "shares_yes", nullable = false, precision = 19, scale = 4)
    private BigDecimal sharesYes;

    @Column(name = "shares_no", nullable = false, precision = 19, scale = 4)
    private BigDecimal sharesNo;

    protected LmsrState() {}

    /** A fresh market: nobody has bought anything yet. */
    public LmsrState(BigDecimal liquidityB) {
        this.liquidityB = liquidityB;
        this.sharesYes = BigDecimal.ZERO;
        this.sharesNo = BigDecimal.ZERO;
    }

    public BigDecimal getLiquidityB() {
        return liquidityB;
    }

    public BigDecimal getSharesYes() {
        return sharesYes;
    }

    public BigDecimal getSharesNo() {
        return sharesNo;
    }
}
