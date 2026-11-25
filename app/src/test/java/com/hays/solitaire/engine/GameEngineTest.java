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

    // Scoring Tests

    @Test
    public void drawingCardIncrementsMovesWithoutChangingScore() {
        state.setScore(25);
        assertTrue(engine.drawCard());
        assertEquals(25, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void multipleDrawsIncrementMovesIndividually() {
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertEquals(0, state.getScore());
        assertEquals(3, state.getMoves());
    }

    @Test
    public void recyclingDeductsTwentyPointsAndCountsOneMove() {
        while (!state.getStock().isEmpty()) {
            assertTrue(engine.drawCard());
        }
        state.setScore(50);
        int previousMoves = state.getMoves();
        assertTrue(engine.recycleDiscard());
        assertEquals(30, state.getScore());
        assertEquals(previousMoves + 1, state.getMoves());
    }

    @Test
    public void recyclingCannotReduceScoreBelowZero() {
        while (!state.getStock().isEmpty()) {
            assertTrue(engine.drawCard());
        }
        state.setScore(15);
        int previousMoves = state.getMoves();
        assertTrue(engine.recycleDiscard());
        assertEquals(0, state.getScore());
        assertEquals(previousMoves + 1, state.getMoves());
    }

    @Test
    public void recyclingAtZeroStillCountsAsMove() {
        while (!state.getStock().isEmpty()) {
            assertTrue(engine.drawCard());
        }
        int previousMoves = state.getMoves();
        assertTrue(engine.recycleDiscard());
        assertEquals(0, state.getScore());
        assertEquals(previousMoves + 1, state.getMoves());
    }

    @Test
    public void repeatedRecyclesApplyPenaltyEachTime() {
        state.setScore(100);
        for (int cycle = 0; cycle < 3; cycle++) {
            while (!state.getStock().isEmpty()) {
                assertTrue(engine.drawCard());
            }
            assertTrue(engine.recycleDiscard());
        }
        assertEquals(40, state.getScore());
        assertEquals(75, state.getMoves());
    }

    // Tableau to tableau scoring tests
    // Tableau to tableau scoring tests

    @Test
    public void tableauTransferWithoutRevealAwardsNoPoints() {

        clearPiles();

        state.setScore(25);

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.QUEEN)
        );

        state.getTableauColumn(1).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.KING)
        );

        assertTrue(engine.moveTableauToTableau(
                0, 0, 1
        ));

        assertEquals(25, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void tableauTransferWithRevealAwardsFivePoints() {

        clearPiles();

        Card hidden = new Card(
                Card.Suit.DIAMONDS, Card.Rank.FIVE
        );

        state.getTableauColumn(0).add(hidden);

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.QUEEN)
        );

        state.getTableauColumn(1).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.KING)
        );

        assertTrue(engine.moveTableauToTableau(
                0, 1, 1
        ));

        assertTrue(hidden.isFaceUp());
        assertEquals(5, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void exposingAlreadyFaceUpCardAwardsNoRevealPoints() {

        clearPiles();

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.CLUBS, Card.Rank.KING)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.QUEEN)
        );

        state.getTableauColumn(1).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.KING)
        );

        assertTrue(engine.moveTableauToTableau(
                0, 1, 1
        ));

        assertEquals(0, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void transferringSequenceCountsAsOneMove() {

        clearPiles();

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.QUEEN)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.CLUBS, Card.Rank.JACK)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.DIAMONDS, Card.Rank.TEN)
        );

        state.getTableauColumn(1).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.KING)
        );

        assertTrue(engine.moveTableauToTableau(
                0, 0, 1
        ));

        assertEquals(4, state.getTableauColumn(1).size());
        assertEquals(0, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void transferringSequenceWithRevealCountsAsOneMove() {

        clearPiles();

        Card hidden = new Card(
                Card.Suit.CLUBS, Card.Rank.FIVE
        );

        state.getTableauColumn(0).add(hidden);

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.QUEEN)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.CLUBS, Card.Rank.JACK)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.DIAMONDS, Card.Rank.TEN)
        );

        state.getTableauColumn(1).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.KING)
        );

        assertTrue(engine.moveTableauToTableau(
                0, 1, 1
        ));

        assertTrue(hidden.isFaceUp());
        assertEquals(4, state.getTableauColumn(1).size());
        assertEquals(5, state.getScore());
        assertEquals(1, state.getMoves());
    }

    // Tableau to foundation scoring tests
    // Tableau to foundation scoring tests

    @Test
    public void tableauToFoundationAwardsTenPoints() {

        clearPiles();

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertEquals(10, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void tableauToFoundationWithRevealAwardsFifteenPoints() {

        clearPiles();

        Card hidden = new Card(
                Card.Suit.CLUBS, Card.Rank.JACK
        );

        state.getTableauColumn(0).add(hidden);

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertTrue(hidden.isFaceUp());
        assertEquals(15, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void tableauToFoundationWithoutNewRevealAwardsTenPoints() {

        clearPiles();

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.CLUBS, Card.Rank.TWO)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertEquals(10, state.getScore());
        assertEquals(1, state.getMoves());
    }

    // Discard scoring tests
    // Discard scoring tests

    @Test
    public void discardToTableauAwardsFivePoints() {

        clearPiles();

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.KING)
        );

        assertTrue(engine.moveDiscardToTableau(0));

        assertEquals(5, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void discardToFoundationAwardsTenPoints() {

        clearPiles();

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertEquals(10, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void exposingDiscardCardDoesNotAwardRevealPoints() {

        clearPiles();

        state.getDiscard().add(
                faceUpCard(Card.Suit.CLUBS, Card.Rank.FIVE)
        );

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertEquals(1, state.getDiscard().size());
        assertEquals(10, state.getScore());
        assertEquals(1, state.getMoves());
    }

    // Foundation to tableau scoring tests
    // Foundation removal scoring tests

    @Test
    public void foundationToTableauDeductsTenPoints() {

        clearPiles();

        state.setScore(30);

        state.getFoundation(Card.Suit.HEARTS).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.TWO)
        );

        assertTrue(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        assertEquals(20, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void foundationRemovalCannotReduceScoreBelowZero() {

        clearPiles();

        state.setScore(5);

        state.getFoundation(Card.Suit.HEARTS).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.TWO)
        );

        assertTrue(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        assertEquals(0, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void repeatedFoundationTransfersDoNotAccumulatePoints() {

        clearPiles();

        state.setScore(50);

        state.getFoundation(Card.Suit.HEARTS).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.TWO)
        );

        for (int cycle = 0; cycle < 5; cycle++) {

            assertTrue(engine.moveFoundationToTableau(
                    Card.Suit.HEARTS, 0
            ));

            assertTrue(engine.moveTableauToFoundation(
                    0, Card.Suit.HEARTS
            ));
        }

        assertEquals(50, state.getScore());
        assertEquals(10, state.getMoves());
    }

    // Invalid action scoring tests
    // Invalid action scoring tests

    @Test
    public void invalidDrawDoesNotChangeScoreOrMoves() {

        clearPiles();

        state.setScore(50);
        state.setMoves(10);

        assertFalse(engine.drawCard());

        assertEquals(50, state.getScore());
        assertEquals(10, state.getMoves());
    }

    @Test
    public void invalidRecycleDoesNotChangeScoreOrMoves() {

        state.setScore(50);
        state.setMoves(10);

        assertFalse(engine.recycleDiscard());

        assertEquals(50, state.getScore());
        assertEquals(10, state.getMoves());
    }

    @Test
    public void invalidCardTransfersDoNotChangeScoreOrMoves() {

        clearPiles();

        state.setScore(50);
        state.setMoves(10);

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

        assertEquals(50, state.getScore());
        assertEquals(10, state.getMoves());
    }

    @Test
    public void rejectedTableauMoveDoesNotAwardRevealPoints() {

        clearPiles();

        state.setScore(50);
        state.setMoves(10);

        Card hidden = new Card(
                Card.Suit.DIAMONDS, Card.Rank.FIVE
        );

        state.getTableauColumn(0).add(hidden);

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.CLUBS, Card.Rank.QUEEN)
        );

        state.getTableauColumn(1).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.KING)
        );

        assertFalse(engine.moveTableauToTableau(
                0, 1, 1
        ));

        assertFalse(hidden.isFaceUp());
        assertEquals(50, state.getScore());
        assertEquals(10, state.getMoves());
    }

    @Test
    public void completedGameDoesNotAllowScoringOrMoveChanges() {

        prepareNearWin();

        state.getFoundation(Card.Suit.HEARTS).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.KING)
        );

        state.setGameWon(true);
        state.setScore(600);
        state.setMoves(150);

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

        assertEquals(600, state.getScore());
        assertEquals(150, state.getMoves());
        assertTrue(state.isGameWon());
    }

    // Winning move scoring tests
    // Winning move scoring tests

    @Test
    public void finalTableauMoveUpdatesScoreAndMoveCount() {

        prepareNearWin();

        state.setScore(590);
        state.setMoves(149);

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.KING)
        );

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertTrue(state.isGameWon());
        assertEquals(600, state.getScore());
        assertEquals(150, state.getMoves());
    }

    @Test
    public void finalDiscardMoveUpdatesScoreAndMoveCount() {

        prepareNearWin();

        state.setScore(590);
        state.setMoves(149);

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.KING)
        );

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertTrue(state.isGameWon());
        assertEquals(600, state.getScore());
        assertEquals(150, state.getMoves());
    }

    // Combined gameplay scoring tests
    // Combined gameplay scoring tests

    @Test
    public void differentGameplayActionsAccumulateScoreAndMoves() {

        clearPiles();

        Card ace = new Card(
                Card.Suit.HEARTS, Card.Rank.ACE
        );

        state.getStock().add(ace);

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.TWO)
        );

        // Draw: 0 points, +1 move

        assertTrue(engine.drawCard());

        assertEquals(0, state.getScore());
        assertEquals(1, state.getMoves());

        // Discard to tableau: +5 points, +1 move

        assertTrue(engine.moveDiscardToTableau(0));

        assertEquals(5, state.getScore());
        assertEquals(2, state.getMoves());

        // Tableau to foundation: +10 points, +1 move

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertEquals(15, state.getScore());
        assertEquals(3, state.getMoves());

        // Foundation to tableau: -10 points, +1 move

        assertTrue(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        assertEquals(5, state.getScore());
        assertEquals(4, state.getMoves());

        // Tableau to foundation: +10 points, +1 move

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertEquals(15, state.getScore());
        assertEquals(5, state.getMoves());
    }

    @Test
    public void normalGameplayDoesNotConsumeUndosOrChangeTime() {

        state.setRemainingUndos(2);
        state.setElapsedTimeMs(5000);

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        assertEquals(2, state.getRemainingUndos());
        assertEquals(5000L, state.getElapsedTimeMs());
        assertEquals(2, state.getMoves());
    }

    // Automatic history recording tests

    @Test
    public void newEngineHasNoUndoHistory() {

        assertEquals(0, engine.getUndoHistorySize());
        assertFalse(engine.canUndo());
        assertEquals(3, state.getRemainingUndos());
    }

    @Test
    public void successfulActionAutomaticallyRecordsHistory() {

        assertTrue(engine.drawCard());

        assertEquals(1, engine.getUndoHistorySize());
        assertTrue(engine.canUndo());
        assertEquals(3, state.getRemainingUndos());
    }

    @Test
    public void invalidActionsDoNotCreateHistory() {

        clearPiles();

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

        assertEquals(0, engine.getUndoHistorySize());
        assertFalse(engine.canUndo());
    }

    @Test
    public void invalidActionPreservesExistingHistory() {

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.drawCard());

        assertFalse(engine.recycleDiscard());

        assertEquals(1, engine.getUndoHistorySize());

        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(snapshot);
    }

    @Test
    public void automaticHistoryRespectsMaximumSize() {

        for (int i = 0; i < 5; i++) {
            assertTrue(engine.drawCard());
        }

        assertEquals(3, engine.getUndoHistorySize());
        assertEquals(3, state.getRemainingUndos());
    }

    @Test
    public void exhaustedUndoAllowancePreventsRecording() {

        state.setRemainingUndos(0);

        assertTrue(engine.drawCard());

        assertEquals(0, engine.getUndoHistorySize());
        assertFalse(engine.canUndo());
        assertEquals(1, state.getMoves());
    }

    // Gameplay undo integration tests

    @Test
    public void undoDrawRestoresBoardAndAppliesPenalty() {

        state.setScore(50);

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.drawCard());
        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertEquals(40, state.getScore());
        assertEquals(2, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
        assertEquals(0, engine.getUndoHistorySize());
    }

    @Test
    public void undoRecycleRestoresBoardWithoutRefundingPenalty() {

        state.setScore(100);

        while (!state.getStock().isEmpty()) {
            assertTrue(engine.drawCard());
        }

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.recycleDiscard());

        assertEquals(80, state.getScore());

        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertEquals(70, state.getScore());
        assertEquals(26, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void undoTableauTransferRestoresBoard() {

        clearPiles();

        state.setScore(50);

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.QUEEN)
        );

        state.getTableauColumn(1).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.KING)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.moveTableauToTableau(
                0, 0, 1
        ));

        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertEquals(40, state.getScore());
        assertEquals(2, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void undoTableauSequenceReversesAutomaticReveal() {

        clearPiles();

        state.setScore(50);

        state.getTableauColumn(0).add(
                new Card(Card.Suit.DIAMONDS, Card.Rank.FIVE)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.QUEEN)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.CLUBS, Card.Rank.JACK)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.DIAMONDS, Card.Rank.TEN)
        );

        state.getTableauColumn(1).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.KING)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.moveTableauToTableau(
                0, 1, 1
        ));

        assertTrue(
                state.getTableauColumn(0).get(0).isFaceUp()
        );

        assertEquals(55, state.getScore());
        assertEquals(1, engine.getUndoHistorySize());

        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertFalse(
                state.getTableauColumn(0).get(0).isFaceUp()
        );

        assertEquals(40, state.getScore());
        assertEquals(2, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void undoTableauToFoundationReversesFoundationPoints() {

        clearPiles();

        state.setScore(50);

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertEquals(60, state.getScore());

        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertEquals(40, state.getScore());
        assertEquals(2, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void undoFoundationMoveReversesBothScoringEvents() {

        clearPiles();

        state.setScore(50);

        state.getTableauColumn(0).add(
                new Card(Card.Suit.CLUBS, Card.Rank.JACK)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.moveTableauToFoundation(
                0, Card.Suit.HEARTS
        ));

        assertEquals(65, state.getScore());
        assertEquals(1, engine.getUndoHistorySize());

        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertFalse(
                state.getTableauColumn(0).get(0).isFaceUp()
        );

        assertEquals(40, state.getScore());
        assertEquals(2, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void undoDiscardToTableauReversesAwardedPoints() {

        clearPiles();

        state.setScore(50);

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.KING)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.moveDiscardToTableau(0));

        assertEquals(55, state.getScore());

        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertEquals(40, state.getScore());
        assertEquals(2, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void undoDiscardToFoundationReversesAwardedPoints() {

        clearPiles();

        state.setScore(50);

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertEquals(60, state.getScore());

        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertEquals(40, state.getScore());
        assertEquals(2, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void undoFoundationRemovalReversesDeduction() {

        clearPiles();

        state.setScore(50);

        state.getFoundation(Card.Suit.HEARTS).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.TWO)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        assertEquals(40, state.getScore());

        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertEquals(40, state.getScore());
        assertEquals(2, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
    }

    // Undo allowance and consecutive undo tests

    @Test
    public void engineEnforcesThreeUndoLimit() {

        state.setScore(100);

        for (int i = 0; i < 5; i++) {
            assertTrue(engine.drawCard());
        }

        assertTrue(engine.undo());
        assertTrue(engine.undo());
        assertTrue(engine.undo());

        assertFalse(engine.undo());
        assertFalse(engine.canUndo());

        assertEquals(0, state.getRemainingUndos());
        assertEquals(0, engine.getUndoHistorySize());
        assertEquals(70, state.getScore());
        assertEquals(8, state.getMoves());
        assertEquals(2, state.getDiscard().size());
    }

    @Test
    public void newActionsDoNotRestoreUsedUndos() {

        assertTrue(engine.drawCard());
        assertTrue(engine.undo());

        assertEquals(2, state.getRemainingUndos());

        for (int i = 0; i < 5; i++) {
            assertTrue(engine.drawCard());
        }

        assertEquals(2, state.getRemainingUndos());
        assertEquals(2, engine.getUndoHistorySize());

        assertTrue(engine.undo());
        assertTrue(engine.undo());

        assertFalse(engine.canUndo());
        assertEquals(0, state.getRemainingUndos());
    }

    @Test
    public void gameplayContinuesAfterUndosAreExhausted() {

        for (int i = 0; i < 3; i++) {

            assertTrue(engine.drawCard());
            assertTrue(engine.undo());
        }

        assertEquals(0, state.getRemainingUndos());

        assertTrue(engine.drawCard());

        assertEquals(1, state.getDiscard().size());
        assertEquals(7, state.getMoves());
        assertEquals(0, engine.getUndoHistorySize());
        assertFalse(engine.canUndo());
    }

    @Test
    public void consecutiveUndosRestoreCorrectBoardOrder() {

        GameSnapshot initial = new GameSnapshot(state);

        assertTrue(engine.drawCard());

        GameSnapshot afterFirstDraw =
                new GameSnapshot(state);

        assertTrue(engine.drawCard());

        GameSnapshot afterSecondDraw =
                new GameSnapshot(state);

        assertTrue(engine.drawCard());

        assertTrue(engine.undo());
        assertBoardMatchesSnapshot(afterSecondDraw);

        assertTrue(engine.undo());
        assertBoardMatchesSnapshot(afterFirstDraw);

        assertTrue(engine.undo());
        assertBoardMatchesSnapshot(initial);
    }

    @Test
    public void unsuccessfulEngineUndoDoesNotChangeGameValues() {

        state.setScore(50);
        state.setMoves(10);
        state.setElapsedTimeMs(5000);

        GameSnapshot snapshot = new GameSnapshot(state);

        assertFalse(engine.undo());

        assertBoardMatchesSnapshot(snapshot);

        assertEquals(50, state.getScore());
        assertEquals(10, state.getMoves());
        assertEquals(5000L, state.getElapsedTimeMs());
        assertEquals(3, state.getRemainingUndos());
    }

    @Test
    public void engineUndoPreservesElapsedTime() {

        state.setElapsedTimeMs(5000);

        assertTrue(engine.drawCard());

        state.setElapsedTimeMs(15000);

        assertTrue(engine.undo());

        assertEquals(15000L, state.getElapsedTimeMs());
    }

    // Undo scoring edge cases

    @Test
    public void consecutiveUndosPreservePreviousUndoPenalties() {

        clearPiles();

        state.setScore(50);

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        state.getStock().add(
                new Card(Card.Suit.CLUBS, Card.Rank.FIVE)
        );

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertEquals(60, state.getScore());

        assertTrue(engine.drawCard());

        assertTrue(engine.undo());

        assertEquals(50, state.getScore());

        assertTrue(engine.undo());

        assertEquals(30, state.getScore());
        assertEquals(4, state.getMoves());
        assertEquals(1, state.getRemainingUndos());
    }

    @Test
    public void consecutiveUndosPreserveRecyclePenalty() {

        clearPiles();

        state.setScore(50);

        state.getStock().add(
                new Card(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.drawCard());
        assertTrue(engine.recycleDiscard());

        assertEquals(30, state.getScore());

        assertTrue(engine.undo());

        assertEquals(20, state.getScore());

        assertTrue(engine.undo());

        assertEquals(10, state.getScore());
        assertEquals(4, state.getMoves());

        assertBoardMatchesSnapshot(snapshot);
    }

    @Test
    public void undoFoundationRemovalRespectsScoreMinimum() {

        clearPiles();

        state.setScore(5);

        state.getFoundation(Card.Suit.HEARTS).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.TWO)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        assertEquals(0, state.getScore());

        assertTrue(engine.undo());

        assertEquals(0, state.getScore());

        assertBoardMatchesSnapshot(snapshot);
    }

    @Test
    public void undoAtZeroStillCountsAndConsumesAllowance() {

        assertEquals(0, state.getScore());

        assertTrue(engine.drawCard());
        assertTrue(engine.undo());

        assertEquals(0, state.getScore());
        assertEquals(2, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void consecutiveUndosRespectScoreMinimum() {

        clearPiles();

        state.setScore(5);

        state.getFoundation(Card.Suit.HEARTS).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.SPADES, Card.Rank.TWO)
        );

        state.getStock().add(
                new Card(Card.Suit.CLUBS, Card.Rank.FIVE)
        );

        GameSnapshot snapshot = new GameSnapshot(state);

        assertTrue(engine.moveFoundationToTableau(
                Card.Suit.HEARTS, 0
        ));

        assertEquals(0, state.getScore());

        assertTrue(engine.drawCard());

        assertTrue(engine.undo());

        assertEquals(0, state.getScore());

        assertTrue(engine.undo());

        assertEquals(0, state.getScore());
        assertEquals(4, state.getMoves());
        assertEquals(1, state.getRemainingUndos());

        assertBoardMatchesSnapshot(snapshot);
    }

    // Restart integration tests

    @Test
    public void engineRestartRestoresOriginalDeal() {

        GameSnapshot original = new GameSnapshot(state);

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.undo());

        engine.restart();

        assertBoardMatchesSnapshot(original);
    }

    @Test
    public void engineRestartPreservesOriginalDeck() {

        ArrayList<Card> originalDeck =
                state.getOriginalDeck();

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        engine.restart();

        assertPilesEqual(
                originalDeck,
                state.getOriginalDeck()
        );
    }

    @Test
    public void engineRestartResetsGameValues() {

        assertTrue(engine.drawCard());

        state.setScore(100);
        state.setMoves(25);
        state.setElapsedTimeMs(15000);
        state.setRemainingUndos(1);

        engine.restart();

        assertEquals(0, state.getScore());
        assertEquals(0, state.getMoves());
        assertEquals(0L, state.getElapsedTimeMs());
        assertEquals(3, state.getRemainingUndos());
        assertFalse(state.isGameWon());
    }

    @Test
    public void engineRestartClearsUndoHistory() {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        assertEquals(2, engine.getUndoHistorySize());

        engine.restart();

        assertEquals(0, engine.getUndoHistorySize());
        assertFalse(engine.canUndo());
        assertFalse(engine.undo());
    }

    @Test
    public void engineRestartRestoresExhaustedUndoAllowance() {

        for (int i = 0; i < 3; i++) {

            assertTrue(engine.drawCard());
            assertTrue(engine.undo());
        }

        assertEquals(0, state.getRemainingUndos());

        engine.restart();

        assertEquals(3, state.getRemainingUndos());

        assertTrue(engine.drawCard());
        assertTrue(engine.canUndo());
        assertTrue(engine.undo());

        assertEquals(2, state.getRemainingUndos());
    }

    @Test
    public void undoAfterRestartCannotRestorePreviousGame() {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        engine.restart();

        GameSnapshot restarted = new GameSnapshot(state);

        assertTrue(engine.drawCard());
        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(restarted);

        assertFalse(engine.canUndo());
        assertEquals(0, engine.getUndoHistorySize());
    }

    // New game integration tests

    @Test
    public void engineNewGameCreatesValidInitialBoard() {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        engine.newGame();

        assertEquals(24, state.getStock().size());
        assertTrue(state.getDiscard().isEmpty());

        for (Card.Suit suit : Card.Suit.values()) {

            assertTrue(state.getFoundation(suit).isEmpty());
        }

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            ArrayList<Card> column =
                    state.getTableauColumn(i);

            assertEquals(i + 1, column.size());

            for (int j = 0; j < column.size(); j++) {

                assertEquals(
                        j == column.size() - 1,
                        column.get(j).isFaceUp()
                );
            }
        }

        assertEquals(52, countCards());
        assertEquals(52, countUniqueCards());
    }

    @Test
    public void engineNewGameResetsGameValues() {

        assertTrue(engine.drawCard());

        state.setScore(100);
        state.setMoves(25);
        state.setElapsedTimeMs(15000);
        state.setRemainingUndos(1);

        engine.newGame();

        assertEquals(0, state.getScore());
        assertEquals(0, state.getMoves());
        assertEquals(0L, state.getElapsedTimeMs());
        assertEquals(3, state.getRemainingUndos());
        assertFalse(state.isGameWon());
    }

    @Test
    public void engineNewGameClearsUndoHistory() {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        assertEquals(2, engine.getUndoHistorySize());

        engine.newGame();

        assertEquals(0, engine.getUndoHistorySize());
        assertFalse(engine.canUndo());
        assertFalse(engine.undo());
    }

    @Test
    public void undoAfterNewGameCannotRestorePreviousGame() {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        engine.newGame();

        GameSnapshot newDeal = new GameSnapshot(state);

        assertTrue(engine.drawCard());
        assertTrue(engine.undo());

        assertBoardMatchesSnapshot(newDeal);

        assertFalse(engine.canUndo());
    }

    @Test
    public void restartAfterNewGameUsesLatestDeal() {

        engine.newGame();

        GameSnapshot newDeal = new GameSnapshot(state);

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        engine.restart();

        assertBoardMatchesSnapshot(newDeal);
    }

    // Completed game integration tests

    @Test
    public void winningMoveDisablesUndo() {

        prepareNearWin();

        state.setScore(590);
        state.setMoves(149);

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.KING)
        );

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertTrue(state.isGameWon());

        assertFalse(engine.canUndo());
        assertFalse(engine.undo());

        assertEquals(600, state.getScore());
        assertEquals(150, state.getMoves());
        assertEquals(3, state.getRemainingUndos());
    }

    @Test
    public void completedGameCanBeRestarted() {

        prepareNearWin();

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.KING)
        );

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertTrue(state.isGameWon());

        engine.restart();

        assertFalse(state.isGameWon());
        assertEquals(0, state.getScore());
        assertEquals(0, state.getMoves());
        assertEquals(3, state.getRemainingUndos());
        assertEquals(0, engine.getUndoHistorySize());

        assertTrue(engine.drawCard());
    }

    @Test
    public void completedGameAllowsNewGame() {

        prepareNearWin();

        state.getDiscard().add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.KING)
        );

        assertTrue(engine.moveDiscardToFoundation(
                Card.Suit.HEARTS
        ));

        assertTrue(state.isGameWon());

        engine.newGame();

        assertFalse(state.isGameWon());
        assertEquals(0, state.getScore());
        assertEquals(0, state.getMoves());
        assertEquals(3, state.getRemainingUndos());
        assertEquals(0, engine.getUndoHistorySize());

        assertTrue(engine.drawCard());
    }

    // Card integrity tests

    @Test
    public void repeatedUndosPreserveAllCards() {

        for (int i = 0; i < 3; i++) {

            assertTrue(engine.drawCard());

            assertEquals(52, countCards());
            assertEquals(52, countUniqueCards());

            assertTrue(engine.undo());

            assertEquals(52, countCards());
            assertEquals(52, countUniqueCards());
        }
    }

    @Test
    public void undoRecyclePreservesAllCards() {

        while (!state.getStock().isEmpty()) {

            assertTrue(engine.drawCard());
        }

        assertTrue(engine.recycleDiscard());

        assertEquals(52, countCards());
        assertEquals(52, countUniqueCards());

        assertTrue(engine.undo());

        assertEquals(52, countCards());
        assertEquals(52, countUniqueCards());
        assertEquals(24, state.getDiscard().size());
        assertTrue(state.getStock().isEmpty());
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