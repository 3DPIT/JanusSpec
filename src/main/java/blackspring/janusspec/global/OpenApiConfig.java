package blackspring.janusspec.global;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI janusSpecOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JanusSpec API")
                        .description("Swagger API 버전 관리 및 변경 추적 시스템")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Blackspring")
                                .email("admin@example.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Server")
                ));
    }
}

