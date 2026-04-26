package net.ljcomputing.camelpoc.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import net.ljcomputing.camelpoc.model.AddressRecord;
import net.ljcomputing.camelpoc.model.GeoCoordinate;

@Service
public class NominatimService {

    private static final Logger log = LoggerFactory.getLogger(NominatimService.class);
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";
    public static final long RATE_LIMIT_MS = 1100L;

    private final RestClient restClient;

    public NominatimService(final RestClient.Builder builder) {
        this.restClient = builder
            .baseUrl(NOMINATIM_BASE_URL)
            .defaultHeader("User-Agent", "camel-poc/1.0.0")
            .defaultHeader("Accept", "application/json")
            .build();
    }

    public Optional<GeoCoordinate> geocode(final AddressRecord address) {
        log.info("Geocoding: {}", address.getAddress1());
        try {
            final List<Map<String, Object>> results = restClient.get()
                .uri(u -> u.path("/search")
                    .queryParam("street", address.getAddress1())
                    .queryParam("city", address.getCity())
                    .queryParam("state", address.getState())
                    .queryParam("postalcode", address.getZip())
                    .queryParam("country", "US")
                    .queryParam("format", "json")
                    .queryParam("limit", "1")
                    .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (results != null && !results.isEmpty()) {
                final Map<String, Object> hit = results.get(0);
                final double lat = Double.parseDouble((String) hit.get("lat"));
                final double lon = Double.parseDouble((String) hit.get("lon"));
                log.info("Geocoded '{}': lat={} lon={}", address.getAddress1(), lat, lon);
                return Optional.of(new GeoCoordinate(lat, lon));
            }

            log.warn("No geocode result for: {}", address);
            return Optional.empty();
        } catch (final Exception e) {
            log.error("Geocoding failed for {}: {}", address, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
