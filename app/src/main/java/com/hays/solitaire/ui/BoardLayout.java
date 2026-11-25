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
}