package net.ljcomputing.camelpoc.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/oauth")
public class TokenController {

    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;

    public TokenController(JwtTokenService jwtTokenService, AuthenticationManager authenticationManager) {
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/token")
    public TokenResponse token(@RequestBody TokenRequest tokenRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                tokenRequest.username,
                tokenRequest.password
            )
        );
        String accessToken = jwtTokenService.generateToken(
            authentication.getName(),
            authentication.getAuthorities()
        );
        return new TokenResponse(accessToken, "Bearer", jwtTokenService.getExpiration());
    }

    public static class TokenRequest {
        public String username;
        public String password;
    }

    public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
    ) {}
}
