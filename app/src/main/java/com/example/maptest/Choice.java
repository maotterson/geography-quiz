package com.example.maptest;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

public class Choice {
    private double x;
    private double y;
    private String cityName;
    private String stateName;
    private LatLng latLng;
    private double distanceOff;

    //constructor
    public Choice(String cityName, String stateName, double x, double y){
        this.cityName=cityName;
        this.stateName=stateName;
        this.x=x;
        this.y=y;
        this.latLng=new LatLng(x,y);
    }

    //constructor using json-object
    public Choice(JSONObject jsonCity){
        try{
            cityName=jsonCity.getString("City");
            stateName=jsonCity.getString("State");
            x=Double.parseDouble(jsonCity.getString("x"));
            y=Double.parseDouble(jsonCity.getString("y"));
            this.latLng=new LatLng(x,y);
        }
        catch(Exception ex){

        }
    }

    //gets
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getCityName() {
        return cityName;
    }

    public String getStateName() {
        return stateName;
    }
    public LatLng getLatLng(){
        return latLng;
    }

    public void setDistanceOff(double distanceOff) {

        this.distanceOff = distanceOff;
    }

    public double getDistanceOff() {
        return distanceOff;
    }

    public double getScoredBasedOnDistance(double distance, Difficulty difficulty){
        return MapGame.getScore(distance, difficulty);
    }

    public String toString(){
        return this.cityName + ", " + this.stateName;
    }
}
