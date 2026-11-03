package br.com.oficina.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class AppConfig {

    public static final String ESQUEMA_SEGURANCA = "bearerAuth";

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Oficina Mecânica - API")
                        .version("1.0.0")
                        .description("""
                                MVP do Sistema Integrado de Atendimento e Execução de Serviços.

                                1. Obtenha um token em **POST /api/auth/login**.
                                2. Clique em **Authorize** e cole o token.
                                3. As rotas **/api/public/** não exigem token: o cliente se identifica pelo CPF/CNPJ.
                                """))
                // URL relativa: o Swagger chama a API no mesmo endereço em que foi aberto
                // (localhost, Codespaces ou atrás de proxy)
                .servers(List.of(new Server().url("/")))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURANCA,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURANCA));
    }
}
