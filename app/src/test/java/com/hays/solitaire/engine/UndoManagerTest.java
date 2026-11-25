package com.hays.solitaire.engine;

import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.GameState;
import com.hays.solitaire.model.GameSnapshot;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class UndoManagerTest {

    private GameState state;
    private UndoManager undoManager;
    private GameEngine engine;

    @Before
    public void setUp() {

        state = new GameState();
        undoManager = new UndoManager(state);
        engine = new GameEngine(state);
    }

    // Initial state and validation tests

    @Test
    public void newUndoManagerHasEmptyHistory() {

        assertEquals(0, undoManager.getHistorySize());
        assertEquals(3, state.getRemainingUndos());
    }

    @Test
    public void cannotUndoWithoutHistory() {

        assertFalse(undoManager.canUndo());
        assertFalse(undoManager.undo());
    }

    @Test
    public void recordedActionEnablesUndo() {

        performDraw();

        assertTrue(undoManager.canUndo());
        assertEquals(1, undoManager.getHistorySize());
    }

    @Test
    public void cannotUndoWhenAllowanceIsExhausted() {

        performDraw();

        state.setRemainingUndos(0);

        assertFalse(undoManager.canUndo());
        assertFalse(undoManager.undo());
    }

    @Test
    public void cannotUndoCompletedGame() {

        performDraw();

        state.setGameWon(true);

        assertFalse(undoManager.canUndo());
        assertFalse(undoManager.undo());
    }

    @Test
    public void unsuccessfulUndoDoesNotChangeGameValues() {

        state.setScore(50);
        state.setMoves(10);
        state.setElapsedTimeMs(5000);

        assertFalse(undoManager.undo());

        assertEquals(50, state.getScore());
        assertEquals(10, state.getMoves());
        assertEquals(5000L, state.getElapsedTimeMs());
        assertEquals(3, state.getRemainingUndos());
    }

    // History management tests

    @Test
    public void recordingActionDoesNotConsumeUndo() {

        performDraw();

        assertEquals(3, state.getRemainingUndos());
        assertEquals(1, undoManager.getHistorySize());
    }

    @Test
    public void historyCannotExceedMaximumUndos() {

        for (int i = 0; i < 5; i++) {
            performDraw();
        }

        assertEquals(3, undoManager.getHistorySize());
    }

    @Test
    public void historyRespectsRemainingUndoAllowance() {

        state.setRemainingUndos(1);

        performDraw();
        performDraw();
        performDraw();

        assertEquals(1, undoManager.getHistorySize());
    }

    @Test
    public void actionsAreNotRecordedWithoutRemainingUndos() {

        state.setRemainingUndos(0);

        performDraw();

        assertEquals(0, undoManager.getHistorySize());
        assertFalse(undoManager.canUndo());
    }

    @Test
    public void clearingHistoryRemovesAllEntries() {

        performDraw();
        performDraw();

        undoManager.clearHistory();

        assertEquals(0, undoManager.getHistorySize());
        assertFalse(undoManager.canUndo());
    }

    @Test
    public void clearingHistoryDoesNotResetUndoAllowance() {

        state.setRemainingUndos(2);

        performDraw();

        undoManager.clearHistory();

        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void clearingHistoryDoesNotResetGameValues() {

        performDraw();

        state.setScore(50);
        state.setMoves(10);
        state.setElapsedTimeMs(5000);

        undoManager.clearHistory();

        assertEquals(50, state.getScore());
        assertEquals(10, state.getMoves());
        assertEquals(5000L, state.getElapsedTimeMs());
        assertEquals(1, state.getDiscard().size());
    }

    // Undo allowance tests

    @Test
    public void successfulUndoConsumesOneUndo() {

        performDraw();

        assertTrue(undoManager.undo());

        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void successfulUndoRemovesHistoryEntry() {

        performDraw();
        performDraw();

        assertTrue(undoManager.undo());

        assertEquals(1, undoManager.getHistorySize());
    }

    @Test
    public void playerCannotUndoMoreThanThreeTimes() {

        for (int i = 0; i < 5; i++) {
            performDraw();
        }

        assertTrue(undoManager.undo());
        assertTrue(undoManager.undo());
        assertTrue(undoManager.undo());

        assertFalse(undoManager.undo());

        assertEquals(0, state.getRemainingUndos());
        assertEquals(0, undoManager.getHistorySize());
        assertEquals(8, state.getMoves());
    }

    @Test
    public void newActionsDoNotReplenishUndoAllowance() {

        performDraw();

        assertTrue(undoManager.undo());

        performDraw();
        performDraw();

        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void undoDoesNotCreateAnotherHistoryEntry() {

        performDraw();

        assertTrue(undoManager.undo());

        assertEquals(0, undoManager.getHistorySize());
        assertFalse(undoManager.canUndo());
    }

    // Stack ordering and card restoration tests

    @Test
    public void undoRestoresMostRecentActionFirst() {

        performDraw();
        performDraw();
        performDraw();

        GameSnapshot expected = new GameSnapshot(state);

        performDraw();

        assertTrue(undoManager.undo());

        assertBoardMatchesSnapshot(expected);
    }

    @Test
    public void multipleUndosRestoreActionsInReverseOrder() {

        GameSnapshot initial = new GameSnapshot(state);

        performDraw();

        GameSnapshot afterFirstDraw =
                new GameSnapshot(state);

        performDraw();

        GameSnapshot afterSecondDraw =
                new GameSnapshot(state);

        performDraw();

        assertTrue(undoManager.undo());
        assertBoardMatchesSnapshot(afterSecondDraw);

        assertTrue(undoManager.undo());
        assertBoardMatchesSnapshot(afterFirstDraw);

        assertTrue(undoManager.undo());
        assertBoardMatchesSnapshot(initial);
    }

    @Test
    public void oldestHistoryEntriesAreDiscarded() {

        performDraw();
        performDraw();

        GameSnapshot expected = new GameSnapshot(state);

        performDraw();
        performDraw();
        performDraw();

        assertTrue(undoManager.undo());
        assertTrue(undoManager.undo());
        assertTrue(undoManager.undo());

        assertBoardMatchesSnapshot(expected);

        assertFalse(undoManager.undo());
    }

    @Test
    public void undoDrawReturnsCardFaceDownToStock() {

        Card expected = state.getStock().get(
                state.getStock().size() - 1
        ).copy();

        performDraw();

        assertTrue(undoManager.undo());

        Card restored = state.getStock().get(
                state.getStock().size() - 1
        );

        assertEquals(expected.getSuit(), restored.getSuit());
        assertEquals(expected.getRank(), restored.getRank());

        assertFalse(restored.isFaceUp());
        assertEquals(24, state.getStock().size());
        assertTrue(state.getDiscard().isEmpty());
    }

    @Test
    public void undoReversesAutomaticReveal() {

        clearPiles();

        Card hidden = new Card(
                Card.Suit.CLUBS, Card.Rank.JACK
        );

        state.getTableauColumn(0).add(hidden);

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        int previousScore = state.getScore();

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        undoManager.recordAction(
                snapshot,
                state.getScore() - previousScore
        );

        assertTrue(hidden.isFaceUp());

        assertTrue(undoManager.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertFalse(
                state.getTableauColumn(0).get(0).isFaceUp()
        );

        assertTrue(
                state.getTableauColumn(0).get(1).isFaceUp()
        );

        assertTrue(
                state.getFoundation(Card.Suit.HEARTS).isEmpty()
        );
    }

    // Score reversal tests

    @Test
    public void undoDrawDeductsTenPoints() {

        state.setScore(50);

        performDraw();

        assertTrue(undoManager.undo());

        assertEquals(40, state.getScore());
    }

    @Test
    public void undoFoundationMoveReversesAwardedPoints() {

        clearPiles();

        state.setScore(50);

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        int previousScore = state.getScore();

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        undoManager.recordAction(
                snapshot,
                state.getScore() - previousScore
        );

        assertEquals(60, state.getScore());

        assertTrue(undoManager.undo());

        assertEquals(40, state.getScore());
    }

    @Test
    public void undoFoundationMoveReversesRevealPoints() {

        clearPiles();

        state.setScore(50);

        state.getTableauColumn(0).add(
                new Card(Card.Suit.CLUBS, Card.Rank.JACK)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        int previousScore = state.getScore();

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        undoManager.recordAction(
                snapshot,
                state.getScore() - previousScore
        );

        assertEquals(65, state.getScore());

        assertTrue(undoManager.undo());

        assertEquals(40, state.getScore());
    }

    @Test
    public void undoFoundationRemovalReversesDeductedPoints() {

        clearPiles();

        state.setScore(50);

        state.getFoundation(Card.Suit.HEARTS).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.TWO)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        int previousScore = state.getScore();

        assertTrue(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        undoManager.recordAction(
                snapshot,
                state.getScore() - previousScore
        );

        assertEquals(40, state.getScore());

        assertTrue(undoManager.undo());

        assertEquals(40, state.getScore());

        assertBoardMatchesSnapshot(snapshot);
    }

    @Test
    public void undoCannotReduceScoreBelowZero() {

        state.setScore(5);

        performDraw();

        assertTrue(undoManager.undo());

        assertEquals(0, state.getScore());
    }

    @Test
    public void undoAtZeroStillConsumesAllowance() {

        performDraw();

        assertTrue(undoManager.undo());

        assertEquals(0, state.getScore());
        assertEquals(2, state.getRemainingUndos());
        assertEquals(2, state.getMoves());
    }

    // Nonrefundable penalty tests

    @Test
    public void undoRecycleDoesNotRefundRecyclePenalty() {

        state.setScore(100);

        while (!state.getStock().isEmpty()) {
            assertTrue(engine.drawCard());
        }

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.recycleDiscard());

        undoManager.recordAction(snapshot, 0);

        assertEquals(80, state.getScore());

        assertTrue(undoManager.undo());

        assertEquals(70, state.getScore());

        assertBoardMatchesSnapshot(snapshot);
    }

    @Test
    public void multipleUndosPreservePreviousUndoPenalties() {

        state.setScore(50);

        performDraw();
        performDraw();
        performDraw();

        assertTrue(undoManager.undo());
        assertEquals(40, state.getScore());

        assertTrue(undoManager.undo());
        assertEquals(30, state.getScore());

        assertTrue(undoManager.undo());
        assertEquals(20, state.getScore());
    }

    @Test
    public void undoEarlierActionDoesNotRefundRecyclePenalty() {

        clearPiles();

        state.setScore(50);

        state.getStock().add(
                new Card(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        performDraw();

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.recycleDiscard());

        undoManager.recordAction(snapshot, 0);

        assertEquals(30, state.getScore());

        assertTrue(undoManager.undo());
        assertEquals(20, state.getScore());

        assertTrue(undoManager.undo());
        assertEquals(10, state.getScore());

        assertEquals(1, state.getStock().size());
        assertTrue(state.getDiscard().isEmpty());
    }

    // Move counting and elapsed time tests

    @Test
    public void undoIncrementsMovesInsteadOfRestoringCount() {

        state.setMoves(10);

        performDraw();

        assertEquals(11, state.getMoves());

        assertTrue(undoManager.undo());

        assertEquals(12, state.getMoves());
    }

    @Test
    public void multipleUndosEachCountAsOneMove() {

        performDraw();
        performDraw();
        performDraw();

        assertEquals(3, state.getMoves());

        assertTrue(undoManager.undo());
        assertTrue(undoManager.undo());
        assertTrue(undoManager.undo());

        assertEquals(6, state.getMoves());
    }

    @Test
    public void undoDoesNotRestoreElapsedTime() {

        state.setElapsedTimeMs(5000);

        performDraw();

        state.setElapsedTimeMs(15000);

        assertTrue(undoManager.undo());

        assertEquals(15000L, state.getElapsedTimeMs());
    }

    // Helper methods
    // performDraw() temporarily performs work that GameEngine will handle
    private void performDraw() {

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.drawCard());

        undoManager.recordAction(snapshot, 0);
    }

    private Card faceUpCard(
            Card.Suit suit,
            Card.Rank rank) {

        Card card = new Card(suit, rank);

        card.setFaceUp(true);

        return card;
    }

    private void clearPiles() {

        state.getStock().clear();
        state.getDiscard().clear();

        for (Card.Suit suit : Card.Suit.values()) {

            state.getFoundation(suit).clear();
        }

        for (ArrayList<Card> column : state.getTableau()) {

            column.clear();
        }
    }

    private void assertBoardMatchesSnapshot(
            GameSnapshot snapshot) {

        assertPilesEqual(
                snapshot.getStock(),
                state.getStock()
        );

        assertPilesEqual(
                snapshot.getDiscard(),
                state.getDiscard()
        );

        for (Card.Suit suit : Card.Suit.values()) {

            assertPilesEqual(
                    snapshot.getFoundations().get(suit),
                    state.getFoundation(suit)
            );
        }

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertPilesEqual(
                    snapshot.getTableau().get(i),
                    state.getTableauColumn(i)
            );
        }

        assertEquals(
                snapshot.isGameWon(),
                state.isGameWon()
        );
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