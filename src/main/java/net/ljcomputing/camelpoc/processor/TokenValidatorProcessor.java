package net.ljcomputing.camelpoc.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import net.ljcomputing.camelpoc.security.JwtTokenService;

/**
 * Validates the Bearer token from the Authorization header.
 * Sets exchange property "tokenValid" to true/false and writes an error body
 * when invalid so the route can short-circuit with a 401.
 */
@Component("tokenValidatorProcessor")
public class TokenValidatorProcessor implements Processor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TokenValidatorProcessor.class);

    private final JwtTokenService jwtTokenService;

    public TokenValidatorProcessor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public void process(Exchange exchange) {
        String authHeader = exchange.getMessage().getHeader("Authorization", String.class);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            reject(exchange, "Missing or malformed Authorization header");
            return;
        }
        String token = authHeader.substring(7).trim();
        log.debug("Validating token: {}", token);
        
        try {
            jwtTokenService.validateToken(token);
            exchange.setProperty("tokenValid", true);
        } catch (Exception e) {
            reject(exchange, "Invalid or expired token: " + e.getMessage());
        }
    }

    private void reject(Exchange exchange, String reason) {
        exchange.setProperty("tokenValid", false);
        exchange.setProperty("tokenError", reason);
    }
}
