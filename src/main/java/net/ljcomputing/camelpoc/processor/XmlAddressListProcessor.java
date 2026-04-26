package net.ljcomputing.camelpoc.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.ljcomputing.camelpoc.model.AddressRecord;

@Component("xmlAddressListProcessor")
public class XmlAddressListProcessor implements Processor {

    private final ObjectMapper objectMapper;

    public XmlAddressListProcessor(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(final Exchange exchange) {
        final Map<String, Object> body = exchange.getIn().getBody(Map.class);
        final Object raw = body.get("address");

        final List<AddressRecord> records = new ArrayList<>();
        if (raw instanceof List) {
            for (final Object item : (List<?>) raw) {
                records.add(objectMapper.convertValue(item, AddressRecord.class));
            }
        } else if (raw instanceof Map) {
            records.add(objectMapper.convertValue(raw, AddressRecord.class));
        }

        exchange.getIn().setBody(records);
    }
}
