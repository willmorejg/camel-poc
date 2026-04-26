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

import net.ljcomputing.camelpoc.dao.DuckDbDAO;
import net.ljcomputing.camelpoc.model.GeoCoordinate;
import net.ljcomputing.camelpoc.model.GeocodedAddressRecord;
import net.ljcomputing.camelpoc.service.NominatimService;
import net.ljcomputing.camelpoc.service.OsmTileService;

@Component("geocodeAddressRecordListProcessor")
public class GeocodeAddressRecordListProcessor implements Processor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(GeocodeAddressRecordListProcessor.class);

    private static final TypeReference<List<GeocodedAddressRecord>> GEOCODED_ADDRESS_RECORD_LIST = new TypeReference<>() {
    };

    private final NominatimService nominatimService;
    private final OsmTileService osmTileService;
    private final DuckDbDAO duckDbDAO;
    private final ObjectMapper objectMapper;

    public GeocodeAddressRecordListProcessor(
            final NominatimService nominatimService, final OsmTileService osmTileService,
            final DuckDbDAO duckDbDAO, final ObjectMapper objectMapper) {
        this.nominatimService = nominatimService;
        this.osmTileService = osmTileService;
        this.duckDbDAO = duckDbDAO;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(final Exchange exchange) {
        // final Object body = exchange.getIn().getBody();
        // final List<GeocodedAddressRecord> records = objectMapper.convertValue(body,
        // GEOCODED_ADDRESS_RECORD_LIST);
        // exchange.getIn().setBody(records);
        final List<GeocodedAddressRecord> records = objectMapper.convertValue(exchange.getIn().getBody(),
                GEOCODED_ADDRESS_RECORD_LIST);
        final Map<String, String> tileUrls = new LinkedHashMap<>();

        for (int i = 0; i < records.size(); i++) {
            final GeocodedAddressRecord tempAddress = records.get(i);
            log.info("Geocoding {}/{}: {}", i + 1, records.size(), tempAddress.getAddress1());

            final GeocodedAddressRecord dbAddress = duckDbDAO
                    .getGeocodedAddressRecordByAddress1CityStateZip(
                        tempAddress.getAddress1(), tempAddress.getCity(), tempAddress.getState(), tempAddress.getZip());

            if (dbAddress != null && dbAddress.getTileUrl() != null) {
                log.info("Found geocoded record in database for {}: {}, {}", tempAddress.getName(),
                        tempAddress.getAddress1(), dbAddress.getGeocodedCoordinate());
                tempAddress.setGeocodedCoordinate(dbAddress.getGeocodedCoordinate());
                tempAddress.setTileUrl(dbAddress.getTileUrl());
            } else {
                log.info("No geocoded record found in database for {}, inserting placeholder", tempAddress.getName());

                if (i > 0) {
                    log.info("Sleeping {}ms between Nominatim requests...", NominatimService.RATE_LIMIT_MS);
                    try {
                        Thread.sleep(NominatimService.RATE_LIMIT_MS);
                    } catch (InterruptedException ex) {
                        log.error("Interrupted while sleeping between Nominatim requests", ex);
                    }
                }

                final Optional<GeoCoordinate> coord = nominatimService.geocode(tempAddress);
                final String url = coord
                        .map(c -> osmTileService.buildTileUrl(c.latitude(), c.longitude()))
                        .orElse("");
                tempAddress.setGeocodedCoordinate(coord.orElse(null));
                tempAddress.setTileUrl(url);
                duckDbDAO.insertGeocodedAddress(tempAddress);
            }

            Optional.ofNullable(tempAddress.getGeocodedCoordinate()).ifPresent(c -> {
                final double[] pin = osmTileService.computePinOffset(c.latitude(), c.longitude());
                tempAddress.setPinOffsetX(pin[0]);
                tempAddress.setPinOffsetY(pin[1]);
            });

            tileUrls.put(tempAddress.getName(), tempAddress.getTileUrl());
        }

        exchange.getIn().setBody(records);
    }
}
