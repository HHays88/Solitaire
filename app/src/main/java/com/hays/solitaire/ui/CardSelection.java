package com.hays.solitaire.ui;

import com.hays.solitaire.model.Card;

public final class CardSelection {

    // SELECTION TYPES
    public enum Type {
        NONE,
        DISCARD,
        FOUNDATION,
        TABLEAU
    }

    // INSTANCE VARIABLES
    private Type type = Type.NONE;
    private Card.Suit foundationSuit;
    private int tableauColumn = -1;
    private int cardIndex = -1;

    // SELECTION METHODS
    public void selectDiscard() {
        clear();
        type = Type.DISCARD;
    }

    public void selectFoundation(Card.Suit suit) {
        clear();
        type = Type.FOUNDATION;
        foundationSuit = suit;
    }

    public void selectTableau(
            int column,
            int cardIndex) {
        clear();
        type = Type.TABLEAU;
        tableauColumn = column;
        this.cardIndex = cardIndex;
    }

    // CLEAR SELECTION
    public void clear() {
        type = Type.NONE;
        foundationSuit = null;
        tableauColumn = -1;
        cardIndex = -1;
    }

    // SELECTION CHECKS
    public boolean hasSelection() {
        return type != Type.NONE;
    }

    public boolean isDiscardSelected() {
        return type == Type.DISCARD;
    }

    public boolean isFoundationSelected(
            Card.Suit suit) {
        return type == Type.FOUNDATION
                && foundationSuit == suit;
    }

    public boolean isTableauCardSelected(
            int column,
            int index) {
        return type == Type.TABLEAU
                && tableauColumn == column
                && index >= cardIndex;
    }

    // GETTERS
    public Type getType() {
        return type;
    }

    public Card.Suit getFoundationSuit() {
        return foundationSuit;
    }

    public int getTableauColumn() {
        return tableauColumn;
    }

    public int getCardIndex() {
        return cardIndex;
    }
}