package com.hays.solitaire.engine;

import com.hays.solitaire.model.GameState;
import com.hays.solitaire.model.GameSnapshot;
import java.util.ArrayDeque;
import java.util.ArrayList;

// Receives reference to existing GameState object
public final class UndoManager {
    private final GameState state;
    private final ArrayDeque<UndoEntry> history;

    //Constructor
    public UndoManager(GameState state) {
        this.state = state;
        history = new ArrayDeque<>(GameState.MAX_UNDOS);
    }

    // UNDO ENTRY
    // Stores independent snapshots and reversible score adjustments
    // associated with actions
    public static final class UndoEntry {
        private final GameSnapshot snapshot;
        // Undoing a recycle does not undo the penalty.
        // I might fix this later.
        private final int reversibleScoreChange;
        public UndoEntry(
                GameSnapshot snapshot,
                int reversibleScoreChange) {
            if (snapshot == null) {
                throw new IllegalArgumentException(
                        "Undo snapshot cannot be null."
                );
            }
            this.snapshot = snapshot;
            this.reversibleScoreChange = reversibleScoreChange;
        }
        public GameSnapshot getSnapshot() {
            return snapshot;
        }
        public int getReversibleScoreChange() {
            return reversibleScoreChange;
        }
    }

    // RECORD GAMEPLAY ACTIONS
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

    // UNDO OPERATIONS
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

    // HISTORY MANAGEMENT
    public void clearHistory() {
        history.clear();
    }

    public int getHistorySize() {
        return history.size();
    }

    // PERSISTENT STORAGE

    // Export newest-to-oldest without exposing ArrayDeque so exported
    // list can be modified without affecting internal history and we
    // don't need more deep copies in every exported snapshot
    public ArrayList<UndoEntry> exportHistory() {
        return new ArrayList<>(history);
    }

    // Reconstruct history from saved entries maintaining
    // newest-to-oldest order.
    public void importHistory(
            ArrayList<UndoEntry> entries) {
        if (entries == null) {
            throw new IllegalArgumentException(
                    "Undo history cannot be null."
            );
        }
        if (entries.size() > GameState.MAX_UNDOS) {
            throw new IllegalArgumentException(
                    "Undo history exceeds maximum allowed undos."
            );
        }
        if (entries.size() > state.getRemainingUndos()) {
            throw new IllegalArgumentException(
                    "Undo history exceeds remaining undos."
            );
        }
        for (UndoEntry entry : entries) {
            if (entry == null) {
                throw new IllegalArgumentException(
                        "Undo history contains a null entry."
                );
            }
        }

        // Replace existing history only after validation succeeds.
        history.clear();
        history.addAll(entries);
    }
}