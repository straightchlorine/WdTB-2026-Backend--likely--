package pl.wsb.likely.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import pl.wsb.likely.account.domain.UserAccount;

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

    // The account that owns the wallet (one wallet <-> one account).
    // The FK is here; unique, so an account cannot open a second wallet.
    // UserAccount has no field pointing back, so the account class stays unchanged.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", unique = true)
    private UserAccount account;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Wallet() {}

    public Wallet(UserAccount account, LocalDateTime createdAt) {
        this.account = account;
        this.balance = STARTING_BALANCE;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getAccount() {
        return account;
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
