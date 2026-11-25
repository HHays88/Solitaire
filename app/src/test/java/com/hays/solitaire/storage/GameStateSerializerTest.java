package com.hays.solitaire.storage;

import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.Deck;
import com.hays.solitaire.model.GameState;
import com.hays.solitaire.model.GameSnapshot;
import com.hays.solitaire.engine.GameEngine;
import com.hays.solitaire.engine.UndoManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class GameStateSerializerTest {

    private GameState state;
    private GameEngine engine;

    @Before
    public void setUp() {
        state = new GameState(Deck.createDeck());
        engine = new GameEngine(state);
    }

    // SAVE FORMAT TESTS

    @Test
    public void serializedGameContainsCorrectVersion()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(1, json.getInt("version"));
    }

    @Test
    public void serializedGameContainsAllRequiredAttributes()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        String[] requiredAttributes = {
                "version",
                "originalDeck",
                "stock",
                "discard",
                "foundations",
                "tableau",
                "score",
                "moves",
                "remainingUndos",
                "gameWon",
                "undoHistory"
        };

        for (String attribute : requiredAttributes) {
            assertTrue(
                    "Missing attribute: " + attribute,
                    json.has(attribute)
            );
        }
    }

    @Test
    public void serializedGameCanBeConvertedToJsonString()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        String jsonString = json.toString();

        JSONObject parsedJson = new JSONObject(jsonString);

        assertEquals(
                GameStateSerializer.SAVE_VERSION,
                parsedJson.getInt("version")
        );

        assertEquals(
                52,
                parsedJson.getJSONArray("originalDeck").length()
        );
    }

    // ORIGINAL DECK TESTS

    @Test
    public void serializedOriginalDeckContains52Cards()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                Deck.DECK_SIZE,
                json.getJSONArray("originalDeck").length()
        );
    }

    @Test
    public void serializedOriginalDeckPreservesCardOrder()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertSerializedCardsEqual(
                state.getOriginalDeck(),
                json.getJSONArray("originalDeck")
        );
    }

    @Test
    public void serializedOriginalDeckContainsOnlyFaceDownCards()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONArray originalDeck =
                json.getJSONArray("originalDeck");

        for (int i = 0; i < originalDeck.length(); i++) {

            assertFalse(
                    originalDeck.getJSONObject(i)
                            .getBoolean("faceUp")
            );
        }
    }

    @Test
    public void serializedOriginalDeckRemainsUnchangedAfterMoves()
            throws JSONException {

        ArrayList<Card> originalDeck = state.getOriginalDeck();

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertSerializedCardsEqual(
                originalDeck,
                json.getJSONArray("originalDeck")
        );
    }

    // INDIVIDUAL CARD TESTS

    @Test
    public void serializedCardContainsSuitName()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONObject card = json.getJSONArray("originalDeck")
                .getJSONObject(0);

        assertEquals("CLUBS", card.getString("suit"));
    }

    @Test
    public void serializedCardContainsRankName()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONObject card = json.getJSONArray("originalDeck")
                .getJSONObject(0);

        assertEquals("ACE", card.getString("rank"));
    }

    @Test
    public void serializedFaceDownCardPreservesOrientation()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONObject card = json.getJSONArray("stock")
                .getJSONObject(0);

        assertFalse(card.getBoolean("faceUp"));
    }

    @Test
    public void serializedFaceUpCardPreservesOrientation()
            throws JSONException {

        assertTrue(engine.drawCard());

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONObject card = json.getJSONArray("discard")
                .getJSONObject(0);

        assertTrue(card.getBoolean("faceUp"));
    }

    // STOCK AND DISCARD TESTS

    @Test
    public void serializedInitialStockContains24Cards()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                24,
                json.getJSONArray("stock").length()
        );
    }

    @Test
    public void serializedInitialDiscardIsEmpty()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                0,
                json.getJSONArray("discard").length()
        );
    }

    @Test
    public void serializedStockReflectsDrawOperations()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                21,
                json.getJSONArray("stock").length()
        );

        assertSerializedCardsEqual(
                state.getStock(),
                json.getJSONArray("stock")
        );
    }

    @Test
    public void serializedDiscardReflectsDrawOperations()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                3,
                json.getJSONArray("discard").length()
        );

        assertSerializedCardsEqual(
                state.getDiscard(),
                json.getJSONArray("discard")
        );
    }

    @Test
    public void serializedStockCanBeEmpty()
            throws JSONException {

        while (!state.getStock().isEmpty()) {
            assertTrue(engine.drawCard());
        }

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                0,
                json.getJSONArray("stock").length()
        );

        assertEquals(
                24,
                json.getJSONArray("discard").length()
        );
    }

    @Test
    public void serializedRecycledStockPreservesCardOrder()
            throws JSONException {

        ArrayList<Card> originalStock =
                new GameSnapshot(state).getStock();

        while (!state.getStock().isEmpty()) {
            assertTrue(engine.drawCard());
        }

        assertTrue(engine.recycleDiscard());

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertSerializedCardsEqual(
                originalStock,
                json.getJSONArray("stock")
        );

        assertEquals(
                0,
                json.getJSONArray("discard").length()
        );
    }

    // FOUNDATION TESTS

    @Test
    public void serializedFoundationsContainFourSuits()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONObject foundations =
                json.getJSONObject("foundations");

        assertEquals(4, foundations.length());

        for (Card.Suit suit : Card.Suit.values()) {
            assertTrue(foundations.has(suit.name()));
        }
    }

    @Test
    public void serializedInitialFoundationsAreEmpty()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONObject foundations =
                json.getJSONObject("foundations");

        for (Card.Suit suit : Card.Suit.values()) {

            assertEquals(
                    0,
                    foundations.getJSONArray(suit.name())
                            .length()
            );
        }
    }

    @Test
    public void serializedFoundationReflectsCardMovement()
            throws JSONException {

        // The unshuffled deal places the Ace of Clubs
        // in the first tableau column.
        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONArray clubs = json.getJSONObject("foundations")
                .getJSONArray("CLUBS");

        assertEquals(1, clubs.length());

        assertEquals(
                "ACE",
                clubs.getJSONObject(0).getString("rank")
        );

        assertEquals(
                "CLUBS",
                clubs.getJSONObject(0).getString("suit")
        );
    }

    @Test
    public void serializedFoundationsPreserveCardOrder()
            throws JSONException {

        clearPiles();

        for (Card.Suit suit : Card.Suit.values()) {

            state.getFoundation(suit).add(
                    faceUpCard(suit, Card.Rank.ACE)
            );

            state.getFoundation(suit).add(
                    faceUpCard(suit, Card.Rank.TWO)
            );

            state.getFoundation(suit).add(
                    faceUpCard(suit, Card.Rank.THREE)
            );
        }

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONObject foundations =
                json.getJSONObject("foundations");

        for (Card.Suit suit : Card.Suit.values()) {

            assertSerializedCardsEqual(
                    state.getFoundation(suit),
                    foundations.getJSONArray(suit.name())
            );
        }
    }

    // TABLEAU TESTS

    @Test
    public void serializedTableauContainsSevenColumns()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                GameState.TABLEAU_COLUMNS,
                json.getJSONArray("tableau").length()
        );
    }

    @Test
    public void serializedInitialTableauPreservesColumnSizes()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONArray tableau = json.getJSONArray("tableau");

        for (int i = 0; i < tableau.length(); i++) {

            assertEquals(
                    i + 1,
                    tableau.getJSONArray(i).length()
            );
        }
    }

    @Test
    public void serializedTableauPreservesCardOrder()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONArray tableau = json.getJSONArray("tableau");

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertSerializedCardsEqual(
                    state.getTableauColumn(i),
                    tableau.getJSONArray(i)
            );
        }
    }

    @Test
    public void serializedTableauPreservesCardOrientations()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONArray tableau = json.getJSONArray("tableau");

        for (int column = 0; column < tableau.length(); column++) {

            JSONArray cards = tableau.getJSONArray(column);

            for (int i = 0; i < cards.length(); i++) {

                boolean expectedFaceUp =
                        i == cards.length() - 1;

                assertEquals(
                        expectedFaceUp,
                        cards.getJSONObject(i)
                                .getBoolean("faceUp")
                );
            }
        }
    }

    @Test
    public void serializedTableauReflectsCardMovement()
            throws JSONException {

        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONArray tableau = json.getJSONArray("tableau");

        assertEquals(
                0,
                tableau.getJSONArray(0).length()
        );

        assertEquals(
                GameState.TABLEAU_COLUMNS,
                tableau.length()
        );
    }

    // GAME VALUE TESTS

    @Test
    public void serializedGamePreservesScore()
            throws JSONException {

        state.setScore(125);

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(125, json.getInt("score"));
    }

    @Test
    public void serializedGamePreservesMoveCount()
            throws JSONException {

        state.setMoves(32);

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(32, json.getInt("moves"));
    }

    @Test
    public void serializedGamePreservesRemainingUndos()
            throws JSONException {

        state.setRemainingUndos(2);

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(2, json.getInt("remainingUndos"));
    }

    @Test
    public void serializedNewGameIsNotWon()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertFalse(json.getBoolean("gameWon"));
    }

    @Test
    public void serializedCompletedGamePreservesWinStatus()
            throws JSONException {

        state.setGameWon(true);

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertTrue(json.getBoolean("gameWon"));
    }

    // UNDO HISTORY TESTS

    @Test
    public void serializedInitialUndoHistoryIsEmpty()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                0,
                json.getJSONArray("undoHistory").length()
        );
    }

    @Test
    public void serializedUndoHistoryContainsRecordedActions()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                2,
                json.getJSONArray("undoHistory").length()
        );
    }

    @Test
    public void serializedUndoHistoryPreservesNewestToOldestOrder()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONArray history = json.getJSONArray("undoHistory");

        for (int i = 0; i < history.length(); i++) {

            JSONObject snapshot = history.getJSONObject(i)
                    .getJSONObject("snapshot");

            assertEquals(
                    22 + i,
                    snapshot.getJSONArray("stock").length()
            );
        }
    }

    @Test
    public void serializedUndoEntryPreservesZeroScoreChange()
            throws JSONException {

        assertTrue(engine.drawCard());

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONObject entry = json.getJSONArray("undoHistory")
                .getJSONObject(0);

        assertEquals(
                0,
                entry.getInt("reversibleScoreChange")
        );
    }

    @Test
    public void serializedUndoEntryPreservesPositiveScoreChange()
            throws JSONException {

        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONObject entry = json.getJSONArray("undoHistory")
                .getJSONObject(0);

        assertEquals(
                10,
                entry.getInt("reversibleScoreChange")
        );
    }

    @Test
    public void serializedUndoEntryPreservesNegativeScoreChange()
            throws JSONException {

        clearPiles();

        state.setScore(100);

        state.getFoundation(Card.Suit.CLUBS).add(
                faceUpCard(Card.Suit.CLUBS, Card.Rank.ACE)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.TWO)
        );

        assertTrue(
                engine.moveFoundationToTableau(
                        Card.Suit.CLUBS,
                        0
                )
        );

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONObject entry = json.getJSONArray("undoHistory")
                .getJSONObject(0);

        assertEquals(
                -10,
                entry.getInt("reversibleScoreChange")
        );
    }

    @Test
    public void serializedUndoHistoryPreservesCompleteSnapshots()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        ArrayList<UndoManager.UndoEntry> entries =
                engine.exportUndoHistory();

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        JSONArray history = json.getJSONArray("undoHistory");

        for (int i = 0; i < entries.size(); i++) {

            UndoManager.UndoEntry expected = entries.get(i);

            JSONObject actual = history.getJSONObject(i);

            assertEquals(
                    expected.getReversibleScoreChange(),
                    actual.getInt("reversibleScoreChange")
            );

            assertSerializedSnapshotEqual(
                    expected.getSnapshot(),
                    actual.getJSONObject("snapshot")
            );
        }
    }

    @Test
    public void serializedUndoHistoryReflectsConsumedUndo()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        assertTrue(engine.undo());

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                2,
                json.getInt("remainingUndos")
        );

        assertEquals(
                2,
                json.getJSONArray("undoHistory").length()
        );
    }

    // SERIALIZATION INTEGRITY TESTS

    @Test
    public void serializationDoesNotModifyCurrentBoard()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        GameSnapshot before = new GameSnapshot(state);

        GameStateSerializer.serializeGame(engine);

        JSONObject after =
                GameStateSerializer.serializeGame(engine);

        assertSerializedSnapshotEqual(before, after);
    }

    @Test
    public void serializationDoesNotModifyGameValues()
            throws JSONException {

        state.setScore(150);
        state.setMoves(25);
        state.setRemainingUndos(2);

        GameStateSerializer.serializeGame(engine);

        assertEquals(150, state.getScore());
        assertEquals(25, state.getMoves());
        assertEquals(2, state.getRemainingUndos());
        assertFalse(state.isGameWon());
    }

    @Test
    public void serializationDoesNotModifyUndoHistory()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        ArrayList<UndoManager.UndoEntry> before =
                engine.exportUndoHistory();

        GameStateSerializer.serializeGame(engine);

        ArrayList<UndoManager.UndoEntry> after =
                engine.exportUndoHistory();

        assertEquals(before.size(), after.size());

        for (int i = 0; i < before.size(); i++) {
            assertSame(before.get(i), after.get(i));
        }
    }

    @Test
    public void serializedGameRemainsIndependentOfCurrentState()
            throws JSONException {

        JSONObject originalJson =
                GameStateSerializer.serializeGame(engine);

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        JSONObject currentJson =
                GameStateSerializer.serializeGame(engine);

        assertEquals(
                24,
                originalJson.getJSONArray("stock").length()
        );

        assertEquals(
                0,
                originalJson.getJSONArray("discard").length()
        );

        assertEquals(
                22,
                currentJson.getJSONArray("stock").length()
        );

        assertEquals(
                2,
                currentJson.getJSONArray("discard").length()
        );
    }

    // DESERIALIZATION RECONSTRUCTION TESTS

    @Test
    public void deserializationCreatesNewGameEngine()
            throws JSONException {

        GameEngine loaded = deserializeCurrentGame();

        assertNotNull(loaded);
        assertNotSame(engine, loaded);
    }

    @Test
    public void deserializationCreatesIndependentGameState()
            throws JSONException {

        GameEngine loaded = deserializeCurrentGame();

        assertNotSame(state, loaded.getState());
    }

    @Test
    public void deserializationCreatesIndependentCardCollections()
            throws JSONException {

        GameState loaded = deserializeCurrentGame().getState();

        assertNotSame(state.getStock(), loaded.getStock());
        assertNotSame(state.getDiscard(), loaded.getDiscard());
        assertNotSame(state.getFoundations(), loaded.getFoundations());
        assertNotSame(state.getTableau(), loaded.getTableau());

        for (Card.Suit suit : Card.Suit.values()) {

            assertNotSame(
                    state.getFoundation(suit),
                    loaded.getFoundation(suit)
            );
        }

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertNotSame(
                    state.getTableauColumn(i),
                    loaded.getTableauColumn(i)
            );
        }
    }

    @Test
    public void deserializationCreatesIndependentCardObjects()
            throws JSONException {

        GameState loaded = deserializeCurrentGame().getState();

        Card originalCard = state.getStock().get(0);
        Card loadedCard = loaded.getStock().get(0);

        assertNotSame(originalCard, loadedCard);

        assertEquals(originalCard.getSuit(), loadedCard.getSuit());
        assertEquals(originalCard.getRank(), loadedCard.getRank());
        assertEquals(originalCard.isFaceUp(), loadedCard.isFaceUp());
    }

    // DESERIALIZATION CARD COLLECTION TESTS

    @Test
    public void deserializationPreservesOriginalDeck()
            throws JSONException {

        GameState loaded = deserializeCurrentGame().getState();

        assertCardListsEqual(
                state.getOriginalDeck(),
                loaded.getOriginalDeck()
        );
    }

    @Test
    public void deserializationPreservesStock()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        GameState loaded = deserializeCurrentGame().getState();

        assertCardListsEqual(
                state.getStock(),
                loaded.getStock()
        );
    }

    @Test
    public void deserializationPreservesDiscard()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        GameState loaded = deserializeCurrentGame().getState();

        assertCardListsEqual(
                state.getDiscard(),
                loaded.getDiscard()
        );
    }

    @Test
    public void deserializationPreservesFoundations()
            throws JSONException {

        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        GameState loaded = deserializeCurrentGame().getState();

        for (Card.Suit suit : Card.Suit.values()) {

            assertCardListsEqual(
                    state.getFoundation(suit),
                    loaded.getFoundation(suit)
            );
        }
    }

    @Test
    public void deserializationPreservesTableau()
            throws JSONException {

        GameState loaded = deserializeCurrentGame().getState();

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertCardListsEqual(
                    state.getTableauColumn(i),
                    loaded.getTableauColumn(i)
            );
        }
    }

    @Test
    public void deserializationPreservesEmptyTableauColumn()
            throws JSONException {

        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        GameState loaded = deserializeCurrentGame().getState();

        assertTrue(loaded.getTableauColumn(0).isEmpty());

        assertEquals(
                GameState.TABLEAU_COLUMNS,
                loaded.getTableau().size()
        );
    }

    // DESERIALIZATION GAME VALUE TESTS

    @Test
    public void deserializationPreservesScore()
            throws JSONException {

        state.setScore(125);

        GameState loaded = deserializeCurrentGame().getState();

        assertEquals(125, loaded.getScore());
    }

    @Test
    public void deserializationPreservesMoveCount()
            throws JSONException {

        state.setMoves(32);

        GameState loaded = deserializeCurrentGame().getState();

        assertEquals(32, loaded.getMoves());
    }

    @Test
    public void deserializationPreservesRemainingUndos()
            throws JSONException {

        state.setRemainingUndos(2);

        GameState loaded = deserializeCurrentGame().getState();

        assertEquals(2, loaded.getRemainingUndos());
    }

    @Test
    public void deserializationPreservesUnfinishedGameStatus()
            throws JSONException {

        GameState loaded = deserializeCurrentGame().getState();

        assertFalse(loaded.isGameWon());
    }

    @Test
    public void deserializationPreservesCompletedGameStatus()
            throws JSONException {

        clearPiles();

        for (Card.Suit suit : Card.Suit.values()) {

            for (Card.Rank rank : Card.Rank.values()) {

                state.getFoundation(suit).add(
                        faceUpCard(suit, rank)
                );
            }
        }

        state.setGameWon(true);

        GameState loaded = deserializeCurrentGame().getState();

        assertTrue(loaded.isGameWon());
    }

    // DESERIALIZATION UNDO HISTORY TESTS

    @Test
    public void deserializationPreservesEmptyUndoHistory()
            throws JSONException {

        GameEngine loaded = deserializeCurrentGame();

        assertEquals(0, loaded.getUndoHistorySize());
        assertFalse(loaded.canUndo());
    }

    @Test
    public void deserializationPreservesUndoHistorySize()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        GameEngine loaded = deserializeCurrentGame();

        assertEquals(2, loaded.getUndoHistorySize());
        assertTrue(loaded.canUndo());
    }

    @Test
    public void deserializationPreservesUndoHistoryOrder()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        GameEngine loaded = deserializeCurrentGame();

        ArrayList<UndoManager.UndoEntry> entries =
                loaded.exportUndoHistory();

        assertEquals(3, entries.size());

        for (int i = 0; i < entries.size(); i++) {

            assertEquals(
                    22 + i,
                    entries.get(i).getSnapshot().getStock().size()
            );
        }
    }

    @Test
    public void deserializationPreservesPositiveScoreChange()
            throws JSONException {

        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        GameEngine loaded = deserializeCurrentGame();

        assertEquals(
                10,
                loaded.exportUndoHistory().get(0)
                        .getReversibleScoreChange()
        );
    }

    @Test
    public void deserializationPreservesNegativeScoreChange()
            throws JSONException {

        clearPiles();

        state.setScore(100);

        state.getFoundation(Card.Suit.CLUBS).add(
                faceUpCard(Card.Suit.CLUBS, Card.Rank.ACE)
        );

        state.getTableauColumn(0).add(
                faceUpCard(Card.Suit.HEARTS, Card.Rank.TWO)
        );

        assertTrue(
                engine.moveFoundationToTableau(
                        Card.Suit.CLUBS,
                        0
                )
        );

        GameEngine loaded = deserializeCurrentGame();

        assertEquals(
                -10,
                loaded.exportUndoHistory().get(0)
                        .getReversibleScoreChange()
        );
    }

    @Test
    public void deserializationPreservesCompleteUndoSnapshots()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        GameEngine loaded = deserializeCurrentGame();

        ArrayList<UndoManager.UndoEntry> expected =
                engine.exportUndoHistory();

        ArrayList<UndoManager.UndoEntry> actual =
                loaded.exportUndoHistory();

        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {

            assertEquals(
                    expected.get(i).getReversibleScoreChange(),
                    actual.get(i).getReversibleScoreChange()
            );

            assertSnapshotsEqual(
                    expected.get(i).getSnapshot(),
                    actual.get(i).getSnapshot()
            );
        }
    }

    @Test
    public void deserializationPreservesConsumedUndoAllowance()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        assertTrue(engine.undo());

        GameEngine loaded = deserializeCurrentGame();

        assertEquals(2, loaded.getState().getRemainingUndos());
        assertEquals(2, loaded.getUndoHistorySize());
    }

    // GAMEPLAY AFTER DESERIALIZATION TESTS

    @Test
    public void loadedGameCanDrawNextCard()
            throws JSONException {

        assertTrue(engine.drawCard());

        Card expectedCard = state.getStock().get(
                state.getStock().size() - 1
        );

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(loaded.drawCard());

        ArrayList<Card> discard =
                loaded.getState().getDiscard();

        Card actualCard = discard.get(discard.size() - 1);

        assertEquals(expectedCard.getSuit(), actualCard.getSuit());
        assertEquals(expectedCard.getRank(), actualCard.getRank());
        assertTrue(actualCard.isFaceUp());
    }

    @Test
    public void loadedGameCanRecycleDiscard()
            throws JSONException {

        ArrayList<Card> originalStock =
                new GameSnapshot(state).getStock();

        while (!state.getStock().isEmpty()) {
            assertTrue(engine.drawCard());
        }

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(loaded.recycleDiscard());

        assertCardListsEqual(
                originalStock,
                loaded.getState().getStock()
        );

        assertTrue(loaded.getState().getDiscard().isEmpty());
    }

    @Test
    public void loadedGameCanMoveCardToFoundation()
            throws JSONException {

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(
                loaded.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        assertEquals(
                1,
                loaded.getState()
                        .getFoundation(Card.Suit.CLUBS).size()
        );

        assertEquals(10, loaded.getState().getScore());
        assertEquals(1, loaded.getState().getMoves());
    }

    @Test
    public void loadedGameCanUndoPreviousAction()
            throws JSONException {

        GameSnapshot before = new GameSnapshot(state);

        assertTrue(engine.drawCard());

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(loaded.undo());

        assertSnapshotsEqual(
                before,
                new GameSnapshot(loaded.getState())
        );

        assertEquals(2, loaded.getState().getRemainingUndos());
    }

    @Test
    public void loadedGameCanPerformConsecutiveUndos()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(loaded.undo());
        assertTrue(loaded.undo());
        assertTrue(loaded.undo());

        assertEquals(24, loaded.getState().getStock().size());
        assertTrue(loaded.getState().getDiscard().isEmpty());

        assertEquals(0, loaded.getState().getRemainingUndos());
        assertFalse(loaded.canUndo());
    }

    @Test
    public void loadedGameUndoReversesAutomaticReveal()
            throws JSONException {

        clearPiles();

        Card hiddenCard =
                new Card(Card.Suit.SPADES, Card.Rank.KING);

        Card visibleCard =
                faceUpCard(Card.Suit.HEARTS, Card.Rank.ACE);

        state.getTableauColumn(0).add(hiddenCard);
        state.getTableauColumn(0).add(visibleCard);

        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.HEARTS
                )
        );

        assertTrue(hiddenCard.isFaceUp());

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(loaded.undo());

        ArrayList<Card> column =
                loaded.getState().getTableauColumn(0);

        assertEquals(2, column.size());
        assertFalse(column.get(0).isFaceUp());
        assertTrue(column.get(1).isFaceUp());

        assertTrue(
                loaded.getState()
                        .getFoundation(Card.Suit.HEARTS).isEmpty()
        );
    }

    @Test
    public void loadedGameRecordsNewUndoEntries()
            throws JSONException {

        assertTrue(engine.drawCard());

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(loaded.drawCard());

        assertEquals(2, loaded.getUndoHistorySize());

        assertTrue(loaded.undo());

        assertEquals(1, loaded.getState().getDiscard().size());
    }

    @Test
    public void loadedGameUndoPreservesScoringRules()
            throws JSONException {

        state.setScore(100);

        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        assertEquals(110, state.getScore());

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(loaded.undo());

        assertEquals(90, loaded.getState().getScore());
    }

    @Test
    public void loadedCompletedGameRejectsFurtherMoves()
            throws JSONException {

        clearPiles();

        for (Card.Suit suit : Card.Suit.values()) {

            for (Card.Rank rank : Card.Rank.values()) {

                state.getFoundation(suit).add(
                        faceUpCard(suit, rank)
                );
            }
        }

        state.setGameWon(true);

        GameEngine loaded = deserializeCurrentGame();

        assertFalse(loaded.drawCard());
        assertFalse(loaded.recycleDiscard());
        assertFalse(loaded.undo());
        assertTrue(loaded.getState().isGameWon());
    }

    // RESTART AND NEW GAME AFTER DESERIALIZATION TESTS

    @Test
    public void loadedGameRestartsOriginalDeal()
            throws JSONException {

        GameSnapshot originalBoard = new GameSnapshot(state);

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        GameEngine loaded = deserializeCurrentGame();

        loaded.restart();

        assertSnapshotsEqual(
                originalBoard,
                new GameSnapshot(loaded.getState())
        );
    }

    @Test
    public void loadedGameRestartResetsGameValues()
            throws JSONException {

        assertTrue(engine.drawCard());

        state.setScore(100);
        state.setMoves(25);
        state.setRemainingUndos(2);

        GameEngine loaded = deserializeCurrentGame();

        loaded.restart();

        assertEquals(0, loaded.getState().getScore());
        assertEquals(0, loaded.getState().getMoves());

        assertEquals(
                GameState.MAX_UNDOS,
                loaded.getState().getRemainingUndos()
        );

        assertFalse(loaded.getState().isGameWon());
        assertEquals(0, loaded.getUndoHistorySize());
    }

    @Test
    public void loadedGameCanStartNewGame()
            throws JSONException {

        assertTrue(engine.drawCard());

        GameEngine loaded = deserializeCurrentGame();

        loaded.newGame();

        assertEquals(24, loaded.getState().getStock().size());
        assertTrue(loaded.getState().getDiscard().isEmpty());

        assertEquals(
                Deck.DECK_SIZE,
                loaded.getState().getOriginalDeck().size()
        );

        assertEquals(0, loaded.getUndoHistorySize());
        assertEquals(0, loaded.getState().getMoves());
    }

    @Test
    public void loadedGameCanRestartNewDeal()
            throws JSONException {

        GameEngine loaded = deserializeCurrentGame();

        loaded.newGame();

        GameSnapshot newDeal =
                new GameSnapshot(loaded.getState());

        assertTrue(loaded.drawCard());

        loaded.restart();

        assertSnapshotsEqual(
                newDeal,
                new GameSnapshot(loaded.getState())
        );
    }

    // DESERIALIZATION INDEPENDENCE TESTS

    @Test
    public void loadedGameChangesDoNotAffectOriginalGame()
            throws JSONException {

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(loaded.drawCard());

        assertEquals(24, state.getStock().size());
        assertTrue(state.getDiscard().isEmpty());

        assertEquals(23, loaded.getState().getStock().size());
        assertEquals(1, loaded.getState().getDiscard().size());
    }

    @Test
    public void originalGameChangesDoNotAffectLoadedGame()
            throws JSONException {

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(engine.drawCard());

        assertEquals(24, loaded.getState().getStock().size());
        assertTrue(loaded.getState().getDiscard().isEmpty());

        assertEquals(23, state.getStock().size());
        assertEquals(1, state.getDiscard().size());
    }

    @Test
    public void loadedUndoDoesNotModifyOriginalUndoHistory()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        GameEngine loaded = deserializeCurrentGame();

        assertTrue(loaded.undo());

        assertEquals(2, engine.getUndoHistorySize());
        assertEquals(3, state.getRemainingUndos());

        assertEquals(1, loaded.getUndoHistorySize());
        assertEquals(2, loaded.getState().getRemainingUndos());
    }

    // EXISTING DESERIALIZATION VALIDATION TESTS

    @Test(expected = JSONException.class)
    public void deserializationRejectsUnsupportedSaveVersion()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        json.put(
                "version",
                GameStateSerializer.SAVE_VERSION + 1
        );

        GameStateSerializer.deserializeGame(json);
    }

    @Test(expected = JSONException.class)
    public void deserializationRejectsMissingRequiredAttribute()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        json.remove("stock");

        GameStateSerializer.deserializeGame(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deserializationRejectsInvalidSuitName()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        json.getJSONArray("originalDeck")
                .getJSONObject(0)
                .put("suit", "INVALID");

        GameStateSerializer.deserializeGame(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deserializationRejectsInvalidRankName()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        json.getJSONArray("originalDeck")
                .getJSONObject(0)
                .put("rank", "INVALID");

        GameStateSerializer.deserializeGame(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deserializationRejectsNegativeScore()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        json.put("score", -10);

        GameStateSerializer.deserializeGame(json);
    }

    @Test(expected = JSONException.class)
    public void deserializationRejectsNegativeMoves()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        json.put("moves", -1);

        GameStateSerializer.deserializeGame(json);
    }

    @Test(expected = JSONException.class)
    public void deserializationRejectsNegativeRemainingUndos()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        json.put("remainingUndos", -1);

        GameStateSerializer.deserializeGame(json);
    }

    @Test(expected = JSONException.class)
    public void deserializationRejectsExcessiveRemainingUndos()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        json.put(
                "remainingUndos",
                GameState.MAX_UNDOS + 1
        );

        GameStateSerializer.deserializeGame(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deserializationRejectsExcessiveUndoHistory()
            throws JSONException {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        json.put("remainingUndos", 1);

        GameStateSerializer.deserializeGame(json);
    }

    // HELPER METHODS

    // Compare Java card collections against serialized JSON arrays
    private void assertSerializedCardsEqual(
            ArrayList<Card> expected,
            JSONArray actual) throws JSONException {

        assertEquals(expected.size(), actual.length());

        for (int i = 0; i < expected.size(); i++) {

            Card expectedCard = expected.get(i);
            JSONObject actualCard = actual.getJSONObject(i);

            assertEquals(
                    expectedCard.getSuit().name(),
                    actualCard.getString("suit")
            );

            assertEquals(
                    expectedCard.getRank().name(),
                    actualCard.getString("rank")
            );

            assertEquals(
                    expectedCard.isFaceUp(),
                    actualCard.getBoolean("faceUp")
            );
        }
    }

    // Compare a GameSnapshot against its serialized representation
    private void assertSerializedSnapshotEqual(
            GameSnapshot expected,
            JSONObject actual) throws JSONException {

        assertSerializedCardsEqual(
                expected.getStock(),
                actual.getJSONArray("stock")
        );

        assertSerializedCardsEqual(
                expected.getDiscard(),
                actual.getJSONArray("discard")
        );

        JSONObject foundations =
                actual.getJSONObject("foundations");

        assertEquals(
                Card.Suit.values().length,
                foundations.length()
        );

        for (Card.Suit suit : Card.Suit.values()) {

            assertSerializedCardsEqual(
                    expected.getFoundations().get(suit),
                    foundations.getJSONArray(suit.name())
            );
        }

        JSONArray tableau = actual.getJSONArray("tableau");

        assertEquals(
                GameState.TABLEAU_COLUMNS,
                tableau.length()
        );

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertSerializedCardsEqual(
                    expected.getTableau().get(i),
                    tableau.getJSONArray(i)
            );
        }

        assertEquals(
                expected.getScore(),
                actual.getInt("score")
        );

        assertEquals(
                expected.isGameWon(),
                actual.getBoolean("gameWon")
        );
    }

    // Create an individual face-up card
    private Card faceUpCard(Card.Suit suit, Card.Rank rank) {

        Card card = new Card(suit, rank);
        card.setFaceUp(true);

        return card;
    }

    // Clear existing piles for controlled test scenarios
    private void clearPiles() {

        state.getStock().clear();
        state.getDiscard().clear();

        for (Card.Suit suit : Card.Suit.values()) {
            state.getFoundation(suit).clear();
        }

        for (ArrayList<Card> column : state.getTableau()) {
            column.clear();
        }
    }

    // DESERIALIZATION HELPER METHODS

    // Serialize and reconstruct the current game
    private GameEngine deserializeCurrentGame()
            throws JSONException {

        JSONObject json =
                GameStateSerializer.serializeGame(engine);

        return GameStateSerializer.deserializeGame(json);
    }

    // Compare two independent card collections
    private void assertCardListsEqual(
            ArrayList<Card> expected,
            ArrayList<Card> actual) {

        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {

            Card expectedCard = expected.get(i);
            Card actualCard = actual.get(i);

            assertEquals(
                    expectedCard.getSuit(),
                    actualCard.getSuit()
            );

            assertEquals(
                    expectedCard.getRank(),
                    actualCard.getRank()
            );

            assertEquals(
                    expectedCard.isFaceUp(),
                    actualCard.isFaceUp()
            );
        }
    }

    // Compare two independent game snapshots
    private void assertSnapshotsEqual(
            GameSnapshot expected,
            GameSnapshot actual) {

        assertCardListsEqual(
                expected.getStock(),
                actual.getStock()
        );

        assertCardListsEqual(
                expected.getDiscard(),
                actual.getDiscard()
        );

        for (Card.Suit suit : Card.Suit.values()) {

            assertCardListsEqual(
                    expected.getFoundations().get(suit),
                    actual.getFoundations().get(suit)
            );
        }

        for (int i = 0; i < GameState.TABLEAU_COLUMNS; i++) {

            assertCardListsEqual(
                    expected.getTableau().get(i),
                    actual.getTableau().get(i)
            );
        }

        assertEquals(
                expected.getScore(),
                actual.getScore()
        );

        assertEquals(
                expected.isGameWon(),
                actual.isGameWon()
        );
    }
}