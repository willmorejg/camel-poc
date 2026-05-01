package net.ljcomputing.camelpoc.bean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileCleanupBean {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupBean.class);
    private static final String S3_KEY_HEADER = "CamelAwsS3Key";

    private final ProducerTemplate producerTemplate;
    private final String s3BucketName;
    private final String s3ErrorPrefix;

    public FileCleanupBean(
            final ProducerTemplate producerTemplate,
            @Value("${camel.variables.s3BucketName:}") final String s3BucketName,
            @Value("${camel.variables.jsonToProcessErrorPath:json-to-process-error}") final String s3ErrorPrefix) {
        this.producerTemplate = producerTemplate;
        this.s3BucketName = s3BucketName;
        this.s3ErrorPrefix = s3ErrorPrefix;
    }

    public void deleteIfExists(final Exchange exchange) {
        final String absolutePath = exchange.getMessage().getHeader("CamelFileAbsolutePath", String.class);
        if (absolutePath != null) {
            final Path filePath = Paths.get(absolutePath);
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                    log.warn("Cleanup {}: deleted", filePath);
                } catch (final IOException e) {
                    log.warn("Cleanup {}: delete FAILED", filePath, e);
                }
            }
            return;
        }
        // S3 consumer with deleteAfterRead=true already removed the object
        final String s3Key = exchange.getMessage().getHeader(S3_KEY_HEADER, String.class);
        if (s3Key != null) {
            log.info("S3 object already deleted by consumer: {}", s3Key);
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
            } catch (final IOException e) {
                log.warn("Cleanup {}: move to error FAILED", source.getFileName(), e);
            }
            return;
        }

        // S3: source object already deleted by consumer; write original body to error prefix
        final String s3Key = exchange.getMessage().getHeader(S3_KEY_HEADER, String.class);
        if (s3Key != null && s3BucketName != null && !s3BucketName.isEmpty()) {
            final String fileName = s3Key.contains("/") ? s3Key.substring(s3Key.lastIndexOf('/') + 1) : s3Key;
            final String errorKey = s3ErrorPrefix + "/" + UUID.randomUUID() + "-" + fileName;
            final Object body = exchange.getProperty("originalBody", Object.class);
            if (body != null) {
                try {
                    producerTemplate.sendBodyAndHeaders(
                            "aws2-s3://" + s3BucketName,
                            body,
                            Map.of(S3_KEY_HEADER, errorKey));
                    log.warn("S3 cleanup: written error copy to {}", errorKey);
                } catch (final Exception e) {
                    log.warn("S3 cleanup: write to error prefix FAILED for key {}", errorKey, e);
                }
            } else {
                log.warn("S3 cleanup: no originalBody property available for key {}", s3Key);
            }
        }
    }
}
