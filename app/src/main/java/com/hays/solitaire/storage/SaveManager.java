package com.hays.solitaire.storage;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;
import com.hays.solitaire.engine.GameEngine;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class SaveManager {
    private static final String SAVE_FILE_NAME =
            "solitaire_save.json";
    private static final String TAG = "SaveManager";
    private final AtomicFile saveFile;

    // CONSTRUCTORS
    public SaveManager(Context context) {
        this(context.getFilesDir());
    }
    SaveManager(File directory) {
        File file = new File(
                directory,
                SAVE_FILE_NAME
        );
        saveFile = new AtomicFile(file);
    }

    // SAVE GAME
    public boolean saveGame(GameEngine engine) {
        FileOutputStream output = null;
        try {
            // Serialize game before beginning file operations
            JSONObject json =
                    GameStateSerializer.serializeGame(engine);
            byte[] data = json.toString()
                    .getBytes(StandardCharsets.UTF_8);
            // Begin atomic write
            output = saveFile.startWrite();
            output.write(data);
            // Commit successful write
            saveFile.finishWrite(output);
            return true;
        } catch (IOException | JSONException e) {
            // Abandon incomplete write
            if (output != null) {
                saveFile.failWrite(output);
            }
            Log.e(TAG, "Unable to save game.", e);
            return false;
        }
    }

    // LOAD GAME
    public GameEngine loadGame() {
        if (!hasSavedGame()) {
            return null;
        }
        try {
            // Read saved file contents
            byte[] data = saveFile.readFully();
            // Convert saved bytes into JSON text
            String jsonString = new String(
                    data,
                    StandardCharsets.UTF_8
            );
            JSONObject json = new JSONObject(jsonString);
            // Reconstruct saved game
            return GameStateSerializer.deserializeGame(json);
        } catch (IOException
                 | JSONException
                 | IllegalArgumentException e) {
            Log.e(TAG, "Unable to load game.", e);
            return null;
        }
    }


    // SAVE FILE STATUS
    public boolean hasSavedGame() {
        return saveFile.getBaseFile().exists();
    }
}