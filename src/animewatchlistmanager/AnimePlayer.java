package animewatchlistmanager;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

public class AnimePlayer extends Application {

    private static String videoUrl;

    // Call this to launch player
    public static void playVideo(String sourcesJson) {
        try {
            // Parse JSON returned from Zoro /watch endpoint
            JSONObject obj = new JSONObject(sourcesJson);
            JSONArray sources = obj.getJSONArray("sources");

            if (sources.length() == 0) {
                System.out.println("❌ No streaming sources found!");
                return;
            }

            // Take the first available source
            videoUrl = sources.getJSONObject(0).getString("url");
            System.out.println("▶️ Playing URL: " + videoUrl);

            // Launch JavaFX player in a new thread
            new Thread(() -> Application.launch(AnimePlayer.class)).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        webView.getEngine().load(videoUrl);

        stage.setScene(new Scene(webView, 1280, 720));
        stage.setTitle("Anime Player");
        stage.show();
    }
}
