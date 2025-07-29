package kr.hhplus.be.server.user.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.coupon.domain.Coupon;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "`user`")
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Balance balance;

    @OneToMany(mappedBy = "user")
    private List<Coupon> coupons = new ArrayList<>();

    @Builder
    public User(String name) {
        this.name = (name == null || name.isBlank()) ? generateDefaultName() : name;
    }

    private String generateDefaultName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "User_" + LocalDateTime.now().format(formatter);
    }
}
