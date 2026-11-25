package com.hays.solitaire.engine;

// import methods from other classes to avoid rewriting code
import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.Deck;
import com.hays.solitaire.model.GameState;
import com.hays.solitaire.model.GameSnapshot;

import java.util.ArrayList;

public final class GameEngine {

    // DELEGATING METHODS
    // This allows the interface to interact with GameEngine instead
    // of communicating with various managers. Because GameState does
    // not work with UndoManager, GameEngine now coordinates classes
    public boolean canUndo() {
        return undoManager.canUndo();
    }
    public boolean undo() {
        return undoManager.undo();
    }
    // Game management operations
    public void restart() {
        state.restart();
        undoManager.clearHistory();
    }
    public void newGame() {
        state.newGame();
        undoManager.clearHistory();
    }

    // Receive existing game state, reflect changes, and
    // maintain instance of UndoManager
    private final GameState state;
    public int getUndoHistorySize() {
        return undoManager.getHistorySize();
    }
    private final UndoManager undoManager;

    public GameEngine(GameState state) {
        this.state = state;
        this.undoManager = new UndoManager(state);
    }

    // STOCK OPERATIONS
    // Every operation begins with boolean condition checks
    // True performs actions while False rejects actions,
    // leaving the game state unchanged

    // DRAW CARD
    // remove last card from stock, turn fac-up, append
    // to discard pile, transferring actual object
    // between collections
    public boolean drawCard() {
        if (state.isGameWon()
                || !MoveValidator.canDraw(state)) {
            return false;
        }
        GameSnapshot snapshot = captureSnapshot();
        Card card = removeTopCard(state.getStock());
        card.setFaceUp(true);
        state.getDiscard().add(card);
        // increment move count
        ScoreManager.incrementMoves(state);
        recordAction(snapshot, 0);
        return true;
    }

    // RECYCLE DISCARD
    // Removing a card from the end of the discard pile and
    // adding to the end of the stock is equivalent to turning
    // the discard pile over to preserve original draw order
    public boolean recycleDiscard() {
        if (state.isGameWon()
                || !MoveValidator.canRecycle(state)) {
            return false;
        }
        GameSnapshot snapshot = captureSnapshot();
        ArrayList<Card> discard = state.getDiscard();
        ArrayList<Card> stock = state.getStock();
        while (!discard.isEmpty()) {
            Card card = removeTopCard(discard);
            card.setFaceUp(false);
            stock.add(card);
        }
        // apply recycle penalty and increment move count
        ScoreManager.adjustScore(
                state,
                ScoreManager.RECYCLE_PENALTY
        );
        ScoreManager.incrementMoves(state);
        recordAction(snapshot, 0);
        return true;
    }

    // TABLEAU SEQUENCES
    // subList() method accepts fromIndex inclusively
    // and toIndex exclusively as a view of the original
    // collection so we don't lose access to cards in transit

    // TABLEAU TO TABLEAU
    public boolean moveTableauToTableau(
            int sourceColumn,
            int startIndex,
            int destinationColumn) {
        if (state.isGameWon()
                || !MoveValidator.canMoveTableauToTableau(
                state,
                sourceColumn,
                startIndex,
                destinationColumn)) {
            return false;
        }
        GameSnapshot snapshot = captureSnapshot();
        int reversibleScoreChange = 0;
        ArrayList<Card> source =
                state.getTableauColumn(sourceColumn);
        ArrayList<Card> destination =
                state.getTableauColumn(destinationColumn);
        destination.addAll(
                source.subList(startIndex, source.size())
        );
        source.subList(startIndex, source.size()).clear();
        // Adjust score and increment move count
        if (revealTopCard(source)) {
            reversibleScoreChange = ScoreManager.adjustScore(
                    state,
                    ScoreManager.REVEAL_POINTS
            );
        }
        ScoreManager.incrementMoves(state);
        recordAction(snapshot, reversibleScoreChange);
        return true;
    }

    // TABLEAU TO FOUNDATION
    public boolean moveTableauToFoundation(
            int sourceColumn,
            Card.Suit suit) {
        if (state.isGameWon()
                || !MoveValidator.canMoveTableauToFoundation(
                state,
                sourceColumn,
                suit)) {
            return false;
        }
        GameSnapshot snapshot = captureSnapshot();
        ArrayList<Card> source =
                state.getTableauColumn(sourceColumn);
        Card card = removeTopCard(source);
        state.getFoundation(suit).add(card);
        int reversibleScoreChange = ScoreManager.adjustScore(
                state,
                ScoreManager.FOUNDATION_POINTS
        );
        if (revealTopCard(source)) {
            reversibleScoreChange += ScoreManager.adjustScore(
                    state,
                    ScoreManager.REVEAL_POINTS
            );
        }
        ScoreManager.incrementMoves(state);
        recordAction(snapshot, reversibleScoreChange);
        checkForWin();
        return true;
    }

    // DISCARD TO TABLEAU
    public boolean moveDiscardToTableau(
            int destinationColumn) {
        if (state.isGameWon()
                || !MoveValidator.canMoveDiscardToTableau(
                state,
                destinationColumn)) {
            return false;
        }
        GameSnapshot snapshot = captureSnapshot();
        Card card = removeTopCard(state.getDiscard());
        state.getTableauColumn(destinationColumn).add(card);
        int reversibleScoreChange = ScoreManager.adjustScore(
                state,
                ScoreManager.DISCARD_TO_TABLEAU_POINTS
        );
        ScoreManager.incrementMoves(state);
        recordAction(snapshot, reversibleScoreChange);
        return true;
    }

    // DISCARD TO FOUNDATION
    public boolean moveDiscardToFoundation(
            Card.Suit suit) {
        if (state.isGameWon()
                || !MoveValidator.canMoveDiscardToFoundation(
                state,
                suit)) {
            return false;
        }
        GameSnapshot snapshot = captureSnapshot();
        Card card = removeTopCard(state.getDiscard());
        state.getFoundation(suit).add(card);
        int reversibleScoreChange = ScoreManager.adjustScore(
                state,
                ScoreManager.FOUNDATION_POINTS
        );
        ScoreManager.incrementMoves(state);
        recordAction(snapshot, reversibleScoreChange);
        checkForWin();
        return true;
    }

    // FOUNDATION TO TABLEAU
    public boolean moveFoundationToTableau(
            Card.Suit suit,
            int destinationColumn) {
        if (state.isGameWon()
                || !MoveValidator.canMoveFoundationToTableau(
                state,
                suit,
                destinationColumn)) {
            return false;
        }
        GameSnapshot snapshot = captureSnapshot();
        Card card = removeTopCard(
                state.getFoundation(suit)
        );
        state.getTableauColumn(destinationColumn).add(card);
        int reversibleScoreChange = ScoreManager.adjustScore(
                state,
                ScoreManager.FOUNDATION_REMOVAL_PENALTY
        );
        ScoreManager.incrementMoves(state);
        recordAction(snapshot, reversibleScoreChange);
        return true;
    }

    // HELPER METHODS

    // AUTOMATICALLY REVEAL EXPOSED CARDS
    private boolean revealTopCard(ArrayList<Card> column) {
        if (column.isEmpty()) {
            return false;
        }
        Card topCard = column.get(column.size() - 1);
        if (topCard.isFaceUp()) {
            return false;
        }
        topCard.setFaceUp(true);
        return true;
    }

    // REMOVE TOP CARD
    private Card removeTopCard(ArrayList<Card> pile) {
        return pile.remove(pile.size() - 1);
    }

    // CHECK WIN CONDITION
    private void checkForWin() {
        int foundationCount = 0;
        for (Card.Suit suit : Card.Suit.values()) {
            foundationCount +=
                    state.getFoundation(suit).size();
        }
        state.setGameWon(
                foundationCount == Deck.DECK_SIZE
        );
    }

    // GETTER
    public GameState getState() {
        return state;
    }

    // SNAPSHOT AND ACTION RECORDING HELPERS

    // If no undos remain, no deep copies of the game board created
    private GameSnapshot captureSnapshot() {
        if (state.getRemainingUndos() == 0) {
            return null;
        }
        return new GameSnapshot(state);
    }

    // Only valid snapshots are recorded. Centralized to avoid redundancy
    private void recordAction(
            GameSnapshot snapshot,
            int reversibleScoreChange) {
        if (snapshot != null) {
            undoManager.recordAction(
                    snapshot,
                    reversibleScoreChange
            );
        }
    }
}