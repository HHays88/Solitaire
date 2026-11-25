package com.hays.solitaire.engine;

import com.hays.solitaire.model.GameState;
import com.hays.solitaire.model.GameSnapshot;
import java.util.ArrayDeque;

// Receives reference to existing GameState object
public final class UndoManager {
    private final GameState state;
    private final ArrayDeque<UndoEntry> history;
    public UndoManager(GameState state) {
        this.state = state;
        history = new ArrayDeque<>(GameState.MAX_UNDOS);
    }

    // UNDO HISTORY ENTRY
    // Nesting this class because it supports UndoManager
    private static final class UndoEntry {
        private final GameSnapshot snapshot;
        // GameSnapshot stores the score at capture but that could restore
        // previous undo or recycle penalties, so this stores reversible
        // adjustments associated with Undo and Recycle. However, undoing
        // a recycle does not undo the penalty. I might fix this later.
        private final int reversibleScoreChange;
        private UndoEntry(
                GameSnapshot snapshot,
                int reversibleScoreChange) {
            this.snapshot = snapshot;
            this.reversibleScoreChange = reversibleScoreChange;
        }
    }

    // RECORD GAMEPLAY ACTION
    // Checks if any undos remain; if not it does not record history,
    // otherwise UndoEntry is created and pushed to the stack. This
    // method only stores a number of snapshots equal to the number
    // of undos available.
    public void recordAction(
            GameSnapshot snapshot,
            int reversibleScoreChange) {
        if (state.getRemainingUndos() == 0) {
            return;
        }
        history.push(
                new UndoEntry(
                        snapshot,
                        reversibleScoreChange
                )
        );
        while (history.size() > state.getRemainingUndos()) {
            history.removeLast();
        }
    }

    // UNDO VALIDATION
    public boolean canUndo() {
        return !state.isGameWon()
                && state.getRemainingUndos() > 0
                && !history.isEmpty();
    }

    // EXECUTE UNDO
    // Disables the button if canUndo() returns false. This method validates
    // an undo can be performed, pops the history entry, restores the most
    // recent snapshot, reverses any scoring the previous move earned, applies
    // the undo penalty, increments the move count, and reduces the number of
    // available undos
    public boolean undo() {
        if (!canUndo()) {
            return false;
        }
        UndoEntry entry = history.pop();
        entry.snapshot.restore(state);
        ScoreManager.adjustScore(
                state,
                -entry.reversibleScoreChange
        );
        ScoreManager.adjustScore(
                state,
                ScoreManager.UNDO_PENALTY
        );
        ScoreManager.incrementMoves(state);
        state.setRemainingUndos(
                state.getRemainingUndos() - 1
        );
        return true;
    }

    // CLEAR UNDO HISTORY
    // Only called when the game is restarted or a new game is dealt.
    public void clearHistory() {
        history.clear();
    }

    // GETTER
    // Inspect number of recorded actions without exposing the collection
    public int getHistorySize() {
        return history.size();
    }
}