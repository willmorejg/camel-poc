package net.ljcomputing.camelpoc.filter;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        // if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n--- Incoming Request ---");
            sb.append("\n  Method : ").append(request.getMethod());
            sb.append("\n  URI    : ").append(request.getRequestURI());
            String qs = request.getQueryString();
            if (qs != null) {
                sb.append("?").append(qs);
            }
            sb.append("\n  Headers:");
            Collections.list(request.getHeaderNames()).forEach(name ->
                Collections.list(request.getHeaders(name)).forEach(value ->
                    sb.append("\n    ").append(name).append(": ").append(value)));
            sb.append("\n  Parameters:");
            request.getParameterMap().forEach((name, values) ->
                sb.append("\n    ").append(name).append(": ").append(String.join(", ", values)));
            sb.append("\n------------------------");
            log.info(sb.toString());
        // }
        filterChain.doFilter(request, response);
    }
}
