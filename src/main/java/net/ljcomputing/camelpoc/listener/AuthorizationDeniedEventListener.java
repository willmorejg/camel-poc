package net.ljcomputing.camelpoc.listener;

import org.slf4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationDeniedEventListener implements ApplicationListener<AuthorizationDeniedEvent<?>> {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AuthorizationDeniedEventListener.class);

    @Override
    public void onApplicationEvent(final @NonNull AuthorizationDeniedEvent<?> event) {
        // Log the event or perform other actions as needed
        logger.warn("Authorization denied: {}", event.getAuthentication().get().getName());
    }
}