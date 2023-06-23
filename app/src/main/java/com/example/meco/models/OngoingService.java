package com.example.meco.models;

public class OngoingService {

    String mechanic_id;
    String mechanic_name;
    String mechanic_phone;

    public OngoingService(String mechanic_id, String mechanic_name, String mechanic_phone) {
        this.mechanic_id = mechanic_id;
        this.mechanic_name = mechanic_name;
        this.mechanic_phone = mechanic_phone;
    }

    public OngoingService() {
    }

    public String getMechanic_id() {
        return mechanic_id;
    }

    public void setMechanic_id(String mechanic_id) {
        this.mechanic_id = mechanic_id;
    }

    public String getMechanic_name() {
        return mechanic_name;
    }

    public void setMechanic_name(String mechanic_name) {
        this.mechanic_name = mechanic_name;
    }

    public String getMechanic_phone() {
        return mechanic_phone;
    }

    public void setMechanic_phone(String mechanic_phone) {
        this.mechanic_phone = mechanic_phone;
    }
}
