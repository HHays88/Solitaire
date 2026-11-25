package com.hays.solitaire.storage;

import android.content.Context;
import android.util.AtomicFile;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.hays.solitaire.model.Card;
import com.hays.solitaire.model.Deck;
import com.hays.solitaire.model.GameState;
import com.hays.solitaire.model.GameSnapshot;
import com.hays.solitaire.engine.GameEngine;
import com.hays.solitaire.engine.UndoManager;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SaveManagerTest {

    private Context context;
    private File testDirectory;
    private File testSaveFile;

    private SaveManager saveManager;
    private GameState state;
    private GameEngine engine;


    // TEST SETUP

    @Before
    public void setUp() {

        context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();

        testDirectory = new File(
                context.getCacheDir(),
                "solitaire_test_" + UUID.randomUUID()
        );

        assertTrue(testDirectory.mkdirs());

        testSaveFile = new File(
                testDirectory,
                "solitaire_save.json"
        );

        saveManager = new SaveManager(testDirectory);

        state = new GameState(Deck.createDeck());

        engine = new GameEngine(state);
    }


    // TEST CLEANUP

    @After
    public void tearDown() {

        if (testSaveFile != null) {

            new AtomicFile(testSaveFile).delete();
        }

        if (testDirectory != null
                && testDirectory.exists()) {

            assertTrue(testDirectory.delete());
        }
    }

    // BASIC STORAGE TESTS

    @Test
    public void instrumentationContextIsAvailable() {

        assertNotNull(context);
        assertNotNull(context.getFilesDir());

        assertTrue(context.getFilesDir().exists());
        assertTrue(context.getFilesDir().isDirectory());
        assertTrue(context.getFilesDir().canWrite());
    }

    @Test
    public void loadReturnsNullWhenNoSaveExists() {

        assertFalse(saveManager.hasSavedGame());
        assertNull(saveManager.loadGame());
    }

    @Test
    public void savingGameCreatesSaveFile() {

        assertFalse(testSaveFile.exists());

        assertTrue(saveManager.saveGame(engine));

        assertTrue(testSaveFile.exists());
        assertTrue(testSaveFile.isFile());
        assertTrue(testSaveFile.length() > 0);
    }

    @Test
    public void hasSavedGameReturnsTrueAfterSaving() {

        assertFalse(saveManager.hasSavedGame());

        assertTrue(saveManager.saveGame(engine));

        assertTrue(saveManager.hasSavedGame());
    }

    @Test
    public void loadingSavedGameReturnsGameEngine() {

        assertTrue(saveManager.saveGame(engine));

        GameEngine loaded = saveManager.loadGame();

        assertNotNull(loaded);
        assertNotSame(engine, loaded);
        assertNotSame(state, loaded.getState());
    }

    // SAVE AND LOAD ROUND TRIP TESTS

    @Test
    public void saveAndLoadPreservesActiveGame() {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        assertTrue(saveManager.saveGame(engine));

        GameEngine loaded = saveManager.loadGame();

        assertNotNull(loaded);

        GameState loadedState = loaded.getState();

        assertCardListsEqual(
                state.getOriginalDeck(),
                loadedState.getOriginalDeck()
        );

        assertSnapshotsEqual(
                new GameSnapshot(state),
                new GameSnapshot(loadedState)
        );

        assertEquals(
                state.getMoves(),
                loadedState.getMoves()
        );

        assertEquals(
                state.getRemainingUndos(),
                loadedState.getRemainingUndos()
        );
    }

    @Test
    public void saveAndLoadPreservesUndoHistory() {

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        assertTrue(engine.undo());

        assertTrue(saveManager.saveGame(engine));

        GameEngine loaded = saveManager.loadGame();

        assertNotNull(loaded);

        assertEquals(
                state.getRemainingUndos(),
                loaded.getState().getRemainingUndos()
        );

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

        assertTrue(loaded.undo());

        assertEquals(
                1,
                loaded.getState().getRemainingUndos()
        );

        assertEquals(
                1,
                loaded.getState().getDiscard().size()
        );
    }

    // SAVE REPLACEMENT AND PERSISTENCE TESTS

    @Test
    public void savingAgainReplacesPreviousSave() {

        assertTrue(engine.drawCard());

        assertTrue(saveManager.saveGame(engine));

        assertTrue(engine.drawCard());

        state.setScore(75);

        assertTrue(saveManager.saveGame(engine));

        GameEngine loaded = saveManager.loadGame();

        assertNotNull(loaded);

        assertEquals(
                2,
                loaded.getState().getDiscard().size()
        );

        assertEquals(
                22,
                loaded.getState().getStock().size()
        );

        assertEquals(75, loaded.getState().getScore());
    }

    @Test
    public void newSaveManagerInstanceCanLoadExistingSave() {

        assertTrue(engine.drawCard());

        assertTrue(saveManager.saveGame(engine));

        SaveManager newManager =
                new SaveManager(testDirectory);

        assertNotSame(saveManager, newManager);
        assertTrue(newManager.hasSavedGame());

        GameEngine loaded = newManager.loadGame();

        assertNotNull(loaded);

        assertSnapshotsEqual(
                new GameSnapshot(state),
                new GameSnapshot(loaded.getState())
        );
    }

    @Test
    public void loadedGameCanRestartOriginalDeal() {

        GameSnapshot originalDeal =
                new GameSnapshot(state);

        assertTrue(engine.drawCard());
        assertTrue(engine.drawCard());

        assertTrue(
                engine.moveTableauToFoundation(
                        0,
                        Card.Suit.CLUBS
                )
        );

        assertTrue(saveManager.saveGame(engine));

        GameEngine loaded = saveManager.loadGame();

        assertNotNull(loaded);

        loaded.restart();

        assertSnapshotsEqual(
                originalDeal,
                new GameSnapshot(loaded.getState())
        );

        assertEquals(0, loaded.getState().getMoves());

        assertEquals(
                GameState.MAX_UNDOS,
                loaded.getState().getRemainingUndos()
        );

        assertEquals(0, loaded.getUndoHistorySize());
    }

    // SAVE AND LOAD ERROR HANDLING TESTS

    @Test
    public void loadingCorruptedSaveReturnsNull()
            throws IOException {

        writeCorruptedSave();

        assertTrue(saveManager.hasSavedGame());

        assertNull(saveManager.loadGame());

        assertTrue(saveManager.hasSavedGame());
    }

    @Test
    public void validSaveCanReplaceCorruptedSave()
            throws IOException {

        writeCorruptedSave();

        assertNull(saveManager.loadGame());

        assertTrue(saveManager.saveGame(engine));

        GameEngine loaded = saveManager.loadGame();

        assertNotNull(loaded);

        assertSnapshotsEqual(
                new GameSnapshot(state),
                new GameSnapshot(loaded.getState())
        );
    }

    // HELPER METHODS

    // HELPER METHODS

    private void writeCorruptedSave()
            throws IOException {

        try (FileOutputStream output =
                     new FileOutputStream(testSaveFile)) {

            output.write(
                    "Invalid JSON save data"
                            .getBytes(StandardCharsets.UTF_8)
            );
        }
    }

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