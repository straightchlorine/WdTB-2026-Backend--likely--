# Schemat bazy danych

Plik generują reguły projektu (panel Maven → Lifecycle → verify). Nie edytujcie go ręcznie.

Tabele: 7 · kolumny: 46 · relacje: 8 × @ManyToOne, 1 × @OneToMany, 2 × @OneToOne, 0 × @ManyToMany

| Tabela | Encja | Kolumny | Klucze obce |
|---|---|---|---|
| holding | Holding | 6 | question_id, wallet_id |
| question | Question | 11 | creator_id |
| resolution | Resolution | 5 | question_id, resolved_by_id |
| score_result | ScoreResult | 6 | account_id, question_id |
| trade | Trade | 8 | question_id, wallet_id |
| user_account | UserAccount | 5 | — |
| wallet | Wallet | 5 | account_id |

```mermaid
erDiagram
    holding {
        Long id PK
        BigDecimal payout
        Long question_id FK
        BigDecimal shares_no
        BigDecimal shares_yes
        Long wallet_id FK
    }
    question {
        Long id PK
        LocalDateTime close_at
        LocalDateTime created_at
        Long creator_id FK
        String description
        BigDecimal liquidity_b
        BigDecimal shares_no
        BigDecimal shares_yes
        QuestionStatus status
        String title
        long version
    }
    resolution {
        Long id PK
        Outcome outcome
        Long question_id FK
        LocalDateTime resolved_at
        Long resolved_by_id FK
    }
    score_result {
        Long id PK
        Long account_id FK
        BigDecimal brier
        BigDecimal forecast
        Long question_id FK
        LocalDateTime scored_at
    }
    trade {
        Long id PK
        BigDecimal amount
        LocalDateTime created_at
        BigDecimal price_after
        Long question_id FK
        BigDecimal shares
        Outcome side
        Long wallet_id FK
    }
    user_account {
        Long id PK
        LocalDateTime created_at
        String display_name
        String email
        AccountRole role
    }
    wallet {
        Long id PK
        Long account_id FK
        BigDecimal balance
        LocalDateTime created_at
        long version
    }
    question ||--o{ holding : "question_id"
    question ||--o{ score_result : "question_id"
    question ||--o{ trade : "question_id"
    question ||--o| resolution : "question_id"
    user_account ||--o{ question : "creator_id"
    user_account ||--o{ resolution : "resolved_by_id"
    user_account ||--o{ score_result : "account_id"
    user_account ||--o| wallet : "account_id"
    wallet ||--o{ holding : "wallet_id"
    wallet ||--o{ trade : "wallet_id"
```
