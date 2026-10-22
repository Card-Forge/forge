package forge.gamemodes.tutorial;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.Selection;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputAttack;
import forge.gamemodes.match.input.InputBlock;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.match.input.InputPayMana;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.util.SOptionPane;
import forge.player.PlayerControllerHuman;
import forge.util.IHasForgeLog;
import forge.util.ITriggerEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Plays the "How to Play" practice game (see {@link HowToPlay}) one lesson at a time.
 *
 * <p>The game is fixed (res/tutorial/howtoplay/HowToPlay.pzl): a Mountain and Raging Goblin to cast, a Hill Giant to
 * block with, and an opponent whose Goblin Brigand has to attack each combat, so every lesson happens the same way
 * every time. After a short tour of the board the lessons are: play a land, cast a spell and pay its mana, attack,
 * pass the turn, block, then attack until the opponent is out of life.
 *
 * <p>Each lesson is explained in a dialog while the card to use is spotlighted: the game's own selection marking is
 * pointed at it, which makes the card glow and dims everything else. In {@code WATCH} mode the director then makes
 * the move itself, slowly, through the same entry points a mouse click uses (as Forge's macro replay does), and says
 * what changed. In {@code TRY} mode it keeps the spotlight on and waits for the player, repeating the hint if nothing
 * happens for a while.
 *
 * <p>One daemon thread runs the script and polls the game state; it never touches the game from the game thread and
 * only clicks on the UI thread. Anything unexpected ends the tutorial quietly rather than leaving a stuck game.
 */
final class TutorialDirector implements Runnable, IHasForgeLog {
    private static final String LAND = "Mountain";
    private static final String GOBLIN = "Raging Goblin";
    private static final String GIANT = "Hill Giant";
    private static final String BRIGAND = "Goblin Brigand";

    private static final long NUDGE_MS = 40_000;
    private static final long POLL_MS = 120;
    /** How long the spotlight is shown before the demo clicks, so the player can see what is about to be used. */
    private static final long BEAT_MS = 2000;
    /** How long the result stays on screen after a demo move, before the next explanation. */
    private static final long AFTER_MS = 1500;
    /** Pause between presses of OK while the demo passes priority. */
    private static final long PASS_MS = 1500;
    private static final long TRY_MAX_WAIT_MS = 600_000;
    private static final int STARTING_LIFE = 20;

    private static final Supplier<List<Card>> NOTHING = Collections::emptyList;

    /** What a left mouse click looks like to the input handlers; same stand-in the macro replay uses. */
    private static final ITriggerEvent LEFT_CLICK = new ITriggerEvent() {
        @Override
        public int getButton() {
            return 1;
        }

        @Override
        public int getX() {
            return 0;
        }

        @Override
        public int getY() {
            return 0;
        }
    };

    /** A scripted move in WATCH mode. */
    private interface Demo {
        void run() throws InterruptedException;
    }

    private final HostedMatch hosted;
    private final Game game;
    private final PlayerControllerHuman human;
    private final Player me;
    private final Player opp;
    private final boolean watch;
    private final int oppStartLife;
    /** Phases already explained, as "turn:phase"; only touched by the director thread. */
    private final Set<String> explained = new HashSet<>();
    private volatile boolean stopped;
    private boolean spotlit;

    private TutorialDirector(final HostedMatch hosted0, final PlayerControllerHuman human0,
                             final HowToPlay.Mode mode) {
        hosted = hosted0;
        game = hosted0.getGame();
        human = human0;
        me = human0.getPlayer();
        Player other = null;
        for (final Player p : game.getPlayers()) {
            if (p != me) {
                other = p;
            }
        }
        opp = other;
        watch = mode == HowToPlay.Mode.WATCH;
        oppStartLife = opp.getLife();
    }

    /** Starts directing the game that {@code hosted} has just set up; returns null if there is nobody to teach. */
    static TutorialDirector launch(final HostedMatch hosted, final HowToPlay.Mode mode) {
        final List<PlayerControllerHuman> humans = hosted.getHumanControllers();
        if (humans.isEmpty() || hosted.getGame() == null) {
            return null;
        }
        final TutorialDirector director = new TutorialDirector(hosted, humans.get(0), mode);
        // In the demo the director makes every move, so the player's own taps would only knock it out of step.
        director.human.setTutorialInputLocked(director.watch);
        final Thread t = new Thread(director, "Forge-HowToPlay");
        t.setDaemon(true);
        t.start();
        return director;
    }

    void stop() {
        stopped = true;
        human.setTutorialInputLocked(false);
    }

    @Override
    public void run() {
        try {
            script();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final RuntimeException e) {
            netLog.error(e, "The tutorial stopped");
        } finally {
            clearSpotlight();
            GuiBase.getInterface().hideCoach();
            human.setTutorialInputLocked(false);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The script
    // ---------------------------------------------------------------------------------------------------------

    private void script() throws InterruptedException {
        if (!waitUntil(this::inMain1, 40_000)) {
            return;
        }

        tour();
        if (!alive()) {
            return;
        }
        if (!landLesson() || !castLesson()) {
            return;
        }
        attackLesson();
        if (!alive()) {
            return;
        }
        passLesson();
        if (!alive()) {
            return;
        }
        blockLesson();
        if (!alive()) {
            return;
        }
        winLesson();
    }

    /** A short look around the board: the glowing cards are what each page is about. */
    private void tour() throws InterruptedException {
        if (!watch && !SOptionPane.showConfirmDialog("You saw the tour in the demo. Show it again?",
                "Board tour", "Show the tour", "Skip it", false)) {
            return;
        }

        say("Welcome to Magic (1 of 6)",
                "Two players take turns. You have " + STARTING_LIFE + " life; your opponent, at the top, has "
                        + oppStartLife + " in this practice game. Reduce your opponent's life to 0 to win. If yours "
                        + "reaches 0 first, you lose.\n\n"
                        + "Creatures are the main way to deal damage. This tour shows where everything is. "
                        + "Whenever cards glow, those are the ones being talked about.");

        spotlightAndSay(zone(me, ZoneType.Hand), "Your hand (2 of 6)",
                "The glowing cards are your HAND: cards you've drawn but not played yet. Only you can see them. "
                        + "Everything else is dimmed while something is being pointed out.\n\n"
                        + "Your hand holds three Mountains (lands) and a Raging Goblin (a creature).");

        final List<Card> battlefield = new ArrayList<>(zone(me, ZoneType.Battlefield));
        battlefield.addAll(zone(opp, ZoneType.Battlefield));
        spotlightAndSay(battlefield, "The battlefield (3 of 6)",
                "The glowing cards are on the BATTLEFIELD: cards that are in play. Lands and creatures live here.\n\n"
                        + "You control the " + GIANT + " (3/3). Your opponent controls the " + BRIGAND + " (2/2). The "
                        + "two numbers on a creature are POWER (the damage it deals) and TOUGHNESS (the damage it "
                        + "takes to destroy it).");

        say("The other zones (4 of 6)",
                "A zone is a place cards can be.\n\n"
                        + "- LIBRARY: your deck, face down. You draw from the top. Drawing from an empty library "
                        + "loses the game.\n"
                        + "- GRAVEYARD: where spells you've used and creatures that died go, face up.\n"
                        + "- EXILE: cards removed from the game.\n"
                        + "- STACK: spells waiting to resolve (more on this soon).\n\n"
                        + "Forge shows zones as tabs with a name and a card count. Click one to look inside.");

        say("The turn, phase by phase (5 of 6)",
                "A turn is split into phases. The small boxes on the screen light up the current one:\n\n"
                        + "UP upkeep, DR draw, M1 main phase 1, BC beginning of combat, DA declare attackers, "
                        + "DB declare blockers, FS/CD combat damage, EC end of combat, M2 main phase 2, ET end "
                        + "step, CL cleanup.\n\n"
                        + "In short: untap, draw, play lands and cast spells, fight, cast more spells, end. I'll "
                        + "explain each phase when we reach it.");

        say("The prompt and OK button (6 of 6)",
                "The prompt message (by default on the left) always says what the game wants from you.\n\n"
                        + "OK means \"I'm done, continue.\" It is also how you pass priority: when you have nothing "
                        + "more to do in a phase, press OK and the game moves on. Cancel backs out of a choice "
                        + "you're in the middle of.\n\n"
                        + (watch
                        ? "Now the demo starts. Each note is shown first, and nothing moves until you press Next, "
                        + "so take your time. Then the demo points at the card, clicks it for you, and says what "
                        + "changed.\n\nWhile the demo plays, your own taps on the cards and the OK / End Turn "
                        + "buttons are paused, so it can't get out of step. Use Next, and Hide if a note covers "
                        + "something you want to see."
                        : "Now it's your turn to play. Each step glows the card to click and tells you what to do."));
    }

    private boolean landLesson() throws InterruptedException {
        final boolean landed = step("Step 1 of 6: Play a land",
                "It's your MAIN PHASE 1 (M1). This is when you play a land and cast spells.\n\n"
                        + "Lands produce mana, the resource you spend on spells. You may play ONE land per turn, "
                        + "and only in a main phase.",
                "Click the glowing Mountain in your hand to play it.",
                "Watch: the Mountain in your hand will be clicked to play it.",
                () -> cards(find(me, ZoneType.Hand, LAND)),
                () -> click(find(me, ZoneType.Hand, LAND)),
                this::hasLand,
                this::leftMain1,
                10_000);
        if (stopped) {
            return false;
        }
        if (!landed) {
            restartPractice("You moved past your main phase without playing a land, so there's no mana for "
                    + "the next step.");
            return false;
        }
        spotlightAndSay(cards(untappedLand()), "Your Mountain is in play",
                "The Mountain moved from your HAND to the BATTLEFIELD.\n\n"
                        + "Lands untap at the start of each of your turns. TAPPING a land (turning it sideways) "
                        + "gives one mana; a Mountain gives red mana. You've used your land drop for this turn.");
        return alive();
    }

    private boolean castLesson() throws InterruptedException {
        if (!castAndPay()) {
            if (!stopped) {
                restartPractice("The spell wasn't cast this turn.");
            }
            return false;
        }

        if (waitUntil(() -> !game.getStack().isEmpty() && myPriority(), 5_000)) {
            step("The stack",
                    "Your Raging Goblin isn't in play yet. Spells wait on the STACK first (see the Stack tab), so "
                            + "your opponent gets a chance to respond. When both players pass in a row, the top "
                            + "spell resolves.\n\n"
                            + "You have priority now, which means you may act again. You have nothing else to do, "
                            + "so pass.",
                    "Press OK to pass priority and let your spell resolve.",
                    "Watch: OK will be pressed to let the spell resolve.",
                    NOTHING,
                    this::ok,
                    () -> has(me, ZoneType.Battlefield, GOBLIN),
                    () -> false,
                    10_000);
            if (stopped) {
                return false;
            }
        }
        spotlightAndSay(cards(find(me, ZoneType.Battlefield, GOBLIN)), "Raging Goblin resolved",
                "It left the stack and entered the BATTLEFIELD. It's a 1/1: power 1, toughness 1.\n\n"
                        + "A creature that just arrived normally can't attack until your next turn (\"summoning "
                        + "sickness\"). Raging Goblin has HASTE, which skips that wait, so it can attack right "
                        + "now.");
        return alive();
    }

    private void attackLesson() throws InterruptedException {
        passStep("Step 3 of 6: Attack!",
                "Combat is how you hurt your opponent. It has several phases: BEGINNING OF COMBAT (BC), DECLARE "
                        + "ATTACKERS (DA), DECLARE BLOCKERS (DB), COMBAT DAMAGE (CD) and END OF COMBAT (EC).\n\n"
                        + "First leave your main phase and head into combat.",
                "Press OK to move to combat.",
                "Watch: OK will be pressed to move to combat.",
                () -> attackPrompt() || !myTurn(),
                20_000);
        if (!alive() || !attackPrompt()) {
            return;
        }

        step("Declare attackers",
                "This is the DECLARE ATTACKERS phase (DA): you choose which creatures attack. Click a creature to "
                        + "mark it, then press OK.\n\n"
                        + "An attacking creature TAPS (turns sideways) and can't block until your next turn. That's "
                        + "why Hill Giant stays home: it will defend you later.",
                "Click the glowing Raging Goblin, then press OK.",
                "Watch: the Raging Goblin will be marked as an attacker, then OK pressed.",
                () -> cards(find(me, ZoneType.Battlefield, GOBLIN)),
                () -> {
                    click(find(me, ZoneType.Battlefield, GOBLIN));
                    Thread.sleep(1800);
                    ok();
                },
                () -> !attackPrompt(),
                () -> false,
                10_000);
        if (stopped) {
            return;
        }

        waitUntil(() -> opp.getLife() < oppStartLife || !myTurn() || phase() == PhaseType.MAIN2, 15_000);
        final int dealt = oppStartLife - opp.getLife();
        spotlightAndSay(cards(find(me, ZoneType.Battlefield, GOBLIN)), "Combat damage",
                dealt > 0
                        ? "Your opponent had no creature that could block, so Raging Goblin was unblocked and dealt "
                        + dealt + " damage. Your opponent is now at " + opp.getLife() + " life.\n\nThe Goblin is "
                        + "tapped (sideways) until your next untap step."
                        : "Combat is over.");
    }

    private void passLesson() throws InterruptedException {
        passStep("Step 4 of 6: End your turn",
                "After combat you get MAIN PHASE 2 (M2), a second chance to play a land or cast spells; then the "
                        + "END STEP (ET) and CLEANUP (CL). You've nothing left to do, so pass priority each time "
                        + "the game gives it to you until your opponent's turn begins.",
                "Press OK each time the game waits for you, until it's your opponent's turn.",
                "Watch: OK will be pressed through the rest of your turn.",
                () -> !myTurn(),
                25_000);
        if (!alive()) {
            return;
        }
        say("Your opponent's turn",
                "Now your opponent takes a turn. They untap, upkeep, draw a card, and play a land. Then their "
                        + BRIGAND + " (2/2) attacks, because it has to attack each combat.\n\n"
                        + "Watch the phase boxes; they light up as the turn goes by. When it's time to defend, "
                        + "the game will ask you to declare blockers.");
    }

    private void blockLesson() throws InterruptedException {
        final String waitHint = "Your opponent's turn is in progress. Press OK whenever the game asks for it.";
        advanceTo(() -> blockPrompt() || myTurn() && phase() == PhaseType.MAIN1, 120_000, waitHint);
        if (!alive()) {
            return;
        }
        if (!blockPrompt()) {
            say("No attack this time", "Your opponent didn't attack, so there was nothing to block. When "
                    + "they do, you choose which of your untapped creatures blocks which attacker.");
            return;
        }

        step("Step 5 of 6: Block",
                "The " + BRIGAND + " (2/2) is attacking you. This is the DECLARE BLOCKERS phase (DB), and now YOU "
                        + "choose.\n\n"
                        + "BLOCKING means putting one of your untapped creatures in the attacker's way. The two "
                        + "fight each other and you take no damage. Hill Giant is 3/3: it takes 2 damage and "
                        + "survives (toughness 3), while the Brigand takes 3 and dies (toughness 2).",
                "Click the glowing Hill Giant to block the Brigand, then press OK.",
                "Watch: the Hill Giant will be clicked to block, then OK pressed.",
                () -> cards(find(me, ZoneType.Battlefield, GIANT)),
                () -> {
                    click(find(me, ZoneType.Battlefield, GIANT));
                    Thread.sleep(1800);
                    ok();
                },
                () -> !blockPrompt(),
                () -> false,
                10_000);
        if (stopped) {
            return;
        }
        waitUntil(() -> !has(opp, ZoneType.Battlefield, BRIGAND) || me.getLife() < STARTING_LIFE, 12_000);
        if (!has(opp, ZoneType.Battlefield, BRIGAND)) {
            spotlightAndSay(cards(find(me, ZoneType.Battlefield, GIANT)), "Blocked!",
                    "In COMBAT DAMAGE, Hill Giant and Goblin Brigand hit each other at the same time. The Brigand "
                            + "took 3 damage and was destroyed (it went to the GRAVEYARD). Hill Giant took 2 but has "
                            + "3 toughness, so it lived. Damage wears off at the end of the turn.");
        } else {
            say("Not blocked", "You took " + (STARTING_LIFE - me.getLife()) + " damage. Blocking is optional, "
                    + "but a good block like this one saves life and can destroy the attacker.");
        }
    }

    private void winLesson() throws InterruptedException {
        final String waitHint = "Your opponent's turn is in progress. Press OK whenever the game asks for it.";
        advanceTo(this::inMain1, 120_000, waitHint);
        for (int round = 0; round < 3 && alive(); round++) {
            final int oppLife = opp.getLife();
            passStep("Step 6 of 6: Win the game",
                    "It's your turn again: you untapped and drew a card. Your opponent is at " + oppLife
                            + " life. Attack with every creature you can; the game is won when a player's life "
                            + "reaches 0.\n\nFirst move to combat.",
                    "Press OK to move to combat.",
                    "Watch: OK will be pressed to move to combat.",
                    () -> attackPrompt() || !myTurn(),
                    20_000);
            if (!alive()) {
                return;
            }
            if (attackPrompt()) {
                step("Attack with everything",
                        "Click every creature you want to attack with, then press OK. Your opponent has no "
                                + "untapped creatures to block with, so all the damage goes through.",
                        "Click the glowing creatures (Raging Goblin and Hill Giant), then press OK.",
                        "Watch: both creatures will be marked as attackers, then OK pressed.",
                        this::readyAttackers,
                        () -> {
                            for (final Card c : readyAttackers()) {
                                click(c);
                                Thread.sleep(1500);
                            }
                            ok();
                        },
                        () -> !attackPrompt(),
                        () -> false,
                        15_000);
                if (stopped) {
                    return;
                }
            }
            waitUntil(() -> game.isGameOver() || opp.getLife() < oppLife, 15_000);
            if (game.isGameOver() || opp.getLife() <= 0) {
                break;
            }
            say("Not quite", "Your opponent survived at " + opp.getLife() + " life. Pass the turn and attack again.");
            advanceTo(this::inMain1, 120_000, waitHint);
        }

        // The end-of-game hook may already have called stop(), so this is shown whether or not we were stopped.
        if (opp.getLife() <= 0) {
            if (watch) {
                HowToPlay.demoFinished();
            }
            GuiBase.getInterface().hideCoach();
            SOptionPane.showMessageDialog(watch
                            ? "That's a whole game: play a land, cast a spell and pay for it, attack, block, "
                            + "and win.\n\nOn the next screen choose \"Try it yourself\" to play the same game "
                            + "with the cards glowing for you."
                            : "You played a land, cast a spell and paid for it, attacked, blocked, and won.\n\n"
                            + "From here, try a real match from the home screen. You can always come back to "
                            + "How to Play.",
                    "You win!", SOptionPane.INFORMATION_ICON);
        }
    }

    /** Casts the Goblin and pays for it; a cancelled payment sends the spell back to hand, so ask again. */
    private boolean castAndPay() throws InterruptedException {
        for (int attempt = 0; attempt < 3 && alive(); attempt++) {
            final boolean cast = step("Step 2 of 6: Cast a spell",
                    "Spells cost mana, shown in the top corner of the card. Raging Goblin costs {R}: one red "
                            + "mana, which the Mountain you just played makes.\n\n"
                            + "Creature spells are cast the same way lands are played: by clicking them in your hand.",
                    "Click the glowing Raging Goblin in your hand.",
                    "Watch: the Raging Goblin in your hand will be clicked to cast it.",
                    () -> cards(find(me, ZoneType.Hand, GOBLIN)),
                    () -> click(find(me, ZoneType.Hand, GOBLIN)),
                    () -> !has(me, ZoneType.Hand, GOBLIN),
                    this::leftMain1,
                    10_000);
            if (!cast) {
                return false;
            }
            if (!waitUntil(this::payPrompt, 4_000)) {
                return true; // paid for automatically
            }
            final boolean paid = step("Pay the mana",
                    "Casting asks you to pay the cost. You pay by TAPPING lands for mana. Tapping the Mountain "
                            + "gives {R}, which pays the whole cost and sends the spell onto the stack.",
                    "Click the glowing Mountain on your battlefield to tap it for red mana.",
                    "Watch: the Mountain will be clicked to tap it for red mana.",
                    () -> cards(untappedLand()),
                    () -> click(untappedLand()),
                    () -> !payPrompt(),
                    () -> has(me, ZoneType.Hand, GOBLIN),
                    10_000);
            if (stopped) {
                return false;
            }
            if (paid) {
                return true;
            }
            say("No problem", "Cancelling puts the spell back in your hand. Let's cast it again.");
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Step machinery
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Explains a lesson with the card to use glowing, then either performs it (WATCH) or waits for the player to
     * (TRY).
     *
     * @return true when {@code done} came true; false when {@code missed} did first, or when the tutorial ended
     */
    private boolean step(final String title, final String body, final String tryLine, final String watchLine,
                         final Supplier<List<Card>> spot, final Demo demo, final BooleanSupplier done,
                         final BooleanSupplier missed, final long watchTimeoutMs) throws InterruptedException {
        final BooleanSupplier over = () -> safe(done) || safe(missed);
        spotlight(spot); // glowing while the note is up, so the player sees what it is about
        if (watch) {
            say(title, body + "\n\n" + watchLine);
            if (!alive()) {
                clearSpotlight();
                return false;
            }
            spotlight(spot);
            Thread.sleep(BEAT_MS);
            demo.run();
            final boolean ok = waitUntil(done, watchTimeoutMs);
            clearSpotlight();
            if (ok) {
                Thread.sleep(AFTER_MS);
                return true;
            }
            if (alive()) {
                desync();
            }
            return false;
        }
        // The player does this one: the note stays up until they do it or press "Got it", and goes away by itself
        // once the move is made.
        say(title, body + "\n\n" + tryLine, "Got it", over);
        long idle = 0;
        while (alive()) {
            spotlight(spot); // set again now and then: the game redraws its own marks as the prompt changes
            if (waitUntil(over, 2_500)) {
                clearSpotlight();
                return safe(done);
            }
            idle += 2_500;
            if (idle >= NUDGE_MS) {
                idle = 0;
                say("Need a hand?", tryLine, "Got it", over);
            }
        }
        clearSpotlight();
        return false;
    }

    /**
     * A lesson that is just passing priority: explains it, then presses OK for the player (WATCH) or waits and
     * reminds them (TRY). Each new phase on the way is explained.
     */
    private boolean passStep(final String title, final String body, final String tryLine, final String watchLine,
                             final BooleanSupplier done, final long watchTimeoutMs) throws InterruptedException {
        if (watch) {
            say(title, body + "\n\n" + watchLine);
        } else {
            say(title, body + "\n\n" + tryLine, "Got it", done);
        }
        if (!alive()) {
            return false;
        }
        if (watch) {
            Thread.sleep(AFTER_MS);
        }
        final boolean ok = advanceTo(done, watch ? watchTimeoutMs : TRY_MAX_WAIT_MS, tryLine);
        if (!ok && watch && alive()) {
            desync();
        }
        return ok;
    }

    /**
     * Gets the game to {@code target}: in WATCH mode by pressing OK whenever the game waits for it, in TRY mode by
     * reminding the player to. Each phase the player gets priority in is explained once. Returns whether the target
     * was reached in time.
     */
    private boolean advanceTo(final BooleanSupplier target, final long timeoutMs, final String hint)
            throws InterruptedException {
        final long end = System.currentTimeMillis() + timeoutMs;
        long lastNudge = System.currentTimeMillis();
        while (true) {
            if (safe(target)) {
                return true;
            }
            if (!alive() || System.currentTimeMillis() > end) {
                return false;
            }
            if (safe(this::myPriority)) {
                explainPhase();
                if (watch) {
                    ok();
                    Thread.sleep(PASS_MS);
                    continue;
                }
            }
            if (!watch && !hint.isEmpty() && System.currentTimeMillis() - lastNudge > NUDGE_MS
                    && safe(this::myPriority)) {
                lastNudge = System.currentTimeMillis();
                say("Keep going", hint, "Got it", target);
            }
            Thread.sleep(POLL_MS);
        }
    }

    /** Polls until {@code cond} holds; false on timeout, or when the game or tutorial ended first. */
    private boolean waitUntil(final BooleanSupplier cond, final long timeoutMs) throws InterruptedException {
        final long end = System.currentTimeMillis() + timeoutMs;
        while (true) {
            if (safe(cond)) {
                return true;
            }
            if (!alive() || System.currentTimeMillis() > end) {
                return false;
            }
            Thread.sleep(POLL_MS);
        }
    }

    private void say(final String title, final String text) {
        say(title, text, "Next", null);
    }

    /**
     * Shows a note and waits for the player to press its button, or until {@code until} holds (if given), or the game
     * or tutorial ends. Where the GUI has a coach panel the note floats over the top of the board without dimming
     * anything; otherwise it is a dialog.
     */
    private void say(final String title, final String text, final String label, final BooleanSupplier until) {
        if (stopped) {
            return;
        }
        final CountDownLatch pressed = new CountDownLatch(1);
        if (!GuiBase.getInterface().showCoach(title, text, label, pressed::countDown)) {
            SOptionPane.showMessageDialog(text, title, SOptionPane.INFORMATION_ICON);
            return;
        }
        try {
            while (!stopped && !game.isGameOver() && !(until != null && safe(until))) {
                if (pressed.await(150, TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            GuiBase.getInterface().hideCoach();
        }
    }

    private void spotlightAndSay(final List<Card> cards, final String title, final String text) {
        spotlightNow(cards);
        say(title, text);
        clearSpotlight();
    }

    private void desync() {
        say("The demo got out of sync", "Something unexpected happened, so the demo stops here. You can still "
                + "choose \"Try it yourself\" from How to Play.");
        stopped = true;
    }

    private void restartPractice(final String why) {
        say("Let's try that again", why + "\n\nThe practice game starts over from the beginning.");
        if (!stopped) {
            stopped = true;
            FThreads.invokeInEdtLater(hosted::restartMatch);
        }
    }

    /** Explains the phase the player has priority in, once per turn, for the phases no lesson covers itself. */
    private void explainPhase() {
        final PhaseType p;
        final boolean mine;
        try {
            p = phase();
            mine = myTurn();
        } catch (final RuntimeException e) {
            return;
        }
        final String note = phaseNote(p, mine);
        if (note != null && explained.add(game.getPhaseHandler().getTurn() + ":" + p)) {
            final String heading = p.nameForUi + (mine ? "" : " (opponent's turn)");
            if (watch) {
                say(heading, note);
            } else {
                say(heading, note, "Got it", () -> !myPriority()); // pressing OK in the game also dismisses it
            }
        }
    }

    private static String phaseNote(final PhaseType phase, final boolean mine) {
        switch (phase) {
            case UPKEEP:
                return "UPKEEP (UP): the first stop of the turn. Some cards say \"at the beginning of your upkeep\"; "
                        + "none matter here.";
            case DRAW:
                return "DRAW (DR): " + (mine ? "you draw" : "your opponent draws")
                        + " a card from the top of the library.";
            case COMBAT_BEGIN:
                return "BEGINNING OF COMBAT (BC): the last moment to act before attackers are chosen.";
            case COMBAT_DAMAGE:
                return "COMBAT DAMAGE (CD): attackers and blockers deal their damage at the same time.";
            case COMBAT_END:
                return "END OF COMBAT (EC): combat is over. Creatures that attacked stay tapped.";
            case MAIN2:
                return "MAIN PHASE 2 (M2): a second chance to play a land (if you haven't) or cast spells. Players "
                        + "often cast creatures here so the opponent has seen less before blocking.";
            case END_OF_TURN:
                return "END STEP (ET): \"at the beginning of the end step\" effects happen here. Next is CLEANUP "
                        + "(CL): damage on creatures wears off, and a player with more than seven cards in hand "
                        + "discards down to seven.";
            default:
                return null;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The spotlight: the game's own selection marking, pointed at the card to use
    // ---------------------------------------------------------------------------------------------------------

    private void spotlight(final Supplier<List<Card>> spot) {
        List<Card> cards;
        try {
            cards = spot.get();
        } catch (final RuntimeException e) {
            cards = Collections.emptyList();
        }
        spotlightNow(cards);
    }

    /** Makes the cards glow and dims the rest, as when the game asks the player to pick one of them. */
    private void spotlightNow(final List<Card> cards) {
        if (cards.isEmpty() || stopped) {
            return;
        }
        final List<CardView> views = new ArrayList<>();
        for (final Card c : cards) {
            views.add(c.getView());
        }
        spotlit = true;
        human.getGui().setSelection(new Selection(views, 1, views.size(), List.of(), List.of()));
    }

    private void clearSpotlight() {
        if (spotlit) {
            spotlit = false;
            human.getGui().setSelection(Selection.NONE);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Reading the game (polled from the director thread, so every read is allowed to fail harmlessly)
    // ---------------------------------------------------------------------------------------------------------

    private boolean alive() {
        return !stopped && !game.isGameOver() && hosted.getGame() == game;
    }

    private boolean safe(final BooleanSupplier supplier) {
        try {
            return supplier.getAsBoolean();
        } catch (final RuntimeException e) {
            return false; // the game changed under us mid-read; the next poll sees a consistent state
        }
    }

    private Input input() {
        return human.getInputProxy().getInput();
    }

    private PhaseType phase() {
        return game.getPhaseHandler().getPhase();
    }

    private boolean myTurn() {
        return game.getPhaseHandler().getPlayerTurn() == me;
    }

    private boolean myPriority() {
        return input() instanceof InputPassPriority;
    }

    private boolean inMain1() {
        return myTurn() && phase() == PhaseType.MAIN1 && myPriority();
    }

    private boolean leftMain1() {
        return !myTurn() || phase() != PhaseType.MAIN1;
    }

    private boolean attackPrompt() {
        return input() instanceof InputAttack;
    }

    private boolean blockPrompt() {
        return input() instanceof InputBlock;
    }

    private boolean payPrompt() {
        return input() instanceof InputPayMana;
    }

    private List<Card> zone(final Player player, final ZoneType zone) {
        final List<Card> list = new ArrayList<>();
        for (final Card c : player.getCardsIn(zone)) {
            list.add(c);
        }
        return list;
    }

    private List<Card> cards(final Card card) {
        return card == null ? Collections.<Card>emptyList() : Collections.singletonList(card);
    }

    private Card find(final Player player, final ZoneType zone, final String name) {
        for (final Card c : player.getCardsIn(zone)) {
            if (name.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }

    private boolean has(final Player player, final ZoneType zone, final String name) {
        return find(player, zone, name) != null;
    }

    private boolean hasLand() {
        for (final Card c : me.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand()) {
                return true;
            }
        }
        return false;
    }

    private Card untappedLand() {
        for (final Card c : me.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand() && !c.isTapped()) {
                return c;
            }
        }
        return null;
    }

    private List<Card> readyAttackers() {
        final List<Card> ready = new ArrayList<>();
        for (final Card c : me.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature() && !c.isTapped() && !c.isSick()) {
                ready.add(c);
            }
        }
        return ready;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Clicking, the way the player would
    // ---------------------------------------------------------------------------------------------------------

    // The demo's own moves go through runScripted, which is the only way past the input lock the demo puts on the
    // player's taps (see PlayerControllerHuman.setTutorialInputLocked).
    private void click(final Card card) {
        if (card != null) {
            FThreads.invokeInEdtNowOrLater(() ->
                    human.runScripted(() -> human.selectCard(card.getView(), null, LEFT_CLICK)));
        }
    }

    private void ok() {
        FThreads.invokeInEdtNowOrLater(() -> human.runScripted(human::selectButtonOk));
    }
}
