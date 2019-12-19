package com.example.maptest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RoundSummaryAdapter extends RecyclerView.Adapter<RoundSummaryAdapter.MyViewHolder> {
    Round round;
    MapGame game;
    Context context;

    public RoundSummaryAdapter(Round round, MapGame game, Context context){
        this.round=round;
        this.game=game;
        this.context=context;
    }

    public void setRound(Round round) {
        this.round = round;
    }

    @NonNull
    @Override
    public RoundSummaryAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.round_summary_choice, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Choice[] choices = round.getChoices();
        Choice currentChoice=choices[position];


        holder.getTxtGuess().setText(context.getString(R.string.textGuessSummaryHeader)+ " #" + Integer.toString(position+1));
        holder.getTxtCityState().setText(currentChoice.toString());
        holder.getTxtDistance().setText(context.getString(R.string.textDistance)+ ": " + Integer.toString((int)currentChoice.getDistanceOff()));
        holder.getTxtScore().setText(context.getString(R.string.textScoreHeader)+ Integer.toString((int)currentChoice.getScoredBasedOnDistance(currentChoice.getDistanceOff(),game.getDifficulty())));
    }

    @Override
    public int getItemCount() {
        Choice[] choices = round.getChoices();
        return choices.length;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        private TextView txtGuess;
        private TextView txtCityState;
        private TextView txtDistance;
        private TextView txtScore;

        public MyViewHolder(View itemView) {
            super(itemView);
            txtGuess = itemView.findViewById(R.id.txtRndSumGuess);
            txtCityState = itemView.findViewById(R.id.txtRndSumCityState);
            txtDistance = itemView.findViewById(R.id.txtRndSumDistance);
            txtScore = itemView.findViewById(R.id.txtRndSumScore);
        }

        public TextView getTxtCityState() {
            return txtCityState;
        }

        public TextView getTxtDistance() {
            return txtDistance;
        }

        public TextView getTxtScore() {
            return txtScore;
        }
        public TextView getTxtGuess(){
            return txtGuess;
        }
    }
}