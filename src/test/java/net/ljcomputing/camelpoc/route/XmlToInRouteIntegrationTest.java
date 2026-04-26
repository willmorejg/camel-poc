package net.ljcomputing.camelpoc.route;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
class XmlToInRouteIntegrationTest {

    private static final Path INPUT_DIR;
    private static final Pattern FILENAME_PATTERN = Pattern.compile("<filename>([^<]+)</filename>");

    static {
        try {
            INPUT_DIR = Files.createTempDirectory("xml-to-in-it-");
        } catch (final IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("camel.variables.jsonInputPath", INPUT_DIR::toString);
        registry.add("camel.main.routes-include-pattern", () -> "classpath:routes/xml-to-in.yaml");
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanInputDir() throws IOException {
        try (Stream<Path> files = Files.list(INPUT_DIR)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    void postsSoapEnvelopeAndStoresOnlyAddressesSection() throws Exception {
        final String soapPayload = """
                <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adr=\"http://ljcomputing.net/address\">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <adr:submitAddressRecordsRequest>
                      <addresses>
                        <address>
                          <name>Integration Tester</name>
                          <address1>101 Main Street</address1>
                          <city>Union</city>
                          <state>NJ</state>
                          <zip>07083</zip>
                        </address>
                      </addresses>
                    </adr:submitAddressRecordsRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;

        final MvcResult asyncResult = mockMvc.perform(post("/v1/ws/address-records")
                .contentType("text/xml")
                        .content(soapPayload))
            .andExpect(request().asyncStarted())
            .andReturn();

        final MvcResult result = mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
                .andReturn();

        final String response = result.getResponse().getContentAsString();
        final Matcher matcher = FILENAME_PATTERN.matcher(response);
        assertTrue(matcher.find(), "SOAP response should include filename");
        final String responseFileName = matcher.group(1);

        final List<Path> createdFiles;
        try (Stream<Path> files = Files.list(INPUT_DIR)) {
            createdFiles = files.filter(Files::isRegularFile).toList();
        }

        assertEquals(1, createdFiles.size(), "Exactly one XML file should be written");

        final Path writtenFile = createdFiles.get(0);
        assertEquals(responseFileName, writtenFile.getFileName().toString(), "Response filename should match written file");

        final String writtenXml = Files.readString(writtenFile);
        assertTrue(writtenXml.contains("<addresses"));
        assertTrue(writtenXml.contains("<name>Integration Tester</name>"));
        assertFalse(writtenXml.contains("Envelope"));
        assertFalse(writtenXml.contains("submitAddressRecordsRequest"));
    }
}
