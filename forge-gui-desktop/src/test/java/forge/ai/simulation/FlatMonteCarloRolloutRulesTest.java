package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import forge.ai.PlayerControllerAi;
import forge.game.card.Card;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/** How rollouts are judged and compared, and the opponent's answers inside a rollout. */
public class FlatMonteCarloRolloutRulesTest extends FlatMcTestBase {

    @Test
    public void onlyTheSamplesThatEveryLineHasAreShared() {
        final int[] count = { 4, 3, 1, 2 };
        final boolean[] invalid = { false, false, true, false };
        final boolean[] pruned = { false, false, false, true };
        assertEquals(FlatMonteCarloPicker.sharedSamples(count, invalid, pruned, 4), 3);
        assertEquals(FlatMonteCarloPicker.meanOfFirst(new long[] { 10, 20, 30, 1000 }, 3), 20.0);
    }

    @Test
    public void twoSamplesAreNotEnoughToDropALine() {
        final long[][] vals = { { 500, 500, 500 }, { 100, 100, 100 } };
        final boolean[] pruned = new boolean[2];
        assertEquals(FlatMonteCarloPicker.pruneBehind(vals, new int[] { 2, 2 }, new boolean[2], pruned, 2, 80), 0);
        assertEquals(FlatMonteCarloPicker.pruneBehind(vals, new int[] { 3, 3 }, new boolean[2], pruned, 2, 80), 1);
        assertTrue(pruned[1]);
    }

    @Test
    public void dyingOnTheOpponentsTurnCostsTheFullPenalty() {
        assertEquals(FlatMonteCarloPicker.deathOnOpponentTurn(1000), 1000 - FlatMonteCarloPicker.LOOKAHEAD_DEATH_PENALTY);
        assertTrue(FlatMonteCarloPicker.deathOnOpponentTurn(-19000) > FlatMonteCarloPicker.LOSS_VALUE);
    }

    /** A lethal Lava Axe into an untapped Counterspell: every card the opponent can be dealt is one. */
    private FlatMonteCarloPicker.Result axeIntoCounterspell(final boolean opponentResponses) {
        return axeIntoCounterspell(opponentResponses, null);
    }

    private FlatMonteCarloPicker.Result axeIntoCounterspell(final boolean opponentResponses, final String aiCreature) {
        setUpGame();
        if (aiCreature != null) {
            put(aiCreature, ai, ZoneType.Battlefield);
        }
        put("Mountain", 5, ai, ZoneType.Battlefield);
        put("Island", 2, opp, ZoneType.Battlefield);
        final SpellAbility axe = put("Lava Axe", ai, ZoneType.Hand).getFirstSpellAbility();
        put("Counterspell", opp, ZoneType.Hand);
        put("Mountain", 12, ai, ZoneType.Library);
        put("Counterspell", 12, opp, ZoneType.Library);
        opp.setLife(5, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        axe.setActivatingPlayer(ai);

        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);
        cfg.opponentResponses = opponentResponses;
        cfg.budgetNanos = TimeUnit.SECONDS.toNanos(10);
        // on the AI's turn passing is only compared when there is an instant to hold, so RESPOND mode (which always
        // compares passing) is not wanted here: a second play gives the comparison its other line
        final SpellAbility bolt = put("Shock", ai, ZoneType.Hand).getFirstSpellAbility();
        bolt.setActivatingPlayer(ai);
        FlatMonteCarloPicker.applyTarget(bolt, opp);
        FlatMonteCarloPicker.applyTarget(axe, opp);
        cfg.targetVariants = 0;
        return FlatMonteCarloPicker.choose(ai, Arrays.asList(axe, bolt), cfg, null);
    }

    @Test(timeOut = 120000)
    public void withoutResponsesTheBurnSpellAlwaysWins() {
        final FlatMonteCarloPicker.Result r = axeIntoCounterspell(false);
        assertEquals(r.reason, "wins", String.valueOf(r));
        assertEquals(r.rollouts, FlatMonteCarloPicker.WIN_CONFIRM_SAMPLES, "a win is confirmed: " + r);
    }

    @Test(timeOut = 120000)
    public void theOpponentCountersItInTheRollouts() {
        final FlatMonteCarloPicker.Result r = axeIntoCounterspell(true);
        assertNotEquals(r.reason, "wins", String.valueOf(r));
    }

    /** Prowess goes on the stack above the spell: the opponent must still get to counter the spell beneath it. */
    @Test(timeOut = 120000)
    public void aCastTriggerOnTopDoesNotHideTheSpellFromTheOpponent() {
        final FlatMonteCarloPicker.Result r = axeIntoCounterspell(true, "Monastery Swiftspear");
        assertNotEquals(r.reason, "wins", String.valueOf(r));
    }

    // three players, in the order they were seated: opponent, AI, third
    private Player third;

    /** The AI casts a Lava Axe at the first opponent in a three player game; the stack then plays out. */
    private void axeAtTheFirstOpponent(final boolean firstHasCancel, final int aiCancels, final boolean thirdHasCancel) {
        axeAtTheFirstOpponent(firstHasCancel, aiCancels, thirdHasCancel, false);
    }

    private void axeAtTheFirstOpponent(final boolean firstHasCancel, final int aiCancels, final boolean thirdHasCancel,
                                       final boolean thirdWaits) {
        game = initAndCreateThreePlayerGame();
        opp = game.getPlayers().get(0);
        ai = game.getPlayers().get(1);
        third = game.getPlayers().get(2);
        put("Mountain", 5, ai, ZoneType.Battlefield);
        put("Island", 3, ai, ZoneType.Battlefield);
        put("Island", 3, opp, ZoneType.Battlefield);
        put("Island", 3, third, ZoneType.Battlefield);
        final SpellAbility axe = put("Lava Axe", ai, ZoneType.Hand).getFirstSpellAbility();
        put("Cancel", aiCancels, ai, ZoneType.Hand);
        // the first opponent is the weakest one; without a Cancel it still holds a card
        put(firstHasCancel ? "Cancel" : "Mountain", opp, ZoneType.Hand);
        put(thirdHasCancel ? "Cancel" : "Mountain", third, ZoneType.Hand);
        opp.setLife(10, null);
        third.setLife(20, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        axe.setActivatingPlayer(ai);
        FlatMonteCarloPicker.applyTarget(axe, opp);
        assertEquals(ai.getWeakestOpponent(), opp);

        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);
        ((PlayerControllerAi) ai.getController()).playChosenSpellAbility(axe);
        if (thirdWaits) {
            third.runWithController(() -> FlatMonteCarloPicker.resolveWithResponses(game, ai, cfg, 0),
                    new AnswersTheSecondTime(game, third, "Lava Axe"));
        } else {
            FlatMonteCarloPicker.resolveWithResponses(game, ai, cfg, 0);
        }
    }

    @Test(timeOut = 120000)
    public void nobodyHasAnAnswerSoTheSpellResolves() {
        axeAtTheFirstOpponent(false, 0, false);
        assertEquals(opp.getLife(), 5);
    }

    @Test(timeOut = 120000)
    public void anOpponentOtherThanTheWeakestCanAnswer() {
        axeAtTheFirstOpponent(false, 0, true);
        assertEquals(opp.getLife(), 10);
        assertEquals(third.getCardsIn(ZoneType.Hand).size(), 0, "the third player used the Cancel");
    }

    @Test(timeOut = 120000)
    public void theAiCanCounterTheCounter() {
        axeAtTheFirstOpponent(true, 1, false);
        assertEquals(opp.getLife(), 5);
    }

    @Test(timeOut = 120000)
    public void anotherOpponentCanJoinACounterExchange() {
        axeAtTheFirstOpponent(true, 1, true);
        assertEquals(opp.getLife(), 10);
        assertEquals(opp.getCardsIn(ZoneType.Hand).size(), 0);
        assertEquals(ai.getCardsIn(ZoneType.Hand).size(), 0);
        assertEquals(third.getCardsIn(ZoneType.Hand).size(), 0);
    }

    /** The AI puts a play on the stack with the opponent holding a Stifle, and the stack plays out. */
    private void playIntoStifle(final SpellAbility sa) {
        put("Island", opp, ZoneType.Battlefield);
        put("Stifle", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        sa.setActivatingPlayer(ai);
        assertTrue(FlatMonteCarloPicker.applyTarget(sa, opp) || !sa.usesTargeting());
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);
        ((PlayerControllerAi) ai.getController()).playChosenSpellAbility(sa);
        FlatMonteCarloPicker.resolveWithResponses(game, ai, cfg, 0);
    }

    @Test(timeOut = 120000)
    public void anOpponentCanStifleATrigger() {
        setUpGame();
        put("Mountain", 2, ai, ZoneType.Battlefield);
        final SpellAbility pyromancer = put("Viashino Pyromancer", ai, ZoneType.Hand).getFirstSpellAbility();
        playIntoStifle(pyromancer);
        assertEquals(opp.getCardsIn(ZoneType.Hand).size(), 0, "the Stifle was used");
        assertEquals(opp.getLife(), 20);
    }

    @Test(timeOut = 120000)
    public void anOpponentCanStifleAnActivatedAbility() {
        setUpGame();
        final Card pinger = put("Prodigal Pyromancer", ai, ZoneType.Battlefield);
        pinger.setSickness(false);
        SpellAbility ping = null;
        for (final SpellAbility sa : pinger.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                ping = sa;
            }
        }
        playIntoStifle(ping);
        assertEquals(opp.getCardsIn(ZoneType.Hand).size(), 0, "the Stifle was used");
        assertEquals(opp.getLife(), 20);
    }

    @Test(timeOut = 120000)
    public void anOpponentWithAnEmptyHandCanAnswerFromTheBattlefield() {
        setUpGame();
        final Card mother = put("Mother of Runes", opp, ZoneType.Battlefield);
        mother.setSickness(false);
        final Card bears = put("Grizzly Bears", opp, ZoneType.Battlefield);
        put("Mountain", ai, ZoneType.Battlefield);
        final SpellAbility shock = put("Shock", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        shock.setActivatingPlayer(ai);
        shock.resetTargets();
        shock.getTargets().add(bears);
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);
        ((PlayerControllerAi) ai.getController()).playChosenSpellAbility(shock);
        assertEquals(opp.getCardsIn(ZoneType.Hand).size(), 0);
        FlatMonteCarloPicker.resolveWithResponses(game, ai, cfg, 0);
        assertTrue(mother.isTapped(), "Mother of Runes was activated");
        assertTrue(bears.isInPlay(), "the creature was protected");
    }

    /** The AI and the third player are a team; the AI puts a spell on the stack and the stack plays out. */
    private void castWithATeammate(final SpellAbility sa) {
        ai.setTeam(1);
        third.setTeam(1);
        opp.setTeam(2);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        sa.setActivatingPlayer(ai);
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);
        ((PlayerControllerAi) ai.getController()).playChosenSpellAbility(sa);
        FlatMonteCarloPicker.resolveWithResponses(game, ai, cfg, 0);
    }

    private void setUpTeams() {
        game = initAndCreateThreePlayerGame();
        opp = game.getPlayers().get(0);
        ai = game.getPlayers().get(1);
        third = game.getPlayers().get(2);
    }

    @Test(timeOut = 120000)
    public void aTeammateCanAnswer() {
        setUpTeams();
        final Card mother = put("Mother of Runes", third, ZoneType.Battlefield);
        mother.setSickness(false);
        final Card bears = put("Grizzly Bears", third, ZoneType.Battlefield);
        put("Mountain", ai, ZoneType.Battlefield);
        final SpellAbility shock = put("Shock", ai, ZoneType.Hand).getFirstSpellAbility();
        shock.resetTargets();
        shock.getTargets().add(bears);
        castWithATeammate(shock);
        assertTrue(mother.isTapped(), "the teammate activated Mother of Runes");
        assertTrue(bears.isInPlay(), "the creature was protected");
    }

    @Test(timeOut = 120000)
    public void aTeammateDoesNotCounterTheTeamsOwnSpell() {
        setUpTeams();
        put("Island", 3, third, ZoneType.Battlefield);
        put("Cancel", third, ZoneType.Hand);
        put("Mountain", 5, ai, ZoneType.Battlefield);
        final SpellAbility axe = put("Lava Axe", ai, ZoneType.Hand).getFirstSpellAbility();
        FlatMonteCarloPicker.applyTarget(axe, opp);
        opp.setLife(20, null);
        castWithATeammate(axe);
        assertEquals(third.getCardsIn(ZoneType.Hand).size(), 1, "the Cancel was kept");
        assertEquals(opp.getLife(), 15);
    }

    /** Notes who is asked about what, in order, and passes, or plays as usual if told to. */
    private static final class RecordsPriority extends PlayerControllerAi {
        private final String who;
        private final List<String> asked;
        private boolean plays;

        RecordsPriority(final Game game, final Player player, final String who, final List<String> asked) {
            super(game, player, player.getLobbyPlayer());
            this.who = who;
            this.asked = asked;
        }

        @Override
        public List<SpellAbility> chooseSpellAbilityToPlay() {
            asked.add(who + ": " + getGame().getStack().peekAbility().getHostCard().getName());
            return plays ? super.chooseSpellAbilityToPlay() : null;
        }
    }

    /**
     * Three players; the AI casts an Ambush Viper on the given player's turn, and the third player's Soul Warden
     * triggers when it resolves.
     *
     * @return who was asked about what while the stack played out
     */
    private List<String> priorityAroundSoulWarden(final int turnOfSeat) {
        game = initAndCreateThreePlayerGame();
        // seated in this order
        opp = game.getPlayers().get(0);
        ai = game.getPlayers().get(1);
        third = game.getPlayers().get(2);
        put("Soul Warden", third, ZoneType.Battlefield);
        put("Forest", 2, ai, ZoneType.Battlefield);
        final SpellAbility viper = put("Ambush Viper", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(turnOfSeat));
        game.getAction().checkStateEffects(true);
        viper.setActivatingPlayer(ai);
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);
        ((PlayerControllerAi) ai.getController()).playChosenSpellAbility(viper);

        final List<String> asked = new ArrayList<>();
        opp.runWithController(() -> ai.runWithController(() -> third.runWithController(
                () -> FlatMonteCarloPicker.resolveWithResponses(game, ai, cfg, 0),
                new RecordsPriority(game, third, "third", asked)),
                new RecordsPriority(game, ai, "ai", asked)),
                new RecordsPriority(game, opp, "opp", asked));
        assertEquals(third.getLife(), 21, "the trigger resolved");
        return asked;
    }

    @Test(timeOut = 120000)
    public void theCasterGetsPriorityFirstAndTheActivePlayerAfterAResolution() {
        assertEquals(priorityAroundSoulWarden(1), Arrays.asList(
                "ai: Ambush Viper", "third: Ambush Viper", "opp: Ambush Viper",
                "ai: Soul Warden", "third: Soul Warden", "opp: Soul Warden"));
    }

    @Test(timeOut = 120000)
    public void onAnotherPlayersTurnThatPlayerGetsPriorityAfterAResolution() {
        assertEquals(priorityAroundSoulWarden(0), Arrays.asList(
                "ai: Ambush Viper", "third: Ambush Viper", "opp: Ambush Viper",
                "opp: Soul Warden", "ai: Soul Warden", "third: Soul Warden"));
    }

    @Test(timeOut = 120000)
    public void whoeverRespondsGetsPriorityNext() {
        game = initAndCreateThreePlayerGame();
        opp = game.getPlayers().get(0);
        ai = game.getPlayers().get(1);
        third = game.getPlayers().get(2);
        put("Mountain", 5, ai, ZoneType.Battlefield);
        put("Island", 3, third, ZoneType.Battlefield);
        put("Cancel", third, ZoneType.Hand);
        final SpellAbility axe = put("Lava Axe", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        axe.setActivatingPlayer(ai);
        FlatMonteCarloPicker.applyTarget(axe, opp);
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);
        ((PlayerControllerAi) ai.getController()).playChosenSpellAbility(axe);

        final List<String> asked = new ArrayList<>();
        final RecordsPriority counters = new RecordsPriority(game, third, "third", asked);
        counters.plays = true;
        opp.runWithController(() -> ai.runWithController(() -> third.runWithController(
                () -> FlatMonteCarloPicker.resolveWithResponses(game, ai, cfg, 0), counters),
                new RecordsPriority(game, ai, "ai", asked)),
                new RecordsPriority(game, opp, "opp", asked));
        assertEquals(asked.subList(0, 3), Arrays.asList("ai: Lava Axe", "third: Lava Axe", "third: Cancel"));
    }

    /** Passes on everything until it is asked about the named spell for the second time; then plays as usual. */
    private static final class AnswersTheSecondTime extends PlayerControllerAi {
        private final String spell;
        private int timesAsked;

        AnswersTheSecondTime(final Game game, final Player player, final String spell) {
            super(game, player, player.getLobbyPlayer());
            this.spell = spell;
        }

        @Override
        public List<SpellAbility> chooseSpellAbilityToPlay() {
            final boolean aboutIt = getGame().getStack().peekAbility().getHostCard().getName().equals(spell);
            return aboutIt && ++timesAsked >= 2 ? super.chooseSpellAbilityToPlay() : null;
        }
    }

    @Test(timeOut = 120000)
    public void aPlayerWhoPassedIsAskedAgainAfterACounterExchange() {
        axeAtTheFirstOpponent(true, 1, true, true);
        assertEquals(third.getCardsIn(ZoneType.Hand).size(), 0, "the third player answered the second time");
        assertEquals(opp.getLife(), 10);
    }
}
