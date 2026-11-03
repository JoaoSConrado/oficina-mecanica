package br.com.oficina.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final SegurancaProperties props;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, SegurancaProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    public TokenEmitido emitir(Authentication autenticacao) {
        Instant agora = Instant.now(clock);
        Instant expiraEm = agora.plus(props.jwtExpiracao());
        List<String> papeis = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replaceFirst("^ROLE_", ""))
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("oficina-mecanica")
                .subject(autenticacao.getName())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim(SecurityConfig.CLAIM_PAPEIS, papeis)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }
}
