package kr.hhplus.be.server.balance.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class BalanceRepositoryTest {

    @Autowired
    BalanceRepository balanceRepository;

    @Test
    void contextLoads() {
        assertThat(balanceRepository).isNotNull();
    }
}
