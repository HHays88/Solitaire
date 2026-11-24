package com.hays.solitaire.model;

import java.util.ArrayList;
import java.util.Collections;

public final class Deck {

    public static final int DECK_SIZE = 52;

    private Deck() {

    }

    public static ArrayList<Card> createDeck() {
        ArrayList<Card> cards = new ArrayList<>(DECK_SIZE);
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        return cards;
    }

    public static ArrayList<Card> createShuffledDeck() {
        ArrayList<Card> cards = createDeck();
        Collections.shuffle(cards);
        return cards;
    }
}
