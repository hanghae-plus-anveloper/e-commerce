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
            .title("E-Commerce API")
            .version("v1.0.0")
            .contact(new Contact()
                .name("Anveloper")
                .email("anveloper.dev@gmail.com")));
  }
}
