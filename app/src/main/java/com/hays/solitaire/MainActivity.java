package com.hays.solitaire;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Bundle;
import android.view.WindowInsets;
import com.hays.solitaire.engine.GameEngine;
import com.hays.solitaire.model.GameState;
import com.hays.solitaire.ui.SolitaireView;

public class MainActivity extends Activity {

    // INSTANCE VARIABLES
    private GameEngine gameEngine;
    private SolitaireView solitaireView;

    // ACTIVITY INITIALIZATION
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameEngine = new GameEngine(new GameState());
        solitaireView = new SolitaireView(
                this,
                gameEngine.getState()
        );
        setContentView(solitaireView);
        solitaireView.setOnApplyWindowInsetsListener(
                (view, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(
                            WindowInsets.Type.systemBars()
                                    | WindowInsets.Type.displayCutout()
                    );
                    view.setPadding(
                            insets.left,
                            insets.top,
                            insets.right,
                            insets.bottom
                    );
                    return windowInsets;
                }
        );
        solitaireView.requestApplyInsets();
    }
}