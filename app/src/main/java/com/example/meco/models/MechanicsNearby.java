package com.example.meco.models;

import android.location.Location;

import com.google.firebase.firestore.GeoPoint;

public class MechanicsNearby {

    String name;
    String mechanic_id;
    GeoPoint location;

//    public String getMechanic_id() {
//        return mechanic_id;
//    }
//
//    public void setMechanic_id(String mechanic_id) {
//        this.mechanic_id = mechanic_id;
//    }

    double distance;
    String phone;




    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public MechanicsNearby() {
    }

    public String getMechanic_id() {
        return mechanic_id;
    }

    public void setMechanic_id(String id) {
        this.mechanic_id = id;
    }

    public MechanicsNearby(String name, Location distance) {
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
