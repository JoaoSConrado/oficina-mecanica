package br.com.oficina.api;

import br.com.oficina.infrastructure.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
@SecurityRequirements
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    @Operation(summary = "Gera um token JWT para as APIs administrativas")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usuario(), request.senha()));
        var token = tokenService.emitir(autenticacao);
        return new TokenResponse(token.token(), "Bearer", token.expiraEm());
    }

    public record LoginRequest(@NotBlank String usuario, @NotBlank String senha) {
    }

    public record TokenResponse(String token, String tipo, Instant expiraEm) {
    }
}
