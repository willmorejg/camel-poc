package net.ljcomputing.camelpoc.model;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Component
public class GeocodedAddressRecord extends AddressRecord {

    private GeoCoordinate geocodedCoordinate;
    private String tileUrl;
    private double pinOffsetX;
    private double pinOffsetY;

    public GeocodedAddressRecord() {
        super();
    }

    public GeocodedAddressRecord(String name, String address1, String city, String state, String zip, String zip4, String tileUrl, GeoCoordinate geocodedCoordinate) {
        super(name, address1, null, city, state, zip, zip4);
        this.geocodedCoordinate = geocodedCoordinate;
        this.tileUrl = tileUrl;
    }

    public GeoCoordinate getGeocodedCoordinate() {
        return geocodedCoordinate;
    }

    public void setGeocodedCoordinate(GeoCoordinate geocodedCoordinate) {
        this.geocodedCoordinate = geocodedCoordinate;
    }

    public String getTileUrl() {
        return tileUrl;
    }

    public void setTileUrl(String tileUrl) {
        this.tileUrl = tileUrl;
    }

    public double getPinOffsetX() {
        return pinOffsetX;
    }

    public void setPinOffsetX(double pinOffsetX) {
        this.pinOffsetX = pinOffsetX;
    }

    public double getPinOffsetY() {
        return pinOffsetY;
    }

    public void setPinOffsetY(double pinOffsetY) {
        this.pinOffsetY = pinOffsetY;
    }
}
