package com.hays.solitaire.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;
import com.hays.solitaire.R;
import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.GameState;
import java.util.ArrayList;

public final class SolitaireView extends View {

    // INSTANCE VARIABLES
    private final GameState state;
    private final BoardLayout boardLayout;
    private final CardRenderer cardRenderer;
    private final CardSelection selection;
    private final int backgroundColor;

    // CONSTRUCTOR
    public SolitaireView(
            Context context,
            GameState state) {
        super(context);
        this.state = state;
        boardLayout = new BoardLayout(context);
        cardRenderer = new CardRenderer(context);
        selection = new CardSelection();
        backgroundColor = context.getColor(
                R.color.board_background
        );
    }

    // BOARD RENDERING
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(backgroundColor);
        updateBoardLayout();
        if (!boardLayout.isReady()) {
            return;
        }
        drawStock(canvas);
        drawDiscard(canvas);
        drawFoundations(canvas);
        drawTableau(canvas);
    }

    // BOARD LAYOUT
    private void updateBoardLayout() {
        boardLayout.update(
                getWidth(),
                getHeight(),
                getPaddingLeft(),
                getPaddingTop(),
                getPaddingRight(),
                getPaddingBottom()
        );
    }

    // STOCK
    private void drawStock(Canvas canvas) {
        RectF bounds = boardLayout.getStockBounds();
        ArrayList<Card> stock = state.getStock();
        if (stock.isEmpty()) {
            cardRenderer.drawEmptyPile(
                    canvas,
                    bounds
            );
        } else {
            Card topCard = stock.get(stock.size() - 1);
            cardRenderer.drawCard(
                    canvas,
                    topCard,
                    bounds,
                    false
            );
        }
    }

    // DISCARD
    private void drawDiscard(Canvas canvas) {
        RectF bounds = boardLayout.getDiscardBounds();
        ArrayList<Card> discard = state.getDiscard();
        if (discard.isEmpty()) {
            cardRenderer.drawEmptyPile(
                    canvas,
                    bounds
            );
        } else {
            Card topCard = discard.get(
                    discard.size() - 1
            );
            cardRenderer.drawCard(
                    canvas,
                    topCard,
                    bounds,
                    selection.isDiscardSelected()
            );
        }
    }

    // FOUNDATIONS
    private void drawFoundations(Canvas canvas) {
        for (Card.Suit suit : Card.Suit.values()) {
            RectF bounds =
                    boardLayout.getFoundationBounds(suit);
            ArrayList<Card> foundation =
                    state.getFoundation(suit);
            if (foundation.isEmpty()) {
                cardRenderer.drawEmptyFoundation(
                        canvas,
                        bounds,
                        suit
                );
            } else {
                Card topCard = foundation.get(
                        foundation.size() - 1
                );
                cardRenderer.drawCard(
                        canvas,
                        topCard,
                        bounds,
                        selection.isFoundationSelected(suit)
                );
            }
        }
    }

    // TABLEAU
    private void drawTableau(Canvas canvas) {
        for (int column = 0;
             column < GameState.TABLEAU_COLUMNS;
             column++) {
            drawTableauColumn(canvas, column);
        }
    }

    private void drawTableauColumn(
            Canvas canvas,
            int column) {
        ArrayList<Card> cards =
                state.getTableauColumn(column);
        if (cards.isEmpty()) {
            RectF bounds =
                    boardLayout.getEmptyTableauBounds(column);
            cardRenderer.drawEmptyPile(
                    canvas,
                    bounds
            );
            return;
        }
        for (int cardIndex = 0;
             cardIndex < cards.size();
             cardIndex++) {
            Card card = cards.get(cardIndex);
            RectF bounds =
                    boardLayout.getTableauCardBounds(
                            column,
                            cardIndex,
                            cards
                    );
            cardRenderer.drawCard(
                    canvas,
                    card,
                    bounds,
                    selection.isTableauCardSelected(
                            column,
                            cardIndex
                    )
            );
        }
    }

    // SELECTION MANAGEMENT
    public void selectDiscard() {
        selection.selectDiscard();
        invalidate();
    }

    public void selectFoundation(Card.Suit suit) {
        selection.selectFoundation(suit);
        invalidate();
    }

    public void selectTableau(
            int column,
            int cardIndex) {
        selection.selectTableau(
                column,
                cardIndex
        );
        invalidate();
    }

    public void clearSelection() {
        selection.clear();
        invalidate();
    }
}