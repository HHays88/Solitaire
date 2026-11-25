package com.hays.solitaire.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;
import java.util.ArrayList;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.hays.solitaire.engine.GameEngine;
import com.hays.solitaire.R;
import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.GameState;
import com.hays.solitaire.ui.BoardLayout.HitResult;
import com.hays.solitaire.engine.MoveValidator;

public final class SolitaireView extends View {

    // INSTANCE VARIABLES
    private final GameState state;
    private final BoardLayout boardLayout;
    private final CardRenderer cardRenderer;
    private final CardSelection selection;
    private final int backgroundColor;
    private final GameEngine gameEngine;
    private final int touchSlop;
    private float touchStartX;
    private float touchStartY;
    private boolean tapCandidate;
    private Runnable onGameChangedListener;

    // CONSTRUCTOR
    public SolitaireView(
            Context context,
            GameEngine gameEngine) {
        super(context);
        this.gameEngine = gameEngine;
        this.state = gameEngine.getState();
        boardLayout = new BoardLayout(context);
        cardRenderer = new CardRenderer(context);
        selection = new CardSelection();
        backgroundColor =
                context.getColor(R.color.board_background);
        touchSlop = ViewConfiguration.get(context)
                .getScaledTouchSlop();
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

    // TOUCH INTERACTION
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                tapCandidate = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (hasExceededTouchSlop(
                        event.getX(),
                        event.getY())) {
                    tapCandidate = false;
                }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                tapCandidate = false;
                return true;
            case MotionEvent.ACTION_UP:
                if (tapCandidate
                        && !hasExceededTouchSlop(
                        event.getX(),
                        event.getY())) {
                    performClick();
                    handleTap(
                            event.getX(),
                            event.getY()
                    );
                }
                tapCandidate = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                tapCandidate = false;
                return true;
            default:
                return true;
        }
    }

    public void setOnGameChangedListener(
            Runnable listener) {
        onGameChangedListener = listener;
    }

    private void notifyGameChanged() {
        if (onGameChangedListener != null) {
            onGameChangedListener.run();
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    // HELPER METHODS

    // HANDLE TAP, SOURCE, DESTINATION
    private void handleTap(float x, float y) {
        if (state.isGameWon()) {
            return;
        }
        updateBoardLayout();
        HitResult hit = boardLayout.hitTest(
                x,
                y,
                state
        );
        if (hit.getType() == HitResult.Type.STOCK) {
            handleStockTap();
            return;
        }
        if (hit.getType() == HitResult.Type.NONE) {
            clearSelection();
            return;
        }
        if (selection.hasSelection()) {
            handleDestinationTap(hit);
        } else {
            handleSourceTap(hit);
        }
    }

    private void handleSourceTap(HitResult hit) {
        switch (hit.getType()) {
            case DISCARD:
                handleDiscardTap();
                break;
            case FOUNDATION:
                handleFoundationTap(
                        hit.getFoundationSuit()
                );
                break;
            case TABLEAU_CARD:
                handleTableauTap(
                        hit.getTableauColumn(),
                        hit.getCardIndex()
                );
                break;
            default:
                break;
        }
    }

    private void handleDestinationTap(HitResult hit) {
        boolean moved = false;
        switch (hit.getType()) {
            case DISCARD:
                if (selection.isDiscardSelected()) {
                    clearSelection();
                }
                return;
            case FOUNDATION:
                if (selection.isFoundationSelected(
                        hit.getFoundationSuit())) {
                    clearSelection();
                    return;
                }
                moved = moveToFoundation(
                        hit.getFoundationSuit()
                );
                break;
            case TABLEAU_CARD:
                if (selection.isTableauCardSelected(
                        hit.getTableauColumn(),
                        hit.getCardIndex())) {
                    clearSelection();
                    return;
                }
                Card destinationCard =
                        state.getTableauColumn(
                                hit.getTableauColumn()
                        ).get(hit.getCardIndex());
                if (!destinationCard.isFaceUp()) {
                    return;
                }
                moved = moveToTableau(
                        hit.getTableauColumn()
                );
                break;
            case EMPTY_TABLEAU:
                moved = moveToTableau(
                        hit.getTableauColumn()
                );
                break;
            default:
                return;
        }
        if (moved) {
            clearSelection();
            notifyGameChanged();
        }
    }

    // TABLEAU TAPS
    private boolean moveToTableau(
            int destinationColumn) {
        switch (selection.getType()) {
            case DISCARD:
                return gameEngine.moveDiscardToTableau(
                        destinationColumn
                );
            case FOUNDATION:
                return gameEngine.moveFoundationToTableau(
                        selection.getFoundationSuit(),
                        destinationColumn
                );
            case TABLEAU:
                return gameEngine.moveTableauToTableau(
                        selection.getTableauColumn(),
                        selection.getCardIndex(),
                        destinationColumn
                );
            default:
                return false;
        }
    }
    private void handleTableauTap(
            int column,
            int cardIndex) {
        ArrayList<Card> cards =
                state.getTableauColumn(column);
        Card card = cards.get(cardIndex);
        if (!card.isFaceUp()) {
            return;
        }
        if (selection.isTableauCardSelected(
                column,
                cardIndex)) {
            clearSelection();
            return;
        }
        if (!MoveValidator.isValidTableauSequence(
                cards,
                cardIndex)) {
            return;
        }
        selectTableau(column, cardIndex);
    }

    // FOUNDATION TAPS
    private boolean moveToFoundation(Card.Suit suit) {
        switch (selection.getType()) {
            case DISCARD:
                return gameEngine.moveDiscardToFoundation(
                        suit
                );
            case TABLEAU:
                int column =
                        selection.getTableauColumn();
                int cardIndex =
                        selection.getCardIndex();
                ArrayList<Card> cards =
                        state.getTableauColumn(column);
                if (cardIndex != cards.size() - 1) {
                    return false;
                }
                return gameEngine.moveTableauToFoundation(
                        column,
                        suit
                );
            default:
                return false;
        }
    }
    private void handleFoundationTap(Card.Suit suit) {
        if (state.getFoundation(suit).isEmpty()) {
            return;
        }
        if (selection.isFoundationSelected(suit)) {
            clearSelection();
        } else {
            selectFoundation(suit);
        }
    }

    // STOCK TAPS
    private void handleStockTap() {
        selection.clear();
        boolean changed;
        if (!state.getStock().isEmpty()) {
            changed = gameEngine.drawCard();
        } else {
            changed = gameEngine.recycleDiscard();
        }
        invalidate();
        if (changed) {
            notifyGameChanged();
        }
    }

    // DISCARD TAPS
    private void handleDiscardTap() {
        if (state.getDiscard().isEmpty()) {
            return;
        }
        if (selection.isDiscardSelected()) {
            clearSelection();
        } else {
            selectDiscard();
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

    // PREVENT DRAG AS TAP
    private boolean hasExceededTouchSlop(
            float x,
            float y) {
        float deltaX = x - touchStartX;
        float deltaY = y - touchStartY;
        return (deltaX * deltaX + deltaY * deltaY)
                > ((float) touchSlop * touchSlop);
    }
}