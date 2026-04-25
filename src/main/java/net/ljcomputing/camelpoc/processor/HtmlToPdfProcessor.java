package net.ljcomputing.camelpoc.processor;

import java.io.ByteArrayOutputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;

@Component("htmlToPdfProcessor")
public class HtmlToPdfProcessor implements Processor {

    @Override
    public void process(final Exchange exchange) throws Exception {
        final String html = exchange.getIn().getBody(String.class);
        final byte[] pdfBytes = convertHtmlToPdf(html);
        exchange.getIn().setBody(pdfBytes);
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
