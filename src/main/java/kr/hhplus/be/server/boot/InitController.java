package kr.hhplus.be.server.boot;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/init")
@RequiredArgsConstructor
public class InitController {

    private final BootDataInitializer bootDataInitializer;

    @PostMapping
    public ResponseEntity<String> reset() throws Exception {
        bootDataInitializer.run(null); // ApplicationRunner 그대로 재사용
        return ResponseEntity.ok("System initialized to default state.");
    }
}
