package kr.hhplus.be.server.domain.user;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "BALANCE_HISTORY")
public class BalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "balance_id", nullable = false)
    @JsonBackReference
    private Balance balance;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private int remainingBalance;

    @Column(nullable = false)
    private BalanceChangeType type;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    protected BalanceHistory() {}

    public BalanceHistory(Balance balance, int amount, int remainingBalance, BalanceChangeType type) {
        this.balance = balance;
        this.amount = amount;
        this.remainingBalance = remainingBalance;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
}
