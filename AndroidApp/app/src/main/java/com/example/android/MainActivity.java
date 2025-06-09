package com.example.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.adapter.RFIDAdapter;
import com.example.android.api.ApiClient;
import com.example.android.api.RetrofitClient;
import com.example.android.api.WeatherApi;
import com.example.android.model.RFIDUser;
import com.example.android.model.WeatherResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {


    private DatabaseReference dbRef;
    private boolean isListening = false;

    private TextView tvResult, tvDHT_Temp, tvDHT_Hum;
    LinearLayout btnLight, btnFan, btnDoor, btnAc, btnTV, btnSensor;

    private ImageView ivIcon;
    DatabaseReference dhtRef;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    // Biến trạng thái
    boolean isLightOn = false;
    boolean isFanOn = false;
    boolean isDoorOn = false;
    boolean isAcOn = false;
    boolean isTvOn = false;
    boolean isSensorOn = false;

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
        tvDHT_Temp = findViewById(R.id.tvDHT_Temp);
        tvDHT_Hum = findViewById(R.id.tvDHT_Hum);
        btnLight = findViewById(R.id.btnLight);
        btnFan = findViewById(R.id.btnFan);
        btnDoor = findViewById(R.id.btnDoor);
        btnAc = findViewById(R.id.btnAc);
        btnTV = findViewById(R.id.btnTV);
        btnSensor = findViewById(R.id.btnSensor);
        drawerLayout = findViewById(R.id.drawerLayout);
        iconMenu = findViewById(R.id.iconMenu);

        NavigationView navigationView = findViewById(R.id.navigationView);
        iconMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START); // Mở drawer
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Xử lý khi chọn Home
                Toast.makeText(MainActivity.this, "Home selected", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                // Xử lý khi chọn Settings
                showAddCardDialog();
            } else if (id == R.id.nav_rfid) {
                // Xử lý khi chọn RFID
                showRFIDListDialog();
            }
            drawerLayout.closeDrawer(GravityCompat.START); // Đóng drawer sau khi chọn
            return true;
        });


        dbRef = FirebaseDatabase.getInstance().getReference();



        // ---Xử lí nút nhấn ---
        // --- LIGHT ---
        CardView cvLight = findViewById(R.id.cvLight);
        ImageView ivLight = findViewById(R.id.ivLight);

        cvLight.setOnClickListener(v -> {
            isLightOn = !isLightOn;

            // Bắt đầu hiệu ứng fade
            cvLight.animate()
                    .alpha(0.5f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        // Sau khi fade 0.5, đổi màu rồi fade lại 1.0
                        if (isLightOn) {

                            ivLight.setColorFilter(ContextCompat.getColor(this, R.color.yellow), PorterDuff.Mode.SRC_IN);
                            cvLight.setCardBackgroundColor(ContextCompat.getColor(this, R.color.light_on_bg));
                            Toast.makeText(MainActivity.this, "Đèn đã bật", Toast.LENGTH_SHORT).show();
                        } else {
                            ivLight.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
                            cvLight.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
                            Toast.makeText(MainActivity.this, "Đèn đã tắt", Toast.LENGTH_SHORT).show();
                        }

                        // Cập nhật lên Firebase
                        FirebaseDatabase.getInstance().getReference()
                                .child("Devices")
                                .child("Light")
                                .setValue(isLightOn);

                        cvLight.animate()
                                .alpha(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
        });


        // --- FAN ---
        CardView cvFan = findViewById(R.id.cvFan);
        ImageView ivFan = findViewById(R.id.ivFan);

        cvFan.setOnClickListener(v -> {
            isFanOn = !isFanOn;
            cvFan.animate()
                    .alpha(0.5f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        if (isFanOn) {
                            ivFan.setColorFilter(ContextCompat.getColor(this, R.color.xanh_than), PorterDuff.Mode.SRC_IN);
                            cvFan.setCardBackgroundColor(ContextCompat.getColor(this, R.color.fan_on_bg));
                            Toast.makeText(MainActivity.this, "Quạt đã bật", Toast.LENGTH_SHORT).show();
                        } else {
                            ivFan.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
                            cvFan.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
                            Toast.makeText(MainActivity.this, "Quạt đã tắt", Toast.LENGTH_SHORT).show();
                        }
                        // Cập nhật lên Firebase
                        FirebaseDatabase.getInstance().getReference()
                                .child("Devices")
                                .child("Fan")
                                .setValue(isFanOn);
                cvFan.animate()
                        .alpha(1f)
                        .setDuration(100)
                        .start();
            }).start();
        });

        // --- DOOR ---
        CardView cvLock = findViewById(R.id.cvLock);
        ImageView ivLock = findViewById(R.id.ivLock);

        cvLock.setOnClickListener(v -> {
            isDoorOn = !isDoorOn;
            cvLock.animate().alpha(0.5f).setDuration(100).withEndAction(() -> {
                if (isDoorOn) {
                    ivLock.setColorFilter(ContextCompat.getColor(this, R.color.green), PorterDuff.Mode.SRC_IN);
                    cvLock.setCardBackgroundColor(ContextCompat.getColor(this, R.color.door_on_bg));
                    Toast.makeText(MainActivity.this, "Cửa đã mở", Toast.LENGTH_SHORT).show();
                } else {
                    ivLock.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
                    cvLock.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
                    Toast.makeText(MainActivity.this, "Cửa đã đóng", Toast.LENGTH_SHORT).show();
                }
                // Cập nhật lên Firebase
                FirebaseDatabase.getInstance().getReference()
                        .child("Devices")
                        .child("Door")
                        .setValue(isDoorOn);
                cvLock.animate().alpha(1f).setDuration(100).start();
            }).start();
        });

        // --- AC ---
        CardView cvAir = findViewById(R.id.cvAir);
        ImageView ivAir = findViewById(R.id.ivAir);

        cvAir.setOnClickListener(v -> {
            isAcOn = !isAcOn;
            cvAir.animate().alpha(0.5f).setDuration(100).withEndAction(() -> {
                if (isAcOn) {
                    ivAir.setColorFilter(ContextCompat.getColor(this, R.color.light_blue), PorterDuff.Mode.SRC_IN);
                    cvAir.setCardBackgroundColor(ContextCompat.getColor(this, R.color.ac_on_bg));
                    Toast.makeText(MainActivity.this, "Máy lạnh đã bật", Toast.LENGTH_SHORT).show();
                } else {
                    ivAir.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
                    cvAir.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
                    Toast.makeText(MainActivity.this, "Máy lạnh đã tắt", Toast.LENGTH_SHORT).show();
                }
                // Cập nhật lên Firebase
                FirebaseDatabase.getInstance().getReference()
                        .child("Devices")
                        .child("Ac")
                        .setValue(isAcOn);
                cvAir.animate().alpha(1f).setDuration(100).start();
            }).start();
        });

        // --- TV ---
        CardView cvTv = findViewById(R.id.cvTv);
        ImageView ivTv = findViewById(R.id.ivTv);

        cvTv.setOnClickListener(v -> {
            isTvOn = !isTvOn;
            cvTv.animate().alpha(0.5f).setDuration(100).withEndAction(() -> {
                if (isTvOn) {
                    ivTv.setColorFilter(ContextCompat.getColor(this, R.color.orange), PorterDuff.Mode.SRC_IN);
                    cvTv.setCardBackgroundColor(ContextCompat.getColor(this, R.color.tv_on_bg));
                    Toast.makeText(MainActivity.this, "TV đã bật", Toast.LENGTH_SHORT).show();
                } else {
                    ivTv.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
                    cvTv.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
                    Toast.makeText(MainActivity.this, "TV đã tắt", Toast.LENGTH_SHORT).show();
                }
                // Cập nhật lên Firebase
                FirebaseDatabase.getInstance().getReference()
                        .child("Devices")
                        .child("TV")
                        .setValue(isTvOn);
                cvTv.animate().alpha(1f).setDuration(100).start();
            }).start();
        });

        // --- SENSOR ---
        CardView cvWifi = findViewById(R.id.cvWifi);
        ImageView ivWifi = findViewById(R.id.ivWifi);

        cvWifi.setOnClickListener(v -> {
            isSensorOn = !isSensorOn;
            cvWifi.animate().alpha(0.5f).setDuration(100).withEndAction(() -> {
                if (isSensorOn) {
                    ivWifi.setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.SRC_IN);
                    cvWifi.setCardBackgroundColor(ContextCompat.getColor(this, R.color.sensor_on_bg));
                    Toast.makeText(MainActivity.this, "Sensor đã bật", Toast.LENGTH_SHORT).show();
                } else {
                    ivWifi.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
                    cvWifi.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
                    Toast.makeText(MainActivity.this, "Sensor đã tắt", Toast.LENGTH_SHORT).show();
                }
                // Cập nhật lên Firebase
                FirebaseDatabase.getInstance().getReference()
                        .child("Devices")
                        .child("Sensors")
                        .setValue(isSensorOn);
                cvWifi.animate().alpha(1f).setDuration(100).start();
            }).start();
        });





        // --- đồng bộ trạng thái nút nhấn với Firebase ---
        // Light
        DatabaseReference deviceRef = FirebaseDatabase.getInstance().getReference("Devices");
        deviceRef.child("Light").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOn = snapshot.getValue(Boolean.class);
                if (isOn != null) {
                    isLightOn = isOn;  // cập nhật biến trạng thái
                    updateDeviceUI(cvLight, ivLight, isLightOn,
                            R.color.yellow, R.color.white,
                            R.color.light_on_bg, R.color.card_bg);

                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Fan
        deviceRef.child("Fan").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOn = snapshot.getValue(Boolean.class);
                if (isOn != null) {
                    isFanOn = isOn;  // cập nhật biến trạng thái
                    updateDeviceUI(cvFan,ivFan,isFanOn,
                            R.color.xanh_than, R.color.white,
                            R.color.fan_on_bg, R.color.card_bg);

                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Door
        deviceRef.child("Door").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOn = snapshot.getValue(Boolean.class);
                if (isOn != null) {
                    isDoorOn = isOn;  // cập nhật biến trạng thái
                    updateDeviceUI(cvLock, ivLock, isDoorOn,
                            R.color.green, R.color.white,
                            R.color.door_on_bg, R.color.card_bg);

                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // AC
        deviceRef.child("Ac").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOn = snapshot.getValue(Boolean.class);
                if (isOn != null) {
                    isAcOn = isOn;  // cập nhật biến trạng thái
                    updateDeviceUI(cvAir, ivAir, isAcOn,
                            R.color.blue, R.color.white,
                            R.color.ac_on_bg, R.color.card_bg);

                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // TV
        deviceRef.child("TV").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOn = snapshot.getValue(Boolean.class);
                if (isOn != null) {
                    isTvOn = isOn;  // cập nhật biến trạng thái
                    updateDeviceUI(cvTv, ivTv, isTvOn,
                            R.color.orange, R.color.white,
                            R.color.tv_on_bg, R.color.card_bg);

                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Sensor
        deviceRef.child("Sensors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOn = snapshot.getValue(Boolean.class);
                if (isOn != null) {
                    isSensorOn = isOn;  // cập nhật biến trạng thái
                    updateDeviceUI(cvWifi, ivWifi, isSensorOn,
                            R.color.purple, R.color.white,
                            R.color.sensor_on_bg, R.color.card_bg);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });



        ///////////////////////////////////////////////////////////////////////////////////////
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Trỏ tới nút "sensors" trong Firebase
        dhtRef = FirebaseDatabase.getInstance().getReference("sensors");
        // Đọc dữ liệu một lần hoặc lắng nghe thay đổi theo thời gian thực
        dhtRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Đọc nhiệt độ
                Double temp = snapshot.child("temperature").getValue(Double.class);
                // Đọc độ ẩm
                Double hum = snapshot.child("humidity").getValue(Double.class);

                if (temp != null && hum != null) {
                    tvDHT_Temp.setText(String.format("%.1f°c", temp));
                    tvDHT_Hum.setText(String.format("%.1f%%", hum));
                } else {
                    tvDHT_Temp.setText("Không có dữ liệu");
                    tvDHT_Hum.setText("Không có dữ liệu");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvDHT_Temp.setText("Lỗi đọc nhiệt độ");
                tvDHT_Hum.setText("Lỗi đọc độ ẩm");
            }
        });
        checkLocationPermission();
    }



    private void updateDeviceUI(CardView cardView, ImageView imageView, boolean isOn,int colorIconOn, int colorIconOff,int colorBgOn, int colorBgOff) {
        cardView.animate().alpha(0.5f).setDuration(100).withEndAction(() -> {
            if (isOn) {
                imageView.setColorFilter(ContextCompat.getColor(this, colorIconOn), PorterDuff.Mode.SRC_IN);
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, colorBgOn));
            } else {
                imageView.setColorFilter(ContextCompat.getColor(this, colorIconOff), PorterDuff.Mode.SRC_IN);
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, colorBgOff));
            }
            cardView.animate().alpha(1f).setDuration(100).start();
        }).start();
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
                        double lat =location.getLatitude();


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

                    tvWeather.setText(String.format(Locale.getDefault(), "%s, %.1f°C", description,temp ));
                    Glide.with(MainActivity.this)
                            .load(iconUrl)
                            .into(ivWeatherIcon);
                    Log.d("Weather", "lat=" + lat + ", lon=" + lon);

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


    private void showAddCardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_card, null);
        EditText etUserName = view.findViewById(R.id.etUserName);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        btnConfirm.setOnClickListener(v -> {
            String name = etUserName.getText().toString().trim();
            if (name.isEmpty()) {
                etUserName.setError("Không được để trống");
                return;
            }

            // Gửi lên Firebase
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("new_card");
            ref.child("name").setValue(name);
            ref.child("request").setValue(true);

            Toast.makeText(this, "🕒 Vui lòng quét thẻ trong 5 giây...", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
    }
    private void showRFIDListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_rfid_list, null);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerRFID);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        List<RFIDUser> rfidList = new ArrayList<>();
        RFIDAdapter adapter = new RFIDAdapter(this, rfidList);
        recyclerView.setAdapter(adapter);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("rfid");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rfidList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    rfidList.add(new RFIDUser(uid, name));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
