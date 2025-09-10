package kr.hhplus.be.server.user.controller;

import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<Long> getUserIdByName(String name) {
        User user = userService.findByName(name);
        return ResponseEntity.ok(user.getId());
    }
}
