package kr.hhplus.be.server.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("이름이 null이거나 공백이면 자동으로 기본 이름이 설정된다")
    void userNameFallback() {
        User user1 = User.builder().name(null).build();
        User user2 = User.builder().name("").build();
        User user3 = User.builder().name("   ").build();

        assertThat(user1.getName()).startsWith("User_");
        assertThat(user2.getName()).startsWith("User_");
        assertThat(user3.getName()).startsWith("User_");
    }

    @Test
    @DisplayName("이름이 유효하면 그대로 사용된다")
    void userNameValid() {
        User user = User.builder().name("성진").build();
        assertThat(user.getName()).isEqualTo("성진");
    }
}
