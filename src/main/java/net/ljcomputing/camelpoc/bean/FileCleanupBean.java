package net.ljcomputing.camelpoc.bean;

import java.nio.file.Files;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileCleanupBean {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupBean.class);

    @Value("${camel.variables.jsonInputPath:./in}")
    String jsonInputPath;

    @Value("${camel.variables.jsonErrorPath:./error}")
    String jsonErrorPath;

    public void deleteIfExists(final Exchange exchange) {
        final String fileName = exchange.getMessage().getHeader(Exchange.FILE_NAME, String.class);
        if (fileName != null) {
            final java.nio.file.Path filePath = 
                java.nio.file.Paths.get(jsonInputPath + 
                    java.nio.file.FileSystems.getDefault().getSeparator() + 
                        fileName);
            
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                    log.warn("Cleanup {}: deleted", fileName);
                } catch (final Exception e) {
                    log.warn("Cleanup {}: delete FAILED", fileName, e);
                }
            }
        }
    }

    public void moveToError(final Exchange exchange) {
        final String fileName = exchange.getMessage().getHeader(Exchange.FILE_NAME, String.class);
        if (fileName != null) {
            final java.nio.file.Path sourcePath = 
                java.nio.file.Paths.get(jsonInputPath + 
                    java.nio.file.FileSystems.getDefault().getSeparator() + 
                        fileName);
            final java.nio.file.Path targetPath = 
                java.nio.file.Paths.get(jsonErrorPath + 
                    java.nio.file.FileSystems.getDefault().getSeparator() + 
                        fileName);

            try {
                Files.move(sourcePath, targetPath);
                log.warn("Cleanup {}: moved to error", fileName);
            } catch (final Exception e) {
                log.warn("Cleanup {}: move to error FAILED", fileName, e);
            }
        }
    }
}