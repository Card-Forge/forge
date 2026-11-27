package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import forge.ai.PlayerControllerAi;
import forge.game.ability.ApiType;
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

    /**
     * On the opponent's turn their Shock aims at the AI's Grizzly Bears; the AI answers with Mother of Runes.
     *
     * @return who was asked about what while the stack played out
     */
    private List<String> protectAgainstShock(final boolean responses) {
        setUpGame();
        final Card mother = put("Mother of Runes", ai, ZoneType.Battlefield);
        mother.setSickness(false);
        final Card bears = put("Grizzly Bears", ai, ZoneType.Battlefield);
        put("Mountain", opp, ZoneType.Battlefield);
        final SpellAbility shock = put("Shock", opp, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);
        shock.setActivatingPlayer(opp);
        shock.resetTargets();
        shock.getTargets().add(bears);
        ((PlayerControllerAi) opp.getController()).playChosenSpellAbility(shock);
        SpellAbility protect = null;
        for (final SpellAbility sa : mother.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                protect = sa;
            }
        }
        protect.setActivatingPlayer(ai);
        protect.resetTargets();
        protect.getTargets().add(bears);
        ((PlayerControllerAi) ai.getController()).playChosenSpellAbility(protect);
        assertEquals(game.getStack().size(), 2);
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);
        cfg.opponentResponses = responses;

        final List<String> asked = new ArrayList<>();
        opp.runWithController(() -> ai.runWithController(
                () -> FlatMonteCarloPicker.resolveWithResponses(game, ai, cfg, 1),
                new RecordsPriority(game, ai, "ai", asked)),
                new RecordsPriority(game, opp, "opp", asked));
        return asked;
    }

    @Test(timeOut = 120000)
    public void theSpellUnderneathGetsItsOwnRoundOfPriority() {
        assertEquals(protectAgainstShock(true), Arrays.asList(
                "ai: Mother of Runes", "opp: Mother of Runes", "opp: Shock", "ai: Shock"));
        assertTrue(game.getStack().isEmpty());
    }

    @Test(timeOut = 120000)
    public void withoutResponsesTheSpellUnderneathIsLeftForTheFollowUps() {
        assertEquals(protectAgainstShock(false), new ArrayList<String>());
        assertEquals(game.getStack().size(), 1);
    }

    @Test(timeOut = 120000)
    public void whenTheAiPassesTheOthersStillGetPriority() {
        game = initAndCreateThreePlayerGame();
        // seated in this order
        opp = game.getPlayers().get(0);
        ai = game.getPlayers().get(1);
        third = game.getPlayers().get(2);
        put("Mountain", 2, opp, ZoneType.Battlefield);
        final List<Card> shocks = Arrays.asList(put("Shock", opp, ZoneType.Hand), put("Shock", opp, ZoneType.Hand));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);
        for (final Card shock : shocks) {
            final SpellAbility sa = shock.getFirstSpellAbility();
            sa.setActivatingPlayer(opp);
            sa.resetTargets();
            sa.getTargets().add(ai);
            ((PlayerControllerAi) opp.getController()).playChosenSpellAbility(sa);
        }
        assertEquals(game.getStack().size(), 2);
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);

        final List<String> asked = new ArrayList<>();
        opp.runWithController(() -> ai.runWithController(() -> third.runWithController(
                () -> FlatMonteCarloPicker.resolveAfterPass(game, ai, cfg),
                new RecordsPriority(game, third, "third", asked)),
                new RecordsPriority(game, ai, "ai", asked)),
                new RecordsPriority(game, opp, "opp", asked));
        assertEquals(asked, Arrays.asList(
                "third: Shock", "opp: Shock", // the AI has passed on the top one
                "opp: Shock", "ai: Shock", "third: Shock"));
        assertEquals(ai.getLife(), 16);
    }

    /** Notes when it is asked, then makes its scripted plays one at a time, and passes when they run out. */
    private static final class PlaysInOrder extends PlayerControllerAi {
        private final String who;
        private final List<String> asked;
        private final List<SpellAbility> plays;

        PlaysInOrder(final Game game, final Player player, final String who, final List<String> asked,
                final SpellAbility... plays) {
            super(game, player, player.getLobbyPlayer());
            this.who = who;
            this.asked = asked;
            this.plays = new ArrayList<>(Arrays.asList(plays));
        }

        @Override
        public List<SpellAbility> chooseSpellAbilityToPlay() {
            asked.add(who + ": " + getGame().getStack().peekAbility().getHostCard().getName());
            return plays.isEmpty() ? null : Arrays.asList(plays.remove(0));
        }
    }

    /** Three players; the AI has a Lava Axe on the stack and the third player has an untapped Mountain. */
    private SpellAbility axeWithAMountainOpposite() {
        game = initAndCreateThreePlayerGame();
        // seated in this order
        opp = game.getPlayers().get(0);
        ai = game.getPlayers().get(1);
        third = game.getPlayers().get(2);
        put("Mountain", 5, ai, ZoneType.Battlefield);
        final Card mountain = put("Mountain", third, ZoneType.Battlefield);
        final SpellAbility axe = put("Lava Axe", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        axe.setActivatingPlayer(ai);
        FlatMonteCarloPicker.applyTarget(axe, opp);
        ((PlayerControllerAi) ai.getController()).playChosenSpellAbility(axe);
        final SpellAbility mana = mountain.getManaAbilities().get(0);
        mana.setActivatingPlayer(third);
        return mana;
    }

    private List<String> resolveWithThirdPlaying(final SpellAbility... plays) {
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);
        final List<String> asked = new ArrayList<>();
        opp.runWithController(() -> ai.runWithController(() -> third.runWithController(
                () -> FlatMonteCarloPicker.resolveWithResponses(game, ai, cfg, 0),
                new PlaysInOrder(game, third, "third", asked, plays)),
                new RecordsPriority(game, ai, "ai", asked)),
                new RecordsPriority(game, opp, "opp", asked));
        return asked;
    }

    @Test(timeOut = 120000)
    public void aManaAbilityKeepsPriorityAndStartsANewRound() {
        final SpellAbility mana = axeWithAMountainOpposite();
        assertEquals(resolveWithThirdPlaying(mana), Arrays.asList(
                "ai: Lava Axe",
                "third: Lava Axe", // taps for mana
                "third: Lava Axe", "opp: Lava Axe", "ai: Lava Axe"));
        assertTrue(mana.getHostCard().isTapped());
    }

    @Test(timeOut = 120000)
    public void theManaCanBeSpentBeforeAnybodyElseActs() {
        final SpellAbility mana = axeWithAMountainOpposite();
        final SpellAbility shock = put("Shock", third, ZoneType.Hand).getFirstSpellAbility();
        shock.setActivatingPlayer(third);
        shock.resetTargets();
        shock.getTargets().add(ai);
        final List<String> asked = resolveWithThirdPlaying(mana, shock);
        assertEquals(asked.subList(0, 4), Arrays.asList(
                "ai: Lava Axe", "third: Lava Axe", "third: Lava Axe", "third: Shock"));
        assertEquals(ai.getLife(), 18);
    }

    @Test(timeOut = 120000)
    public void aManaAbilityThatCannotBeActivatedCountsAsAPass() {
        final SpellAbility mana = axeWithAMountainOpposite();
        mana.getHostCard().setTapped(true);
        assertEquals(resolveWithThirdPlaying(mana), Arrays.asList("ai: Lava Axe", "third: Lava Axe", "opp: Lava Axe"));
        assertTrue(game.getStack().isEmpty());
    }

    /** The AI has Sensei's Divining Top and has just activated its ability to reorder the library. */
    private Card reorderWithTop() {
        setUpGame();
        final Card top = put("Sensei's Divining Top", ai, ZoneType.Battlefield);
        put("Island", ai, ZoneType.Battlefield);
        for (final String name : new String[] {"Grizzly Bears", "Shock", "Lava Axe", "Cancel"}) {
            put(name, ai, ZoneType.Library);
        }
        // enough cards left that the AI's guard against drawing itself out does not apply
        put("Forest", 6, ai, ZoneType.Library);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        final SpellAbility reorder = abilityOf(top, ApiType.RearrangeTopOfLibrary);
        reorder.setActivatingPlayer(ai);
        ((PlayerControllerAi) ai.getController()).playChosenSpellAbility(reorder);
        assertEquals(game.getStack().size(), 1);
        return top;
    }

    private static SpellAbility abilityOf(final Card card, final ApiType api) {
        for (final SpellAbility sa : card.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        throw new AssertionError(card + " has no " + api + " ability");
    }

    @Test(timeOut = 120000)
    public void theAiDrawsWithTopInResponseToItsOwnReorder() {
        final Card top = reorderWithTop();
        final List<SpellAbility> choice = ai.getController().chooseSpellAbilityToPlay();
        assertTrue(choice != null && !choice.isEmpty(), "the AI responds to its own ability");
        assertEquals(choice.get(0).getHostCard(), top);
        assertEquals(choice.get(0).getApi(), ApiType.Draw);
    }

    @Test(timeOut = 120000)
    public void bothTopAbilitiesResolveWhenTheAiRespondsToItself() {
        final Card top = reorderWithTop();
        final Card nextDraw = ai.getCardsIn(ZoneType.Library).get(0);
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        final int library = ai.getCardsIn(ZoneType.Library).size();
        final List<String> seen = new ArrayList<>();
        final PlayerControllerAi watching = new PlayerControllerAi(game, ai, ai.getLobbyPlayer()) {
            @Override
            public List<SpellAbility> chooseSpellAbilityToPlay() {
                final List<SpellAbility> choice = super.chooseSpellAbilityToPlay();
                seen.add(game.getStack().size() + " " + game.getStack().peekAbility().getApi()
                        + ", Top in " + game.getCardState(top).getZone().getZoneType()
                        + " -> " + (choice == null || choice.isEmpty() ? "pass" : choice.get(0).getApi()));
                return choice;
            }
        };
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);

        ai.runWithController(() -> FlatMonteCarloPicker.resolveWithResponses(game, ai, cfg, 0), watching);

        assertEquals(seen, Arrays.asList(
                "1 RearrangeTopOfLibrary, Top in Battlefield -> Draw",
                "2 Draw, Top in Battlefield -> pass",
                // the draw has resolved, and the reorder is still waiting
                "1 RearrangeTopOfLibrary, Top in Library -> pass"));
        assertTrue(game.getStack().isEmpty(), "the reorder resolved too");
        assertEquals(ai.getCardsIn(ZoneType.Hand).size(), hand + 1);
        assertEquals(game.getCardState(nextDraw).getZone().getZoneType(), ZoneType.Hand,
                "the card on top before the reorder was drawn");
        // Top went on top of the library; the reorder then arranged the top three, Top among them
        final List<Card> topThree = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            topThree.add(ai.getCardsIn(ZoneType.Library).get(i));
        }
        assertTrue(topThree.contains(game.getCardState(top)), "Top is among the top three cards: " + topThree);
        // one card drawn and Top added
        assertEquals(ai.getCardsIn(ZoneType.Library).size(), library);
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
