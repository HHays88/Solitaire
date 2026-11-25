package com.hays.solitaire.engine;

import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.Deck;
import com.hays.solitaire.model.GameState;
import com.hays.solitaire.model.GameSnapshot;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class GameEngineTest {

    private GameState state;
    private GameEngine engine;

    @Before
    public void setUp() {

        state = new GameState();
        engine = new GameEngine(state);
    }

    // Stock operation tests

    @Test
    public void drawTransfersOneCardToDiscard() {

        Card expected = state.getStock().get(
                state.getStock().size() - 1
        );

        assertTrue(engine.drawCard());

        assertEquals(23, state.getStock().size());
        assertEquals(1, state.getDiscard().size());

        assertSame(expected, state.getDiscard().get(0));
        assertTrue(expected.isFaceUp());
    }

    @Test
    public void multipleDrawsPreserveDiscardOrder() {

        Card first = state.getStock().get(23);
        Card second = state.getStock().get(22);

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        assertEquals(22, state.getStock().size());
        assertEquals(2, state.getDiscard().size());

        assertSame(first, state.getDiscard().get(0));
        assertSame(second, state.getDiscard().get(1));
    }

    @Test
    public void cannotDrawFromEmptyStock() {

        clearPiles();

        assertFalse(engine.drawCard());

        assertTrue(state.getStock().isEmpty());
        assertTrue(state.getDiscard().isEmpty());
    }

    @Test
    public void recycleReturnsCardsToStock() {

        while (!state.getStock().isEmpty()) {
            assertTrue(engine.drawCard());
        }

        assertTrue(engine.recycleDiscard());

        assertEquals(24, state.getStock().size());
        assertTrue(state.getDiscard().isEmpty());

        for (Card card : state.getStock()) {
            assertFalse(card.isFaceUp());
        }
    }

    @Test
    public void recyclePreservesOriginalDrawingOrder() {

        ArrayList<Card> originalStock =
                new ArrayList<>(state.getStock());

        while (!state.getStock().isEmpty()) {
            assertTrue(engine.drawCard());
        }

        assertTrue(engine.recycleDiscard());

        assertEquals(
                originalStock.size(),
                state.getStock().size()
        );

        for (int i = 0; i < originalStock.size(); i++) {

            assertSame(
                    originalStock.get(i),
                    state.getStock().get(i)
            );
        }
    }

    @Test
    public void cannotRecycleWhenStockContainsCards() {

        assertTrue(engine.drawCard());

        assertFalse(engine.recycleDiscard());

        assertEquals(23, state.getStock().size());
        assertEquals(1, state.getDiscard().size());
    }

    @Test
    public void cannotRecycleEmptyDiscard() {

        clearPiles();

        assertFalse(engine.recycleDiscard());
    }

    @Test
    public void multipleRecyclesAreAllowed() {

        for (int cycle = 0; cycle < 5; cycle++) {

            while (!state.getStock().isEmpty()) {
                assertTrue(engine.drawCard());
            }

            assertTrue(engine.recycleDiscard());

            assertEquals(24, state.getStock().size());
            assertTrue(state.getDiscard().isEmpty());
        }
    }

    // Tableau to tableau tests

    @Test
    public void tableauTransfersSingleCard() {

        clearPiles();

        Card queen = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        );

        Card king = faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        );

        state.getTableauColumn(0).add(queen);
        state.getTableauColumn(1).add(king);

        assertTrue(engine.moveTableauToTableau(
                0, 0, 1
        ));

        assertTrue(state.getTableauColumn(0).isEmpty());
        assertEquals(2, state.getTableauColumn(1).size());

        assertSame(king, state.getTableauColumn(1).get(0));
        assertSame(queen, state.getTableauColumn(1).get(1));
    }

    @Test
    public void tableauTransfersSequenceInCorrectOrder() {

        clearPiles();

        Card queen = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        );

        Card jack = faceUpCard(
                Card.Suit.CLUBS, Card.Rank.JACK
        );

        Card ten = faceUpCard(
                Card.Suit.DIAMONDS, Card.Rank.TEN
        );

        Card king = faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        );

        state.getTableauColumn(0).add(queen);
        state.getTableauColumn(0).add(jack);
        state.getTableauColumn(0).add(ten);

        state.getTableauColumn(1).add(king);

        assertTrue(engine.moveTableauToTableau(
                0, 0, 1
        ));

        assertTrue(state.getTableauColumn(0).isEmpty());

        ArrayList<Card> destination =
                state.getTableauColumn(1);

        assertEquals(4, destination.size());

        assertSame(king, destination.get(0));
        assertSame(queen, destination.get(1));
        assertSame(jack, destination.get(2));
        assertSame(ten, destination.get(3));
    }

    @Test
    public void tableauTransfersKingSequenceToEmptyColumn() {

        clearPiles();

        Card king = faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        );

        Card queen = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        );

        state.getTableauColumn(0).add(king);
        state.getTableauColumn(0).add(queen);

        assertTrue(engine.moveTableauToTableau(
                0, 0, 1
        ));

        assertTrue(state.getTableauColumn(0).isEmpty());
        assertEquals(2, state.getTableauColumn(1).size());

        assertSame(king, state.getTableauColumn(1).get(0));
        assertSame(queen, state.getTableauColumn(1).get(1));
    }

    @Test
    public void tableauRevealsExposedCardAfterMove() {

        clearPiles();

        Card hidden = new Card(
                Card.Suit.DIAMONDS, Card.Rank.FIVE
        );

        Card queen = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        );

        Card king = faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        );

        state.getTableauColumn(0).add(hidden);
        state.getTableauColumn(0).add(queen);

        state.getTableauColumn(1).add(king);

        assertFalse(hidden.isFaceUp());

        assertTrue(engine.moveTableauToTableau(
                0, 1, 1
        ));

        assertEquals(1, state.getTableauColumn(0).size());
        assertTrue(hidden.isFaceUp());
    }

    @Test
    public void invalidTableauMoveDoesNotModifyBoard() {

        clearPiles();

        Card queen = faceUpCard(
                Card.Suit.CLUBS, Card.Rank.QUEEN
        );

        Card king = faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        );

        state.getTableauColumn(0).add(queen);
        state.getTableauColumn(1).add(king);

        GameSnapshot before = new GameSnapshot(state);

        assertFalse(engine.moveTableauToTableau(
                0, 0, 1
        ));

        assertBoardMatchesSnapshot(before);
    }

    // Tableau to foundation tests

    @Test
    public void tableauTransfersCardToFoundation() {

        clearPiles();

        Card ace = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        );

        state.getTableauColumn(0).add(ace);

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertTrue(state.getTableauColumn(0).isEmpty());

        assertEquals(
                1,
                state.getFoundation(Card.Suit.HEARTS).size()
        );

        assertSame(
                ace,
                state.getFoundation(Card.Suit.HEARTS).get(0)
        );
    }

    @Test
    public void foundationMoveRevealsExposedTableauCard() {

        clearPiles();

        Card hidden = new Card(
                Card.Suit.CLUBS, Card.Rank.JACK
        );

        Card ace = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        );

        state.getTableauColumn(0).add(hidden);
        state.getTableauColumn(0).add(ace);

        assertFalse(hidden.isFaceUp());

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertEquals(1, state.getTableauColumn(0).size());
        assertTrue(hidden.isFaceUp());
    }

    @Test
    public void invalidFoundationMoveDoesNotRevealCard() {

        clearPiles();

        Card hidden = new Card(
                Card.Suit.CLUBS, Card.Rank.JACK
        );

        Card two = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.TWO
        );

        state.getTableauColumn(0).add(hidden);
        state.getTableauColumn(0).add(two);

        GameSnapshot before = new GameSnapshot(state);

        assertFalse(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertFalse(hidden.isFaceUp());
        assertBoardMatchesSnapshot(before);
    }

    // Discard to tableau tests

    @Test
    public void discardTransfersTopCardToTableau() {

        clearPiles();

        Card ace = faceUpCard(
                Card.Suit.CLUBS, Card.Rank.ACE
        );

        Card king = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.KING
        );

        state.getDiscard().add(ace);
        state.getDiscard().add(king);

        assertTrue(engine.moveDiscardToTableau(0));

        assertEquals(1, state.getDiscard().size());
        assertSame(ace, state.getDiscard().get(0));

        assertEquals(1, state.getTableauColumn(0).size());
        assertSame(king, state.getTableauColumn(0).get(0));
    }

    @Test
    public void invalidDiscardToTableauMoveChangesNothing() {

        clearPiles();

        state.getDiscard().add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.QUEEN
        ));

        GameSnapshot before = new GameSnapshot(state);

        assertFalse(engine.moveDiscardToTableau(0));

        assertBoardMatchesSnapshot(before);
    }

    // Discard to foundation tests

    @Test
    public void discardTransfersTopCardToFoundation() {

        clearPiles();

        Card king = faceUpCard(
                Card.Suit.SPADES, Card.Rank.KING
        );

        Card ace = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        );

        state.getDiscard().add(king);
        state.getDiscard().add(ace);

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertEquals(1, state.getDiscard().size());
        assertSame(king, state.getDiscard().get(0));

        assertSame(
                ace,
                state.getFoundation(Card.Suit.HEARTS).get(0)
        );
    }

    @Test
    public void invalidDiscardToFoundationMoveChangesNothing() {

        clearPiles();

        state.getDiscard().add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        ));

        GameSnapshot before = new GameSnapshot(state);

        assertFalse(engine.moveDiscardToFoundation(
                Card.Suit.CLUBS
        ));

        assertBoardMatchesSnapshot(before);
    }

    // Foundation to tableau tests

    @Test
    public void foundationTransfersCardToTableau() {

        clearPiles();

        Card ace = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        );

        Card two = faceUpCard(
                Card.Suit.SPADES, Card.Rank.TWO
        );

        state.getFoundation(Card.Suit.HEARTS).add(ace);
        state.getTableauColumn(0).add(two);

        assertTrue(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        assertTrue(
                state.getFoundation(Card.Suit.HEARTS).isEmpty()
        );

        assertEquals(2, state.getTableauColumn(0).size());
        assertSame(ace, state.getTableauColumn(0).get(1));
    }

    @Test
    public void invalidFoundationToTableauMoveChangesNothing() {

        clearPiles();

        state.getFoundation(Card.Suit.HEARTS).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        GameSnapshot before = new GameSnapshot(state);

        assertFalse(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        assertBoardMatchesSnapshot(before);
    }

    // Snapshot integration test

    @Test
    public void snapshotReversesMoveAndAutomaticReveal() {

        clearPiles();

        Card hidden = new Card(
                Card.Suit.CLUBS, Card.Rank.JACK
        );

        Card ace = faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        );

        state.getTableauColumn(0).add(hidden);
        state.getTableauColumn(0).add(ace);

        GameSnapshot before = new GameSnapshot(state);

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertTrue(hidden.isFaceUp());

        before.restore(state);

        assertBoardMatchesSnapshot(before);

        assertFalse(
                state.getTableauColumn(0).get(0).isFaceUp()
        );

        assertTrue(
                state.getTableauColumn(0).get(1).isFaceUp()
        );
    }

    // Win detection tests

    @Test
    public void incompleteFoundationsDoNotWinGame() {

        clearPiles();

        state.getDiscard().add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.ACE
        ));

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertFalse(state.isGameWon());
    }

    @Test
    public void finalTableauCardCompletesGame() {

        prepareNearWin();

        state.getTableauColumn(0).add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.KING
        ));

        assertFalse(state.isGameWon());

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertTrue(state.isGameWon());
        assertEquals(52, countCards());
    }

    @Test
    public void finalDiscardCardCompletesGame() {

        prepareNearWin();

        state.getDiscard().add(faceUpCard(
                Card.Suit.HEARTS, Card.Rank.KING
        ));

        assertFalse(state.isGameWon());

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertTrue(state.isGameWon());
        assertEquals(52, countCards());
    }

    @Test
    public void completedGameRejectsAllGameplayOperations() {

        state.setGameWon(true);

        GameSnapshot before = new GameSnapshot(state);

        assertFalse(engine.drawCard());
        assertFalse(engine.recycleDiscard());

        assertFalse(engine.moveTableauToTableau(
                0, 0, 1
        ));

        assertFalse(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertFalse(engine.moveDiscardToTableau(0));

        assertFalse(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertFalse(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        assertTrue(state.isGameWon());
        assertBoardMatchesSnapshot(before);
    }

    // Card conservation tests

    @Test
    public void drawingAndRecyclingPreserveAllCards() {

        for (int cycle = 0; cycle < 3; cycle++) {

            while (!state.getStock().isEmpty()) {

                assertTrue(engine.drawCard());
                assertEquals(52, countCards());
                assertEquals(52, countUniqueCards());
            }

            assertTrue(engine.recycleDiscard());

            assertEquals(52, countCards());
            assertEquals(52, countUniqueCards());
        }
    }

    @Test
    public void transfersPreserveAllCards() {

        clearPiles();

        ArrayList<Card> deck = Deck.createDeck();

        for (Card card : deck) {

            if (card.getSuit() == Card.Suit.HEARTS
                    && card.getRank() == Card.Rank.ACE) {

                card.setFaceUp(true);
                state.getDiscard().add(card);

            } else if (card.getSuit() == Card.Suit.SPADES
                    && card.getRank() == Card.Rank.TWO) {

                card.setFaceUp(true);
                state.getTableauColumn(0).add(card);

            } else {

                state.getStock().add(card);
            }
        }

        assertEquals(52, countCards());

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertEquals(52, countCards());
        assertEquals(52, countUniqueCards());

        assertTrue(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        assertEquals(52, countCards());
        assertEquals(52, countUniqueCards());

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertEquals(52, countCards());
        assertEquals(52, countUniqueCards());
    }

    // Getter test

    @Test
    public void engineReferencesProvidedGameState() {

        assertSame(state, engine.getState());
    }

    // Helper methods

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

    private void prepareNearWin() {

        clearPiles();

        for (Card.Suit suit : Card.Suit.values()) {

            for (Card.Rank rank : Card.Rank.values()) {

                if (suit == Card.Suit.HEARTS
                        && rank == Card.Rank.KING) {

                    continue;
                }

                state.getFoundation(suit).add(
                        faceUpCard(suit, rank)
                );
            }
        }
    }

    private ArrayList<Card> getAllCards() {

        ArrayList<Card> cards = new ArrayList<>();

        cards.addAll(state.getStock());
        cards.addAll(state.getDiscard());

        for (Card.Suit suit : Card.Suit.values()) {
            cards.addAll(state.getFoundation(suit));
        }

        for (ArrayList<Card> column : state.getTableau()) {
            cards.addAll(column);
        }

        return cards;
    }

    private int countCards() {

        return getAllCards().size();
    }

    private int countUniqueCards() {

        Set<String> uniqueCards = new HashSet<>();

        for (Card card : getAllCards()) {
            uniqueCards.add(card.toString());
        }

        return uniqueCards.size();
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