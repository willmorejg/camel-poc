package net.ljcomputing.camelpoc.processor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.ljcomputing.camelpoc.model.AddressRecord;
import net.ljcomputing.camelpoc.model.GeoCoordinate;
import net.ljcomputing.camelpoc.service.NominatimService;

@Component("nominatimGeocoderProcessor")
public class NominatimGeocoderProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocoderProcessor.class);
    private static final TypeReference<List<AddressRecord>> ADDRESS_LIST = new TypeReference<>() {};

    private final NominatimService nominatimService;
    private final ObjectMapper objectMapper;

    public NominatimGeocoderProcessor(final NominatimService nominatimService, final ObjectMapper objectMapper) {
        this.nominatimService = nominatimService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final List<AddressRecord> addresses = objectMapper.convertValue(exchange.getIn().getBody(), ADDRESS_LIST);
        final List<Map<String, Object>> features = new ArrayList<>();

        for (int i = 0; i < addresses.size(); i++) {
            if (i > 0) {
                log.info("Sleeping {}ms between Nominatim requests...", NominatimService.RATE_LIMIT_MS);
                Thread.sleep(NominatimService.RATE_LIMIT_MS);
            }
            final AddressRecord address = addresses.get(i);
            log.info("Processing address {}/{}: {}", i + 1, addresses.size(), address.getAddress1());
            features.add(buildFeature(address));
        }

        final Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);

        exchange.getIn().setBody(featureCollection);
    }

    private Map<String, Object> buildFeature(final AddressRecord address) {
        final Optional<GeoCoordinate> coord = nominatimService.geocode(address);

        final Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "Point");
        geometry.put("coordinates", coord.map(GeoCoordinate::toGeoJsonCoordinates).orElse(null));

        final Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("geometry", geometry);
        feature.put("properties", buildProperties(address));
        return feature;
    }

    private Map<String, Object> buildProperties(final AddressRecord address) {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("name", address.getName());
        props.put("address1", address.getAddress1());
        props.put("address2", address.getAddress2());
        props.put("city", address.getCity());
        props.put("state", address.getState());
        props.put("zip", address.getZip());
        props.put("zip4", address.getZip4());
        return props;
    }
}
