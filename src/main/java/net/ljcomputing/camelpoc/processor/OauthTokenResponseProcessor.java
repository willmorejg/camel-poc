package net.ljcomputing.camelpoc.processor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component("oauthTokenResponseProcessor")
public class OauthTokenResponseProcessor implements Processor {

    private final ObjectMapper objectMapper;

    public OauthTokenResponseProcessor(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final String token = exchange.getIn().getBody(String.class);
        final Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", token);
        response.put("token_type", "Bearer");
        response.put("expires_in", 3600);
        exchange.getIn().setBody(objectMapper.writeValueAsString(response));
    }
}
