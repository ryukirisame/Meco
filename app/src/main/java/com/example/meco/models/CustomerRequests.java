package com.example.meco.models;

import com.google.firebase.firestore.GeoPoint;

public class CustomerRequests {


    String customer_id;
    GeoPoint customer_location;
    String customer_message;
    String mechanic_id;
    String customer_distance;
    String customer_name;

    public String getCustomer_name() {
        return customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }



    public String getCustomer_distance() {
        return customer_distance;
    }

    public void setCustomer_distance(String customer_distance) {
        this.customer_distance = customer_distance;
    }



    public CustomerRequests(String customer_id, GeoPoint customer_location, String customer_message, String mechanic_id) {
        this.customer_id = customer_id;
        this.customer_location = customer_location;
        this.customer_message = customer_message;
        this.mechanic_id = mechanic_id;
    }

    public CustomerRequests() {
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
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

    public String getMechanic_id() {
        return mechanic_id;
    }

    public void setMechanic_id(String mechanic_id) {
        this.mechanic_id = mechanic_id;
    }
}
