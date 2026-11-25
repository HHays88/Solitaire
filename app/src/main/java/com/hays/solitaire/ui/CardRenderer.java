package com.hays.solitaire.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import com.hays.solitaire.R;
import com.hays.solitaire.model.Card;

public final class CardRenderer {
    // CARD PROPORTIONS
    public static final float CARD_ASPECT_RATIO = 1.5f;
    private static final float RANK_TEXT_RATIO = 0.25f;
    private static final float CORNER_SUIT_RATIO = 0.20f;
    private static final float CENTER_SUIT_RATIO = 0.40f;

    // COLORS
    private final int cardFaceColor;
    private final int cardBorderColor;
    private final int cardBackColor;
    private final int cardBackDetailColor;
    private final int blackSuitColor;
    private final int redSuitColor;
    private final int emptyPileColor;
    private final int emptyPileBorderColor;
    private final int selectedColor;

    // DIMENSIONS
    private final float cornerRadius;
    private final float borderWidth;
    private final float innerPadding;
    private final float selectionBorderWidth;

    // DRAWING OBJECTS
    private final Paint fillPaint;
    private final Paint borderPaint;
    private final Paint textPaint;
    private final RectF drawingBounds;

    // CONSTRUCTOR
    public CardRenderer(Context context) {
        Resources resources = context.getResources();
        // Load colors
        cardFaceColor =
                context.getColor(R.color.card_face);
        cardBorderColor =
                context.getColor(R.color.card_border);
        cardBackColor =
                context.getColor(R.color.card_back);
        cardBackDetailColor =
                context.getColor(R.color.card_back_detail);
        blackSuitColor =
                context.getColor(R.color.card_black);
        redSuitColor =
                context.getColor(R.color.card_red);
        emptyPileColor =
                context.getColor(R.color.empty_pile_background);
        emptyPileBorderColor =
                context.getColor(R.color.empty_pile_border);
        selectedColor =
                context.getColor(R.color.card_selected);
        // Load dimensions
        cornerRadius = resources.getDimension(
                R.dimen.card_corner_radius
        );
        borderWidth = resources.getDimension(
                R.dimen.card_border_width
        );
        innerPadding = resources.getDimension(
                R.dimen.card_inner_padding
        );
        selectionBorderWidth = resources.getDimension(
                R.dimen.selection_border_width
        );
        // Initialize drawing objects
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(borderWidth);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        drawingBounds = new RectF();
    }

    // CARD RENDERING
    public void drawCard(
            Canvas canvas,
            Card card,
            RectF bounds,
            boolean selected) {
        if (card.isFaceUp()) {
            drawCardFace(canvas, card, bounds);
        } else {
            drawCardBack(canvas, bounds);
        }
        if (selected) {
            drawSelectionBorder(canvas, bounds);
        }
    }

    // FACE-UP CARDS
    private void drawCardFace(
            Canvas canvas,
            Card card,
            RectF bounds) {
        drawBackground(
                canvas,
                bounds,
                cardFaceColor
        );
        drawBorder(
                canvas,
                bounds,
                cardBorderColor,
                borderWidth
        );
        textPaint.setColor(
                card.isRed()
                        ? redSuitColor
                        : blackSuitColor
        );
        drawCornerLabels(canvas, card, bounds);
        drawCenterSuit(canvas, card, bounds);
    }

    // CARD RANK AND CORNER SUIT
    private void drawCornerLabels(
            Canvas canvas,
            Card card,
            RectF bounds) {
        float rankSize =
                bounds.width() * RANK_TEXT_RATIO;
        float suitSize =
                bounds.width() * CORNER_SUIT_RATIO;
        String rank = card.getRank().getLabel();
        String suit = card.getSuit().getSymbol();
        float cornerX =
                bounds.left + bounds.width() * 0.20f;
        float cornerY =
                bounds.top + innerPadding;
        // Upper-left corner
        drawCorner(
                canvas,
                rank,
                suit,
                cornerX,
                cornerY,
                rankSize,
                suitSize
        );
        // Lower-right corner
        canvas.save();
        canvas.rotate(
                180f,
                bounds.centerX(),
                bounds.centerY()
        );
        drawCorner(
                canvas,
                rank,
                suit,
                cornerX,
                cornerY,
                rankSize,
                suitSize
        );
        canvas.restore();
    }

    private void drawCorner(
            Canvas canvas,
            String rank,
            String suit,
            float x,
            float y,
            float rankSize,
            float suitSize) {
        textPaint.setTextSize(rankSize);
        float rankBaseline =
                y - textPaint.ascent();
        canvas.drawText(
                rank,
                x,
                rankBaseline,
                textPaint
        );
        float suitTop =
                rankBaseline + textPaint.descent();
        textPaint.setTextSize(suitSize);
        float suitBaseline =
                suitTop - textPaint.ascent();
        canvas.drawText(
                suit,
                x,
                suitBaseline,
                textPaint
        );
    }


    // CENTER SUIT SYMBOL
    private void drawCenterSuit(
            Canvas canvas,
            Card card,
            RectF bounds) {
        textPaint.setTextSize(
                bounds.width() * CENTER_SUIT_RATIO
        );
        drawCenteredText(
                canvas,
                card.getSuit().getSymbol(),
                bounds
        );
    }

    // FACE-DOWN CARDS
    private void drawCardBack(
            Canvas canvas,
            RectF bounds) {
        drawBackground(
                canvas,
                bounds,
                cardFaceColor
        );
        drawBorder(
                canvas,
                bounds,
                cardBorderColor,
                borderWidth
        );
        drawingBounds.set(bounds);
        drawingBounds.inset(
                innerPadding,
                innerPadding
        );
        if (drawingBounds.isEmpty()) {
            return;
        }
        drawBackground(
                canvas,
                drawingBounds,
                cardBackColor
        );
        drawCardBackPattern(
                canvas,
                drawingBounds
        );
    }

    // CARD BACK PATTERN
    private void drawCardBackPattern(
            Canvas canvas,
            RectF bounds) {
        borderPaint.setColor(cardBackDetailColor);
        borderPaint.setStrokeWidth(borderWidth);
        float centerX = bounds.centerX();
        float centerY = bounds.centerY();
        float halfWidth = bounds.width() * 0.35f;
        float halfHeight = bounds.height() * 0.35f;
        canvas.drawLine(
                centerX,
                centerY - halfHeight,
                centerX + halfWidth,
                centerY,
                borderPaint
        );
        canvas.drawLine(
                centerX + halfWidth,
                centerY,
                centerX,
                centerY + halfHeight,
                borderPaint
        );
        canvas.drawLine(
                centerX,
                centerY + halfHeight,
                centerX - halfWidth,
                centerY,
                borderPaint
        );
        canvas.drawLine(
                centerX - halfWidth,
                centerY,
                centerX,
                centerY - halfHeight,
                borderPaint
        );
    }

    // EMPTY PILES
    public void drawEmptyPile(
            Canvas canvas,
            RectF bounds) {
        drawBackground(
                canvas,
                bounds,
                emptyPileColor
        );
        drawBorder(
                canvas,
                bounds,
                emptyPileBorderColor,
                borderWidth
        );
    }

    // EMPTY FOUNDATIONS
    public void drawEmptyFoundation(
            Canvas canvas,
            RectF bounds,
            Card.Suit suit) {
        drawEmptyPile(canvas, bounds);
        textPaint.setColor(emptyPileBorderColor);
        textPaint.setTextSize(
                bounds.width() * CENTER_SUIT_RATIO
        );
        drawCenteredText(
                canvas,
                suit.getSymbol(),
                bounds
        );
    }

    // SELECTION BORDER
    public void drawSelectionBorder(
            Canvas canvas,
            RectF bounds) {
        drawBorder(
                canvas,
                bounds,
                selectedColor,
                selectionBorderWidth
        );
    }

    // DRAWING HELPERS

    private void drawBackground(
            Canvas canvas,
            RectF bounds,
            int color) {
        fillPaint.setColor(color);
        canvas.drawRoundRect(
                bounds,
                cornerRadius,
                cornerRadius,
                fillPaint
        );
    }

    private void drawBorder(
            Canvas canvas,
            RectF bounds,
            int color,
            float strokeWidth) {
        borderPaint.setColor(color);
        borderPaint.setStrokeWidth(strokeWidth);
        canvas.drawRoundRect(
                bounds,
                cornerRadius,
                cornerRadius,
                borderPaint
        );
    }

    private void drawCenteredText(
            Canvas canvas,
            String text,
            RectF bounds) {
        float baseline = bounds.centerY()
                - (textPaint.ascent()
                + textPaint.descent()) / 2f;
        canvas.drawText(
                text,
                bounds.centerX(),
                baseline,
                textPaint
        );
    }
}