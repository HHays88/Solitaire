package com.hays.solitaire.model;

import java.util.ArrayList;
import java.util.EnumMap;
import org.junit.Test;
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

    // Snapshot reconstruction tests
    @Test
    public void reconstructedSnapshotPreservesCardCollections() {
        GameState state = new GameState();
        GameSnapshot snapshot = new GameSnapshot(
                state.getStock(),
                state.getDiscard(),
                state.getFoundations(),
                state.getTableau(),
                state.getScore(),
                state.isGameWon()
        );
        assertSnapshotsEqual(
                new GameSnapshot(state),
                snapshot
        );
    }

    @Test
    public void reconstructedSnapshotPreservesScore() {
        GameState state = new GameState();
        GameSnapshot snapshot = new GameSnapshot(
                state.getStock(),
                state.getDiscard(),
                state.getFoundations(),
                state.getTableau(),
                125,
                false
        );
        assertEquals(125, snapshot.getScore());
    }

    @Test
    public void reconstructedSnapshotPreservesWinStatus() {
        GameState state = new GameState();
        GameSnapshot snapshot = new GameSnapshot(
                state.getStock(),
                state.getDiscard(),
                state.getFoundations(),
                state.getTableau(),
                600,
                true
        );
        assertTrue(snapshot.isGameWon());
    }

    @Test
    public void bothConstructorsProduceEquivalentSnapshots() {
        GameState state = new GameState();
        state.setScore(75);
        Card card = state.getStock().remove(
                state.getStock().size() - 1
        );
        card.setFaceUp(true);
        state.getDiscard().add(card);
        GameSnapshot captured = new GameSnapshot(state);
        GameSnapshot reconstructed = new GameSnapshot(
                captured.getStock(),
                captured.getDiscard(),
                captured.getFoundations(),
                captured.getTableau(),
                captured.getScore(),
                captured.isGameWon()
        );
        assertSnapshotsEqual(captured, reconstructed);
    }

    // Reconstructed snapshot defensive copying tests

    @Test
    public void reconstructedSnapshotCopiesStock() {
        GameState state = new GameState();
        ArrayList<Card> stock = state.getStock();
        GameSnapshot snapshot = reconstructSnapshot(state);
        stock.get(0).setFaceUp(true);
        stock.clear();
        assertEquals(24, snapshot.getStock().size());
        assertFalse(
                snapshot.getStock().get(0).isFaceUp()
        );
    }

    @Test
    public void reconstructedSnapshotCopiesDiscard() {
        GameState state = new GameState();
        Card card = state.getStock().remove(
                state.getStock().size() - 1
        );
        card.setFaceUp(true);
        state.getDiscard().add(card);
        GameSnapshot snapshot = reconstructSnapshot(state);
        card.setFaceUp(false);
        state.getDiscard().clear();
        assertEquals(1, snapshot.getDiscard().size());
        assertTrue(
                snapshot.getDiscard().get(0).isFaceUp()
        );
    }

    @Test
    public void reconstructedSnapshotCopiesFoundations() {
        GameState state = new GameState();
        Card ace = new Card(
                Card.Suit.HEARTS,
                Card.Rank.ACE
        );
        ace.setFaceUp(true);
        state.getFoundation(Card.Suit.HEARTS).add(ace);
        GameSnapshot snapshot = reconstructSnapshot(state);
        ace.setFaceUp(false);
        state.getFoundations().clear();
        ArrayList<Card> savedFoundation =
                snapshot.getFoundations().get(Card.Suit.HEARTS);
        assertEquals(4, snapshot.getFoundations().size());
        assertEquals(1, savedFoundation.size());
        assertTrue(savedFoundation.get(0).isFaceUp());
    }

    @Test
    public void reconstructedSnapshotCopiesTableau() {
        GameState state = new GameState();
        GameSnapshot snapshot = reconstructSnapshot(state);
        state.getTableauColumn(0).get(0).setFaceUp(false);
        state.getTableauColumn(0).clear();
        state.getTableau().clear();
        assertEquals(7, snapshot.getTableau().size());
        assertEquals(
                1,
                snapshot.getTableau().get(0).size()
        );
        assertTrue(
                snapshot.getTableau().get(0).get(0).isFaceUp()
        );
    }

    // Null collection validation tests
    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsNullStock() {
        GameState state = new GameState();
        new GameSnapshot(
                null,
                state.getDiscard(),
                state.getFoundations(),
                state.getTableau(),
                0,
                false
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsNullDiscard() {
        GameState state = new GameState();
        new GameSnapshot(
                state.getStock(),
                null,
                state.getFoundations(),
                state.getTableau(),
                0,
                false
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsNullFoundations() {
        GameState state = new GameState();
        new GameSnapshot(
                state.getStock(),
                state.getDiscard(),
                null,
                state.getTableau(),
                0,
                false
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsNullTableau() {
        GameState state = new GameState();
        new GameSnapshot(
                state.getStock(),
                state.getDiscard(),
                state.getFoundations(),
                null,
                0,
                false
        );
    }

    // Foundation structure validation tests
    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsMissingFoundation() {
        GameState state = new GameState();
        state.getFoundations().remove(Card.Suit.HEARTS);
        reconstructSnapshot(state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsEmptyFoundationMap() {
        GameState state = new GameState();
        state.getFoundations().clear();
        reconstructSnapshot(state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsNullFoundationPile() {
        GameState state = new GameState();
        state.getFoundations().put(
                Card.Suit.HEARTS,
                null
        );
        reconstructSnapshot(state);
    }

    // Tableau structure validation tests
    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsMissingTableauColumn() {
        GameState state = new GameState();
        state.getTableau().remove(0);
        reconstructSnapshot(state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsExtraTableauColumn() {
        GameState state = new GameState();
        state.getTableau().add(new ArrayList<>());
        reconstructSnapshot(state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsEmptyTableauCollection() {
        GameState state = new GameState();
        state.getTableau().clear();
        reconstructSnapshot(state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsNullTableauColumn() {
        GameState state = new GameState();
        state.getTableau().set(0, null);
        reconstructSnapshot(state);
    }

    // Snapshot score validation tests
    @Test(expected = IllegalArgumentException.class)
    public void reconstructedSnapshotRejectsNegativeScore() {
        GameState state = new GameState();
        new GameSnapshot(
                state.getStock(),
                state.getDiscard(),
                state.getFoundations(),
                state.getTableau(),
                -10,
                false
        );
    }

    @Test
    public void reconstructedSnapshotAcceptsZeroScore() {
        GameState state = new GameState();
        GameSnapshot snapshot = new GameSnapshot(
                state.getStock(),
                state.getDiscard(),
                state.getFoundations(),
                state.getTableau(),
                0,
                false
        );
        assertEquals(0, snapshot.getScore());
    }

    // Reconstructed snapshot restoration tests
    @Test
    public void reconstructedSnapshotRestoresBoard() {
        GameState state = new GameState();
        GameSnapshot snapshot = reconstructSnapshot(state);
        Card card = state.getStock().remove(
                state.getStock().size() - 1
        );
        card.setFaceUp(true);
        state.getDiscard().add(card);
        snapshot.restore(state);
        assertSnapshotsEqual(
                snapshot,
                new GameSnapshot(state)
        );
    }

    @Test
    public void reconstructedSnapshotRestoresCardOrientation() {
        GameState state = new GameState();
        GameSnapshot snapshot = reconstructSnapshot(state);
        state.getTableauColumn(1).get(0).setFaceUp(true);
        snapshot.restore(state);
        assertFalse(
                state.getTableauColumn(1).get(0).isFaceUp()
        );
        assertTrue(
                state.getTableauColumn(1).get(1).isFaceUp()
        );
    }

    @Test
    public void reconstructedSnapshotRestoresWinStatus() {
        GameState state = new GameState();
        GameSnapshot snapshot = reconstructSnapshot(state);
        state.setGameWon(true);
        snapshot.restore(state);
        assertFalse(state.isGameWon());
    }

    @Test
    public void reconstructedSnapshotPreservesUnrelatedGameValues() {
        GameState state = new GameState();
        GameSnapshot snapshot = reconstructSnapshot(state);
        state.setScore(100);
        state.setMoves(25);
        state.setElapsedTimeMs(15000);
        state.setRemainingUndos(1);
        snapshot.restore(state);
        assertEquals(100, state.getScore());
        assertEquals(25, state.getMoves());
        assertEquals(15000L, state.getElapsedTimeMs());
        assertEquals(1, state.getRemainingUndos());
    }

    @Test
    public void reconstructedSnapshotCanBeRestoredMultipleTimes() {
        GameState state = new GameState();
        GameSnapshot snapshot = reconstructSnapshot(state);
        for (int i = 0; i < 3; i++) {
            Card card = state.getStock().remove(
                    state.getStock().size() - 1
            );
            card.setFaceUp(true);
            state.getDiscard().add(card);
            snapshot.restore(state);
            assertSnapshotsEqual(
                    snapshot,
                    new GameSnapshot(state)
            );
        }
    }

    // Valid empty collection tests
    @Test
    public void reconstructedSnapshotAcceptsEmptyPiles() {
        GameState state = new GameState();
        state.getStock().clear();
        state.getDiscard().clear();
        for (Card.Suit suit : Card.Suit.values()) {
            state.getFoundation(suit).clear();
        }
        for (ArrayList<Card> column : state.getTableau()) {
            column.clear();
        }
        GameSnapshot snapshot = reconstructSnapshot(state);
        assertTrue(snapshot.getStock().isEmpty());
        assertTrue(snapshot.getDiscard().isEmpty());
        assertEquals(4, snapshot.getFoundations().size());
        assertEquals(7, snapshot.getTableau().size());
        for (Card.Suit suit : Card.Suit.values()) {
            assertTrue(
                    snapshot.getFoundations().get(suit).isEmpty()
            );
        }
        for (ArrayList<Card> column : snapshot.getTableau()) {
            assertTrue(column.isEmpty());
        }
    }

    // HELPER METHODS

    // Reconstruct Snapshot
    private GameSnapshot reconstructSnapshot(GameState state) {
        return new GameSnapshot(
                state.getStock(),
                state.getDiscard(),
                state.getFoundations(),
                state.getTableau(),
                state.getScore(),
                state.isGameWon()
        );
    }

    // Compare snapshots
    private void assertSnapshotsEqual(
            GameSnapshot expected,
            GameSnapshot actual) {
        assertSnapshotCardListsEqual(
                expected.getStock(),
                actual.getStock()
        );
        assertSnapshotCardListsEqual(
                expected.getDiscard(),
                actual.getDiscard()
        );
        EnumMap<Card.Suit, ArrayList<Card>> expectedFoundations =
                expected.getFoundations();
        EnumMap<Card.Suit, ArrayList<Card>> actualFoundations =
                actual.getFoundations();
        for (Card.Suit suit : Card.Suit.values()) {
            assertSnapshotCardListsEqual(
                    expectedFoundations.get(suit),
                    actualFoundations.get(suit)
            );
        }
        ArrayList<ArrayList<Card>> expectedTableau =
                expected.getTableau();
        ArrayList<ArrayList<Card>> actualTableau =
                actual.getTableau();
        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {
            assertSnapshotCardListsEqual(
                    expectedTableau.get(i),
                    actualTableau.get(i)
            );
        }
        assertEquals(
                expected.getScore(),
                actual.getScore()
        );
        assertEquals(
                expected.isGameWon(),
                actual.isGameWon()
        );
    }

    // Compare individual card collections
    private void assertSnapshotCardListsEqual(
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