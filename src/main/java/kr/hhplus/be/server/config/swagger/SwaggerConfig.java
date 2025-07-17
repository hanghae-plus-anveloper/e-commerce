package kr.hhplus.be.server.config.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("HHPLUS User API")
            .version("v1.0.0")
            .description("잔액 조회 및 충전 관련 API 문서입니다.")
            .contact(new Contact()
                .name("An Seongjin")
                .email("an.seongjin@example.com")));
  }
}
