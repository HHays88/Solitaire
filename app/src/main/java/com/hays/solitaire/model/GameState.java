package com.hays.solitaire.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

public class GameState {

    // Explicitly define number of tableaus and undo restrictions
    public static final int TABLEAU_COLUMNS = 7;
    public static final int MAX_UNDOS = 3;

    // Array lists save the original deck state, stock, and discard piles
    private final ArrayList<Card> originalDeck;
    private final ArrayList<Card> stock;
    private final ArrayList<Card> discard;

    // EnumMap creates one foundation per suit
    private final EnumMap<Card.Suit, ArrayList<Card>> foundations;

    // Outer collection identifies column, inner collection identifies
    // stacked cards in the pile
    private final ArrayList<ArrayList<Card>> tableau;

    // Initialize variables for Score, Move Count,
    // available undos, and detecting if game complete
    private int score;
    private int moves;
    private int remainingUndos;
    private boolean gameWon;

    // Private helper method to validate the deck isn't corrupted
    private static void validateOriginalDeck(
            ArrayList<Card> deck) {
        if (deck == null || deck.size() != Deck.DECK_SIZE) {
            throw new IllegalArgumentException(
                    "Original deck must contain 52 cards."
            );
        }
        Set<String> uniqueCards = new HashSet<>();
        for (Card card : deck) {
            if (card == null
                    || card.getSuit() == null
                    || card.getRank() == null) {
                throw new IllegalArgumentException(
                        "Original deck contains an invalid card."
                );
            }
            String cardID = card.getSuit().name()
                    + ":" + card.getRank().name();
            if (!uniqueCards.add(cardID)) {
                throw new IllegalArgumentException(
                        "Original deck contains duplicate cards."
                );
            }
            if (card.isFaceUp()) {
                throw new IllegalArgumentException(
                        "Original deck cards must be face down."
                );
            }
        }
    }

    // create initial game state with deck, stock, discard, foundations,
    // tableau columns, and map suits to foundation piles
    public GameState() {
        this(Deck.createShuffledDeck());
    }
    public GameState(ArrayList<Card> originalDeck) {
        validateOriginalDeck(originalDeck);
        this.originalDeck = new ArrayList<>(Deck.DECK_SIZE);
        stock = new ArrayList<>(Deck.DECK_SIZE);
        discard = new ArrayList<>(Deck.DECK_SIZE);
        foundations = new EnumMap<>(Card.Suit.class);
        tableau = new ArrayList<>(TABLEAU_COLUMNS);
        for (Card.Suit suit : Card.Suit.values()) {
            foundations.put(suit, new ArrayList<>(13));
        }
        for (int i = 0; i < TABLEAU_COLUMNS; i++) {
            tableau.add(new ArrayList<>());
        }
        this.originalDeck.addAll(copyCards(originalDeck));
        restart();
    }

    // newGame() is an extension of restart() to replace the deck
    // before resetting the game
    public void newGame(){
        // remove previous deck
        originalDeck.clear();
        // store newly created deck
        originalDeck.addAll(Deck.createShuffledDeck());

        // Apply same logic to restart method
        restart();

        // once these instructions execute, newGame finishes
    }

    public void restart() {
        clearPiles();
        resetGameValues();

        // Deal 28 cards across 7 tableaus, set top card faceup

        // Preserve deal into originalDeck so that changing a card in the
        // working deck does not change corresponding cards in originalDeck
        // CopyCards() defined later in this file
        ArrayList<Card> deck = copyCards(originalDeck);

        // Track position in deck to determine what card gets dealt next
        int deckIndex = 0;

        // Outer loop controls wich row is dealt, inner loop controls which
        // columns receive cards in a row
        for (int row = 0; row < TABLEAU_COLUMNS; row++) {

            // Start one column farther to right with each new row to deal
            // different number of cards per pile (0=1, 1=2, etc)
            for (int column = row;
                column < TABLEAU_COLUMNS;
                column++) {

                // Retrieve next card using post-increment
                Card card = deck.get(deckIndex++);

                // Card is faceUp if row number equals column number
                card.setFaceUp(row == column);

                // Retrieves column number and appends cards to that column
                tableau.get(column).add(card);
            }
        }

        // Work backward through deck from index 51 to 28 to ensure cards
        // are drawn in the original shuffle order then decrease index
        // by one each iteration
        for (int i = deck.size() - 1; i >= deckIndex; i--) {

            // Retrieve card at index and assign to Card variable without
            // removing from deck
            Card card = deck.get(i);

            // All cards are face down
            card.setFaceUp(false);

            // Append card to end of the ArrayList using index
            stock.add(card);
        }
    }

    // Clear all piles- stock, discard, foundations, and tableaus
    private void clearPiles() {
        stock.clear();
        discard.clear();

        for (ArrayList<Card> pile : foundations.values()) {
            pile.clear();
        }

        for (ArrayList<Card> column : tableau) {
            column.clear();
        }
    }

    // Reset score, move count, undo allocation, and declare
    // win state to false
    private void resetGameValues() {
        score = 0;
        moves = 0;
        remainingUndos = MAX_UNDOS;
        gameWon = false;
    }

    // Define copyCards() method to do a deep copy so that changing the cards in
    // the original deck does not affect this list to support Reset functionality
    static ArrayList<Card>copyCards(
            ArrayList<Card> cards) {
        // Allocate array capacity for cards
        ArrayList<Card> copy = new ArrayList<>(cards.size());

        // Iterates through every card in the list and copies it to new list
        for (Card card : cards) {
            copy.add(card.copy());
        }
        // returns completed list to any method calling copyCards()
        return copy;
    }

    // CARD COLLECTION GETTERS
    // No card collection setters because I only want GameState
    // to control adding, removing, and moving cards in the game.
    public ArrayList<Card> getOriginalDeck() {
        return copyCards(originalDeck);
    }
    public ArrayList<Card> getStock() {
        return stock;
    }
    public ArrayList<Card> getDiscard() {
        return discard;
    }
    public EnumMap<Card.Suit, ArrayList<Card>> getFoundations() {
        return foundations;
    }
    public ArrayList<Card> getFoundation(Card.Suit suit) {
        return foundations.get(suit);
    }
    public ArrayList<ArrayList<Card>> getTableau() {
        return tableau;
    }
    public ArrayList<Card> getTableauColumn(int column) {
        return tableau.get(column);
    }

    // GAME VALUE GETTERS
    public int getScore() {
        return score;
    }
    public int getMoves() {
        return moves;
    }
    public int getRemainingUndos() {
        return remainingUndos;
    }
    public boolean isGameWon() {
        return gameWon;
    }

    // GAME VALUE SETTERS
    public void setScore(int score) {
        this.score = Math.max(0, score);
    }
    public void setMoves(int moves) {
        this.moves = Math.max(0, moves);
    }
    public void setRemainingUndos(int remainingUndos) {
        this.remainingUndos = Math.max(
                0, Math.min(MAX_UNDOS, remainingUndos)
        );
    }
    public void setGameWon(boolean gameWon) {
        this.gameWon = gameWon;
    }
}
