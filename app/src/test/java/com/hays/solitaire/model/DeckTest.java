package com.hays.solitaire.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class DeckTest {

    @Test
    public void deckContains52Cards() {

        ArrayList<Card> deck = Deck.createDeck();

        assertEquals(52, deck.size());
    }

    @Test
    public void deckContainsNoDuplicates() {

        ArrayList<Card> deck = Deck.createDeck();

        Set<String> uniqueCards = new HashSet<>();

        for (Card card : deck) {
            uniqueCards.add(card.toString());
        }

        assertEquals(52, uniqueCards.size());
    }

    @Test
    public void deckContains13CardsPerSuit() {

        ArrayList<Card> deck = Deck.createDeck();

        for (Card.Suit suit : Card.Suit.values()) {

            int count = 0;

            for (Card card : deck) {

                if (card.getSuit() == suit) {
                    count++;
                }
            }

            assertEquals(13, count);
        }
    }

    @Test
    public void allCardsStartFaceDown() {

        ArrayList<Card> deck = Deck.createDeck();

        for (Card card : deck) {
            assertFalse(card.isFaceUp());
        }
    }

    @Test
    public void shuffledDeckPreservesAllCards() {

        ArrayList<Card> original = Deck.createDeck();
        ArrayList<Card> shuffled = Deck.createShuffledDeck();

        Set<String> originalCards = new HashSet<>();
        Set<String> shuffledCards = new HashSet<>();

        for (Card card : original) {
            originalCards.add(card.toString());
        }

        for (Card card : shuffled) {
            shuffledCards.add(card.toString());
        }

        assertEquals(52, shuffled.size());
        assertEquals(originalCards, shuffledCards);
    }
}