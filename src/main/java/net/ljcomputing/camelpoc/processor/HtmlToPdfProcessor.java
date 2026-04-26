package net.ljcomputing.camelpoc.processor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;

@Component("htmlToPdfProcessor")
public class HtmlToPdfProcessor implements Processor {

    private static final Pattern IMG_SRC_PATTERN =
            Pattern.compile("(<img[^>]+src=[\"'])(https?://[^\"']+)([\"'])");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void process(final Exchange exchange) throws Exception {
        final String html = exchange.getIn().getBody(String.class);
        final byte[] pdfBytes = convertHtmlToPdf(embedExternalImages(html));
        exchange.getIn().setBody(pdfBytes);
    }

    private String embedExternalImages(final String html) {
        final Matcher matcher = IMG_SRC_PATTERN.matcher(html);
        final StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            final String prefix = matcher.group(1);
            final String url = matcher.group(2);
            final String quote = matcher.group(3);
            String replacement;
            try {
                final HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                final HttpResponse<InputStream> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                final byte[] imageBytes = response.body().readAllBytes();
                final String contentType = response.headers()
                        .firstValue("Content-Type")
                        .orElse("image/png")
                        .split(";")[0].trim();
                final String base64 = Base64.getEncoder().encodeToString(imageBytes);
                replacement = prefix + "data:" + contentType + ";base64," + base64 + quote;
            } catch (final Exception e) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private byte[] convertHtmlToPdf(final String html) throws DocumentException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (final Exception e) {
            throw new DocumentException(e);
        }
    }
}
