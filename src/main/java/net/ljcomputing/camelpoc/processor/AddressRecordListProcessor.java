package net.ljcomputing.camelpoc.processor;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.ljcomputing.camelpoc.model.AddressRecord;

@Component("addressRecordListProcessor")
public class AddressRecordListProcessor implements Processor {

    private static final TypeReference<List<AddressRecord>> ADDRESS_RECORD_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public AddressRecordListProcessor(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(final Exchange exchange) {
        final Object body = exchange.getIn().getBody();
        final List<AddressRecord> records = objectMapper.convertValue(body, ADDRESS_RECORD_LIST);
        exchange.getIn().setBody(records);
    }
}
