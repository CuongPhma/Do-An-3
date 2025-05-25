package com.example.android;

import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private Switch switchLight;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchLight = findViewById(R.id.switchLight);

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://do-an-3-9959f-default-rtdb.asia-southeast1.firebasedatabase.app/");
        dbRef = database.getReference("devices").child("den");
        dbRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String value = snapshot.getValue(String.class);
                if (value != null) {
                    switchLight.setChecked(value.equals("ON"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi đọc status", error.toException());
            }
        });

        switchLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String value = isChecked ? "ON" : "OFF";
            Log.d("DEBUG", "switchLight = " + value);

            dbRef.child("status").setValue(value)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", "Ghi thành công: " + value);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Lỗi ghi Firebase", e);
                    });

        });
    }
}
