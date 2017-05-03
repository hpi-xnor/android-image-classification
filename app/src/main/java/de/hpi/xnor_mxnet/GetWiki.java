package de.hpi.xnor_mxnet;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

//Json parser for wikipedia
class GetWiki extends AsyncTask<String, Object, LocationDetails> {

    private static final String WIK = "WikiActivity";

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected LocationDetails doInBackground(String... strings) {
        String urlTitle = strings[0];
        String url = "https://en.wikipedia.org/w/api.php?format=json&action=query&redirects&exintro=&explaintext=&prop=extracts&titles=" + urlTitle;
        HttpHandler handler = new HttpHandler();

        // Making a request to url and getting response
        String jsonStr = handler.makeServiceCall(url);

        if (jsonStr != null) {
            try {
                JSONObject jsonObj = new JSONObject(jsonStr);
                JSONObject pages = jsonObj.getJSONObject("query").getJSONObject("pages");
                Iterator<String> keys = pages.keys();
                String pageId= keys.next();
                JSONObject page = pages.getJSONObject(pageId);

                String title = page.getString("title");
                String extract = page.getString("extract");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String date = sdf.format(new Date());

                return new LocationDetails(title,extract,date);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(LocationDetails result) {
        super.onPostExecute(result);
        Log.i(WIK,"good");
    }
}