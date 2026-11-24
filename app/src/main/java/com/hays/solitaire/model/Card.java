package com.hays.solitaire.model;

public class Card {

    public enum Suit {
        CLUBS("♣", false),
        DIAMONDS("♦", true),
        SPADES("♠", false),
        HEARTS("♥", true);

        private final String symbol;
        private final boolean red;

        Suit(String symbol, boolean red) {
            this.symbol = symbol;
            this.red = red;
        }

        public String getSymbol() {
            return symbol;
        }

        public boolean isRed() {
            return red;
        }
    }

    public enum Rank {
        ACE(1, "A"),
        TWO(2, "2"),
        THREE(3, "3"),
        FOUR(4, "4"),
        FIVE(5, "5"),
        SIX(6, "6"),
        SEVEN(7, "7"),
        EIGHT(8, "8"),
        NINE(9, "9"),
        TEN(10, "10"),
        JACK(11, "J"),
        QUEEN(12, "Q"),
        KING(13, "K");

        private final int value;
        private final String label;

        Rank(int value, String label) {
            this.value = value;
            this.label = label;
        }

        public int getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    private final Suit suit;
    private final Rank rank;
    private boolean faceUp;

    public Card (Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
        this.faceUp = false;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    public boolean isFaceUp() {
        return faceUp;
    }

    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    public boolean isRed() {
        return suit.isRed();
    }

    public int getValue() {
        return rank.getValue();
    }

    public Card copy() {
        Card card = new Card(suit, rank);
        card.setFaceUp(faceUp);
        return card;
    }

    @Override
    public String toString() {
        return rank.getLabel() + suit.getSymbol();
    }


}
