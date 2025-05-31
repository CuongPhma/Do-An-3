package com.example.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.api.ApiClient;
import com.example.android.api.RetrofitClient;
import com.example.android.api.WeatherApi;
import com.example.android.model.WeatherResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Locale;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private TextView tvResult;
    private ImageView ivIcon;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    /// /////////////////////////////
    private DrawerLayout drawerLayout;
    private ImageView iconMenu;

    private final String API_KEY = "b7fdd75b876a170618de039f8ea11be7"; // Thay bằng API key thật

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.tvWeather);
        ivIcon = findViewById(R.id.ivWeatherIcon);
        /// /////////////////////

        drawerLayout = findViewById(R.id.drawerLayout);
        iconMenu = findViewById(R.id.iconMenu);

        iconMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START); // Mở drawer
        });
        /// //////////////////////////
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Chưa có quyền -> yêu cầu
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Có quyền rồi, lấy vị trí
            getLastLocation();
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Nếu tới đây mà vẫn chưa có quyền, thoát luôn
            tvResult.setText("Chưa có quyền truy cập vị trí");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        getWeatherByCoordinates(lat, lon);
                    } else {
                        tvResult.setText("Không lấy được vị trí hiện tại");
                    }
                })
                .addOnFailureListener(e -> tvResult.setText("Lỗi lấy vị trí: " + e.getMessage()));
    }


    private void getWeatherByCoordinates(double lat, double lon) {
        String apiKey = "b7fdd75b876a170618de039f8ea11be7"; // Thay bằng API Key của bạn

        WeatherApi weatherApi = RetrofitClient.getClient().create(WeatherApi.class);
        Call<WeatherResponse> call = weatherApi.getWeatherByCoordinates(lat, lon, apiKey, "metric", "vi");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherResponse = response.body();

                    // Lấy thông tin thời tiết
                    String description = weatherResponse.getWeather().get(0).getDescription();
                    float temp = weatherResponse.getMain().getTemp();
                    String iconCode = weatherResponse.getWeather().get(0).getIcon();
                    String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                    // Cập nhật giao diện
                    TextView tvWeather = findViewById(R.id.tvWeather);
                    ImageView ivWeatherIcon = findViewById(R.id.ivWeatherIcon);

                    tvWeather.setText(String.format(Locale.getDefault(), "%s\n%.1f°C", description,temp ));
                    Glide.with(MainActivity.this)
                            .load(iconUrl)
                            .into(ivWeatherIcon);
                } else {
                    Toast.makeText(MainActivity.this, "Không lấy được dữ liệu thời tiết", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền vị trí để app hoạt động", Toast.LENGTH_LONG).show();
                tvResult.setText("Không có quyền truy cập vị trí");
            }
        }
    }
}
