package animewatchlistmanager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AnimeAPI {

    /**
     * Makes a GET request to the given URL and returns the response as a String.
     * Returns empty JSON object "{}" if any error occurs.
     */
    public static String getRequest(String urlString) {
    HttpURLConnection connection = null;
    BufferedReader reader = null;
    StringBuilder response = new StringBuilder();

    try {
        URL url = new URL(urlString);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int status = connection.getResponseCode();

        if (status != HttpURLConnection.HTTP_OK) {
            System.out.println("⚠ HTTP Error: " + status + " for URL: " + urlString);
            return "{}";
        }

        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        String respStr = response.toString().trim();
        // If it doesn’t start with { or [, return empty JSON
        if (!(respStr.startsWith("{") || respStr.startsWith("["))) {
            System.out.println("⚠ Response is not JSON, returning empty JSON: " + respStr);
            return "{}";
        }

        return respStr;

    } catch (Exception e) {
        System.out.println("❌ Failed to fetch URL: " + urlString);
        e.printStackTrace();
        return "{}";
    } finally {
        try {
            if (reader != null) reader.close();
            if (connection != null) connection.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    

    // Optional: You can keep your old helper methods like searchAnime(), getAnimeInfo(), etc.
}
    }
}
