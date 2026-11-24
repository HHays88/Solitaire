package com.hays.solitaire.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class CardTest {

    @Test
    public void cardHasCorrectAttributes() {

        Card card = new Card(
                Card.Suit.HEARTS,
                Card.Rank.QUEEN
        );

        assertEquals(Card.Suit.HEARTS, card.getSuit());
        assertEquals(Card.Rank.QUEEN, card.getRank());
        assertEquals(12, card.getValue());
        assertTrue(card.isRed());
        assertFalse(card.isFaceUp());
    }

    @Test
    public void cardCanBeFlipped() {

        Card card = new Card(
                Card.Suit.SPADES,
                Card.Rank.ACE
        );

        assertFalse(card.isFaceUp());

        card.setFaceUp(true);
        assertTrue(card.isFaceUp());

        card.setFaceUp(false);
        assertFalse(card.isFaceUp());
    }

    @Test
    public void suitsHaveCorrectColors() {

        assertTrue(Card.Suit.HEARTS.isRed());
        assertTrue(Card.Suit.DIAMONDS.isRed());

        assertFalse(Card.Suit.CLUBS.isRed());
        assertFalse(Card.Suit.SPADES.isRed());
    }

    @Test
    public void ranksHaveCorrectValues() {

        Card.Rank[] ranks = Card.Rank.values();

        for (int i = 0; i < ranks.length; i++) {
            assertEquals(i + 1, ranks[i].getValue());
        }
    }

    @Test
    public void copiedCardIsIndependent() {

        Card original = new Card(
                Card.Suit.CLUBS,
                Card.Rank.KING
        );

        original.setFaceUp(true);

        Card copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getSuit(), copy.getSuit());
        assertEquals(original.getRank(), copy.getRank());
        assertTrue(copy.isFaceUp());

        copy.setFaceUp(false);

        assertTrue(original.isFaceUp());
        assertFalse(copy.isFaceUp());
    }
}