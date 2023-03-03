package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;
import static android.content.ContentValues.TAG;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import edu.ucsd.cse110.sharednotes.model.Note;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NoteAPI {
    // TODO: Implement the API using OkHttp!
    // TODO: - getNote (maybe getNoteAsync)
    // TODO: - putNote (don't need putNotAsync, probably)
    // TODO: Read the docs: https://square.github.io/okhttp/
    // TODO: Read the docs: https://sharednotes.goto.ucsd.edu/docs

    private volatile static NoteAPI instance = null;

    private OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    @AnyThread
    public Note getNote(String title){
        String encodedTitle = title.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + encodedTitle)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            return Note.fromJSON(body);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void putNote(Note note) {
        String encodedTitle = note.title.replace(" ", "%20");
        String url = "https://sharednotes.goto.ucsd.edu/notes/gl";// + encodedTitle;

        // Build the request body as JSON.
        String json = note.toJSON();

        MediaType JSON = MediaType.get("application/json; charset=utf-8");


        // Build the request with the JSON body and appropriate headers.
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        // Use OkHttp3 to send the request and get the response.
        try {
            Response response = client.newCall(request).execute();
            System.out.println("goling");

            // Handle any errors that occur during the PUT.
            if (!response.isSuccessful()) {
                System.out.println("boling");

                Log.e(TAG, "Failed to update remote note: " + response.code() + " " + response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     *
     * This method should can be called on a background thread (Android
     * disallows network requests on the main thread).
     */
    @WorkerThread
    public String echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedMsg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + encodedMsg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @AnyThread
    public Future<String> echoAsync(String msg) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> echo(msg));

        // We can use future.get(1, SECONDS) to wait for the result.
        return future;
    }

    @AnyThread
    public void putNoteAsync(Note note) {
        // Define a new executor with a single thread to handle the PUT request.
        Executor executor = Executors.newSingleThreadExecutor();

        // Submit a new Runnable task to the executor that executes the PUT request.
        executor.execute(() -> {
            // Copy the existing putNote implementation here.
            String encodedTitle = note.title.replace(" ", "%20");
            String url = "https://sharednotes.goto.ucsd.edu/notes/" + encodedTitle;

            // Build the request body as JSON.
            String json = note.toJSON();

            // Build the request with the JSON body and appropriate headers.
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .build();

            // Use OkHttp3 to send the request and get the response.
            try {
                Response response = client.newCall(request).execute();
                System.out.println("goling");

                // Handle any errors that occur during the PUT.
                if (!response.isSuccessful()) {
                    System.out.println("boling");

                    Log.e(TAG, "Failed to update remote note: " + response.code() + " " + response.message());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
