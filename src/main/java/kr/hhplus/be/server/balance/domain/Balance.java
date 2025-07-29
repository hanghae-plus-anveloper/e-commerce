package kr.hhplus.be.server.balance.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.hhplus.be.server.user.domain.User;
import lombok.Getter;

@Getter
@Entity
@Table(name = "BALANCE")
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private int balance;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "balance", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<BalanceHistory> histories = new ArrayList<>();

    protected Balance() {
    }

    public Balance(User user, int balance) {
        this.user = user;
        this.balance = balance;
        this.updatedAt = LocalDateTime.now();
    }

    public void charge(int amount) {
        this.balance += amount;
        this.updatedAt = LocalDateTime.now();
        this.histories.add(BalanceHistory.charge(this, amount, this.balance));
    }

    public void use(int amount) {
        if (amount > balance)
            throw new IllegalArgumentException("잔액 부족");
        this.balance -= amount;
        this.updatedAt = LocalDateTime.now();
        this.histories.add(BalanceHistory.use(this, amount, this.balance));
    }
}
