package com.hays.solitaire.engine;

import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.GameState;
import java.util.ArrayList;

// Does not store attributes or maintain own state, this
// file receives information to evaluate it and returns
// a boolean if the move is valid or not.
public final class MoveValidator {
    private MoveValidator() {
    }

    // Tableau placement validation
    public static boolean canPlaceOnTableau(
            Card card,
            ArrayList<Card> destination) {
        if (card == null || !card.isFaceUp()) {
            return false;
        }
        if (destination.isEmpty()) {
            return card.getRank() == Card.Rank.KING;
        }
        Card topCard = destination.get(
                destination.size() - 1
        );
        return topCard.isFaceUp()
                && card.isRed() != topCard.isRed()
                && card.getValue() == topCard.getValue() - 1;
    }

    // Foundation placement validation
    public static boolean canPlaceOnFoundation(
            Card card,
            Card.Suit suit,
            ArrayList<Card> destination) {
        if (card == null || !card.isFaceUp()) {
            return false;
        }
        if (card.getSuit() != suit) {
            return false;
        }
        if (destination.isEmpty()) {
            return card.getRank() == Card.Rank.ACE;
        }
        Card topCard = destination.get(
                destination.size() - 1
        );
        return topCard.isFaceUp()
                && card.getSuit() == topCard.getSuit()
                && card.getValue() == topCard.getValue() + 1;
    }

    // Tableau sequence validation checks ever card in the column
    public static boolean isValidTableauSequence(
            ArrayList<Card> column,
            int startIndex) {
        if (startIndex < 0 || startIndex >= column.size()) {
            return false;
        }
        for (int i = startIndex; i < column.size(); i++) {
            Card current = column.get(i);
            if (!current.isFaceUp()) {
                return false;
            }
            if (i > startIndex) {
                Card previous = column.get(i - 1);
                if (current.isRed() == previous.isRed()
                        || current.getValue()
                        != previous.getValue() - 1) {
                    return false;
                }
            }
        }
        return true;
    }

    // Tableau to tableau
    public static boolean canMoveTableauToTableau(
            GameState state,
            int sourceColumn,
            int startIndex,
            int destinationColumn) {
        if (!isValidColumn(sourceColumn)
                || !isValidColumn(destinationColumn)
                || sourceColumn == destinationColumn) {
            return false;
        }
        ArrayList<Card> source =
                state.getTableauColumn(sourceColumn);
        ArrayList<Card> destination =
                state.getTableauColumn(destinationColumn);
        if (!isValidTableauSequence(source, startIndex)) {
            return false;
        }
        return canPlaceOnTableau(
                source.get(startIndex),
                destination
        );
    }

    // Tableau to foundation
    public static boolean canMoveTableauToFoundation(
            GameState state,
            int sourceColumn,
            Card.Suit suit) {
        if (!isValidColumn(sourceColumn) || suit == null) {
            return false;
        }
        Card card = getTopCard(
                state.getTableauColumn(sourceColumn)
        );
        return canPlaceOnFoundation(
                card,
                suit,
                state.getFoundation(suit)
        );
    }

    // Discard to tableau
    public static boolean canMoveDiscardToTableau(
            GameState state,
            int destinationColumn) {
        if (!isValidColumn(destinationColumn)) {
            return false;
        }
        Card card = getTopCard(state.getDiscard());
        return canPlaceOnTableau(
                card,
                state.getTableauColumn(destinationColumn)
        );
    }

    // Discard to foundation
    public static boolean canMoveDiscardToFoundation(
            GameState state,
            Card.Suit suit) {
        if (suit == null) {
            return false;
        }
        Card card = getTopCard(state.getDiscard());
        return canPlaceOnFoundation(
                card,
                suit,
                state.getFoundation(suit)
        );
    }

    // Foundation to tableau
    public static boolean canMoveFoundationToTableau(
            GameState state,
            Card.Suit suit,
            int destinationColumn) {
        if (suit == null
                || !isValidColumn(destinationColumn)) {
            return false;
        }
        Card card = getTopCard(
                state.getFoundation(suit)
        );
        return canPlaceOnTableau(
                card,
                state.getTableauColumn(destinationColumn)
        );
    }

    // Stock operations
    public static boolean canDraw(GameState state) {
        return !state.getStock().isEmpty();
    }
    public static boolean canRecycle(GameState state) {
        return state.getStock().isEmpty()
                && !state.getDiscard().isEmpty();
    }

    // Helper methods
    private static boolean isValidColumn(int column) {
        return column >= 0
                && column < GameState.TABLEAU_COLUMNS;
    }

    private static Card getTopCard(ArrayList<Card> pile) {
        if (pile.isEmpty()) {
            return null;
        }
        return pile.get(pile.size() - 1);
    }
}