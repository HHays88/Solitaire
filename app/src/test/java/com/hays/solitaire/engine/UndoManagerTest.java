package com.hays.solitaire.engine;

import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.GameState;
import com.hays.solitaire.model.GameSnapshot;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
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

        assertFalse(undoManager.undo());

        assertEquals(50, state.getScore());
        assertEquals(10, state.getMoves());
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

        undoManager.clearHistory();

        assertEquals(50, state.getScore());
        assertEquals(10, state.getMoves());
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

    // Move counting tests

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

    // HISTORY EXPORT TESTS

    @Test
    public void exportHistoryReturnsEmptyListInitially() {
        assertTrue(undoManager.exportHistory().isEmpty());
    }

    @Test
    public void exportHistoryContainsRecordedEntries() {
        performDraw();
        performDraw();

        assertEquals(2, undoManager.exportHistory().size());
    }

    @Test
    public void exportHistoryPreservesNewestToOldestOrder() {
        performDraw();
        performDraw();
        performDraw();

        ArrayList<UndoManager.UndoEntry> entries =
                undoManager.exportHistory();

        assertEquals(
                22,
                entries.get(0).getSnapshot().getStock().size()
        );

        assertEquals(
                23,
                entries.get(1).getSnapshot().getStock().size()
        );

        assertEquals(
                24,
                entries.get(2).getSnapshot().getStock().size()
        );
    }

    @Test
    public void exportHistoryPreservesReversibleScoreChange() {
        GameSnapshot snapshot = new GameSnapshot(state);

        undoManager.recordAction(snapshot, 15);

        assertEquals(
                15,
                undoManager.exportHistory()
                        .get(0).getReversibleScoreChange()
        );
    }

    @Test
    public void modifyingExportedHistoryDoesNotChangeInternalHistory() {
        performDraw();
        performDraw();

        ArrayList<UndoManager.UndoEntry> entries =
                undoManager.exportHistory();

        entries.clear();

        assertEquals(2, undoManager.getHistorySize());
        assertTrue(undoManager.canUndo());
    }

    // UNDO ENTRY TESTS

    @Test
    public void undoEntryPreservesSnapshot() {
        GameSnapshot snapshot = new GameSnapshot(state);

        UndoManager.UndoEntry entry =
                new UndoManager.UndoEntry(snapshot, 0);

        assertSame(snapshot, entry.getSnapshot());
    }

    @Test
    public void undoEntryPreservesNegativeScoreChange() {
        GameSnapshot snapshot = new GameSnapshot(state);

        UndoManager.UndoEntry entry =
                new UndoManager.UndoEntry(snapshot, -10);

        assertEquals(-10, entry.getReversibleScoreChange());
    }

    @Test(expected = IllegalArgumentException.class)
    public void undoEntryRejectsNullSnapshot() {
        new UndoManager.UndoEntry(null, 0);
    }

    // HISTORY IMPORT TESTS

    @Test
    public void importHistoryRestoresEntries() {
        performDraw();
        performDraw();

        ArrayList<UndoManager.UndoEntry> entries =
                undoManager.exportHistory();

        UndoManager restoredManager = new UndoManager(state);

        restoredManager.importHistory(entries);

        assertEquals(2, restoredManager.getHistorySize());
        assertTrue(restoredManager.canUndo());
    }

    @Test
    public void importHistoryPreservesEntryOrder() {
        performDraw();
        performDraw();
        performDraw();

        ArrayList<UndoManager.UndoEntry> entries =
                undoManager.exportHistory();

        UndoManager restoredManager = new UndoManager(state);

        restoredManager.importHistory(entries);

        ArrayList<UndoManager.UndoEntry> restoredEntries =
                restoredManager.exportHistory();

        for (int i = 0; i < entries.size(); i++) {
            assertSame(entries.get(i), restoredEntries.get(i));
        }
    }

    @Test
    public void importHistoryReplacesExistingHistory() {
        performDraw();
        performDraw();

        ArrayList<UndoManager.UndoEntry> entries =
                new ArrayList<>();

        entries.add(
                new UndoManager.UndoEntry(
                        new GameSnapshot(state),
                        0
                )
        );

        undoManager.importHistory(entries);

        assertEquals(1, undoManager.getHistorySize());
    }

    @Test
    public void importEmptyHistoryClearsExistingHistory() {
        performDraw();
        performDraw();

        undoManager.importHistory(new ArrayList<>());

        assertEquals(0, undoManager.getHistorySize());
        assertFalse(undoManager.canUndo());
    }

    @Test
    public void modifyingImportedListDoesNotChangeInternalHistory() {
        performDraw();
        performDraw();

        ArrayList<UndoManager.UndoEntry> entries =
                undoManager.exportHistory();

        UndoManager restoredManager = new UndoManager(state);

        restoredManager.importHistory(entries);

        entries.clear();

        assertEquals(2, restoredManager.getHistorySize());
    }

    @Test
    public void importHistoryDoesNotModifyGameValues() {
        performDraw();

        ArrayList<UndoManager.UndoEntry> entries =
                undoManager.exportHistory();

        state.setScore(100);
        state.setMoves(25);
        state.setRemainingUndos(2);

        undoManager.importHistory(entries);

        assertEquals(100, state.getScore());
        assertEquals(25, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
    }

    // HISTORY IMPORT VALIDATION TESTS

    @Test(expected = IllegalArgumentException.class)
    public void importHistoryRejectsNullList() {
        undoManager.importHistory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void importHistoryRejectsNullEntry() {
        ArrayList<UndoManager.UndoEntry> entries =
                new ArrayList<>();

        entries.add(null);

        undoManager.importHistory(entries);
    }

    @Test(expected = IllegalArgumentException.class)
    public void importHistoryRejectsMoreThanMaximumUndos() {
        ArrayList<UndoManager.UndoEntry> entries =
                new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            entries.add(
                    new UndoManager.UndoEntry(
                            new GameSnapshot(state),
                            0
                    )
            );
        }

        undoManager.importHistory(entries);
    }

    @Test(expected = IllegalArgumentException.class)
    public void importHistoryRejectsMoreThanRemainingUndos() {
        performDraw();
        performDraw();

        ArrayList<UndoManager.UndoEntry> entries =
                undoManager.exportHistory();

        state.setRemainingUndos(1);

        undoManager.importHistory(entries);
    }

    @Test(expected = IllegalArgumentException.class)
    public void importHistoryRejectsEntriesWhenNoUndosRemain() {
        performDraw();

        ArrayList<UndoManager.UndoEntry> entries =
                undoManager.exportHistory();

        state.setRemainingUndos(0);

        undoManager.importHistory(entries);
    }

    @Test
    public void importEmptyHistoryAllowedWhenNoUndosRemain() {
        state.setRemainingUndos(0);

        undoManager.importHistory(new ArrayList<>());

        assertEquals(0, undoManager.getHistorySize());
        assertFalse(undoManager.canUndo());
    }

    @Test
    public void invalidImportPreservesExistingHistory() {
        performDraw();
        performDraw();

        ArrayList<UndoManager.UndoEntry> originalEntries =
                undoManager.exportHistory();

        ArrayList<UndoManager.UndoEntry> invalidEntries =
                new ArrayList<>();

        invalidEntries.add(null);

        try {
            undoManager.importHistory(invalidEntries);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException expected) {
            assertEquals(2, undoManager.getHistorySize());

            ArrayList<UndoManager.UndoEntry> currentEntries =
                    undoManager.exportHistory();

            assertSame(
                    originalEntries.get(0),
                    currentEntries.get(0)
            );

            assertSame(
                    originalEntries.get(1),
                    currentEntries.get(1)
            );
        }
    }

    // RECONSTRUCTED UNDO OPERATION TESTS

    @Test
    public void importedHistoryAllowsUndo() {
        GameSnapshot original = new GameSnapshot(state);

        performDraw();

        UndoManager restoredManager = new UndoManager(state);

        restoredManager.importHistory(
                undoManager.exportHistory()
        );

        assertTrue(restoredManager.undo());

        assertBoardMatchesSnapshot(original);
    }

    @Test
    public void importedHistoryUndoesMostRecentActionFirst() {
        performDraw();
        performDraw();
        performDraw();

        UndoManager restoredManager = new UndoManager(state);

        restoredManager.importHistory(
                undoManager.exportHistory()
        );

        assertTrue(restoredManager.undo());

        assertEquals(22, state.getStock().size());
        assertEquals(2, state.getDiscard().size());
    }

    @Test
    public void importedHistorySupportsConsecutiveUndos() {
        performDraw();
        performDraw();
        performDraw();

        UndoManager restoredManager = new UndoManager(state);

        restoredManager.importHistory(
                undoManager.exportHistory()
        );

        assertTrue(restoredManager.undo());
        assertEquals(22, state.getStock().size());

        assertTrue(restoredManager.undo());
        assertEquals(23, state.getStock().size());

        assertTrue(restoredManager.undo());
        assertEquals(24, state.getStock().size());

        assertFalse(restoredManager.undo());
        assertEquals(0, state.getRemainingUndos());
    }

    @Test
    public void importedHistoryReversesPositiveScoreChange() {
        state.setScore(100);

        GameSnapshot snapshot = new GameSnapshot(state);

        state.setScore(115);

        ArrayList<UndoManager.UndoEntry> entries =
                new ArrayList<>();

        entries.add(
                new UndoManager.UndoEntry(snapshot, 15)
        );

        undoManager.importHistory(entries);

        assertTrue(undoManager.undo());

        assertEquals(90, state.getScore());
    }

    @Test
    public void importedHistoryReversesNegativeScoreChange() {
        state.setScore(100);

        GameSnapshot snapshot = new GameSnapshot(state);

        state.setScore(90);

        ArrayList<UndoManager.UndoEntry> entries =
                new ArrayList<>();

        entries.add(
                new UndoManager.UndoEntry(snapshot, -10)
        );

        undoManager.importHistory(entries);

        assertTrue(undoManager.undo());

        assertEquals(90, state.getScore());
    }

    @Test
    public void importedHistoryDoesNotRefundRecyclePenalty() {
        state.setScore(100);

        GameSnapshot snapshot = new GameSnapshot(state);

        state.setScore(80);

        ArrayList<UndoManager.UndoEntry> entries =
                new ArrayList<>();

        entries.add(
                new UndoManager.UndoEntry(snapshot, 0)
        );

        undoManager.importHistory(entries);

        assertTrue(undoManager.undo());

        assertEquals(70, state.getScore());
    }

    @Test
    public void importedHistoryUndoConsumesRemainingUndo() {
        performDraw();

        UndoManager restoredManager = new UndoManager(state);

        restoredManager.importHistory(
                undoManager.exportHistory()
        );

        state.setRemainingUndos(1);

        assertTrue(restoredManager.undo());

        assertEquals(0, state.getRemainingUndos());
        assertFalse(restoredManager.canUndo());
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