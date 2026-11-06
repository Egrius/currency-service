package by.Egrius.currency_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Prices API")
                        .description("Тестовое задание - сервис, который по расписанию забирает среднюю цену торговой пары " +
                                "у биржи Binance, сохраняет её в БД и отдаёт через REST")
                        .contact(new Contact().name("Egrius")));
    }
}
