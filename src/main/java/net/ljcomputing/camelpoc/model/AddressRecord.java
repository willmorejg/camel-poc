package net.ljcomputing.camelpoc.model;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Component
public class AddressRecord {

    private String name;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String zip;
    private String zip4;

    public AddressRecord() {}

    public AddressRecord(String name, String address1, String address2, String city, String state, String zip, String zip4) {
        this.name = name;
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.zip4 = zip4;
    }

    public AddressRecord(String name, String address1, String city, String state, String zip, String zip4) {
        this.name = name;
        this.address1 = address1;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.zip4 = zip4;
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZip() {
        return zip;
    }

    public String getZip4() {
        return zip4;
    }

    // --- Setters ---

    public void setName(final String name) {
        this.name = name;
    }

    public void setAddress1(final String address1) {
        this.address1 = address1;
    }

    public void setAddress2(final String address2) {
        this.address2 = address2;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public void setZip(final String zip) {
        this.zip = zip;
    }

    public void setZip4(final String zip4) {
        this.zip4 = zip4;
    }

    /** Convenience: full single-line address for the template */
    public String getFullAddress() {
        final StringBuilder sb = new StringBuilder();
        sb.append(address1 != null ? address1 : "");
        if (address2 != null && !address2.isBlank()) {
            sb.append(", ").append(address2);
        }
        sb.append(", ").append(city != null ? city : "");
        sb.append(", ").append(state != null ? state : "");
        sb.append(" ").append(zip != null ? zip : "");
        if (zip4 != null && !zip4.isBlank()) {
            sb.append("-").append(zip4);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "AddressRecord{name='" + name + "', city='" + city + "', state='" + state + "'}";
    }
}
