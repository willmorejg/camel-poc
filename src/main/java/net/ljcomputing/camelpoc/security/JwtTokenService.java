package net.ljcomputing.camelpoc.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.ljcomputing.camelpoc.config.JwtProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(String username, Collection<? extends GrantedAuthority> authorities) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.expiration() * 1000L);

        List<String> roles = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(username)
            .issuer("camel-poc")
            .issueTime(now)
            .expirationTime(expiry)
            .claim("roles", roles)
            .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
            .type(JOSEObjectType.JWT)
            .build();

        SignedJWT jwt = new SignedJWT(header, claims);

        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.secret());
            jwt.sign(new MACSigner(keyBytes));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }

        return jwt.serialize();
    }

    public JWTClaimsSet validateAndGetClaims(String token) throws ParseException, JOSEException {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.secret());
        SignedJWT jwt = SignedJWT.parse(token);
        if (!jwt.verify(new MACVerifier(keyBytes))) {
            throw new JOSEException("JWT signature verification failed");
        }
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        Date expiry = claims.getExpirationTime();
        if (expiry == null || expiry.before(new Date())) {
            throw new JOSEException("JWT has expired");
        }
        return claims;
    }

    public void validateToken(String token) throws ParseException, JOSEException {
        validateAndGetClaims(token);
    }

    public long getExpiration() {
        return jwtProperties.expiration();
    }
}
