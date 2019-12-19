package com.example.maptest;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Round {
    //array of all the choices for the given round
    private Choice[] choices;
    private int number;
    private int currentSelection=0;
    private int roundScore=0;

    //new round constructor
    public Round(JSONObject jsonCurrentRoundCities, int number){
        this.choices = generateChoices(jsonCurrentRoundCities);
        this.number = number;
    }

    public Choice[] getChoices() {
        return choices;
    }

    public int getNumber() {
        return number;
    }

    public int getCurrentSelection() {
        return currentSelection;
    }

    public Choice getCurrentChoice(){
        return choices[currentSelection-1];
    }

    public void nextSelection(){
        currentSelection++;
    }

    public int updateRoundScore(double distance, Difficulty difficulty){
        roundScore+=MapGame.getScore(distance, difficulty);
        return roundScore;
    }

    public int getRoundScore() {
        return roundScore;
    }

    public Choice[] generateChoices(JSONObject jsonCurrentRoundCities){
        try{
            Choice[] choices = new Choice[10];
            JSONArray arrayCurrentRoundCities = jsonCurrentRoundCities.getJSONArray("cities");

            //add the jsonarray contents to an arraylist and shuffle the collection
            ArrayList<JSONObject> arrayListCurrentRoundCities = new ArrayList<JSONObject>();
            for(int i=0;i<arrayCurrentRoundCities.length();i++){
                arrayListCurrentRoundCities.add(arrayCurrentRoundCities.getJSONObject(i));
            }
            Collections.shuffle(arrayListCurrentRoundCities);

            //take the first ten items in the arraylist and add them to the choices array
            for(int i=0;i<10;i++){
                JSONObject jsonCityToAdd = arrayListCurrentRoundCities.get(i);
                Choice choiceToAdd = new Choice(jsonCityToAdd);
                choices[i]=choiceToAdd;
            }
            return choices;
        }
        catch(Exception ex){
            return null;
        }
    }

}
