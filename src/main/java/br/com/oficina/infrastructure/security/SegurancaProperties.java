package br.com.oficina.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração de autenticação, lida de variáveis de ambiente (ver .env.example).
 *
 * @param jwtSecret    chave HMAC com ao menos 32 caracteres; se vazia, uma chave aleatória é gerada
 *                     na inicialização (tokens deixam de valer a cada restart)
 * @param jwtExpiracao validade do token emitido
 * @param adminUsuario usuário administrativo
 * @param adminSenha   senha do usuário administrativo
 */
@ConfigurationProperties("oficina.seguranca")
public record SegurancaProperties(String jwtSecret, Duration jwtExpiracao, String adminUsuario, String adminSenha) {

    public SegurancaProperties {
        if (jwtExpiracao == null) {
            jwtExpiracao = Duration.ofHours(2);
        }
    }
}
