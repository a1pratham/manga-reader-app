package com.example.manhwanest.sources;
import java.io.Serializable;

public class Chapter implements Serializable {

    private String id;
    private String number;

    public Chapter(String id, String number) {
        this.id = id;
        this.number = number;
    }

    public String getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }
}