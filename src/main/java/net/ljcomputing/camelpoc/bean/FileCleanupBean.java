package net.ljcomputing.camelpoc.bean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileCleanupBean {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupBean.class);

    public void deleteIfExists(final Exchange exchange) {
        final String absolutePath = exchange.getMessage().getHeader("CamelFileAbsolutePath", String.class);
        if (absolutePath != null) {
            final Path filePath = Paths.get(absolutePath);
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                    log.warn("Cleanup {}: deleted", filePath);
                } catch (final Exception e) {
                    log.warn("Cleanup {}: delete FAILED", filePath, e);
                }
            }
        }
    }

    public void moveToError(final Exchange exchange) {
        final String absolutePath = exchange.getMessage().getHeader("CamelFileAbsolutePath", String.class);
        if (absolutePath != null) {
            final Path source = Paths.get(absolutePath);
            final Path errorDir = source.getParent().resolveSibling("error");
            final Path target = errorDir.resolve(UUID.randomUUID() + "-" + source.getFileName());

            try {
                Files.createDirectories(errorDir);
                Files.move(source, target);
                log.warn("Cleanup {}: moved to error as {}", source.getFileName(), target);
            } catch (final Exception e) {
                log.warn("Cleanup {}: move to error FAILED", source.getFileName(), e);
            }
        }
    }
}
