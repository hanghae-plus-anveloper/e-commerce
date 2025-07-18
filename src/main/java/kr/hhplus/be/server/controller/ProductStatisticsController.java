package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.api.ProductStatisticsApi;
import kr.hhplus.be.server.dto.TopProductResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductStatisticsController implements ProductStatisticsApi {

    @Override
    public ResponseEntity<List<TopProductResponseDto>> getTopProducts() {
        List<TopProductResponseDto> mockTopProducts = List.of(
            new TopProductResponseDto(1L, "고급 세제", 3900, 45, 320),
            new TopProductResponseDto(2L, "프리미엄 키친타올", 2900, 78, 280),
            new TopProductResponseDto(3L, "무향 물티슈", 1500, 120, 260),
            new TopProductResponseDto(4L, "종이컵 100개입", 1900, 99, 240),
            new TopProductResponseDto(5L, "쓰레기봉투 100L", 1200, 150, 210)
        );

        return ResponseEntity.ok(mockTopProducts);
    }
}
