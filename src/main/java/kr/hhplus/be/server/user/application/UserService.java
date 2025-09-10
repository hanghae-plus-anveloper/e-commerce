package kr.hhplus.be.server.user.application;

import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import kr.hhplus.be.server.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. id = " + userId));
    }

    public User findByName(String name) {
        return userRepository.findByName(name)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .name(name)
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
