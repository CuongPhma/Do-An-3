package com.example.android.model;

public class RFIDUser {
    public String name;
    public String uid;

    public RFIDUser() {}

    public RFIDUser(String uid, String name) {
        this.uid = uid;
        this.name = name;
    }
}
