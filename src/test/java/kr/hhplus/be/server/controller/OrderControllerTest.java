package kr.hhplus.be.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.controller.order.OrderController;
import kr.hhplus.be.server.controller.order.OrderItemRequestDto;
import kr.hhplus.be.server.controller.order.OrderRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("정상적인 주문 요청 시 201 Created 응답")
    void createOrder_success() throws Exception {
        // given
        OrderRequestDto request = new OrderRequestDto(
            1L,
            List.of(
                new OrderItemRequestDto(101L, 2),
                new OrderItemRequestDto(102L, 1)
            ),
            5L
        );

        String requestJson = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.totalAmount").exists());
    }
}
