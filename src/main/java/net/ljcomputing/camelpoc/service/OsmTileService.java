package net.ljcomputing.camelpoc.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.ljcomputing.camelpoc.model.GeoCoordinate;

@Service("osmTileService")
public class OsmTileService {

    private static final Logger log = LoggerFactory.getLogger(OsmTileService.class);
    private static final int ZOOM = 15;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Value("${camel.variables.geoJsonOutputPath:/data/geo/out}")
    private String geoJsonOutputPath;

    private final ObjectMapper objectMapper;

    public OsmTileService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String getTileUrl(final String baseFileName, final String addressName) {
        return findCoordinate(baseFileName, addressName)
            .map(c -> buildTileUrl(c.latitude(), c.longitude()))
            .orElse("");
    }

    private Optional<GeoCoordinate> findCoordinate(final String baseFileName, final String addressName) {
        final Path geoFile = Paths.get(geoJsonOutputPath, baseFileName + ".geojson");
        if (!Files.exists(geoFile)) {
            log.warn("GeoJSON file not found: {}", geoFile);
            return Optional.empty();
        }

        try {
            final Map<String, Object> featureCollection = objectMapper.readValue(geoFile.toFile(), MAP_TYPE);
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> features = (List<Map<String, Object>>) featureCollection.get("features");
            if (features == null) {
                return Optional.empty();
            }

            for (final Map<String, Object> feature : features) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
                if (properties == null || !addressName.equals(properties.get("name"))) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                final Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
                if (geometry == null) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                final List<Double> coords = (List<Double>) geometry.get("coordinates");
                if (coords != null && coords.size() >= 2) {
                    // GeoJSON coordinates are [longitude, latitude]
                    return Optional.of(new GeoCoordinate(coords.get(1), coords.get(0)));
                }
            }
        } catch (final IOException e) {
            log.error("Failed to read GeoJSON file {}: {}", geoFile, e.getMessage(), e);
        }

        return Optional.empty();
    }

    public String buildTileUrl(final double lat, final double lon) {
        final int n = 1 << ZOOM;
        final int tileX = (int) Math.floor((lon + 180.0) / 360.0 * n);
        final double latRad = Math.toRadians(lat);
        final int tileY = (int) Math.floor(
            (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n);
        return "https://tile.openstreetmap.org/" + ZOOM + "/" + tileX + "/" + tileY + ".png";
    }

    /** Returns [offsetX%, offsetY%] (0–100) of lat/lon within its tile. */
    public double[] computePinOffset(final double lat, final double lon) {
        final int n = 1 << ZOOM;
        final double latRad = Math.toRadians(lat);
        final double globalX = (lon + 180.0) / 360.0 * n * 256.0;
        final double globalY = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n * 256.0;
        final int tileX = (int) Math.floor(globalX / 256.0);
        final int tileY = (int) Math.floor(globalY / 256.0);
        final double offsetX = (globalX - tileX * 256.0) / 256.0 * 100.0;
        final double offsetY = (globalY - tileY * 256.0) / 256.0 * 100.0;
        return new double[]{ offsetX, offsetY };
    }
}
