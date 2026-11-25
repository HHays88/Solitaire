package com.hays.solitaire.model;

// Capture independent copies of the game before a move is made to allow
// restoring card positions with Undo but without affecting unrelated values.
// Everything created here is a separate deep copy of the original GameState.

import java.util.ArrayList;
import java.util.EnumMap;

// Importing copyCards() method after making it package-private to share method
import static com.hays.solitaire.model.GameState.copyCards;

public final class GameSnapshot {

    // Store independent copies of card collections
    private final ArrayList<Card> stock;
    private final ArrayList<Card> discard;
    private final EnumMap<Card.Suit, ArrayList<Card>> foundations;
    private final ArrayList<ArrayList<Card>> tableau;

    // Store captured game values
    private final int score;
    private final boolean gameWon;

    // CONSTRUCTORS
    // Receive existing game to capture conditions. Creating a snapshot
    // will only require declaring:
    // GameSnapshot snapshot = new GameSnapshot(gameState);
    public GameSnapshot(GameState state) {
        this(
                state.getStock(),
                state.getDiscard(),
                state.getFoundations(),
                state.getTableau(),
                state.getScore(),
                state.isGameWon()
        );
    }

    // Reconstruct snapshot from saved data or initialize a snapshot
    // using information received from the first constructor
    public GameSnapshot(
            ArrayList<Card> stock,
            ArrayList<Card> discard,
            EnumMap<Card.Suit, ArrayList<Card>> foundations,
            ArrayList<ArrayList<Card>> tableau,
            int score,
            boolean gameWon) {
        // Validate collection structure before copying
        validateCollections(
                stock,
                discard,
                foundations,
                tableau
        );
        // Reject invalid snapshot scores
        if (score < 0) {
            throw new IllegalArgumentException(
                    "Snapshot score cannot be negative."
            );
        }
        // Create independent copies of stock and discard
        this.stock = copyCards(stock);
        this.discard = copyCards(discard);
        // Create independent copies of each foundation pile
        this.foundations = new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : Card.Suit.values()) {
            this.foundations.put(
                    suit,
                    copyCards(foundations.get(suit))
            );
        }
        // Create independent copies of each tableau column
        this.tableau = new ArrayList<>(
                GameState.TABLEAU_COLUMNS
        );
        for (ArrayList<Card> column : tableau) {
            this.tableau.add(copyCards(column));
        }
        // Capture game values
        this.score = score;
        this.gameWon = gameWon;
    }

    // VALIDATION
    // Private helper method to validate snapshot collection structure
    private static void validateCollections(
            ArrayList<Card> stock,
            ArrayList<Card> discard,
            EnumMap<Card.Suit, ArrayList<Card>> foundations,
            ArrayList<ArrayList<Card>> tableau) {
        if (stock == null
                || discard == null
                || foundations == null
                || tableau == null) {
            throw new IllegalArgumentException(
                    "Snapshot collections cannot be null."
            );
        }
        if (foundations.size() != Card.Suit.values().length) {
            throw new IllegalArgumentException(
                    "Snapshot must contain four foundations."
            );
        }
        for (Card.Suit suit : Card.Suit.values()) {
            if (foundations.get(suit) == null) {
                throw new IllegalArgumentException(
                        "Snapshot is missing a foundation pile."
                );
            }
        }
        if (tableau.size() != GameState.TABLEAU_COLUMNS) {
            throw new IllegalArgumentException(
                    "Snapshot must contain seven tableau columns."
            );
        }
        for (ArrayList<Card> column : tableau) {
            if (column == null) {
                throw new IllegalArgumentException(
                        "Tableau columns cannot be null."
                );
            }
        }
    }

    // SNAPSHOT RESTORATION
    // Restore captured board to existing GameState without creating new
    // game or shuffling the deck or calling restart().
    // Replace the contents of each pile with copies of cards in the snapshot.
    public void restore(GameState state) {
        restoreCards(state.getStock(), stock);
        restoreCards(state.getDiscard(), discard);
        for (Card.Suit suit : Card.Suit.values()) {
            restoreCards(
                    state.getFoundation(suit),
                    foundations.get(suit)
            );
        }
        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {
            restoreCards(
                    state.getTableauColumn(i),
                    tableau.get(i)
            );
        }
        state.setGameWon(gameWon);
    }

    // Clear destination pile and populate it with independent copies
    // of the cards stored in the snapshot
    private static void restoreCards(
            ArrayList<Card> destination,
            ArrayList<Card> source) {
        destination.clear();
        destination.addAll(copyCards(source));
    }

    // CARD COLLECTION GETTERS
    public ArrayList<Card> getStock() {
        return copyCards(stock);
    }
    public ArrayList<Card> getDiscard() {
        return copyCards(discard);
    }
    public EnumMap<Card.Suit, ArrayList<Card>> getFoundations() {
        EnumMap<Card.Suit, ArrayList<Card>> copy =
                new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : Card.Suit.values()) {
            copy.put(
                    suit,
                    copyCards(foundations.get(suit))
            );
        }
        return copy;
    }
    public ArrayList<ArrayList<Card>> getTableau() {
        ArrayList<ArrayList<Card>> copy =
                new ArrayList<>(GameState.TABLEAU_COLUMNS);
        for (ArrayList<Card> column : tableau) {
            copy.add(copyCards(column));
        }
        return copy;
    }

    // GAME VALUE GETTERS
    public int getScore() {
        return score;
    }
    public boolean isGameWon() {
        return gameWon;
    }

    // NO SETTERS: A SNAPSHOT CAPTURES INFORMATION WITHOUT
    // ALLOWING ITS STORED VALUES TO BE MODIFIED.
}