package com.example.maptest;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ProgressBar guessTimer;

    private Animation animationFadeIn;
    private Animation animationFadeOut;
    private AnimationSet animationSetFadeInAndOut;
    private ObjectAnimator animationCountdown;

    private CountDownTimer timer;

    private float choiceMarkerStyle = BitmapDescriptorFactory.HUE_BLUE;
    private float guessMarkerStyle = BitmapDescriptorFactory.HUE_RED;


    private LatLng mapCenterUSA = new LatLng(44,-103);
    private float mapZoomLevel = 3.5f;

    private int[] cityResources = new int[10];
    private int[] europeCityResources = new int[10];

    private GoogleMap myMap;
    private JSONObject jsonData;
    private MarkerOptions currentMarker;
    private Marker guessMarker;

    private Button buttonGuess;
    private Button buttonContinue;

    private RecyclerView recyclerRoundSummary;
    private LinearLayout llRoundSummary;
    private RoundSummaryAdapter adapterRoundSummary;

    private MapGame mapGame;

    //time to guess in ms
    private int timeToGuess = 10000;

    //progress bar colors/durations
    private ProgressBarColor normalBarColor = new ProgressBarColor(Color.GREEN,10000);
    private ProgressBarColor warningBarColor1 = new ProgressBarColor(Color.YELLOW, 5000);
    private ProgressBarColor warningBarColor2 = new ProgressBarColor(Color.RED, 1000);

    private boolean guessMade=false;

    private ArrayList<CountDownTimer> activeTimers = new ArrayList<CountDownTimer>();

    private ConstraintLayout gameContentMain;

    private Intent preferencesIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //add start screen click listener
        final ConstraintLayout startScreenLayout = (ConstraintLayout) findViewById(R.id.layoutClickToStart);

        //make click message animated
        final TextView textClickAnywhereToStart = (TextView)findViewById(R.id.textClickAnywhereStart);

        preferencesIntent = new Intent(getApplicationContext(), SettingsActivity.class);

        PreferenceManager.setDefaultValues(this,R.xml.preferences,false);
        PreferenceManager.getDefaultSharedPreferences(this);

        //async task to load
        AsyncTask<Void, Void, Void> loadTask = new AsyncTask <Void, Void, Void>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    //instantiate a new game
                    mapGame=new MapGame(updateDifficulty(PreferenceManager.getDefaultSharedPreferences(getApplicationContext())),Region.United_States);
                    load(mapGame.getRegion());
                }
                catch(Exception ex){
                    ex.printStackTrace();
                    Log.println(Log.ERROR,null,ex.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                //stop the spinning progress bar
                ProgressBar progressBarSplash = (ProgressBar)findViewById(R.id.progressBarSplash);
                progressBarSplash.setVisibility(View.INVISIBLE);

                //fade in and fade out the text
                Animation myFadeInAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.clickanywhereanimation);
                textClickAnywhereToStart.startAnimation(myFadeInAnimation);
                textClickAnywhereToStart.setVisibility(View.VISIBLE);

                //allow clicking to advance to the next screen
                startScreenLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startScreenLayout.setVisibility(View.INVISIBLE);

                        gameContentMain.setVisibility(View.VISIBLE);

                        textClickAnywhereToStart.clearAnimation();
                        startGame();
                    }
                });

            }
        }.execute();
    };

    //runnable to inflate content main in the main thread from the loading task
    final Runnable runnableInflate = new Runnable()
    {
        public void run()
        {
            ConstraintLayout activityMain = (ConstraintLayout) findViewById(R.id.layoutActivityMain);
            getLayoutInflater().inflate(R.layout.content_main,activityMain);
            gameContentMain = (ConstraintLayout) findViewById(R.id.layoutContentMain);
            gameContentMain.setVisibility(View.INVISIBLE);

            enableSettingsFAB();

            initializeMap();
        }
    };

    //attach the settings to the FAB
    public void enableSettingsFAB(){
        FloatingActionButton fabSettings = (FloatingActionButton)findViewById(R.id.fabSettings);
        fabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(preferencesIntent);
            }
        });
    }

    //load elements when initializing the application
    public boolean load(Region region){
        //map the json resources to their country/rounds
        try{
            for(int i=0;i<10;i++){
                if(region == Region.United_States){
                    cityResources[i] = getResources().getIdentifier("us"+Integer.toString(i),"raw",getPackageName());
                }
                //allows the functionality for an additional europe region later (the preference does not yet exist)
                else if(region == Region.Europe){
                    cityResources[i] = getResources().getIdentifier("eu"+Integer.toString(i),"raw",getPackageName());
                }
                else{
                    //this should never be the case
                    throw new Exception("Invalid region");
                }
            }
            jsonData = new JSONObject(getJSON(cityResources[0]));
        }
        catch(Exception ex){
            ex.printStackTrace();
        }


        //layout must be inflated in main thread
        runOnUiThread(runnableInflate);

        return true;
    }

    //start the game
    public void startGame(){
        //add settings onclick event
        FloatingActionButton fabSettings = (FloatingActionButton)findViewById(R.id.fabSettingsMain);
        fabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //display confirmation dialog
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //note: the behavior of the positive and negative buttons in this context are reversed for desired order
                        switch (which){
                            case DialogInterface.BUTTON_NEGATIVE:
                                startActivity(preferencesIntent);
                                break;

                            case DialogInterface.BUTTON_POSITIVE:
                                break;
                        }
                    }
                };

                //note: the behavior of the positive and negative buttons in this context are reversed for desired order (see above)
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(getString(R.string.textConfirmDialog))
                        .setPositiveButton(getString(R.string.no), dialogClickListener)
                        .setNegativeButton(getString(R.string.yes), dialogClickListener);
                AlertDialog alert = builder.create();
                alert.show();
            }
        });


        mapGame.updateRound(jsonData,1);
        mapGame.getCurrentRound().nextSelection();

        llRoundSummary = (LinearLayout)findViewById(R.id.llRoundSummary);
        llRoundSummary.setVisibility(View.INVISIBLE);

        showNextAlert();

        //tie the adapter to the recyclerview, assign linear layout manager
        adapterRoundSummary = new RoundSummaryAdapter(mapGame.getCurrentRound(),mapGame,getApplicationContext());
        recyclerRoundSummary = (RecyclerView)findViewById(R.id.recyclerRoundSummary);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerRoundSummary.setLayoutManager(layoutManager);

        //display the current choice target
        TextView cityToGuess = (TextView)findViewById(R.id.textCityToGuess);
        cityToGuess.setText(Integer.toString(mapGame.getCurrentRound().getCurrentSelection()) + ". " + mapGame.getCurrentChoice().toString());

        //add guess onClickListener
        buttonGuess = (Button)findViewById(R.id.buttonGuess);
        buttonGuess.setOnClickListener(listenerMakeGuess);
        buttonGuess.setVisibility(View.INVISIBLE);

        //add continue onClickListener
        buttonContinue = (Button)findViewById(R.id.buttonContinue);
        buttonContinue.setOnClickListener(listenerContinueRound);

        TextView hideSummary = (TextView)findViewById(R.id.hideSummary);
        hideSummary.setOnClickListener(hideRoundListener);

        //find the timer progress bar
        guessTimer = (ProgressBar)findViewById(R.id.progressBar);
        guessTimer.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

        //map out the progress bar color changes
        normalBarColor.addNextColor(warningBarColor1);
        warningBarColor1.addNextColor(warningBarColor2);

        //add animations
        //fade in animation not used
        animationFadeIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fadein);
        animationFadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                TextView textRoundAlert = (TextView)findViewById(R.id.textRoundAlert);
                textRoundAlert.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animationFadeOut = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fadeout);
        animationFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                TextView textRoundAlert = (TextView)findViewById(R.id.textRoundAlert);
                textRoundAlert.setVisibility(View.INVISIBLE);

                Button makeGuessButton = (Button)findViewById(R.id.buttonGuess);
                makeGuessButton.setVisibility(View.VISIBLE);

                //start the timer once the fade out animation is complete
                startTimer();

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        guessMade=false;

        //display round score
        displayRoundScore(true);

        animateAlert();
    }

    //display the fading round alert
    private void showNextAlert(){
        //show the pre round graphic
        TextView textRoundAlert = (TextView)findViewById(R.id.textRoundAlert);
        textRoundAlert.bringToFront();
        textRoundAlert.setText("ROUND " + mapGame.getCurrentRoundNumber());
        textRoundAlert.setVisibility(View.VISIBLE);
    }

    //handle the round alert fading animation
    private void animateAlert(){
        //add animations to an animation set
        animationSetFadeInAndOut = new AnimationSet(false);
        animationSetFadeInAndOut.addAnimation(animationFadeOut);

        TextView textRoundAlert = (TextView)findViewById(R.id.textRoundAlert);
        textRoundAlert.setAnimation(animationSetFadeInAndOut);
        animationSetFadeInAndOut.start();
    }

    //initialize the map
    public void initializeMap(){
        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        //set zoom for larger displays
        if(screenSize==Configuration.SCREENLAYOUT_SIZE_XLARGE || screenSize==Configuration.SCREENLAYOUT_SIZE_LARGE){
            mapZoomLevel = 4.0f;
        }
    }

    //when the map is ready
    @Override
    public void onMapReady(GoogleMap map) {
        //initialize the map
        myMap = map;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(mapCenterUSA,mapZoomLevel));
        map.setOnMapClickListener(mapClickListener);

        //set the map style to hide labels
        if(mapGame.getDifficulty()==Difficulty.Very_Hard){
            map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyleveryhard)
            );
        }
        else{
            map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle)
            );
        }
    }

    public View.OnClickListener listenerMakeGuess = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            guessMade=true;
            makeGuess(currentMarker);
        }
    };

    public View.OnClickListener listenerContinueRound = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mapGame.getCurrentRound().getCurrentSelection()==10) {
                endRound();
            }
            else
            {
                continueRound();
            }
        }
    };

    public void startRound(){
        mapGame.getCurrentRound().nextSelection();

        //hide continue button
        buttonContinue.setVisibility(View.INVISIBLE);
        buttonGuess.setEnabled(false);

        //add the map click listener
        myMap.setOnMapClickListener(mapClickListener);

        //hide the guess summary
        LinearLayout boxSummary = (LinearLayout)findViewById(R.id.summaryBox);
        boxSummary.setVisibility(View.INVISIBLE);

        //reposition map display
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapCenterUSA,mapZoomLevel));

        //set the map style to hide labels
        if(mapGame.getDifficulty()==Difficulty.Very_Hard){
            myMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyleveryhard)
            );
        }
        else{
            myMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle)
            );
        }

        //clear the map contents
        myMap.clear();

        //get the next choice from the game
        Round currentRound = mapGame.getCurrentRound();
        Choice currentChoice = currentRound.getCurrentChoice();

        //display the current choice to guess at the top
        TextView choiceToGuess = (TextView)findViewById(R.id.textCityToGuess);
        choiceToGuess.setVisibility(View.VISIBLE);
        choiceToGuess.setText(Integer.toString(mapGame.getCurrentRound().getCurrentSelection()) + ". " + currentChoice.toString());

        guessTimer.setVisibility(View.VISIBLE);
        guessTimer.setProgress(100);
        guessMade=false;

        //reset the marker
        currentMarker = null;
        //display round score
        displayRoundScore(true);
    }

    public void continueRound(){
        mapGame.getCurrentRound().nextSelection();

        //hide continue button, show make guess button
        buttonGuess.setVisibility(View.VISIBLE);
        buttonContinue.setVisibility(View.INVISIBLE);
        buttonGuess.setEnabled(false);

        //add the map click listener
        myMap.setOnMapClickListener(mapClickListener);

        //hide the guess summary
        LinearLayout boxSummary = (LinearLayout)findViewById(R.id.summaryBox);
        boxSummary.setVisibility(View.INVISIBLE);

        //reposition map display
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapCenterUSA,mapZoomLevel));

        //set the map style to hide labels
        if(mapGame.getDifficulty()==Difficulty.Very_Hard){
            myMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyleveryhard)
            );
        }
        else{
            myMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle)
            );
        }

        //clear the map contents
        myMap.clear();

        //get the next choice from the game
        Round currentRound = mapGame.getCurrentRound();
        Choice currentChoice = currentRound.getCurrentChoice();

        //display the current choice to guess at the top
        TextView choiceToGuess = (TextView)findViewById(R.id.textCityToGuess);
        choiceToGuess.setVisibility(View.VISIBLE);
        choiceToGuess.setText(Integer.toString(mapGame.getCurrentRound().getCurrentSelection()) + ". " + currentChoice.toString());

        //show and start the countdown
        guessTimer.setVisibility(View.VISIBLE);
        startTimer();
        guessMade=false;

        //reset the marker
        currentMarker = null;

        //display round score
        displayRoundScore(true);
    }

    public void endRound(){
        //hide the continue button & guess summary box
        buttonContinue.setVisibility(View.INVISIBLE);
        LinearLayout summaryBox = (LinearLayout)findViewById(R.id.summaryBox);
        summaryBox.setVisibility(View.INVISIBLE);

        //update the adapter and display the summary
        adapterRoundSummary.setRound(mapGame.getCurrentRound());
        recyclerRoundSummary.setAdapter(adapterRoundSummary);
        adapterRoundSummary.notifyDataSetChanged();

        llRoundSummary.setVisibility(View.VISIBLE);
        TextView textRoundScoreSummary = findViewById(R.id.textRoundScoreSummary);
        textRoundScoreSummary.setText("Score: " + mapGame.getCurrentRound().getRoundScore());

        TextView textRoundSummary = findViewById(R.id.txtRoundSummary);
        textRoundSummary.setText(getString(R.string.textRound) + " #" + mapGame.getCurrentRoundNumber() + " " + getString(R.string.textSummaryHeader));

        //see if the user has scored enough points to advance to the next round
        if(mapGame.checkIfPassedRound() && mapGame.getCurrentRoundNumber()<12){
            nextRoundEvents();
        }

        //hide round score
        displayRoundScore(false);


    }
    public void makeGuess(MarkerOptions currentMarker){
        //hide the countdown
        guessTimer.setVisibility(View.INVISIBLE);
        animationCountdown.cancel();
        guessMade=true;

        //if the user made a guess in the allotted timeframe
        Guess currentGuess;
        if(currentMarker!=null){
            LatLng guessCoords = currentMarker.getPosition();
            currentGuess = new Guess(guessCoords);
        }
        else{
            currentGuess = new Guess();
        }
        double distanceBetweenGuesses = mapGame.play(currentGuess);
        displayResults(mapGame.getCurrentChoice(),distanceBetweenGuesses);

        //cancel and clear the timers
        for(CountDownTimer t: activeTimers){
            t.cancel();
        }
        activeTimers.clear();
    }

    public void displayRoundScore(boolean showScore){
        TextView textRoundScore = (TextView)findViewById(R.id.textRoundScore);
        TextView textRoundScoreHeading = (TextView)findViewById(R.id.textRoundScoreLabel);

        if(showScore){
            textRoundScore.setText(Integer.toString(mapGame.getCurrentRound().getRoundScore()) + "/" + Integer.toString(MapGame.REQUIRED_ROUND_SCORE));
            textRoundScoreHeading.setText(getString(R.string.textRoundScore));
        }
        else{
            textRoundScore.setText("");
            textRoundScoreHeading.setText("");
        }
    }


    public void displayResults(Choice currentChoice, double distanceBetween){
        //display the actual marker
        MarkerOptions actualMarker = new MarkerOptions()
                .position(currentChoice.getLatLng())
                .title(getString(R.string.textActualLocation))
                .icon(BitmapDescriptorFactory.defaultMarker(choiceMarkerStyle));
        Marker marker = myMap.addMarker(actualMarker);
        marker.showInfoWindow();

        TextView textDistanceResult = (TextView)findViewById(R.id.textDistanceResult);
        TextView textRoundScore = (TextView)findViewById(R.id.textRoundScoreHeading);
        TextView textSummaryCity = (TextView)findViewById(R.id.txtSummaryCity);

        //draw a line connecting the two markers
        // Instantiates a new Polyline object and adds points to define a rectangle
        if(distanceBetween!=-1){
            PolylineOptions rectOptions = new PolylineOptions()
                    .add(currentChoice.getLatLng())
                    .add(guessMarker.getPosition());

            // Get back the mutable Polyline
            Polyline polyline = myMap.addPolyline(rectOptions);
            polyline.setColor(Color.RED);
            polyline.setWidth(4);

            //display the distance between the markers
            textDistanceResult.setText(getString(R.string.textDistance) + ": " + Integer.toString((int)distanceBetween) + " " + getString(R.string.textMiles));
        }
        else{
            textDistanceResult.setText(getString(R.string.textDistance) + ": --");
        }

        textSummaryCity.setText(mapGame.getCurrentChoice().toString());
        textRoundScore.setText(getString(R.string.textScoreHeader) + " " + Integer.toString((int)mapGame.getCurrentRound().getCurrentChoice().getScoredBasedOnDistance(distanceBetween,mapGame.getDifficulty())));

        //show labels with guess
        myMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstylewithlabels)
        );

        //zoom based on how good their guess is
        float resultZoomLevel=getResultZoom(distanceBetween);
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapGame.getCurrentChoice().getLatLng(),resultZoomLevel));

        //display summary box
        LinearLayout summaryBox = findViewById(R.id.summaryBox);
        summaryBox.setVisibility(View.VISIBLE);
        //onTouchListener to make the summary draggable

        summaryBox.setOnTouchListener(draggableSumBoxListener);

        //hide top bar
        TextView txtCityToGuess = findViewById(R.id.textCityToGuess);
        txtCityToGuess.setVisibility(View.INVISIBLE);

        //hide guess button, show continue button
        Button btnGuess = (Button)findViewById(R.id.buttonGuess);
        btnGuess.setVisibility(View.INVISIBLE);
        Button btnContinue = (Button)findViewById(R.id.buttonContinue);
        btnContinue.setVisibility(View.VISIBLE);

        //disable map clicking
        myMap.setOnMapClickListener(null);


        //hide round score
        displayRoundScore(false);

    }

    public void nextRoundEvents(){
        mapGame.nextRound();

        //get the jsonobject associated with the new round
        try
        {
            jsonData = new JSONObject(getJSON(cityResources[mapGame.getCurrentRoundNumber()-1]));
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        mapGame.updateRound(jsonData,mapGame.getCurrentRoundNumber());
    }

    //Events after the game is complete
    private void lostGameEvents(){
        ConstraintLayout activityMain = (ConstraintLayout) findViewById(R.id.layoutActivityMain);
        getLayoutInflater().inflate(R.layout.content_lost,activityMain);
        ConstraintLayout lostGame = (ConstraintLayout) findViewById(R.id.contentLost);
        lostGame.setOnClickListener(newGameListener);
    }

    private void winEvents(){
        ConstraintLayout activityMain = (ConstraintLayout) findViewById(R.id.layoutActivityMain);
        getLayoutInflater().inflate(R.layout.content_win,activityMain);
        ConstraintLayout lostGame = (ConstraintLayout) findViewById(R.id.contentWin);
        lostGame.setOnClickListener(newGameListener);
    }

    //listener that starts a new game on click
    private View.OnClickListener newGameListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            recreate();
        }
    };


    private View.OnTouchListener draggableSumBoxListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //floats to track the change in coordinates
            float dX=0;
            float dY=0;

            //when element is touched
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dX = v.getX() - event.getRawX();
                dY = v.getY() - event.getRawY();
            }
            //when element is moved
            else if(event.getAction() == MotionEvent.ACTION_MOVE){
                v.animate()
                        .x(event.getRawX() + dX - (v.getWidth() / 2))
                        .y(event.getRawY() + dY - (v.getHeight() / 2))
                        .setDuration(0)
                        .start();
                v.setVisibility(View.VISIBLE);
            }
            return true;
        }
    };

    private float getResultZoom(double distanceBetween){
        float resultZoomLevel;
        if(distanceBetween==-1){
            resultZoomLevel=5.0f;
        }
        if(distanceBetween<30){
            resultZoomLevel = 8.5f;
        }
        else if(distanceBetween<75){
            resultZoomLevel = 8.0f;
        }
        else if(distanceBetween<110){
            resultZoomLevel = 7.5f;
        }
        else if(distanceBetween<200){
            resultZoomLevel = 6.5f;
        }
        else if(distanceBetween<300){
            resultZoomLevel = 6.0f;
        }
        else{
            resultZoomLevel = 5.0f;
        }
        return resultZoomLevel;
    }

    public GoogleMap.OnMapClickListener mapClickListener = new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            //enable guess button
            buttonGuess.setEnabled(true);

            //reposition marker
            myMap.clear();
            currentMarker = new MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.textGuessSummaryHeader))
                    .icon(BitmapDescriptorFactory.defaultMarker(guessMarkerStyle));
            guessMarker = myMap.addMarker(currentMarker);

        }
    };



    public String getJSON(int resourceID){
        String json = null;
        try {
            InputStream is = getResources().openRawResource(resourceID);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    View.OnClickListener hideRoundListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mapGame.getCurrentRoundNumber()==10){
                winEvents();
            }
            else if(mapGame.isPassedRound()){
                hideRoundSummary();
                showNextAlert();
                startRound();
                animateAlert();
            }
            else{
                lostGameEvents();
            }
        }
    };

    private void hideRoundSummary(){
        llRoundSummary.setVisibility(View.INVISIBLE);
    }



    //use a linearly interpolated animation to make the progress bar increase
    private void startTimer(){
        animationCountdown = ObjectAnimator.ofInt(guessTimer, getString(R.string.textProgress), 100, 0);
        animationCountdown.setDuration(10000);
        animationCountdown.setInterpolator(new LinearInterpolator());
        animationCountdown.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                setTimerTint(normalBarColor);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                //if they run out of time, make a guess based on the marker location
                if(!guessMade){
                    makeGuess(currentMarker);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) { }

            @Override
            public void onAnimationRepeat(Animator animator) { }

        });
        animationCountdown.start();
    }

    //set the timer tints at the proper times
    public void setTimerTint(final ProgressBarColor currentBarColor){
        //set the color of the progress bar
        guessTimer.setProgressTintList(ColorStateList.valueOf(currentBarColor.getColor()));

        //check to see if it is the last progress bar color
        if(currentBarColor.getNextProgressBarColor()==null){
            return;
        }
        else{
            //get duration of progress bar color
            int duration = currentBarColor.getStartDuration()-currentBarColor.getNextProgressBarColor().getStartDuration();

            //instantiate a timer
            CountDownTimer timer = new CountDownTimer(duration,1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    setTimerTint(currentBarColor.getNextProgressBarColor());
                }
            }.start();

            //add the current timer to the list of active timers (so they can be canceled when a guess is made
            activeTimers.add(timer);

        }
    }

    public Difficulty updateDifficulty(SharedPreferences sharedPreferences){
        //grab difficulty from preferences
        Difficulty difficulty = Difficulty.valueOf(sharedPreferences.getString("pref_difficulty",null));

        return difficulty;
    }


}
