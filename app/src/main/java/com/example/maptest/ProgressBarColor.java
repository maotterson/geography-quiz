package com.example.maptest;

//class to manage the color states of the progress bar
public class ProgressBarColor {
    private int color;
    private int startDuration;
    private ProgressBarColor nextProgressBarColor;
    public ProgressBarColor(int color, int startDuration){
        this.color = color;
        this.startDuration = startDuration;
    }

    public int getColor() {
        return color;
    }
    public int getStartDuration(){
        return startDuration;
    }

    public ProgressBarColor getNextProgressBarColor() {
        return nextProgressBarColor;
    }

    //pointer to the next color if one exists
    public void addNextColor(ProgressBarColor nextProgressBarColor){
        this.nextProgressBarColor = nextProgressBarColor;
    }
}
