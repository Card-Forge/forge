package forge.gamemodes.tutorial;

import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.puzzle.Puzzle;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeConstants;
import forge.player.GamePlayerUtil;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.IHasForgeLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the "How to Play" tutorial, called by the desktop and mobile home screens.
 *
 * <p>The tutorial is one fixed practice game (res/tutorial/howtoplay/HowToPlay.pzl, a normal Forge puzzle state), so
 * nothing in it is random. A {@link TutorialDirector} plays it in one of two modes: {@code WATCH}, where the director
 * makes the moves for the player with explanations, and {@code TRY}, where the same game starts again and the player
 * makes the moves, guided by the same explanations.
 */
public final class HowToPlay implements IHasForgeLog {
    public enum Mode { WATCH, TRY }

    private static final String PUZZLE_FILE = ForgeConstants.TUTORIAL_DIR + "howtoplay/HowToPlay.pzl";

    /** The mode the next (re)start of the practice game uses; the end screen's restart button restarts with it. */
    private static volatile Mode nextMode = Mode.WATCH;
    /** The mode the game that is on screen now was started in. */
    private static volatile Mode playedMode = Mode.WATCH;
    private static volatile WeakReference<GameView> currentGame = new WeakReference<>(null);

    private HowToPlay() {
    }

    /** True if {@code view} is the practice game, so the end screen can offer the right buttons. */
    public static boolean isTutorialGame(final GameView view) {
        return view != null && currentGame.get() == view;
    }

    /** What the end screen's restart button says: after the demo it leads to the player's own attempt. */
    public static String restartLabel() {
        if (nextMode == Mode.WATCH) {
            return "Watch the demo again";
        }
        return playedMode == Mode.WATCH ? "Try it yourself" : "Play again";
    }

    /** What the end screen's quit button says; the tutorial should not read like a puzzle. */
    public static String quitLabel() {
        return "Back to the menu";
    }

    /** Called when the demo has been watched to the end: restarting now starts the player's own attempt. */
    static void demoFinished() {
        nextMode = Mode.TRY;
    }

    /** Asks whether to watch the demo or play it, then starts the practice game. Safe to call from the UI thread. */
    public static void chooseAndStart() {
        FThreads.invokeInBackgroundThread(() -> {
            if (GuiBase.getInterface().isMatchInProgress()) {
                SOptionPane.showMessageDialog("Finish or leave your current match before starting the tutorial.",
                        "How to Play", SOptionPane.INFORMATION_ICON);
                return;
            }
            final List<String> options = new ArrayList<>();
            options.add("Watch the demo");
            options.add("Try it yourself");
            options.add("Cancel");
            final int choice = SOptionPane.showOptionDialog(
                    "Learn the basics of Magic in a short practice game: play a land, cast a spell and pay for it, "
                            + "attack, block, and win.\n\n"
                            + "Watch the demo first and see every move explained, then choose \"Try it yourself\" "
                            + "to play the same game with hints.",
                    "How to Play", SOptionPane.QUESTION_ICON, options, 0);
            if (choice == 0) {
                start(Mode.WATCH);
            } else if (choice == 1) {
                start(Mode.TRY);
            }
        });
    }

    /** Starts the practice game in the given mode. Safe to call from any thread. */
    public static void start(final Mode mode) {
        nextMode = mode;
        // the match screen is built on the UI thread, like any other match
        FThreads.invokeInEdtNowOrLater(() -> {
            try {
                startMatch();
            } catch (final RuntimeException e) {
                netLog.error(e, "Could not start the tutorial");
                FThreads.invokeInBackgroundThread(() ->
                        SOptionPane.showErrorDialog("The tutorial could not be started.\n" + e.getMessage(),
                                "How to Play"));
            }
        });
    }

    private static Puzzle loadPuzzle() {
        final List<String> lines = FileUtil.readFile(PUZZLE_FILE);
        if (lines == null || lines.isEmpty()) {
            throw new IllegalStateException("Tutorial file not found: " + PUZZLE_FILE);
        }
        return new Puzzle(FileSection.parseSections(lines), "HowToPlay", false);
    }

    private static void startMatch() {
        final Puzzle puzzle = loadPuzzle();
        final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
        // The start hook runs again whenever the match is restarted, so each (re)start gets a fresh director,
        // in whatever mode is next: restarting after the demo starts the player's own attempt.
        final TutorialDirector[] director = new TutorialDirector[1];

        hostedMatch.setStartGameHook(() -> {
            if (director[0] != null) {
                director[0].stop();
            }
            puzzle.applyToGame(hostedMatch.getGame());
            playedMode = nextMode;
            currentGame = new WeakReference<>(hostedMatch.getGame().getView());
            director[0] = TutorialDirector.launch(hostedMatch, playedMode);
        });
        hostedMatch.setEndGameHook(() -> {
            if (director[0] != null) {
                director[0].stop();
            }
        });

        final List<RegisteredPlayer> players = new ArrayList<>();
        final RegisteredPlayer human = new RegisteredPlayer(new Deck()).setPlayer(GamePlayerUtil.getGuiPlayer());
        human.setStartingHand(0);
        players.add(human);

        final RegisteredPlayer ai = new RegisteredPlayer(new Deck()).setPlayer(GamePlayerUtil.createAiPlayer());
        ai.setStartingHand(0);
        players.add(ai);

        final GameRules rules = new GameRules(GameType.Puzzle);
        rules.setGamesPerMatch(1);
        hostedMatch.startMatch(rules, null, players, human, GuiBase.getInterface().getNewGuiGame());
    }
}
