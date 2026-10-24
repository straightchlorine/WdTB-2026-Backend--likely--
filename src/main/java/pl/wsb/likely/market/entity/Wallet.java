package pl.wsb.likely.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A player's wallet of points, one per account.
 *
 * <p>The balance can always be rebuilt: {@code STARTING_BALANCE - sum(trade.amount) +
 * sum(holding.payout)}. {@code @Version} stops two buys from the same wallet at the same time,
 * which would both see the same balance.
 */
@Entity
@Table(name = "wallet")
@Check(name = "ck_wallet", constraints = "balance >= 0")
public class Wallet {

    // Everyone starts with the same amount, so this is a constant.
    public static final BigDecimal STARTING_BALANCE = new BigDecimal("1000.0000");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Id of the account that owns the wallet.
    // Plain id instead of the UserAccount object. There is no FK, so the database
    // does not check that the account exists.
    // Service is expected to verify it.
    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Wallet() {}

    public Wallet(Long accountId, LocalDateTime createdAt) {
        this.accountId = accountId;
        this.balance = STARTING_BALANCE;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }
}
