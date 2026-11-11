package forge.gamesimulationtests.priority;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.google.common.collect.Lists;

import forge.ai.AITest;
import forge.ai.AiCostDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCost;
import forge.ai.PlayerControllerAi;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPayment;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlaySpellAbility;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

/**
 * Drives the real {@link forge.game.phase.PhaseHandler#mainLoopStep()} with scripted players and records every time a
 * player receives priority (CR 117), so tests can assert who gets priority, in which step, and what the stack held.
 */
public abstract class PriorityTestBase extends AITest {
    protected static final String WURM = "Panglacial Wurm";
    protected static final int STEP_CAP = 400;

    protected Game game;
    /** The player whose turn it is when the test starts. */
    protected Player a;
    protected Player b;
    /** The third player in a three-player game (null otherwise). */
    protected Player c;
    protected final List<Priority> log = new ArrayList<>();

    /** One time a player received priority. */
    protected record Priority(int turn, PhaseType phase, Player player, int stackSize, String stackTop) {
        @Override
        public String toString() {
            return "T" + turn + " " + phase + " " + player.getName() + " stack=" + stackSize + (stackTop == null ? "" : " top=" + stackTop);
        }
    }

    /** Passes priority unless a scripted action is queued, accepts every optional choice and records priority. */
    protected class ScriptedController extends PlayerControllerAi {
        final Deque<SpellAbility> actions = new ArrayDeque<>();
        /** Queued actions are only taken when receiving priority in this step (any step if null). */
        PhaseType actIn;
        /** Chosen whenever an effect asks this player to choose among cards that include it (e.g. Cytoshape's copy). */
        Card prefer;
        /** Every order this player chose for objects they put on the stack at the same time (CR 405.3, 707.10d). */
        final List<List<SpellAbility>> orderedSimultaneous = new ArrayList<>();
        /** Play from effects and resolve abilities the way PlayerControllerHuman does (through PlaySpellAbility). */
        boolean humanPaths;
        /** Declines optional copies (e.g. Demonstrate) instead of accepting them like every other optional choice. */
        boolean declineCopies;
        /** Cancels while choosing targets, as a human player can, which cancels the cast. */
        boolean cancelTargeting;
        /** If set, the only attack this player declares. */
        Card attackWith;
        /** If set, this player's only block: blockWith blocks blocked. */
        Card blockWith, blocked;

        ScriptedController(final Player p) {
            super(p.getGame(), p, p.getLobbyPlayer());
        }

        @Override
        public List<SpellAbility> chooseSpellAbilityToPlay() {
            final MagicStack stack = getGame().getStack();
            log.add(new Priority(getGame().getPhaseHandler().getTurn(), getGame().getPhaseHandler().getPhase(), player,
                    stack.size(), stack.isEmpty() ? null : stack.peekAbility().getHostCard().getName()));
            final SpellAbility next = actIn == null || actIn == getGame().getPhaseHandler().getPhase() ? actions.poll() : null;
            if (next != null) {
                next.setActivatingPlayer(player);
                return Lists.newArrayList(next);
            }
            return null;
        }

        @Override
        public void declareAttackers(final Player attacker, final Combat combat) {
            // no attacks unless the test scripts one, so the AI can't change the board between turns
            if (attackWith == null) {
                return;
            }
            combat.addAttacker(attackWith, (GameEntity) combat.getDefenders().get(0));
        }

        @Override
        public void declareBlockers(final Player defender, final Combat combat) {
            if (blockWith == null) {
                return;
            }
            combat.addBlocker(blocked, blockWith);
        }

        @Override
        public boolean chooseTargetsFor(final SpellAbility currentAbility) {
            return !cancelTargeting && super.chooseTargetsFor(currentAbility);
        }

        /** Takes every optional replacement, e.g. Archmage Ascension's search instead of a draw. */
        @Override
        public boolean confirmReplacementEffect(final ReplacementEffect replacementEffect, final SpellAbility effectSA,
                final GameEntity affected, final String question) {
            return true;
        }

        /** Pays every "unless" cost it can, e.g. Steam Vents' 2 life. */
        @Override
        public boolean payCostToPreventEffect(final Cost cost, final SpellAbility sa, final boolean alreadyPaid,
                final FCollectionView<Player> allPayers) {
            if (!ComputerUtilCost.canPayCost(cost, sa, player, true)) {
                return false;
            }
            return new CostPayment(cost, sa).payComputerCosts(new AiCostDecision(player, sa, true));
        }

        @Override
        public <T extends GameEntity> T chooseSingleEntityForEffect(final FCollectionView<T> optionList, final DelayedReveal delayedReveal,
                final SpellAbility sa, final String title, final boolean isOptional, final Player targetedPlayer, final Map<String, Object> params) {
            if (prefer != null && optionList.contains(prefer)) {
                @SuppressWarnings("unchecked")
                final T chosen = (T) prefer;
                return chosen;
            }
            return super.chooseSingleEntityForEffect(optionList, delayedReveal, sa, title, isOptional, targetedPlayer, params);
        }

        /** Keeps the given order and records it. */
        @Override
        public List<SpellAbility> orderSimultaneousSa(final List<SpellAbility> sas) {
            orderedSimultaneous.add(new ArrayList<>(sas));
            return sas;
        }

        @Override
        public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, List<String> options, Card cardToShow, Map<String, Object> params) {
            return !(declineCopies && sa != null && sa.getApi() == ApiType.CopySpellAbility);
        }

        @Override
        public boolean playSaFromPlayEffect(final SpellAbility tgtSA) {
            // the AI controller doesn't play lands from effects
            if (humanPaths || tgtSA.isLandAbility()) {
                return PlaySpellAbility.playSpellAbility(this, player, tgtSA);
            }
            return ComputerUtil.playStack(tgtSA, player, getGame());
        }

        @Override
        public void playSpellAbilityNoStack(final SpellAbility effectSA, final boolean canSetupTargets) {
            if (humanPaths) {
                PlaySpellAbility.playSpellAbilityNoStack(this, player, effectSA, !canSetupTargets);
            } else {
                super.playSpellAbilityNoStack(effectSA, canSetupTargets);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------- set-up

    /** Two players with scripted controllers; it is {@code a}'s first main phase. */
    protected void setUpGame() {
        game = initAndCreateGame();
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = false;
        a = game.getPlayers().get(1);
        b = game.getPlayers().get(0);
        a.dangerouslySetController(new ScriptedController(a));
        b.dangerouslySetController(new ScriptedController(b));
        fillLibrary(a, 10);
        fillLibrary(b, 10);
        log.clear();
    }

    /** Three players: it is {@code a}'s first main phase, {@code b} is next in turn order, then {@code c}. */
    protected void setUpThreePlayerGame() {
        game = initAndCreateThreePlayerGame();
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = false;
        a = game.getPhaseHandler().getPlayerTurn();
        b = game.getNextPlayerAfter(a);
        c = game.getNextPlayerAfter(b);
        for (final Player p : game.getPlayers()) {
            p.dangerouslySetController(new ScriptedController(p));
            fillLibrary(p, 10);
        }
        log.clear();
    }

    /** The stack from top to bottom. */
    protected List<SpellAbility> stackTopDown() {
        final List<SpellAbility> out = new ArrayList<>();
        for (final SpellAbilityStackInstance si : game.getStack()) {
            out.add(si.getSpellAbility());
        }
        return out;
    }

    protected ScriptedController ctrl(final Player p) {
        return (ScriptedController) p.getController();
    }

    protected void at(final PhaseType phase, final Player turnOf) {
        game.getPhaseHandler().devModeSet(phase, turnOf);
        game.getPhaseHandler().onStackResolved();
    }

    protected List<Card> lands(final Player p, final int n) {
        return lands("Forest", p, n);
    }

    protected List<Card> lands(final String name, final Player p, final int n) {
        final List<Card> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(addCard(name, p));
        }
        return out;
    }

    /**
     * Lets {@code p} cast Panglacial Wurm whenever they would gain life or draw: Lich turns life gain into draws, and
     * Archmage Ascension (with six quest counters) turns each draw into a search, during which the Wurm can be cast.
     */
    protected void wurmEngine(final Player p) {
        addCard("Lich", p);
        addCard("Archmage Ascension", p).setCounters(CounterEnumType.QUEST, 6);
        giveWurm(p);
    }

    /** A Panglacial Wurm in the library and seven Forests to cast it with. */
    protected void giveWurm(final Player p) {
        addCardToZone(WURM, p, ZoneType.Library);
        lands(p, 7);
    }


    /** The spell Panglacial Wurm's keyword lets {@code p} cast from their library. */
    protected SpellAbility wurmFromLibrary(final Player p) {
        for (final Card c : p.getCardsIn(ZoneType.Library)) {
            if (c.getName().equals(WURM)) {
                return AbilityUtils.getSpellsFromPlayEffect(c, p, CardStateName.Original, true, null).get(0);
            }
        }
        throw new AssertionError("no " + WURM + " in the library of " + p);
    }

    protected SpellAbility firstAbility(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        throw new AssertionError(c + " has no activated ability");
    }



    /** Casts a spell from hand through the AI's casting code (pays the mana with untapped lands and mana abilities). */
    protected Card cast(final String name, final Player p, final GameObject... targets) {
        final Card c = addCardToZone(name, p, ZoneType.Hand);
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        for (final GameObject t : targets) {
            sa.getTargets().add(t);
        }
        assertTrue(ComputerUtil.playStack(sa, p, game), "could not cast " + name);
        return c;
    }

    /** Lets the players pass until the stack is empty and no triggered ability is waiting. */
    protected void resolveAll() {
        runUntil(() -> game.getStack().isEmpty() && !game.getStack().hasSimultaneousStackEntries(), "the stack resolving");
    }

    /** {@code p} activates {@code c}'s first non-mana activated ability, paying its cost, the next time they get priority. */
    protected void activate(final Card c, final Player p) {
        ctrl(p).actions.add(firstAbility(c));
    }

    /** Archmage Ascension with six quest counters: {@code p} may search their library instead of drawing. */
    protected Card archmageAscension(final Player p) {
        final Card c = addCard("Archmage Ascension", p);
        c.setCounters(CounterEnumType.QUEST, 6);
        return c;
    }

    /**
     * {@code p} is at 0 life, kept in the game by Marina Vendrell's Grimoire, and has cast Stunning Reversal this turn:
     * once the Grimoire is gone, losing the game for 0 life (a state-based action) is replaced by drawing seven cards,
     * which Archmage Ascension turns into searches, during which {@code p} casts Panglacial Wurm.
     */
    protected Card stunningReversalEngine(final Player p) {
        final Card grimoire = addCard("Marina Vendrell's Grimoire", p);
        archmageAscension(p);
        addCardToZone(WURM, p, ZoneType.Library);
        p.setLife(0, null);
        lands("Swamp", p, 4);
        cast("Stunning Reversal", p);
        resolveAll();
        // added after the casts so that only the Wurm uses them
        lands(p, 7);
        return grimoire;
    }

    // ------------------------------------------------------------------------------------------------- the loop

    /** Runs the game loop until {@code done} holds; fails if it takes more than {@link #STEP_CAP} steps (a hang). */
    protected void runUntil(final BooleanSupplier done, final String what) {
        for (int i = 0; i < STEP_CAP; i++) {
            if (done.getAsBoolean()) {
                return;
            }
            if (game.isGameOver()) {
                fail("the game ended before " + what + "\n" + log);
            }
            game.getPhaseHandler().mainLoopStep();
        }
        fail("the game loop made no progress towards " + what + " (hang?)\n" + log);
    }

    protected void runUntilPhase(final int turn, final PhaseType phase) {
        runUntil(() -> turn() > turn || (turn() == turn && !game.getPhaseHandler().getPhase().isBefore(phase)),
                "turn " + turn + " " + phase);
    }

    /** Runs until a player receives priority in this turn and step. */
    protected Priority runUntilPriorityIn(final int turn, final PhaseType phase) {
        runUntil(() -> firstPriority(turn, phase) != null || turn() > turn, "priority in turn " + turn + " " + phase);
        final Priority p = firstPriority(turn, phase);
        if (p == null) {
            fail("no player received priority in turn " + turn + " " + phase + "\n" + log);
        }
        return p;
    }

    protected int turn() {
        return game.getPhaseHandler().getTurn();
    }

    protected Priority firstPriority(final int turn, final PhaseType phase) {
        for (final Priority p : log) {
            if (p.turn == turn && p.phase == phase) {
                return p;
            }
        }
        return null;
    }

    protected List<Priority> priorities(final int turn, final PhaseType phase) {
        final List<Priority> out = new ArrayList<>();
        for (final Priority p : log) {
            if (p.turn == turn && p.phase == phase) {
                out.add(p);
            }
        }
        return out;
    }

    protected int count(final String name, final ZoneType zone) {
        return countCardsWithName(game, name, zone);
    }
}
