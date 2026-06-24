package com.escom.app.util;

import java.util.*;

public class JSON {
    // Solo maneja JSON plano: {"key":"value"}
    public static Map<String, Object> parse(String json) {
        Map<String, Object> map = new HashMap<>();
        json = json.trim().substring(1, json.length() - 1); // quitar {}

        for (String pair : json.split(",")) {
            String[] kv = pair.split(":");
            String key = kv[0].trim().replace("\"", "");
            String val = kv[1].trim().replace("\"", "");
            map.put(key, val);
        }
        return map;
    }
}
