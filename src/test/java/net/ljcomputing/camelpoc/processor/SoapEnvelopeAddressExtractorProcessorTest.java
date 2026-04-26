package net.ljcomputing.camelpoc.processor;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SoapEnvelopeAddressExtractorProcessorTest {

    private final SoapEnvelopeAddressExtractorProcessor processor = new SoapEnvelopeAddressExtractorProcessor();

    @Test
    void extractsPlainAddressesXml() throws Exception {
        final String input = """
                <addresses>
                    <address>
                        <name>Jane Doe</name>
                    </address>
                </addresses>
                """;

        final Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(input);

        processor.process(exchange);

        final String extracted = exchange.getIn().getBody(String.class);
        assertTrue(extracted.contains("<addresses>"));
        assertTrue(extracted.contains("<name>Jane Doe</name>"));
    }

    @Test
    void extractsAddressesFromSoapEnvelope() throws Exception {
        final String input = """
                <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adr=\"http://ljcomputing.net/address\">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <adr:submitAddressRecordsRequest>
                      <addresses>
                        <address>
                          <name>John Doe</name>
                        </address>
                      </addresses>
                    </adr:submitAddressRecordsRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;

        final Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(input);

        processor.process(exchange);

        final String extracted = exchange.getIn().getBody(String.class);
        assertTrue(extracted.contains("<addresses>"));
        assertTrue(extracted.contains("<name>John Doe</name>"));
        assertTrue(!extracted.contains("Envelope"));
    }

    @Test
    void throwsWhenAddressesMissing() {
        final String input = """
                <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">
                  <soapenv:Body>
                    <noAddresses/>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> processor.extractAddressesXml(input));
        assertEquals("No <addresses> element found in request payload", ex.getMessage());
    }
}