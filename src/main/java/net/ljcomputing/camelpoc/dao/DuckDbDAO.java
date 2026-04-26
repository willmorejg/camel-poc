package net.ljcomputing.camelpoc.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import net.ljcomputing.camelpoc.model.GeoCoordinate;
import net.ljcomputing.camelpoc.model.GeocodedAddressRecord;

@Service
public class DuckDbDAO {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DuckDbDAO.class);

    private static final String CREATE_TABLE_SQL = 
        new StringBuilder().append("CREATE TABLE IF NOT EXISTS geocoded ")
        .append("(id INTEGER PRIMARY KEY DEFAULT nextval('geocoded_seq_id')") // Auto-incrementing ID using sequence
        .append(", name VARCHAR")
        .append(", address1 VARCHAR")
        .append(", address2 VARCHAR")
        .append(", city VARCHAR")
        .append(", state VARCHAR")
        .append(", zip VARCHAR")
        .append(", zip4 VARCHAR")
        .append(", country VARCHAR DEFAULT 'US'")
        .append(", latitude DOUBLE")
        .append(", longitude DOUBLE")
        .append(", tileurl VARCHAR")
        .append(", geocoded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
        .append(")")
        .toString();

    private final JdbcTemplate jdbcTemplate;

    public DuckDbDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS geocoded_seq_id START 1");
        jdbcTemplate.execute(CREATE_TABLE_SQL);
        log.warn("Created DuckDbDao ...");
    }

    public GeocodedAddressRecord getGeocodedAddressRecordByAddress1CityStateZip(String address1, String city, String state, String zip) {
        log.info("Querying DuckDB for geocoded record with address1='{}', city='{}', state='{}', zip='{}'", address1, city, state, zip);

        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name, address1, city, state, zip, zip4, tileurl, latitude, longitude FROM geocoded WHERE address1 = ? AND city = ? AND state = ? AND zip = ?",

                    (rs, rowNum) -> {
                        final Double latitude = rs.getObject("latitude", Double.class);
                        final Double longitude = rs.getObject("longitude", Double.class);
                        final GeoCoordinate coord = (latitude != null && longitude != null)
                                ? new GeoCoordinate(latitude, longitude)
                                : null;
                        return new GeocodedAddressRecord(
                                rs.getString("name"),
                                rs.getString("address1"),
                                rs.getString("city"),
                                rs.getString("state"),
                                rs.getString("zip"),
                                rs.getString("zip4"),
                                rs.getString("tileurl"),
                                coord);
                    },
                    address1, city, state, zip);
        } catch (final org.springframework.dao.EmptyResultDataAccessException ex) {
            log.info("No geocoded record found for address1='{}', city='{}', state='{}', zip='{}'", address1, city, state, zip);
            return null;
        }
    }

    public void insertGeocodedAddress(GeocodedAddressRecord geocodedRecord) {
        log.info("Inserting geocoded record into DuckDB for address1='{}', city='{}', state='{}', zip='{}'", geocodedRecord.getAddress1(), geocodedRecord.getCity(), geocodedRecord.getState(), geocodedRecord.getZip());
        final GeoCoordinate coord = geocodedRecord.getGeocodedCoordinate();
        final Double latitude = coord != null ? coord.latitude() : null;
        final Double longitude = coord != null ? coord.longitude() : null;
        jdbcTemplate.update(
            "INSERT INTO geocoded (name, address1, address2, city, state, zip, zip4, latitude, longitude, tileurl) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            geocodedRecord.getName(), geocodedRecord.getAddress1(), geocodedRecord.getAddress2(), geocodedRecord.getCity(),
            geocodedRecord.getState(), geocodedRecord.getZip(), geocodedRecord.getZip4(), latitude, longitude, geocodedRecord.getTileUrl()
        );
        log.info("Inserted geocoded record into DuckDB for address1='{}', city='{}', state='{}', zip='{}'", geocodedRecord.getAddress1(), geocodedRecord.getCity(), geocodedRecord.getState(), geocodedRecord.getZip());
    }
}
