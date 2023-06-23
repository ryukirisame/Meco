package com.example.meco.models;

public class CUSTOMER_LOCATION {

    public static double curLat=0.0;
    public static double curLong=0.0;
    public static customerLocationInterface customerLocationInterfaceListener;

    public CUSTOMER_LOCATION(double curLat, double curLong) {
        this.curLat = curLat;
        this.curLong = curLong;
    }

    public double getCurLat() {
        return curLat;
    }
    public static void setLocation(double lat, double lng)
    {
        curLat=lat;
        curLong=lng;
        if(customerLocationInterfaceListener!=null)
        {
            customerLocationInterfaceListener.serveLocation(curLat, curLong);
        }

    }
    public void setCurLat(double curLat) {
        this.curLat = curLat;
    }

    public double getCurLong() {
        return curLong;
    }

    public void setCurLong(double curLong) {
        this.curLong = curLong;
    }

    public static void setCustomerLocationInterfaceListener(customerLocationInterface customerLocationInterfaceListener) {
        CUSTOMER_LOCATION.customerLocationInterfaceListener = customerLocationInterfaceListener;
    }

    public interface customerLocationInterface{
        public void serveLocation(double curLat, double curLong);
    }
}
