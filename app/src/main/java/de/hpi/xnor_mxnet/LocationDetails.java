package de.hpi.xnor_mxnet;

import java.util.HashMap;
import java.util.Map;

class LocationDetails {

    public String title;
    String descritpion;
    String date;

    LocationDetails(String title, String descritpion, String date) {
        this.title = title;
        this.descritpion = descritpion;
        this.date = date;
    }

    Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("description", descritpion);
        result.put("date", date);

        return result;
    }
}
