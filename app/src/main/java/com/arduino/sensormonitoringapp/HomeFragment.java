package com.arduino.sensormonitoringapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {
    private TextView tempValueText, moistureValueText, humidityValueText;
    private ImageView tempIcon, moistureIcon, humidityIcon;
    private MaterialCardView tempCard, moistureCard, humidityCard;
    private DatabaseReference sensorDataRef;
    private String prevTemp = "", prevMoisture = "", prevHumidity = "";


    //Inflate the layout, Firebase and binds the UI elements
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        //Bind views
        tempValueText = view.findViewById(R.id.tempValue);
        moistureValueText = view.findViewById(R.id.moistureValue);
        humidityValueText = view.findViewById(R.id.humidityValue);

        tempIcon = view.findViewById(R.id.tempIcon);
        moistureIcon = view.findViewById(R.id.moistureIcon);
        humidityIcon = view.findViewById(R.id.humidityIcon);

        tempCard = view.findViewById(R.id.tempCard);
        moistureCard = view.findViewById(R.id.moistureCard);
        humidityCard = view.findViewById(R.id.humidityCard);

        //Initialize Firebase
        sensorDataRef = FirebaseDatabase.getInstance().getReference("sensorData");
        fetchInitialValues(); // Fetch initial values

        return view;
    }

    //Retrieves the most recent sensor values from Firebase and starts real-time listening
    private void fetchInitialValues() {
        sensorDataRef.limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    DatabaseReference timeRef = sensorDataRef.child(dateSnapshot.getKey());
                    timeRef.limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot timeSnapshot) {
                            for (DataSnapshot timeEntry : timeSnapshot.getChildren()) {
                                Object tempObject = timeEntry.child("temp").getValue();
                                Object moistureObject = timeEntry.child("moisture").getValue();
                                Object humidityObject = timeEntry.child("humidity").getValue();

                                if (tempObject != null && moistureObject != null && humidityObject != null) {
                                    prevTemp = tempObject.toString() + " °C";
                                    prevMoisture = moistureObject.toString() + " %";
                                    prevHumidity = humidityObject.toString() + " %";
                                    updateValues(tempObject.toString(), moistureObject.toString(), humidityObject.toString());
                                } else {
                                    prevTemp = "-- °C";
                                    prevMoisture = "-- %";
                                    prevHumidity = "-- %";
                                    updateValues("--", "--", "--");
                                }
                                attachRealTimeListener(); // Start listening for changes after initial values are fetched.
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            attachRealTimeListener(); // Start listening for changes even if initial data fetch fails.
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                attachRealTimeListener(); // Start listening for changes even if initial data fetch fails.
            }
        });
    }

    //Continuously listens for new sensor data and updates the UI when changes occur
    private void attachRealTimeListener() {
        sensorDataRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    DatabaseReference timeRef = sensorDataRef.child(dateSnapshot.getKey());
                    timeRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot timeSnapshot) {
                            for (DataSnapshot timeEntry : timeSnapshot.getChildren()) {
                                Object tempObject = timeEntry.child("temp").getValue();
                                Object moistureObject = timeEntry.child("moisture").getValue();
                                Object humidityObject = timeEntry.child("humidity").getValue();

                                if (tempObject != null && moistureObject != null && humidityObject != null) {
                                    updateValues(tempObject.toString(), moistureObject.toString(), humidityObject.toString());
                                } else {
                                    updateValues("--", "--", "--");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    //Updates the displayed sensor values and triggers animations if the values have changed
    private void updateValues(String tempValue, String moistureValue, String humidityValue) {
        if (!tempValue.equals(prevTemp.replace(" °C", "")) ||
                !moistureValue.equals(prevMoisture.replace(" %", "")) ||
                !humidityValue.equals(prevHumidity.replace(" %", ""))) {

            if (!tempValue.equals(prevTemp.replace(" °C", ""))) {
                animateCardChange(tempCard);
            }
            if (!moistureValue.equals(prevMoisture.replace(" %", ""))) {
                animateCardChange(moistureCard);
            }
            if (!humidityValue.equals(prevHumidity.replace(" %", ""))) {
                animateCardChange(humidityCard);
            }

            prevTemp = tempValue + " °C";
            prevMoisture = moistureValue + " %";
            prevHumidity = humidityValue + " %";
        }

        tempValueText.setText(tempValue + " °C");
        moistureValueText.setText(moistureValue + " %");
        humidityValueText.setText(humidityValue + " %");
    }

    //Plays a color and elevation animation when gets an update
    private void animateCardChange(MaterialCardView card) {
        if (getContext() != null) {
            ObjectAnimator elevationAnim = ObjectAnimator.ofFloat(card, "cardElevation", 8f, 0f);
            elevationAnim.setDuration(500);

            ObjectAnimator colorAnim = ObjectAnimator.ofObject(card, "cardBackgroundColor", new ArgbEvaluator(),
                    ContextCompat.getColor(getContext(), R.color.cardBackground),
                    ContextCompat.getColor(getContext(), R.color.greenhouseAccent),
                    ContextCompat.getColor(getContext(), R.color.cardBackground));
            colorAnim.setDuration(500);

            elevationAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    card.setCardElevation(0f);
                }
            });

            elevationAnim.start();
            colorAnim.start();
        }
    }
}
