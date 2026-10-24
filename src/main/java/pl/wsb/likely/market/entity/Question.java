package pl.wsb.likely.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import pl.wsb.likely.account.domain.UserAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // Relation to the user_account.
    // It is planned that it would become:
    //  @Column(name = "creator_id", nullable = false)
    //  private Long creatorId;
    // Currently this field keeps the object itself, lazily loaded from the db.
    // Another plus would be that modules would refer to one another by id only.
    // Since LAB 02 requires a relation with user_account - it's kept as an exception.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private UserAccount creator;

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

    // Trade.question owns the relation (the foreign key is there), so changes here
    // would be ignored by the ORM.
    @OneToMany(mappedBy = "question")
    private List<Trade> trades = new ArrayList<>();

    protected Question() {}

    public Question(
            UserAccount creator,
            String title,
            String description,
            LocalDateTime closeAt,
            BigDecimal liquidityB,
            LocalDateTime createdAt) {
        this.creator = creator;
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

    public UserAccount getCreator() {
        return creator;
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

    public List<Trade> getTrades() {
        return Collections.unmodifiableList(trades);
    }
}
