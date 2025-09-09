package kr.hhplus.be.server.analytics.controller;

import kr.hhplus.be.server.analytics.application.TopProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TopProductController implements TopProductApi {

    private final TopProductService service;

    @Override
    public ResponseEntity<List<TopProductResponseDto>> getTopProducts() {
        List<TopProductResponseDto> result = service.top5InLast3Days()
                .stream()
                .map(TopProductResponseDto::fromView)
                .toList();
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<List<TopProductResponseDto>> getTopProductsRealtime() {
        var result = service.getTop5InLast3DaysFromRedisWithoutCache()
                .stream()
                .map(TopProductResponseDto::fromView)
                .toList();
        return ResponseEntity.ok(result);
    }
}
