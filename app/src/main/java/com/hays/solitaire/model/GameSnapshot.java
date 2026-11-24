package com.hays.solitaire.model;

// Capture independent copies of the game before a move is made to allow
// restoring card positions with Undo but without affecting unrelated values.
// Everything created here is a separate deep copy of the original GameState

import java.util.ArrayList;
import java.util.EnumMap;

// Importing copyCards() method after making it package-private to share method
import static com.hays.solitaire.model.GameState.copyCards;

public class GameSnapshot {
    private final ArrayList<Card> stock;
    private final ArrayList<Card> discard;

    private final EnumMap<Card.Suit, ArrayList<Card>> foundations;
    private final ArrayList<ArrayList<Card>> tableau;

    private final int score;
    private final boolean gameWon;

    // Receive existing game to capture conditions. Creating a snapshot
    // will only require declaring
    // GameSnapshot snapshot = new GameSnapshot(gameState);
    public GameSnapshot(GameState state) {
        stock = copyCards(state.getStock());
        discard = copyCards(state.getDiscard());
        score = state.getScore();
        gameWon = state.isGameWon();

        // Foundation copying
        foundations = new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : Card.Suit.values()) {
            foundations.put(
                    suit,
                    copyCards(state.getFoundation(suit))
            );
        }

        // Tableau copying
        tableau = new ArrayList<>(GameState.TABLEAU_COLUMNS);
        for (ArrayList<Card> column : state.getTableau()) {
            tableau.add(copyCards(column));
        }
    }

    // Restore captured board to existing GameState without creating new
    // game or shuffling the deck or calling restart() to replace a pile
    // with copies of cards in the snapshot.
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
            copy.put(suit, copyCards(foundations.get(suit)));
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

    // NO SETTERS, THIS IS ONLY COPYING INFORMATION NOT MAKING CHANGES
}