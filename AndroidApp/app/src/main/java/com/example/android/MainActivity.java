package com.example.android;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.android.api.ApiClient;
import com.example.android.api.WeatherApi;
import com.example.android.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private EditText etCity;
    private Button btnGetWeather;
    private TextView tvResult;

    private final String API_KEY = "b7fdd75b876a170618de039f8ea11be7"; // ← THAY bằng API key bạn lấy được

    private Switch switchLight;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCity = findViewById(R.id.etCity);
        btnGetWeather = findViewById(R.id.btnGetWeather);
        tvResult = findViewById(R.id.tvResult);
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
        btnGetWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String city = etCity.getText().toString().trim();
                if (city.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Vui lòng nhập tên thành phố", Toast.LENGTH_SHORT).show();
                    return;
                }

                WeatherApi api = ApiClient.getClient().create(WeatherApi.class);
                Call<WeatherResponse> call = api.getWeatherByCity(city, API_KEY, "metric");

                call.enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        if (response.isSuccessful()) {
                            WeatherResponse weather = response.body();
                            String desc = weather.weather[0].description;
                            float temp = weather.main.temp;

                            String result = "Thời tiết: " + desc + "\nNhiệt độ: " + temp + "°C";
                            tvResult.setText(result);
                        } else {
                            tvResult.setText("Không tìm thấy thông tin thời tiết cho: " + city);
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        tvResult.setText("Lỗi khi gọi API: " + t.getMessage());
                    }
                });
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
