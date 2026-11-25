package com.hays.solitaire.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import com.hays.solitaire.R;
import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.GameState;
import java.util.ArrayList;

public final class BoardLayout {

    // CONSTANTS
    private static final int COLUMNS =
            GameState.TABLEAU_COLUMNS;
    private static final int STOCK_COLUMN = 0;
    private static final int DISCARD_COLUMN = 1;
    private static final int FIRST_FOUNDATION_COLUMN = 3;

    // RESOURCE DIMENSIONS
    private final float boardPadding;
    private final float columnSpacing;
    private final float pileRowSpacing;
    private final float faceDownOverlap;
    private final float faceUpOverlap;

    // CALCULATED DIMENSIONS
    private float cardWidth;
    private float cardHeight;
    private float boardLeft;
    private float boardBottom;
    private float upperRowTop;
    private float tableauTop;

    // CONSTRUCTOR
    public BoardLayout(Context context) {
        Resources resources = context.getResources();
        boardPadding = resources.getDimension(
                R.dimen.board_padding
        );
        columnSpacing = resources.getDimension(
                R.dimen.column_spacing
        );
        pileRowSpacing = resources.getDimension(
                R.dimen.pile_row_spacing
        );
        faceDownOverlap = resources.getDimension(
                R.dimen.tableau_face_down_overlap
        );
        faceUpOverlap = resources.getDimension(
                R.dimen.tableau_face_up_overlap
        );
    }


    // BOARD DIMENSION CALCULATION
    public void update(
            int width,
            int height,
            int paddingLeft,
            int paddingTop,
            int paddingRight,
            int paddingBottom) {
        float availableWidth = width
                - paddingLeft
                - paddingRight
                - (2 * boardPadding)
                - ((COLUMNS - 1) * columnSpacing);
        cardWidth = Math.max(
                0f,
                availableWidth / COLUMNS
        );
        cardHeight = cardWidth
                * CardRenderer.CARD_ASPECT_RATIO;
        boardLeft = paddingLeft + boardPadding;
        boardBottom = height
                - paddingBottom
                - boardPadding;
        upperRowTop = paddingTop + boardPadding;
        tableauTop = upperRowTop
                + cardHeight
                + pileRowSpacing;
    }


    // UPPER ROW POSITIONS
    public RectF getStockBounds() {
        return createCardBounds(
                STOCK_COLUMN,
                upperRowTop
        );
    }

    public RectF getDiscardBounds() {
        return createCardBounds(
                DISCARD_COLUMN,
                upperRowTop
        );
    }

    public RectF getFoundationBounds(Card.Suit suit) {
        int column = FIRST_FOUNDATION_COLUMN
                + suit.ordinal();
        return createCardBounds(
                column,
                upperRowTop
        );
    }

    // TABLEAU POSITIONS
    public RectF getEmptyTableauBounds(int column) {
        return createCardBounds(
                column,
                tableauTop
        );
    }

    public RectF getTableauCardBounds(
            int column,
            int cardIndex,
            ArrayList<Card> cards) {
        float scale = calculateOverlapScale(cards);
        float top = tableauTop;
        for (int i = 0; i < cardIndex; i++) {
            top += getCardOverlap(cards.get(i))
                    * scale;
        }
        return createCardBounds(column, top);
    }

    // TABLEAU OVERLAP
    private float getCardOverlap(Card card) {
        float overlap = card.isFaceUp()
                ? faceUpOverlap
                : faceDownOverlap;
        return Math.min(overlap, cardHeight);
    }

    private float calculateOverlapScale(
            ArrayList<Card> cards) {
        if (cards.size() <= 1) {
            return 1f;
        }
        float availableHeight = boardBottom
                - tableauTop
                - cardHeight;
        if (availableHeight <= 0f) {
            return 0f;
        }
        float requiredHeight = 0f;
        for (int i = 0; i < cards.size() - 1; i++) {
            requiredHeight += getCardOverlap(
                    cards.get(i)
            );
        }
        if (requiredHeight <= availableHeight) {
            return 1f;
        }
        return availableHeight / requiredHeight;
    }

    // POSITIONING HELPER
    private RectF createCardBounds(
            int column,
            float top) {
        float left = boardLeft
                + column * (cardWidth + columnSpacing);
        return new RectF(
                left,
                top,
                left + cardWidth,
                top + cardHeight
        );
    }

    // HIT DETECTION
    public HitResult hitTest(
            float x,
            float y,
            GameState state) {
        if (!isReady()) {
            return HitResult.none();
        }
        if (getStockBounds().contains(x, y)) {
            return HitResult.stock();
        }
        if (getDiscardBounds().contains(x, y)) {
            return HitResult.discard();
        }
        for (Card.Suit suit : Card.Suit.values()) {
            if (getFoundationBounds(suit).contains(x, y)) {
                return HitResult.foundation(suit);
            }
        }
        return hitTestTableau(x, y, state);
    }

    private HitResult hitTestTableau(
            float x,
            float y,
            GameState state) {
        for (int column = 0;
             column < COLUMNS;
             column++) {
            ArrayList<Card> cards =
                    state.getTableauColumn(column);
            if (cards.isEmpty()) {
                if (getEmptyTableauBounds(column)
                        .contains(x, y)) {

                    return HitResult.emptyTableau(column);
                }
                continue;
            }
            for (int cardIndex = cards.size() - 1;
                 cardIndex >= 0;
                 cardIndex--) {
                RectF bounds = getTableauCardBounds(
                        column,
                        cardIndex,
                        cards
                );
                if (bounds.contains(x, y)) {
                    return HitResult.tableauCard(
                            column,
                            cardIndex
                    );
                }
            }
        }
        return HitResult.none();
    }

    // GETTERS
    public float getCardWidth() {
        return cardWidth;
    }

    public float getCardHeight() {
        return cardHeight;
    }

    public boolean isReady() {
        return cardWidth > 0f
                && tableauTop + cardHeight <= boardBottom;
    }

    // HIT RESULTS
    public static final class HitResult {

        // HIT TYPES
        public enum Type {
            NONE,
            STOCK,
            DISCARD,
            FOUNDATION,
            TABLEAU_CARD,
            EMPTY_TABLEAU
        }

        // INSTANCE VARIABLES
        private final Type type;
        private final Card.Suit foundationSuit;
        private final int tableauColumn;
        private final int cardIndex;

        // CONSTRUCTOR
        private HitResult(
                Type type,
                Card.Suit foundationSuit,
                int tableauColumn,
                int cardIndex) {
            this.type = type;
            this.foundationSuit = foundationSuit;
            this.tableauColumn = tableauColumn;
            this.cardIndex = cardIndex;
        }

        // FACTORY METHODS
        public static HitResult none() {
            return new HitResult(
                    Type.NONE,
                    null,
                    -1,
                    -1
            );
        }

        public static HitResult stock() {
            return new HitResult(
                    Type.STOCK,
                    null,
                    -1,
                    -1
            );
        }

        public static HitResult discard() {
            return new HitResult(
                    Type.DISCARD,
                    null,
                    -1,
                    -1
            );
        }

        public static HitResult foundation(Card.Suit suit) {
            return new HitResult(
                    Type.FOUNDATION,
                    suit,
                    -1,
                    -1
            );
        }

        public static HitResult tableauCard(
                int column,
                int cardIndex) {
            return new HitResult(
                    Type.TABLEAU_CARD,
                    null,
                    column,
                    cardIndex
            );
        }

        public static HitResult emptyTableau(int column) {
            return new HitResult(
                    Type.EMPTY_TABLEAU,
                    null,
                    column,
                    -1
            );
        }


        // GETTERS
        public Type getType() {
            return type;
        }

        public Card.Suit getFoundationSuit() {
            return foundationSuit;
        }

        public int getTableauColumn() {
            return tableauColumn;
        }

        public int getCardIndex() {
            return cardIndex;
        }
    }
}