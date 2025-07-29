package kr.hhplus.be.server.balance.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "balance_history")
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

    protected BalanceHistory() {
    }

    public BalanceHistory(Balance balance, int amount, int remainingBalance, BalanceChangeType type) {
        this.balance = balance;
        this.amount = amount;
        this.remainingBalance = remainingBalance;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public static BalanceHistory charge(Balance balance, int amount, int remainingBalance) {
        return new BalanceHistory(balance, amount, remainingBalance, BalanceChangeType.CHARGE);
    }

    public static BalanceHistory use(Balance balance, int amount, int remainingBalance) {
        return new BalanceHistory(balance, -amount, remainingBalance, BalanceChangeType.USE);
    }
}
