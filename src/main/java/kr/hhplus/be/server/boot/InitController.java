package kr.hhplus.be.server.boot;

import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile("local")
@RestController
@RequestMapping("/init")
@RequiredArgsConstructor
public class InitController {

    private final BootDataInitializer bootDataInitializer;

    @PostMapping
    public ResponseEntity<Map<String, Object>> reset() throws Exception {
        List<CouponPolicy> policies = bootDataInitializer.runAndReturnPolicy();
        Map<String, Object> result = new HashMap<>();
        result.put("message", "System initialized to default state.");
        result.put("policyIds", policies.stream()
                .map(CouponPolicy::getId)
                .toList());
        return ResponseEntity.ok(result);
    }

}