package com.hays.solitaire.model;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class GameSnapshotTest {

    @Test
    public void snapshotCapturesCurrentGameValues() {

        GameState state = new GameState();

        state.setScore(50);
        state.setGameWon(false);

        GameSnapshot snapshot = new GameSnapshot(state);

        assertEquals(50, snapshot.getScore());
        assertFalse(snapshot.isGameWon());
    }

    @Test
    public void snapshotIsIndependentOfCurrentGame() {

        GameState state = new GameState();

        GameSnapshot snapshot = new GameSnapshot(state);

        state.getStock().get(0).setFaceUp(true);
        state.getStock().clear();

        assertEquals(24, snapshot.getStock().size());
        assertFalse(snapshot.getStock().get(0).isFaceUp());
    }

    @Test
    public void snapshotGettersReturnIndependentCopies() {

        GameState state = new GameState();

        GameSnapshot snapshot = new GameSnapshot(state);

        ArrayList<Card> stock = snapshot.getStock();

        stock.get(0).setFaceUp(true);
        stock.clear();

        assertEquals(24, snapshot.getStock().size());
        assertFalse(snapshot.getStock().get(0).isFaceUp());
    }

    @Test
    public void restoreReturnsDrawnCardToStock() {

        GameState state = new GameState();

        GameSnapshot snapshot = new GameSnapshot(state);

        Card drawnCard = state.getStock().remove(
                state.getStock().size() - 1
        );

        drawnCard.setFaceUp(true);
        state.getDiscard().add(drawnCard);

        snapshot.restore(state);

        assertEquals(24, state.getStock().size());
        assertTrue(state.getDiscard().isEmpty());

        Card restoredCard = state.getStock().get(
                state.getStock().size() - 1
        );

        assertEquals(drawnCard.getSuit(), restoredCard.getSuit());
        assertEquals(drawnCard.getRank(), restoredCard.getRank());
        assertFalse(restoredCard.isFaceUp());
    }

    @Test
    public void restoreReversesAutomaticReveal() {

        GameState state = new GameState();

        ArrayList<Card> column = state.getTableauColumn(1);

        Card hiddenCard = column.get(0);
        Card topCard = column.get(1);

        assertFalse(hiddenCard.isFaceUp());
        assertTrue(topCard.isFaceUp());

        GameSnapshot snapshot = new GameSnapshot(state);

        // Simulate moving the top card.
        column.remove(column.size() - 1);
        state.getTableauColumn(0).add(topCard);

        // Simulate the automatic reveal.
        hiddenCard.setFaceUp(true);

        assertTrue(column.get(0).isFaceUp());

        // Restore the board.
        snapshot.restore(state);

        assertEquals(2, column.size());
        assertEquals(1, state.getTableauColumn(0).size());

        assertFalse(column.get(0).isFaceUp());
        assertTrue(column.get(1).isFaceUp());

        assertEquals(topCard.getSuit(), column.get(1).getSuit());
        assertEquals(topCard.getRank(), column.get(1).getRank());
    }

    @Test
    public void restoreDoesNotModifySnapshot() {

        GameState state = new GameState();

        GameSnapshot snapshot = new GameSnapshot(state);

        snapshot.restore(state);

        state.getStock().get(0).setFaceUp(true);
        state.getStock().clear();

        assertEquals(24, snapshot.getStock().size());
        assertFalse(snapshot.getStock().get(0).isFaceUp());
    }

    @Test
    public void restoreDoesNotResetGameCounters() {

        GameState state = new GameState();

        GameSnapshot snapshot = new GameSnapshot(state);

        state.setMoves(10);
        state.setElapsedTimeMs(30000);
        state.setRemainingUndos(2);

        snapshot.restore(state);

        assertEquals(10, state.getMoves());
        assertEquals(30000L, state.getElapsedTimeMs());
        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void restoreDoesNotResetScore() {

        GameState state = new GameState();

        state.setScore(50);

        GameSnapshot snapshot = new GameSnapshot(state);

        state.setScore(75);

        snapshot.restore(state);

        assertEquals(75, state.getScore());
        assertEquals(50, snapshot.getScore());
    }

    @Test
    public void restoreReturnsGameWonStatus() {

        GameState state = new GameState();

        GameSnapshot snapshot = new GameSnapshot(state);

        state.setGameWon(true);

        snapshot.restore(state);

        assertFalse(state.isGameWon());
    }
}