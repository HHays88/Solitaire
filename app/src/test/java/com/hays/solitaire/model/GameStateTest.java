package com.hays.solitaire.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
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
        state.setRemainingUndos(1);
        state.setGameWon(true);

        state.restart();

        assertEquals(0, state.getScore());
        assertEquals(0, state.getMoves());
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
    public void remainingUndosStayWithinLimits() {

        GameState state = new GameState();

        state.setRemainingUndos(5);
        assertEquals(3, state.getRemainingUndos());

        state.setRemainingUndos(-1);
        assertEquals(0, state.getRemainingUndos());
    }

    // Supplied deck initialization tests

    @Test
    public void constructorPreservesSuppliedDeckOrder() {

        ArrayList<Card> deck = Deck.createDeck();

        GameState state = new GameState(deck);

        assertCardListsEqual(deck, state.getOriginalDeck());
    }

    @Test
    public void constructorCreatesValidInitialBoard() {

        GameState state = new GameState(Deck.createDeck());

        assertEquals(24, state.getStock().size());
        assertTrue(state.getDiscard().isEmpty());
        assertEquals(4, state.getFoundations().size());
        assertEquals(7, state.getTableau().size());

        for (Card.Suit suit : Card.Suit.values()) {
            assertTrue(state.getFoundation(suit).isEmpty());
        }

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {
            assertEquals(i + 1,
                    state.getTableauColumn(i).size());
        }
    }

    @Test
    public void constructorInitializesGameValues() {

        GameState state = new GameState(Deck.createDeck());

        assertEquals(0, state.getScore());
        assertEquals(0, state.getMoves());
        assertEquals(3, state.getRemainingUndos());
        assertFalse(state.isGameWon());
    }

    @Test
    public void identicalDecksProduceIdenticalDeals() {

        ArrayList<Card> deck = Deck.createShuffledDeck();

        GameState first = new GameState(deck);
        GameState second = new GameState(deck);

        assertCardListsEqual(
                first.getStock(),
                second.getStock()
        );

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertCardListsEqual(
                    first.getTableauColumn(i),
                    second.getTableauColumn(i)
            );
        }
    }

    // Deterministic dealing tests

    @Test
    public void suppliedDeckDeterminesFirstTableauRow() {

        ArrayList<Card> deck = Deck.createDeck();

        GameState state = new GameState(deck);

        for (int column = 0;
             column < GameState.TABLEAU_COLUMNS;
             column++) {

            Card expected = deck.get(column);

            Card actual =
                    state.getTableauColumn(column).get(0);

            assertEquals(expected.getSuit(), actual.getSuit());
            assertEquals(expected.getRank(), actual.getRank());
        }
    }

    @Test
    public void suppliedDeckDeterminesFirstStockCard() {

        ArrayList<Card> deck = Deck.createDeck();

        GameState state = new GameState(deck);

        Card expected = deck.get(28);

        Card actual = state.getStock().get(
                state.getStock().size() - 1
        );

        assertEquals(expected.getSuit(), actual.getSuit());
        assertEquals(expected.getRank(), actual.getRank());
        assertFalse(actual.isFaceUp());
    }

    @Test
    public void suppliedDeckDeterminesEntireStockOrder() {

        ArrayList<Card> deck = Deck.createDeck();

        GameState state = new GameState(deck);

        for (int i = 0; i < state.getStock().size(); i++) {

            Card expected = deck.get(deck.size() - 1 - i);
            Card actual = state.getStock().get(i);

            assertEquals(expected.getSuit(), actual.getSuit());
            assertEquals(expected.getRank(), actual.getRank());
            assertFalse(actual.isFaceUp());
        }
    }

    // Defensive copying tests

    @Test
    public void modifyingSuppliedListDoesNotChangeOriginalDeck() {

        ArrayList<Card> deck = Deck.createDeck();

        GameState state = new GameState(deck);

        deck.clear();

        assertEquals(52, state.getOriginalDeck().size());
        assertEquals(24, state.getStock().size());
    }

    @Test
    public void modifyingSuppliedCardDoesNotChangeOriginalDeck() {

        ArrayList<Card> deck = Deck.createDeck();

        GameState state = new GameState(deck);

        deck.get(0).setFaceUp(true);

        assertFalse(
                state.getOriginalDeck().get(0).isFaceUp()
        );
    }

    @Test
    public void constructorDoesNotModifySuppliedDeck() {

        ArrayList<Card> deck = Deck.createDeck();

        new GameState(deck);

        assertEquals(52, deck.size());

        for (Card card : deck) {
            assertFalse(card.isFaceUp());
        }
    }

    @Test
    public void dealingDoesNotModifyStoredOriginalDeck() {

        GameState state = new GameState(Deck.createDeck());

        for (Card card : state.getOriginalDeck()) {
            assertFalse(card.isFaceUp());
        }
    }

    @Test
    public void statesCreatedFromSameDeckAreIndependent() {

        ArrayList<Card> deck = Deck.createDeck();

        GameState first = new GameState(deck);
        GameState second = new GameState(deck);

        first.getStock().get(0).setFaceUp(true);

        first.getStock().remove(
                first.getStock().size() - 1
        );

        assertEquals(23, first.getStock().size());
        assertEquals(24, second.getStock().size());

        assertFalse(second.getStock().get(0).isFaceUp());
    }

    // Original deck validation tests

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNullDeck() {

        new GameState(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsEmptyDeck() {

        new GameState(new ArrayList<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsIncompleteDeck() {

        ArrayList<Card> deck = Deck.createDeck();

        deck.remove(0);

        new GameState(deck);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsOversizedDeck() {

        ArrayList<Card> deck = Deck.createDeck();

        deck.add(
                new Card(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        new GameState(deck);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsDuplicateCards() {

        ArrayList<Card> deck = Deck.createDeck();

        deck.set(0, deck.get(1).copy());

        new GameState(deck);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNullCard() {

        ArrayList<Card> deck = Deck.createDeck();

        deck.set(0, null);

        new GameState(deck);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNullSuit() {

        ArrayList<Card> deck = Deck.createDeck();

        deck.set(0, new Card(null, Card.Rank.ACE));

        new GameState(deck);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNullRank() {

        ArrayList<Card> deck = Deck.createDeck();

        deck.set(0, new Card(Card.Suit.HEARTS, null));

        new GameState(deck);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsFaceUpOriginalCard() {

        ArrayList<Card> deck = Deck.createDeck();

        deck.get(0).setFaceUp(true);

        new GameState(deck);
    }

    // Supplied deck restart and new game tests

    @Test
    public void restartRestoresSuppliedDeckDeal() {

        ArrayList<Card> deck = Deck.createDeck();

        GameState state = new GameState(deck);

        GameState expected = new GameState(deck);

        state.getStock().clear();
        state.getTableauColumn(0).clear();

        state.restart();

        assertCardListsEqual(
                expected.getStock(),
                state.getStock()
        );

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertCardListsEqual(
                    expected.getTableauColumn(i),
                    state.getTableauColumn(i)
            );
        }
    }

    @Test
    public void restartPreservesSuppliedOriginalDeck() {

        ArrayList<Card> deck = Deck.createDeck();

        GameState state = new GameState(deck);

        state.restart();
        state.restart();

        assertCardListsEqual(deck, state.getOriginalDeck());
    }

    @Test
    public void newGameAfterSuppliedDeckCreatesValidDeck() {

        GameState state = new GameState(Deck.createDeck());

        state.newGame();

        ArrayList<Card> newDeck = state.getOriginalDeck();

        assertEquals(52, newDeck.size());

        // Constructing another state validates the new deck.
        GameState reconstructed = new GameState(newDeck);

        assertCardListsEqual(
                reconstructed.getStock(),
                state.getStock()
        );

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertCardListsEqual(
                    reconstructed.getTableauColumn(i),
                    state.getTableauColumn(i)
            );
        }
    }



    // HELPER METHODS
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

    private void assertCardListsEqual(
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