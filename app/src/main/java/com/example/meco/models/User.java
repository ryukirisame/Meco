package com.example.meco.models;

import java.util.ArrayList;

public class User {
    String email;
    String phone;
    String userType;
    String name;
    ArrayList<String> requests= new ArrayList<>();

    public User(String email, String phone, String userType, String name) {
        this.email = email;
        this.phone = phone;
        this.userType = userType;
        this.name = name;
    }

    public User() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getRequests() {
        return requests;
    }

    public void setRequests(ArrayList<String> requests) {
        this.requests = requests;
    }
}
