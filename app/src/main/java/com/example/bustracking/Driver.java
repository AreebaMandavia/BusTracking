package com.example.bustracking;

public class Driver {
    private String busId;
    private String route;
    private String lat;
    private String lng;
    private String docId;
    private String currentUserId;
    private String address;

    public Driver(){}

    public Driver(String busId, String route, String lat, String lng, String docId, String currentUserId,String address) {
        this.busId = busId;
        this.route = route;
        this.lat = lat;
        this.lng = lng;
        this.docId = docId;
        this.currentUserId = currentUserId;
        this.address=address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }
}
