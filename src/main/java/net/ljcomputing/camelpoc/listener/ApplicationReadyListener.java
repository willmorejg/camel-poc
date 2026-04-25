package net.ljcomputing.camelpoc.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@Order(0)
class ApplicationReadyListener 
    implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationReadyListener.class);

  @Override
  public void onApplicationEvent(final @NonNull ApplicationReadyEvent event) {
    logger.warn("Application is ready!!");
  }

}