package com.hays.solitaire.engine;

import com.hays.solitaire.model.GameState;

public final class ScoreManager {

    // SCORING CONSTANTS
    // Centralized declarations to be called across app
    public static final int FOUNDATION_POINTS = 10;
    public static final int DISCARD_TO_TABLEAU_POINTS = 5;
    public static final int REVEAL_POINTS = 5;
    public static final int FOUNDATION_REMOVAL_PENALTY = -10;
    public static final int RECYCLE_PENALTY = -20;
    public static final int UNDO_PENALTY = -10;

    // Utility class, does not maintain game state or attributes
    private ScoreManager() {
    }

    // SCORE ADJUSTMENTS
    public static int adjustScore(
            GameState state,
            int points) {
        int previousScore = state.getScore();
        state.setScore(previousScore + points);
        return state.getScore() - previousScore;
    }

    // MOVE COUNTING
    // Automatic reveals do not call this method as it is
    // counted with the action that revealed the card as one move
    public static void incrementMoves(GameState state) {
        state.setMoves(state.getMoves() + 1);
    }
}