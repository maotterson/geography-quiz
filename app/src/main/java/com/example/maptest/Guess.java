package com.example.maptest;

import com.google.android.gms.maps.model.LatLng;

public class Guess {
    private double x;
    private double y;
    private LatLng guessCoords;
    private boolean guessed;

    public Guess(LatLng latLngGuess){
        x=latLngGuess.latitude;
        y=latLngGuess.longitude;
        guessCoords = latLngGuess;
        guessed=true;
    }

    //empty constructor overload -> no "guess"
    public Guess(){
        guessed=false;
    }

    public double getY() {
        return y;
    }

    public double getX() {
        return x;
    }

    public LatLng getGuessCoords() {
        return guessCoords;
    }

    public boolean isGuessed() {
        return guessed;
    }
}
