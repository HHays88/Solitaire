package com.hays.solitaire.engine;

import com.hays.solitaire.model.GameState;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
public class ScoreManagerTest {

    private GameState state;

    @Before
    public void setUp() {
        state = new GameState();
    }

    // Scoring constant tests

    @Test
    public void scoringConstantsMatchGameRules() {
        assertEquals(10, ScoreManager.FOUNDATION_POINTS);
        assertEquals(5, ScoreManager.DISCARD_TO_TABLEAU_POINTS);
        assertEquals(5, ScoreManager.REVEAL_POINTS);
        assertEquals(-10, ScoreManager.FOUNDATION_REMOVAL_PENALTY);
        assertEquals(-20, ScoreManager.RECYCLE_PENALTY);
        assertEquals(-10, ScoreManager.UNDO_PENALTY);
    }

    // Positive score adjustment tests

    @Test
    public void foundationMoveAwardsTenPoints() {
        int adjustment = ScoreManager.adjustScore(
                state,
                ScoreManager.FOUNDATION_POINTS
        );
        assertEquals(10, state.getScore());
        assertEquals(10, adjustment);
    }

    @Test
    public void discardToTableauAwardsFivePoints() {
        int adjustment = ScoreManager.adjustScore(
                state,
                ScoreManager.DISCARD_TO_TABLEAU_POINTS
        );
        assertEquals(5, state.getScore());
        assertEquals(5, adjustment);
    }

    @Test
    public void automaticRevealAwardsFivePoints() {
        int adjustment = ScoreManager.adjustScore(
                state,
                ScoreManager.REVEAL_POINTS
        );
        assertEquals(5, state.getScore());
        assertEquals(5, adjustment);
    }

    @Test
    public void multipleScoreAdjustmentsAccumulate() {
        ScoreManager.adjustScore(
                state,
                ScoreManager.FOUNDATION_POINTS
        );
        ScoreManager.adjustScore(
                state,
                ScoreManager.REVEAL_POINTS
        );
        ScoreManager.adjustScore(
                state,
                ScoreManager.DISCARD_TO_TABLEAU_POINTS
        );
        assertEquals(20, state.getScore());
    }

    // Negative score adjustment tests

    @Test
    public void foundationRemovalDeductsTenPoints() {
        state.setScore(30);
        int adjustment = ScoreManager.adjustScore(
                state,
                ScoreManager.FOUNDATION_REMOVAL_PENALTY
        );
        assertEquals(20, state.getScore());
        assertEquals(-10, adjustment);
    }

    @Test
    public void recycleDeductsTwentyPoints() {
        state.setScore(30);
        int adjustment = ScoreManager.adjustScore(
                state,
                ScoreManager.RECYCLE_PENALTY
        );
        assertEquals(10, state.getScore());
        assertEquals(-20, adjustment);
    }

    @Test
    public void undoPenaltyDeductsTenPoints() {
        state.setScore(30);
        int adjustment = ScoreManager.adjustScore(
                state,
                ScoreManager.UNDO_PENALTY
        );
        assertEquals(20, state.getScore());
        assertEquals(-10, adjustment);
    }

    // Zero-point minimum tests

    @Test
    public void scoreCannotBecomeNegative() {
        state.setScore(5);
        int adjustment = ScoreManager.adjustScore(
                state,
                ScoreManager.RECYCLE_PENALTY
        );
        assertEquals(0, state.getScore());
        assertEquals(-5, adjustment);
    }

    @Test
    public void penaltyAtZeroReturnsZeroAdjustment() {
        int adjustment = ScoreManager.adjustScore(
                state,
                ScoreManager.UNDO_PENALTY
        );
        assertEquals(0, state.getScore());
        assertEquals(0, adjustment);
    }

    @Test
    public void penaltyCanReduceScoreToExactlyZero() {
        state.setScore(20);
        int adjustment = ScoreManager.adjustScore(
                state,
                ScoreManager.RECYCLE_PENALTY
        );
        assertEquals(0, state.getScore());
        assertEquals(-20, adjustment);
    }

    @Test
    public void scoreCanIncreaseAfterReachingZero() {
        state.setScore(5);
        ScoreManager.adjustScore(
                state,
                ScoreManager.RECYCLE_PENALTY
        );
        int adjustment = ScoreManager.adjustScore(
                state,
                ScoreManager.FOUNDATION_POINTS
        );
        assertEquals(10, state.getScore());
        assertEquals(10, adjustment);
    }

    @Test
    public void zeroAdjustmentDoesNotChangeScore() {
        state.setScore(25);
        int adjustment = ScoreManager.adjustScore(
                state, 0
        );
        assertEquals(25, state.getScore());
        assertEquals(0, adjustment);
    }

    @Test
    public void repeatedPenaltiesCannotProduceNegativeScore() {
        state.setScore(50);
        for (int i = 0; i < 5; i++) {
            ScoreManager.adjustScore(
                    state,
                    ScoreManager.RECYCLE_PENALTY
            );
            assertTrue(state.getScore() >= 0);
        }
        assertEquals(0, state.getScore());
    }

    // Move counting tests

    @Test
    public void incrementMovesAddsOneMove() {
        ScoreManager.incrementMoves(state);
        assertEquals(1, state.getMoves());
    }

    @Test
    public void multipleMovesAccumulate() {
        for (int i = 0; i < 5; i++) {
            ScoreManager.incrementMoves(state);
        }
        assertEquals(5, state.getMoves());
    }

    @Test
    public void incrementMovesPreservesExistingCount() {
        state.setMoves(25);
        ScoreManager.incrementMoves(state);
        assertEquals(26, state.getMoves());
    }

    // Score and move independence tests

    @Test
    public void adjustingScoreDoesNotIncrementMoves() {
        state.setMoves(10);
        ScoreManager.adjustScore(
                state,
                ScoreManager.FOUNDATION_POINTS
        );
        assertEquals(10, state.getScore());
        assertEquals(10, state.getMoves());
    }

    @Test
    public void incrementingMovesDoesNotChangeScore() {
        state.setScore(25);
        ScoreManager.incrementMoves(state);
        assertEquals(25, state.getScore());
        assertEquals(1, state.getMoves());
    }

    @Test
    public void scoringDoesNotChangeOtherGameValues() {
        state.setElapsedTimeMs(5000);
        state.setRemainingUndos(2);
        ScoreManager.adjustScore(
                state,
                ScoreManager.FOUNDATION_POINTS
        );
        ScoreManager.incrementMoves(state);
        assertEquals(5000L, state.getElapsedTimeMs());
        assertEquals(2, state.getRemainingUndos());
        assertFalse(state.isGameWon());
    }
}