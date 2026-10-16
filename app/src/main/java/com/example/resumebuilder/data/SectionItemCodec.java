package com.example.resumebuilder.data;

import com.example.resumebuilder.model.SectionItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

/** Converts the field values of a section entry to and from the JSON text stored in SQLite. */
public final class SectionItemCodec {

    private SectionItemCodec() {
    }

    public static String toJson(SectionItem item) {
        JSONObject json = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : item.getValues().entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Could not encode section item", e);
        }
        return json.toString();
    }

    /** Fills the item from stored JSON. A damaged row is skipped and leaves the item empty. */
    public static void fillFromJson(SectionItem item, String data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        try {
            JSONObject json = new JSONObject(data);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                item.put(key, json.optString(key, ""));
            }
        } catch (JSONException ignored) {
            // Corrupt row: keep the app running and show an empty entry instead of crashing.
        }
    }
}