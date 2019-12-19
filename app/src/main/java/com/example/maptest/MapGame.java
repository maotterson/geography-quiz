package com.example.maptest;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

public class MapGame {
    //arraylist of all the rounds in the game
    private ArrayList<Round> rounds = new ArrayList<Round>();
    private int currentRoundNumber;
    private Round currentRound;
    private boolean passedRound=false;
    private Difficulty difficulty;
    private Region region;

    public static final int REQUIRED_ROUND_SCORE = 7000;

    public MapGame(Difficulty d, Region r){
        difficulty = d;
        region = r;
        initializeGame();
    }

    public ArrayList<Round> getRounds() {
        return rounds;
    }

    public int getCurrentRoundNumber() {
        return currentRoundNumber;
    }

    public Round getCurrentRound() {
        return currentRound;
    }

    public Choice getCurrentChoice(){
        return currentRound.getCurrentChoice();
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Region getRegion() {
        return region;
    }

    public void updateRound(JSONObject jsonData, int currentRoundNumber){
        currentRound=new Round(jsonData,currentRoundNumber);
        this.currentRoundNumber=currentRoundNumber;
        this.rounds.add(currentRound);
    }

    public void initializeGame(){
        //set current round to 1
        currentRoundNumber=1;

        //grab the corresponding resource file to make choices from
        Choice[] currentChoices = new Choice[10];
    }

    public double play(Guess currentGuess){
        double distance;
        if(currentGuess.isGuessed()){
            distance=calculateDistance(currentGuess.getGuessCoords(),currentRound.getCurrentChoice().getLatLng());
        }
        else{
            distance=-1;
        }
        getCurrentChoice().setDistanceOff(distance);
        currentRound.updateRoundScore(distance,difficulty);
        return distance;
    }

    private double calculateDistance(LatLng markerGuessCoords, LatLng markerChoiceCoords){
        double xGuess=markerGuessCoords.latitude;
        double yGuess=markerGuessCoords.longitude;
        double xChoice=markerChoiceCoords.latitude;
        double yChoice=markerChoiceCoords.longitude;

        Location locGuess = new Location("");
        locGuess.setLatitude(xGuess);
        locGuess.setLongitude(yGuess);

        Location locChoice = new Location("");
        locChoice.setLatitude(xChoice);
        locChoice.setLongitude(yChoice);

        double distance = locChoice.distanceTo(locGuess);

        //convert distance from meters to miles
        distance=distance/1609.344;

        return distance;
    }
    public void nextRound(){
        currentRoundNumber++;
    }

    public boolean checkIfPassedRound(){
        boolean passed=false;
        if(currentRound.getRoundScore()>=REQUIRED_ROUND_SCORE){
            passed=true;
        }
        passedRound=passed;
        return passed;
    }

    public boolean isPassedRound() {
        return passedRound;
    }

    public static double getScore(double distance,Difficulty difficulty){
        double guessScore;
        if(distance==-1){
            guessScore=0;
        }
        else{
            //calculate the score based on difficulty and distance off
            //very hard and hard have the same scoring, but different map detail
            switch(difficulty){
                //y = -0.0084x2 + 0.0527x + 981.43
                case Easy:
                    guessScore=-0.0084*distance*distance-0.0527*distance+1000;
                    break;
                //y = -0.0076x2 - 0.6108x + 973.79
                case Medium:
                    guessScore=-0.0076*distance*distance-0.6108*distance+1000;
                    break;
                //y = -0.0013x2 - 3.1456x + 1016.6
                case Hard:
                case Very_Hard:
                    guessScore=-0.0013*distance*distance-3.1456*distance+1000;
                    break;
                default:
                    guessScore=0;
                    break;
            }
        }
        if(guessScore<0){
            guessScore=0;
        }
        return guessScore;
    }
}
