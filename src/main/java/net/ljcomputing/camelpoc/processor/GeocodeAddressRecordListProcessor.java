package net.ljcomputing.camelpoc.processor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.ljcomputing.camelpoc.model.GeoCoordinate;
import net.ljcomputing.camelpoc.model.GeocodedAddressRecord;
import net.ljcomputing.camelpoc.service.NominatimService;
import net.ljcomputing.camelpoc.service.OsmTileService;

@Component("geocodeAddressRecordListProcessor")
public class GeocodeAddressRecordListProcessor implements Processor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GeocodeAddressRecordListProcessor.class);

    private static final TypeReference<List<GeocodedAddressRecord>> GEOCODED_ADDRESS_RECORD_LIST = new TypeReference<>() {
    };

    private final NominatimService nominatimService;
    private final OsmTileService osmTileService;
    private final ObjectMapper objectMapper;    

    public GeocodeAddressRecordListProcessor(
        final NominatimService nominatimService, final OsmTileService osmTileService, 
        final ObjectMapper objectMapper) {
        this.nominatimService = nominatimService;
        this.osmTileService = osmTileService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(final Exchange exchange) {
        // final Object body = exchange.getIn().getBody();
        // final List<GeocodedAddressRecord> records = objectMapper.convertValue(body, GEOCODED_ADDRESS_RECORD_LIST);
        // exchange.getIn().setBody(records);
        final List<GeocodedAddressRecord> records = objectMapper.convertValue(exchange.getIn().getBody(), GEOCODED_ADDRESS_RECORD_LIST);
        final Map<String, String> tileUrls = new LinkedHashMap<>();

        for (int i = 0; i < records.size(); i++) {
            if (i > 0) {
                log.info("Sleeping {}ms between Nominatim requests...", NominatimService.RATE_LIMIT_MS);
                try {
                    Thread.sleep(NominatimService.RATE_LIMIT_MS);
                } catch (InterruptedException ex) {
                    log.error("Interrupted while sleeping between Nominatim requests", ex);
                }
            }
            final GeocodedAddressRecord address = records.get(i);
            log.info("Geocoding {}/{}: {}", i + 1, records.size(), address.getAddress1());
            final Optional<GeoCoordinate> coord = nominatimService.geocode(address);
            final String url = coord
                .map(c -> osmTileService.buildTileUrl(c.latitude(), c.longitude()))
                .orElse("");
            address.setGeocodedCoordinate(coord.orElse(null));
            address.setTileUrl(url);
            coord.ifPresent(c -> {
                final double[] pin = osmTileService.computePinOffset(c.latitude(), c.longitude());
                address.setPinOffsetX(pin[0]);
                address.setPinOffsetY(pin[1]);
            });
            tileUrls.put(address.getName(), url);
        }

        final Map<String, Object> model = new LinkedHashMap<>();
        model.put("records", records);
        model.put("tileUrls", tileUrls);
        exchange.getIn().setBody(records);
    }
}
