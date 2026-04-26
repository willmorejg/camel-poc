package net.ljcomputing.camelpoc.model;

public record GeoCoordinate(double latitude, double longitude) {

    public double[] toGeoJsonCoordinates() {
        return new double[]{ longitude, latitude };
    }
}
