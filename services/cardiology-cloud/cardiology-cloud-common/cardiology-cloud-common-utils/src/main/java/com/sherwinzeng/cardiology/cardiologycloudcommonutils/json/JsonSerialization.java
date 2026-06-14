package com.sherwinzeng.cardiology.cardiologycloudcommonutils.json;

import com.google.gson.Gson;

public class JsonSerialization {
    private final static Gson gson = new Gson();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
}
