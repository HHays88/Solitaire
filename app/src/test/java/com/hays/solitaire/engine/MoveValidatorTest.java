package com.hays.solitaire.engine;

import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.GameState;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class MoveValidatorTest {

    // Tableau placement tests

    @Test
    public void tableauAcceptsDescendingAlternatingColors() {

        ArrayList<Card> destination = new ArrayList<>();

        destination.add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        Card card = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        );

        assertTrue(MoveValidator.canPlaceOnTableau(
                card, destination
        ));
    }

    @Test
    public void tableauRejectsSameColor() {

        ArrayList<Card> destination = new ArrayList<>();

        destination.add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        Card card = faceUpCard(
                Card.Suit.CLUBS, Card.Rank.QUEEN
        );

        assertFalse(MoveValidator.canPlaceOnTableau(
                card, destination
        ));
    }

    @Test
    public void tableauRejectsIncorrectRank() {

        ArrayList<Card> destination = new ArrayList<>();

        destination.add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        Card card = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.JACK
        );

        assertFalse(MoveValidator.canPlaceOnTableau(
                card, destination
        ));
    }

    @Test
    public void emptyTableauAcceptsKing() {

        ArrayList<Card> destination = new ArrayList<>();

        Card card = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.KING
        );

        assertTrue(MoveValidator.canPlaceOnTableau(
                card, destination
        ));
    }

    @Test
    public void emptyTableauRejectsNonKing() {

        ArrayList<Card> destination = new ArrayList<>();

        Card card = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        );

        assertFalse(MoveValidator.canPlaceOnTableau(
                card, destination
        ));
    }

    @Test
    public void tableauRejectsFaceDownCard() {

        ArrayList<Card> destination = new ArrayList<>();

        Card card = new Card(
                Card.Suit.HEARTS, Card.Rank.KING
        );

        assertFalse(MoveValidator.canPlaceOnTableau(
                card, destination
        ));
    }

    @Test
    public void tableauRejectsFaceDownDestination() {

        ArrayList<Card> destination = new ArrayList<>();

        destination.add(new Card(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        Card card = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        );

        assertFalse(MoveValidator.canPlaceOnTableau(
                card, destination
        ));
    }

    @Test
    public void tableauRejectsNullCard() {

        assertFalse(MoveValidator.canPlaceOnTableau(
                null, new ArrayList<>()
        ));
    }

    // Foundation placement tests

    @Test
    public void emptyFoundationAcceptsAce() {

        Card card = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        );

        assertTrue(MoveValidator.canPlaceOnFoundation(
                card,
                Card.Suit.HEARTS,
                new ArrayList<>()
        ));
    }

    @Test
    public void emptyFoundationRejectsNonAce() {

        Card card = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.TWO
        );

        assertFalse(MoveValidator.canPlaceOnFoundation(
                card,
                Card.Suit.HEARTS,
                new ArrayList<>()
        ));
    }

    @Test
    public void foundationAcceptsAscendingSameSuit() {

        ArrayList<Card> destination = new ArrayList<>();

        destination.add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        ));

        Card card = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.TWO
        );

        assertTrue(MoveValidator.canPlaceOnFoundation(
                card, Card.Suit.HEARTS, destination
        ));
    }

    @Test
    public void foundationRejectsWrongSuit() {

        ArrayList<Card> destination = new ArrayList<>();

        destination.add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        ));

        Card card = faceUpCard(
                Card.Suit.DIAMONDS, Card.Rank.TWO
        );

        assertFalse(MoveValidator.canPlaceOnFoundation(
                card, Card.Suit.HEARTS, destination
        ));
    }

    @Test
    public void foundationRejectsSkippedRank() {

        ArrayList<Card> destination = new ArrayList<>();

        destination.add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        ));

        Card card = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.THREE
        );

        assertFalse(MoveValidator.canPlaceOnFoundation(
                card, Card.Suit.HEARTS, destination
        ));
    }

    @Test
    public void foundationRejectsFaceDownCard() {

        Card card = new Card(
                Card.Suit.HEARTS, Card.Rank.ACE
        );

        assertFalse(MoveValidator.canPlaceOnFoundation(
                card,
                Card.Suit.HEARTS,
                new ArrayList<>()
        ));
    }

    @Test
    public void foundationRejectsNullCard() {

        assertFalse(MoveValidator.canPlaceOnFoundation(
                null,
                Card.Suit.HEARTS,
                new ArrayList<>()
        ));
    }

    // Tableau sequence tests

    @Test
    public void validTableauSequenceIsAccepted() {

        ArrayList<Card> column = new ArrayList<>();

        column.add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        column.add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        ));

        column.add(faceUpCard(
                Card.Suit.CLUBS, Card.Rank.JACK
        ));

        assertTrue(MoveValidator.isValidTableauSequence(
                column, 0
        ));
    }

    @Test
    public void partialTableauSequenceIsAccepted() {

        ArrayList<Card> column = new ArrayList<>();

        column.add(new Card(
                Card.Suit.DIAMONDS, Card.Rank.FIVE
        ));

        column.add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        column.add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        ));

        assertTrue(MoveValidator.isValidTableauSequence(
                column, 1
        ));
    }

    @Test
    public void sequenceRejectsFaceDownCards() {

        ArrayList<Card> column = new ArrayList<>();

        column.add(new Card(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        column.add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        ));

        assertFalse(MoveValidator.isValidTableauSequence(
                column, 0
        ));
    }

    @Test
    public void sequenceRejectsSameColor() {

        ArrayList<Card> column = new ArrayList<>();

        column.add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        column.add(faceUpCard(
                Card.Suit.CLUBS, Card.Rank.QUEEN
        ));

        assertFalse(MoveValidator.isValidTableauSequence(
                column, 0
        ));
    }

    @Test
    public void sequenceRejectsIncorrectRank() {

        ArrayList<Card> column = new ArrayList<>();

        column.add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        column.add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.JACK
        ));

        assertFalse(MoveValidator.isValidTableauSequence(
                column, 0
        ));
    }

    @Test
    public void sequenceRejectsInvalidIndex() {

        ArrayList<Card> column = new ArrayList<>();

        column.add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        assertFalse(MoveValidator.isValidTableauSequence(
                column, -1
        ));

        assertFalse(MoveValidator.isValidTableauSequence(
                column, 1
        ));
    }

    @Test
    public void sequenceRejectsEmptyColumn() {

        assertFalse(MoveValidator.isValidTableauSequence(
                new ArrayList<>(), 0
        ));
    }

    // Source-to-destination tests

    @Test
    public void tableauCanMoveSequenceToTableau() {

        GameState state = emptyGameState();

        state.getTableauColumn(0).add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        ));

        state.getTableauColumn(0).add(faceUpCard(
                Card.Suit.CLUBS, Card.Rank.JACK
        ));

        state.getTableauColumn(1).add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        assertTrue(MoveValidator.canMoveTableauToTableau(
                state, 0, 0, 1
        ));
    }

    @Test
    public void tableauCannotMoveToSameColumn() {

        GameState state = new GameState();

        assertFalse(MoveValidator.canMoveTableauToTableau(
                state, 0, 0, 0
        ));
    }

    @Test
    public void tableauRejectsInvalidColumnIndices() {

        GameState state = new GameState();

        assertFalse(MoveValidator.canMoveTableauToTableau(
                state, -1, 0, 1
        ));

        assertFalse(MoveValidator.canMoveTableauToTableau(
                state, 0, 0, 7
        ));
    }

    @Test
    public void tableauCanMoveToFoundation() {

        GameState state = emptyGameState();

        state.getTableauColumn(0).add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        ));

        assertTrue(MoveValidator.canMoveTableauToFoundation(
                state, 0, Card.Suit.HEARTS
        ));
    }

    @Test
    public void tableauCannotMoveFromEmptyColumn() {

        GameState state = emptyGameState();

        assertFalse(MoveValidator.canMoveTableauToFoundation(
                state, 0, Card.Suit.HEARTS
        ));
    }

    @Test
    public void discardCanMoveToTableau() {

        GameState state = emptyGameState();

        state.getDiscard().add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        ));

        assertTrue(MoveValidator.canMoveDiscardToTableau(
                state, 0
        ));
    }

    @Test
    public void discardCanMoveToFoundation() {

        GameState state = emptyGameState();

        state.getDiscard().add(faceUpCard(
                Card.Suit.CLUBS, Card.Rank.ACE
        ));

        assertTrue(MoveValidator.canMoveDiscardToFoundation(
                state, Card.Suit.CLUBS
        ));
    }

    @Test
    public void onlyTopDiscardCardCanBeMoved() {

        GameState state = emptyGameState();

        state.getDiscard().add(faceUpCard(
                Card.Suit.CLUBS, Card.Rank.ACE
        ));

        state.getDiscard().add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.KING
        ));

        assertFalse(MoveValidator.canMoveDiscardToFoundation(
                state, Card.Suit.CLUBS
        ));
    }

    @Test
    public void emptyDiscardCannotBeMoved() {

        GameState state = emptyGameState();

        assertFalse(MoveValidator.canMoveDiscardToTableau(
                state, 0
        ));

        assertFalse(MoveValidator.canMoveDiscardToFoundation(
                state, Card.Suit.HEARTS
        ));
    }

    @Test
    public void foundationCanMoveToTableau() {

        GameState state = emptyGameState();

        state.getFoundation(Card.Suit.HEARTS).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        state.getTableauColumn(0).add(faceUpCard(
                Card.Suit.SPADES, Card.Rank.TWO
        ));

        assertTrue(MoveValidator.canMoveFoundationToTableau(
                state, Card.Suit.HEARTS, 0
        ));
    }

    @Test
    public void emptyFoundationCannotBeMoved() {

        GameState state = emptyGameState();

        assertFalse(MoveValidator.canMoveFoundationToTableau(
                state, Card.Suit.HEARTS, 0
        ));
    }

    // Stock operation tests

    @Test
    public void nonEmptyStockCanBeDrawn() {

        GameState state = new GameState();

        assertTrue(MoveValidator.canDraw(state));
    }

    @Test
    public void emptyStockCannotBeDrawn() {

        GameState state = emptyGameState();

        assertFalse(MoveValidator.canDraw(state));
    }

    @Test
    public void discardCanRecycleWhenStockIsEmpty() {

        GameState state = emptyGameState();

        state.getDiscard().add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        ));

        assertTrue(MoveValidator.canRecycle(state));
    }

    @Test
    public void discardCannotRecycleWhenStockContainsCards() {

        GameState state = new GameState();

        Card card = state.getStock().remove(
                state.getStock().size() - 1
        );

        card.setFaceUp(true);
        state.getDiscard().add(card);

        assertFalse(MoveValidator.canRecycle(state));
    }

    @Test
    public void emptyDiscardCannotBeRecycled() {

        GameState state = emptyGameState();

        assertFalse(MoveValidator.canRecycle(state));
    }

    // Helper methods

    private Card faceUpCard(
            Card.Suit suit,
            Card.Rank rank) {

        Card card = new Card(suit, rank);

        card.setFaceUp(true);

        return card;
    }

    private GameState emptyGameState() {

        GameState state = new GameState();

        state.getStock().clear();
        state.getDiscard().clear();

        for (Card.Suit suit : Card.Suit.values()) {
            state.getFoundation(suit).clear();
        }

        for (ArrayList<Card> column : state.getTableau()) {
            column.clear();
        }

        return state;
    }
}