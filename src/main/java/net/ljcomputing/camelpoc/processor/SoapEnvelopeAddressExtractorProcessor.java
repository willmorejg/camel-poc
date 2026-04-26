package net.ljcomputing.camelpoc.processor;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Component("soapEnvelopeAddressExtractorProcessor")
public class SoapEnvelopeAddressExtractorProcessor implements Processor {

    @Override
    public void process(final Exchange exchange) throws Exception {
        final String xml = exchange.getIn().getBody(String.class);
        exchange.getIn().setBody(extractAddressesXml(xml));
    }

    String extractAddressesXml(final String xml)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException,
            TransformerException {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("Request body is empty");
        }

        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        final DocumentBuilder builder = dbf.newDocumentBuilder();
        final Document document = builder.parse(new InputSource(new StringReader(xml)));

        final XPath xPath = XPathFactory.newInstance().newXPath();

        Node addressesNode = (Node) xPath.evaluate("/*[local-name()='addresses']", document, XPathConstants.NODE);
        if (addressesNode == null) {
            addressesNode = (Node) xPath.evaluate(
                    "/*[local-name()='Envelope']/*[local-name()='Body']//*[local-name()='addresses'][1]",
                    document,
                    XPathConstants.NODE);
        }

        if (addressesNode == null) {
            throw new IllegalArgumentException("No <addresses> element found in request payload");
        }

        return toXml(addressesNode);
    }

    private String toXml(final Node node) throws TransformerException {
        final TransformerFactory tf = TransformerFactory.newInstance();
        tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        final Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        final StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }
}