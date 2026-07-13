package co.com.nequi.franchise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI franchiseApi() {
        return new OpenAPI().info(new Info()
                .title("Franchise API")
                .version("1.0.0")
                .description("""
                        Reactive API to manage a network of franchises, their branches and the \
                        products of each branch.

                        Built with Spring WebFlux (RouterFunctions, no controllers), hexagonal \
                        architecture and R2DBC over PostgreSQL. Every endpoint is non-blocking \
                        end to end.""")
                .contact(new Contact().name("Bruno Ampuero")));
    }
}
