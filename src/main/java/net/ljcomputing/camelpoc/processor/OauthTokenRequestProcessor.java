package net.ljcomputing.camelpoc.processor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("oauthTokenRequestProcessor")
public class OauthTokenRequestProcessor implements Processor {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public OauthTokenRequestProcessor(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final String body = exchange.getIn().getBody(String.class) == null ? "" : exchange.getIn().getBody(String.class);
        final Map<String, String> params = new HashMap<>();

        if (body.trim().startsWith("{")) {
            final Map<String, Object> json = objectMapper.readValue(body, MAP_TYPE);
            putIfPresent(params, "grant_type", json.get("grant_type"));
            putIfPresent(params, "username", json.get("username"));
            putIfPresent(params, "password", json.get("password"));
        } else {
            for (String pair : body.split("&")) {
                final String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    params.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                }
            }
        }

        exchange.setProperty("grant_type", params.getOrDefault("grant_type", ""));
        exchange.setProperty("req_username", params.getOrDefault("username", ""));
        exchange.setProperty("req_password", params.getOrDefault("password", ""));
    }

    private static void putIfPresent(final Map<String, String> target, final String key, final Object value) {
        if (value != null) {
            target.put(key, value.toString());
        }
    }
}
