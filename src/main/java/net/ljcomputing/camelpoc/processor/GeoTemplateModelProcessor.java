package net.ljcomputing.camelpoc.processor;

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
import net.ljcomputing.camelpoc.service.OsmTileService;

@Component("geoTemplateModelProcessor")
public class GeoTemplateModelProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(GeoTemplateModelProcessor.class);
    private static final TypeReference<List<AddressRecord>> ADDRESS_LIST = new TypeReference<>() {};

    private final NominatimService nominatimService;
    private final OsmTileService osmTileService;
    private final ObjectMapper objectMapper;

    public GeoTemplateModelProcessor(
            final NominatimService nominatimService,
            final OsmTileService osmTileService,
            final ObjectMapper objectMapper) {
        this.nominatimService = nominatimService;
        this.osmTileService = osmTileService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final List<AddressRecord> records = objectMapper.convertValue(exchange.getIn().getBody(), ADDRESS_LIST);
        final Map<String, String> tileUrls = new LinkedHashMap<>();

        for (int i = 0; i < records.size(); i++) {
            if (i > 0) {
                log.info("Sleeping {}ms between Nominatim requests...", NominatimService.RATE_LIMIT_MS);
                Thread.sleep(NominatimService.RATE_LIMIT_MS);
            }
            final AddressRecord address = records.get(i);
            log.info("Geocoding {}/{}: {}", i + 1, records.size(), address.getAddress1());
            final Optional<GeoCoordinate> coord = nominatimService.geocode(address);
            final String url = coord
                .map(c -> osmTileService.buildTileUrl(c.latitude(), c.longitude()))
                .orElse("");
            tileUrls.put(address.getName(), url);
        }

        final Map<String, Object> model = new LinkedHashMap<>();
        model.put("records", records);
        model.put("tileUrls", tileUrls);
        exchange.getIn().setBody(model);
    }
}
