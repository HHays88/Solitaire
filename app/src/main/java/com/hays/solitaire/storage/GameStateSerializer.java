package com.hays.solitaire.storage;

import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.GameState;
import com.hays.solitaire.model.GameSnapshot;
import com.hays.solitaire.engine.GameEngine;
import com.hays.solitaire.engine.UndoManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.EnumMap;

// Save version independent of App version, increment if the game save
// file structure is changed to implement compatibility with older saves
public final class GameStateSerializer {
    public static final int SAVE_VERSION = 1;
    // Private constructor
    // Utility class does not require an instance
    private GameStateSerializer() {
    }

    // GAME SERIALIZATION
    // Convert the current game and undo history into JSON
    public static JSONObject serializeGame(
            // SaveManager handles serialization failures
            GameEngine engine) throws JSONException {
        GameState state = engine.getState();
        JSONObject json = new JSONObject();
        json.put("version", SAVE_VERSION);
        // Preserve original deal for restarting
        json.put(
                "originalDeck",
                serializeCards(state.getOriginalDeck())
        );
        // Preserve current card collections
        json.put(
                "stock",
                serializeCards(state.getStock())
        );
        json.put(
                "discard",
                serializeCards(state.getDiscard())
        );
        json.put(
                "foundations",
                serializeFoundations(state.getFoundations())
        );
        json.put(
                "tableau",
                serializeTableau(state.getTableau())
        );
        // Preserve current game values
        json.put("score", state.getScore());
        json.put("moves", state.getMoves());
        json.put("remainingUndos", state.getRemainingUndos());
        json.put("gameWon", state.isGameWon());
        // Preserve undo history
        json.put(
                "undoHistory",
                serializeUndoHistory(engine.exportUndoHistory())
        );
        return json;
    }

    // CARD SERIALIZATION
    // Convert an individual card into a JSON object
    private static JSONObject serializeCard(
            Card card) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("suit", card.getSuit().name());
        json.put("rank", card.getRank().name());
        json.put("faceUp", card.isFaceUp());
        return json;
    }

    // CARD COLLECTION SERIALIZATION
    // Convert a collection of cards into a JSON array
    // while preserving card order
    private static JSONArray serializeCards(
            ArrayList<Card> cards) throws JSONException {
        JSONArray json = new JSONArray();
        for (Card card : cards) {
            json.put(serializeCard(card));
        }
        return json;
    }

    // FOUNDATION SERIALIZATION
    // Convert each foundation pile into a JSON array
    // associated with its suit
    private static JSONObject serializeFoundations(
            EnumMap<Card.Suit, ArrayList<Card>> foundations)
            throws JSONException {
        JSONObject json = new JSONObject();
        for (Card.Suit suit : Card.Suit.values()) {
            json.put(
                    suit.name(),
                    serializeCards(foundations.get(suit))
            );
        }
        return json;
    }

    // TABLEAU SERIALIZATION
    // Convert tableau columns into an array of card arrays
    private static JSONArray serializeTableau(
            ArrayList<ArrayList<Card>> tableau)
            throws JSONException {
        JSONArray json = new JSONArray();
        for (ArrayList<Card> column : tableau) {
            json.put(serializeCards(column));
        }
        return json;
    }

    // SNAPSHOT SERIALIZATION
    // Convert an independent game snapshot into JSON
    private static JSONObject serializeSnapshot(
            GameSnapshot snapshot) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(
                "stock",
                serializeCards(snapshot.getStock())
        );
        json.put(
                "discard",
                serializeCards(snapshot.getDiscard())
        );
        json.put(
                "foundations",
                serializeFoundations(snapshot.getFoundations())
        );
        json.put(
                "tableau",
                serializeTableau(snapshot.getTableau())
        );
        json.put("score", snapshot.getScore());
        json.put("gameWon", snapshot.isGameWon());
        return json;
    }

    // UNDO ENTRY SERIALIZATION
    // Preserve the snapshot and its reversible score change
    private static JSONObject serializeUndoEntry(
            UndoManager.UndoEntry entry)
            throws JSONException {
        JSONObject json = new JSONObject();
        json.put(
                "reversibleScoreChange",
                entry.getReversibleScoreChange()
        );
        json.put(
                "snapshot",
                serializeSnapshot(entry.getSnapshot())
        );
        return json;
    }

    // UNDO HISTORY SERIALIZATION
    // Preserve undo history in newest-to-oldest order
    private static JSONArray serializeUndoHistory(
            ArrayList<UndoManager.UndoEntry> entries)
            throws JSONException {
        JSONArray json = new JSONArray();
        for (UndoManager.UndoEntry entry : entries) {
            json.put(serializeUndoEntry(entry));
        }
        return json;
    }

    // GAME DESERIALIZATION
    // Reconstruct an independent game engine from saved JSON
    public static GameEngine deserializeGame(
            JSONObject json) throws JSONException {
        // Verify save format compatibility
        int version = json.getInt("version");
        if (version != SAVE_VERSION) {
            throw new JSONException(
                    "Unsupported save version: " + version
            );
        }
        // Reconstruct original deck
        ArrayList<Card> originalDeck =
                deserializeCards(
                        json.getJSONArray("originalDeck")
                );
        // Initialize game state using the original deal
        GameState state = new GameState(originalDeck);
        // Reconstruct and restore the current board
        GameSnapshot snapshot = deserializeSnapshot(json);
        snapshot.restore(state);
        // Retrieve saved game values
        int score = snapshot.getScore();
        int moves = json.getInt("moves");
        int remainingUndos = json.getInt("remainingUndos");
        // Validate before applying saved values
        validateGameValues(
                score,
                moves,
                remainingUndos
        );
        // Restore saved game values
        state.setScore(score);
        state.setMoves(moves);
        state.setRemainingUndos(remainingUndos);
        // Initialize engine using reconstructed state
        GameEngine engine = new GameEngine(state);
        // Reconstruct and import undo history
        engine.importUndoHistory(
                deserializeUndoHistory(
                        json.getJSONArray("undoHistory")
                )
        );
        return engine;
    }

    // CARD DESERIALIZATION
    // Reconstruct an individual card from JSON
    private static Card deserializeCard(
            JSONObject json) throws JSONException {
        Card.Suit suit = Card.Suit.valueOf(
                json.getString("suit")
        );
        Card.Rank rank = Card.Rank.valueOf(
                json.getString("rank")
        );
        Card card = new Card(suit, rank);
        card.setFaceUp(
                json.getBoolean("faceUp")
        );
        return card;
    }

    // CARD COLLECTION DESERIALIZATION
    // Reconstruct an ordered collection of cards
    private static ArrayList<Card> deserializeCards(
            JSONArray json) throws JSONException {
        ArrayList<Card> cards =
                new ArrayList<>(json.length());
        for (int i = 0; i < json.length(); i++) {
            cards.add(
                    deserializeCard(
                            json.getJSONObject(i)
                    )
            );
        }
        return cards;
    }

    // FOUNDATION DESERIALIZATION
    // Reconstruct foundation piles associated with each suit
    private static EnumMap<Card.Suit, ArrayList<Card>>
    deserializeFoundations(
            JSONObject json) throws JSONException {
        EnumMap<Card.Suit, ArrayList<Card>> foundations =
                new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : Card.Suit.values()) {
            foundations.put(
                    suit,
                    deserializeCards(
                            json.getJSONArray(suit.name())
                    )
            );
        }
        return foundations;
    }

    // TABLEAU DESERIALIZATION
    // Reconstruct tableau columns in their original order
    private static ArrayList<ArrayList<Card>>
    deserializeTableau(
            JSONArray json) throws JSONException {
        ArrayList<ArrayList<Card>> tableau =
                new ArrayList<>(GameState.TABLEAU_COLUMNS);
        for (int i = 0; i < json.length(); i++) {
            tableau.add(
                    deserializeCards(
                            json.getJSONArray(i)
                    )
            );
        }
        return tableau;
    }

    // SNAPSHOT DESERIALIZATION
    // Reconstruct an independent snapshot from saved JSON
    private static GameSnapshot deserializeSnapshot(
            JSONObject json) throws JSONException {
        ArrayList<Card> stock =
                deserializeCards(
                        json.getJSONArray("stock")
                );
        ArrayList<Card> discard =
                deserializeCards(
                        json.getJSONArray("discard")
                );
        EnumMap<Card.Suit, ArrayList<Card>> foundations =
                deserializeFoundations(
                        json.getJSONObject("foundations")
                );
        ArrayList<ArrayList<Card>> tableau =
                deserializeTableau(
                        json.getJSONArray("tableau")
                );
        int score = json.getInt("score");
        boolean gameWon = json.getBoolean("gameWon");
        return new GameSnapshot(
                stock,
                discard,
                foundations,
                tableau,
                score,
                gameWon
        );
    }

    // UNDO ENTRY DESERIALIZATION
    // Reconstruct a snapshot and its reversible score change
    private static UndoManager.UndoEntry deserializeUndoEntry(
            JSONObject json) throws JSONException {
        int reversibleScoreChange =
                json.getInt("reversibleScoreChange");
        GameSnapshot snapshot =
                deserializeSnapshot(
                        json.getJSONObject("snapshot")
                );
        return new UndoManager.UndoEntry(
                snapshot,
                reversibleScoreChange
        );
    }

    // UNDO HISTORY DESERIALIZATION
    // Reconstruct undo history in newest-to-oldest order
    private static ArrayList<UndoManager.UndoEntry>
    deserializeUndoHistory(
            JSONArray json) throws JSONException {
        ArrayList<UndoManager.UndoEntry> entries =
                new ArrayList<>(json.length());
        for (int i = 0; i < json.length(); i++) {
            entries.add(
                    deserializeUndoEntry(
                            json.getJSONObject(i)
                    )
            );
        }
        return entries;
    }

    // GAME VALUE VALIDATION
    // Reject invalid saved values instead of silently correcting them
    private static void validateGameValues(
            int score,
            int moves,
            int remainingUndos)
            throws JSONException {
        if (score < 0) {
            throw new JSONException(
                    "Saved score cannot be negative."
            );
        }
        if (moves < 0) {
            throw new JSONException(
                    "Saved move count cannot be negative."
            );
        }
        if (remainingUndos < 0
                || remainingUndos > GameState.MAX_UNDOS) {

            throw new JSONException(
                    "Invalid number of remaining undos."
            );
        }
    }
}