package com.hays.solitaire.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class GameStateTest {

    @Test
    public void initialStockContains24Cards() {

        GameState state = new GameState();

        assertEquals(24, state.getStock().size());
    }

    @Test
    public void initialDiscardIsEmpty() {

        GameState state = new GameState();

        assertTrue(state.getDiscard().isEmpty());
    }

    @Test
    public void initialFoundationsAreEmpty() {

        GameState state = new GameState();

        assertEquals(4, state.getFoundations().size());

        for (Card.Suit suit : Card.Suit.values()) {
            assertTrue(state.getFoundation(suit).isEmpty());
        }
    }

    @Test
    public void tableauContainsSevenColumns() {

        GameState state = new GameState();

        assertEquals(7, state.getTableau().size());
    }

    @Test
    public void tableauColumnsContainCorrectCardCounts() {

        GameState state = new GameState();

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertEquals(
                    i + 1,
                    state.getTableauColumn(i).size()
            );
        }
    }

    @Test
    public void onlyTopTableauCardsAreFaceUp() {

        GameState state = new GameState();

        for (ArrayList<Card> column : state.getTableau()) {

            for (int i = 0; i < column.size(); i++) {

                assertEquals(
                        i == column.size() - 1,
                        column.get(i).isFaceUp()
                );
            }
        }
    }

    @Test
    public void stockCardsAreFaceDown() {

        GameState state = new GameState();

        for (Card card : state.getStock()) {
            assertFalse(card.isFaceUp());
        }
    }

    @Test
    public void initialDealContains52UniqueCards() {

        GameState state = new GameState();

        Set<String> uniqueCards = new HashSet<>();

        int totalCards = state.getStock().size();

        for (Card card : state.getStock()) {
            uniqueCards.add(card.toString());
        }

        for (ArrayList<Card> column : state.getTableau()) {

            totalCards += column.size();

            for (Card card : column) {
                uniqueCards.add(card.toString());
            }
        }

        assertEquals(52, totalCards);
        assertEquals(52, uniqueCards.size());
    }

    @Test
    public void originalDeckIsProtected() {

        GameState state = new GameState();

        ArrayList<Card> deck = state.getOriginalDeck();

        deck.get(0).setFaceUp(true);
        deck.clear();

        assertEquals(52, state.getOriginalDeck().size());

        for (Card card : state.getOriginalDeck()) {
            assertFalse(card.isFaceUp());
        }
    }

    @Test
    public void restartRestoresOriginalDeal() {

        GameState state = new GameState();

        GameSnapshot originalDeal = new GameSnapshot(state);

        // Simulate changes to the game.
        Card card = state.getStock().remove(
                state.getStock().size() - 1
        );

        card.setFaceUp(true);
        state.getDiscard().add(card);

        state.getTableauColumn(1).get(0).setFaceUp(true);

        state.restart();

        assertPilesEqual(
                originalDeal.getStock(),
                state.getStock()
        );

        assertTrue(state.getDiscard().isEmpty());

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertPilesEqual(
                    originalDeal.getTableau().get(i),
                    state.getTableauColumn(i)
            );
        }
    }

    @Test
    public void restartResetsGameValues() {

        GameState state = new GameState();

        state.setScore(100);
        state.setMoves(25);
        state.setElapsedTimeMs(60000);
        state.setRemainingUndos(1);
        state.setGameWon(true);

        state.restart();

        assertEquals(0, state.getScore());
        assertEquals(0, state.getMoves());
        assertEquals(0L, state.getElapsedTimeMs());
        assertEquals(3, state.getRemainingUndos());
        assertFalse(state.isGameWon());
    }

    @Test
    public void scoreCannotBeNegative() {

        GameState state = new GameState();

        state.setScore(-20);

        assertEquals(0, state.getScore());
    }

    @Test
    public void movesCannotBeNegative() {

        GameState state = new GameState();

        state.setMoves(-1);

        assertEquals(0, state.getMoves());
    }

    @Test
    public void elapsedTimeCannotBeNegative() {

        GameState state = new GameState();

        state.setElapsedTimeMs(-1000);

        assertEquals(0L, state.getElapsedTimeMs());
    }

    @Test
    public void remainingUndosStayWithinLimits() {

        GameState state = new GameState();

        state.setRemainingUndos(5);
        assertEquals(3, state.getRemainingUndos());

        state.setRemainingUndos(-1);
        assertEquals(0, state.getRemainingUndos());
    }

    private void assertPilesEqual(
            ArrayList<Card> expected,
            ArrayList<Card> actual) {

        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {

            assertEquals(
                    expected.get(i).getSuit(),
                    actual.get(i).getSuit()
            );

            assertEquals(
                    expected.get(i).getRank(),
                    actual.get(i).getRank()
            );

            assertEquals(
                    expected.get(i).isFaceUp(),
                    actual.get(i).isFaceUp()
            );
        }
    }
}