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
import forge.util.Localizer;

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
 * <p>Each lesson is explained in a note while the card to use is spotlighted: the game's own selection marking is
 * pointed at it, which makes the card glow and dims everything else. In {@code WATCH} mode the director then makes
 * the move itself, slowly, through the same entry points a mouse click uses (as Forge's macro replay does), and says
 * what changed. In {@code TRY} mode it keeps the spotlight on and waits for the player, repeating the hint if nothing
 * happens for a while.
 *
 * <p>All text shown to the player comes from the language files (keys starting with lblTut, msgTut and btnTut), so
 * it can be translated. Card names inside those texts are part of the translated sentence; the names used to find
 * the cards in the game ({@link #LAND} and friends) are the English card names and are never shown.
 *
 * <p>One daemon thread runs the script and polls the game state; it never touches the game from the game thread and
 * only clicks on the UI thread. Anything unexpected ends the tutorial quietly rather than leaving a stuck game.
 */
final class TutorialDirector implements Runnable, IHasForgeLog {
    // English card names, used only to find the cards in the game
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
    /**
     * True when this GUI shows one phase label instead of a box per phase (the compact mobile layout), so the notes
     * that point at "the phase boxes" must point at the label instead.
     */
    private final boolean unifiedPhase;
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
        unifiedPhase = GuiBase.getInterface().usesUnifiedPhaseLabel();
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

    /** Looks up a language-file text; {0}, {1}... in it are filled from {@code args}. */
    private static String t(final String key, final Object... args) {
        return Localizer.getInstance().getMessage(key, args);
    }

    /** The key of a text that differs between the per-phase boxes and the unified phase label. */
    private String phaseKey(final String key) {
        return unifiedPhase ? key + "Unified" : key;
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
        if (!watch && !SOptionPane.showConfirmDialog(t("msgTutTourSkip"), t("lblTutTourTitle"),
                t("btnTutTourShow"), t("btnTutTourSkip"), false)) {
            return;
        }

        say(t("lblTutTour1"), t("msgTutTour1", STARTING_LIFE, oppStartLife));

        spotlightAndSay(zone(me, ZoneType.Hand), t("lblTutTour2"), t("msgTutTour2"));

        final List<Card> battlefield = new ArrayList<>(zone(me, ZoneType.Battlefield));
        battlefield.addAll(zone(opp, ZoneType.Battlefield));
        spotlightAndSay(battlefield, t("lblTutTour3"), t("msgTutTour3"));

        say(t("lblTutTour4"), t("msgTutTour4"));

        say(t("lblTutTour5"), t(phaseKey("msgTutTour5")));

        say(t("lblTutTour6"), t("msgTutTour6") + "\n\n" + t(watch ? "msgTutTour6Watch" : "msgTutTour6Try"));
    }

    private boolean landLesson() throws InterruptedException {
        final boolean landed = step(t("lblTutLand"), t("msgTutLand"), t("msgTutLandTry"), t("msgTutLandWatch"),
                () -> cards(find(me, ZoneType.Hand, LAND)),
                () -> click(find(me, ZoneType.Hand, LAND)),
                this::hasLand,
                this::leftMain1,
                10_000);
        if (stopped) {
            return false;
        }
        if (!landed) {
            restartPractice(t("msgTutLandMissed"));
            return false;
        }
        spotlightAndSay(cards(untappedLand()), t("lblTutLandDone"), t("msgTutLandDone"));
        return alive();
    }

    private boolean castLesson() throws InterruptedException {
        if (!castAndPay()) {
            if (!stopped) {
                restartPractice(t("msgTutCastMissed"));
            }
            return false;
        }

        if (waitUntil(() -> !game.getStack().isEmpty() && myPriority(), 5_000)) {
            step(t("lblTutStack"), t("msgTutStack"), t("msgTutStackTry"), t("msgTutStackWatch"),
                    NOTHING,
                    this::ok,
                    () -> has(me, ZoneType.Battlefield, GOBLIN),
                    () -> false,
                    10_000);
            if (stopped) {
                return false;
            }
        }
        spotlightAndSay(cards(find(me, ZoneType.Battlefield, GOBLIN)), t("lblTutResolved"), t("msgTutResolved"));
        return alive();
    }

    private void attackLesson() throws InterruptedException {
        passStep(t("lblTutAttack"), t("msgTutAttack"), t("msgTutToCombatTry"), t("msgTutToCombatWatch"),
                () -> attackPrompt() || !myTurn(),
                20_000);
        if (!alive() || !attackPrompt()) {
            return;
        }

        step(t("lblTutDeclare"), t("msgTutDeclare"), t("msgTutDeclareTry"), t("msgTutDeclareWatch"),
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
        spotlightAndSay(cards(find(me, ZoneType.Battlefield, GOBLIN)), t("lblTutDamage"),
                dealt > 0 ? t("msgTutDamageDealt", dealt, opp.getLife()) : t("msgTutCombatOver"));
    }

    private void passLesson() throws InterruptedException {
        passStep(t("lblTutPass"), t("msgTutPass"), t("msgTutPassTry"), t("msgTutPassWatch"),
                () -> !myTurn(),
                25_000);
        if (!alive()) {
            return;
        }
        say(t("lblTutOppTurn"), t(phaseKey("msgTutOppTurn")));
    }

    private void blockLesson() throws InterruptedException {
        final String waitHint = t("msgTutWaitHint");
        advanceTo(() -> blockPrompt() || myTurn() && phase() == PhaseType.MAIN1, 120_000, waitHint);
        if (!alive()) {
            return;
        }
        if (!blockPrompt()) {
            say(t("lblTutNoAttack"), t("msgTutNoAttack"));
            return;
        }

        step(t("lblTutBlock"), t("msgTutBlock"), t("msgTutBlockTry"), t("msgTutBlockWatch"),
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
            spotlightAndSay(cards(find(me, ZoneType.Battlefield, GIANT)), t("lblTutBlocked"), t("msgTutBlocked"));
        } else {
            say(t("lblTutNotBlocked"), t("msgTutNotBlocked", STARTING_LIFE - me.getLife()));
        }
    }

    private void winLesson() throws InterruptedException {
        final String waitHint = t("msgTutWaitHint");
        advanceTo(this::inMain1, 120_000, waitHint);
        for (int round = 0; round < 3 && alive(); round++) {
            final int oppLife = opp.getLife();
            passStep(t("lblTutWin"), t("msgTutWin", oppLife), t("msgTutToCombatTry"), t("msgTutToCombatWatch"),
                    () -> attackPrompt() || !myTurn(),
                    20_000);
            if (!alive()) {
                return;
            }
            if (attackPrompt()) {
                step(t("lblTutAllAttack"), t("msgTutAllAttack"), t("msgTutAllAttackTry"),
                        t("msgTutAllAttackWatch"),
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
            say(t("lblTutNotQuite"), t("msgTutNotQuite", opp.getLife()));
            advanceTo(this::inMain1, 120_000, waitHint);
        }

        // The end-of-game hook may already have called stop(), so this is shown whether or not we were stopped.
        if (opp.getLife() <= 0) {
            if (watch) {
                HowToPlay.demoFinished();
            }
            GuiBase.getInterface().hideCoach();
            SOptionPane.showMessageDialog(t(watch ? "msgTutYouWinWatch" : "msgTutYouWinTry"), t("lblTutYouWin"),
                    SOptionPane.INFORMATION_ICON);
        }
    }

    /** Casts the Goblin and pays for it; a cancelled payment sends the spell back to hand, so ask again. */
    private boolean castAndPay() throws InterruptedException {
        for (int attempt = 0; attempt < 3 && alive(); attempt++) {
            final boolean cast = step(t("lblTutCast"), t("msgTutCast"), t("msgTutCastTry"), t("msgTutCastWatch"),
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
            final boolean paid = step(t("lblTutPay"), t("msgTutPay"), t("msgTutPayTry"), t("msgTutPayWatch"),
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
            say(t("lblTutNoProblem"), t("msgTutNoProblem"));
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
        say(title, body + "\n\n" + tryLine, t("btnTutGotIt"), over);
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
                say(t("lblTutNeedHand"), tryLine, t("btnTutGotIt"), over);
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
            say(title, body + "\n\n" + tryLine, t("btnTutGotIt"), done);
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
                say(t("lblTutKeepGoing"), hint, t("btnTutGotIt"), target);
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
        say(title, text, t("btnTutNext"), null);
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
        say(t("lblTutDesync"), t("msgTutDesync"));
        stopped = true;
    }

    private void restartPractice(final String why) {
        say(t("lblTutRestart"), t("msgTutRestart", why));
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
            final String heading = mine ? p.nameForUi : t("lblTutPhaseOpp", p.nameForUi);
            if (watch) {
                say(heading, note);
            } else {
                say(heading, note, t("btnTutGotIt"), () -> !myPriority()); // pressing OK in the game also dismisses it
            }
        }
    }

    private static String phaseNote(final PhaseType phase, final boolean mine) {
        switch (phase) {
            case UPKEEP:
                return t("msgTutPhaseUpkeep");
            case DRAW:
                return t(mine ? "msgTutPhaseDrawMine" : "msgTutPhaseDrawOpp");
            case COMBAT_BEGIN:
                return t("msgTutPhaseCombatBegin");
            case COMBAT_DAMAGE:
                return t("msgTutPhaseCombatDamage");
            case COMBAT_END:
                return t("msgTutPhaseCombatEnd");
            case MAIN2:
                return t("msgTutPhaseMain2");
            case END_OF_TURN:
                return t("msgTutPhaseEnd");
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