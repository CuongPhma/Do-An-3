package com.example.android.model;
import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    @SerializedName("main")
    public Main main;

    @SerializedName("weather")
    public Weather[] weather;

    public class Main {
        @SerializedName("temp")
        public float temp;
    }

    public class Weather {
        @SerializedName("description")
        public String description;
    }
}
