package com.hays.solitaire;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Insets;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.hays.solitaire.engine.GameEngine;
import com.hays.solitaire.model.GameState;
import com.hays.solitaire.storage.SaveManager;
import com.hays.solitaire.ui.SolitaireView;

public class MainActivity extends Activity {

    // INSTANCE VARIABLES
    private GameEngine gameEngine;
    private SolitaireView solitaireView;
    private SaveManager saveManager;
    private TextView scoreText;
    private TextView movesText;
    private Button undoButton;

    // ACTIVITY INITIALIZATION
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveManager = new SaveManager(this);
        initializeGame();
        setContentView(R.layout.activity_main);
        initializeInterface();
        configureWindowInsets();
        updateHeader();
    }

    // GAME INITIALIZATION
    private void initializeGame() {
        boolean savedGameExists =
                saveManager.hasSavedGame();
        gameEngine = saveManager.loadGame();
        if (gameEngine == null) {
            if (savedGameExists) {
                Toast.makeText(
                        this,
                        R.string.load_error,
                        Toast.LENGTH_LONG
                ).show();
            }
            gameEngine = new GameEngine(
                    new GameState()
            );
        }
    }

    // INTERFACE INITIALIZATION
    private void initializeInterface() {
        scoreText = findViewById(R.id.score_text);
        movesText = findViewById(R.id.moves_text);
        undoButton = findViewById(R.id.undo_button);
        Button menuButton =
                findViewById(R.id.menu_button);
        FrameLayout boardContainer =
                findViewById(R.id.board_container);
        solitaireView = new SolitaireView(
                this,
                gameEngine
        );
        boardContainer.addView(
                solitaireView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
        solitaireView.setOnGameChangedListener(
                this::onGameChanged
        );
        undoButton.setOnClickListener(
                view -> handleUndo()
        );
        menuButton.setOnClickListener(
                this::showMenu
        );
    }

    // WINDOW INSETS
    private void configureWindowInsets() {
        View root = findViewById(R.id.main);
        root.setOnApplyWindowInsetsListener(
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
        root.requestApplyInsets();
    }


    // HEADER
    private void updateHeader() {
        GameState state = gameEngine.getState();
        scoreText.setText(
                getString(
                        R.string.score_format,
                        state.getScore()
                )
        );
        movesText.setText(
                getString(
                        R.string.moves_format,
                        state.getMoves()
                )
        );
        undoButton.setText(
                getString(
                        R.string.undo_format,
                        state.getRemainingUndos()
                )
        );
        undoButton.setEnabled(
                gameEngine.canUndo()
        );
    }

    // GAME CHANGE HANDLING
    private void onGameChanged() {
        updateHeader();
        saveCurrentGame();
        if (gameEngine.getState().isGameWon()) {
            showWinDialog();
        }
    }

    // UNDO
    private void handleUndo() {
        if (gameEngine.undo()) {
            solitaireView.clearSelection();
            onGameChanged();
        }
    }

    // MENU
    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(
                this,
                anchor
        );
        menu.getMenu().add(
                0,
                1,
                0,
                R.string.new_game
        );
        menu.getMenu().add(
                0,
                2,
                1,
                R.string.restart
        );
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    confirmNewGame();
                    return true;
                case 2:
                    confirmRestart();
                    return true;
                default:
                    return false;
            }
        });
        menu.show();
    }

    // NEW GAME
    private void confirmNewGame() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.new_game_title)
                .setMessage(R.string.new_game_message)
                .setNegativeButton(
                        R.string.cancel,
                        null
                )
                .setPositiveButton(
                        R.string.ok,
                        (dialog, which) -> {

                            gameEngine.newGame();

                            solitaireView.clearSelection();

                            onGameChanged();
                        }
                )
                .show();
    }

    // RESTART
    private void confirmRestart() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.restart_title)
                .setMessage(R.string.restart_message)
                .setNegativeButton(
                        R.string.cancel,
                        null
                )
                .setPositiveButton(
                        R.string.ok,
                        (dialog, which) -> {

                            gameEngine.restart();

                            solitaireView.clearSelection();

                            onGameChanged();
                        }
                )
                .show();
    }

    // WIN NOTIFICATION
    private void showWinDialog() {
        GameState state = gameEngine.getState();
        new AlertDialog.Builder(this)
                .setTitle(R.string.win_title)
                .setMessage(
                        getString(
                                R.string.win_message,
                                state.getScore(),
                                state.getMoves()
                        )
                )
                .setPositiveButton(
                        R.string.ok,
                        null
                )
                .show();
    }

    // PERSISTENT STORAGE
    private void saveCurrentGame() {
        if (!saveManager.saveGame(gameEngine)) {
            Toast.makeText(
                    this,
                    R.string.save_error,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ACTIVITY LIFECYCLE
    @Override
    protected void onPause() {
        super.onPause();
        if (gameEngine != null) {
            saveCurrentGame();
        }
    }
}