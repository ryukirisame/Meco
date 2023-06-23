package com.example.meco.models;

import com.google.firebase.firestore.GeoPoint;

public class AcceptedCustomers {

    String customer_id;



    String distance;
    String customer_name;
    GeoPoint customer_location;
    String customer_message;
    String customer_phone;


    public AcceptedCustomers(String customer_id, String customer_name, GeoPoint customer_location, String customer_message, String customer_phone) {
        this.customer_id = customer_id;
        this.customer_name = customer_name;
        this.customer_location = customer_location;
        this.customer_message = customer_message;
        this.customer_phone = customer_phone;
    }

    public AcceptedCustomers() {
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public String getCustomer_name() {
        return customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }

    public GeoPoint getCustomer_location() {
        return customer_location;
    }

    public void setCustomer_location(GeoPoint customer_location) {
        this.customer_location = customer_location;
    }

    public String getCustomer_message() {
        return customer_message;
    }

    public void setCustomer_message(String customer_message) {
        this.customer_message = customer_message;
    }

    public String getCustomer_phone() {
        return customer_phone;
    }

    public void setCustomer_phone(String customer_phone) {
        this.customer_phone = customer_phone;
    }
    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}
