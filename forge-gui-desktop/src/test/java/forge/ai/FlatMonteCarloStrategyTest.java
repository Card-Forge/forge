package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMonteCarloStats;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Decision tests for the strategic side of the Monte Carlo AI: does it use card draw and selection, does it keep up
 * the right instant, does it answer the real threat, does it pick a sensible land and a creature that matters, and does
 * it act on the opponent's turn. Each test builds a small position and asks the AI for its next play the way the game
 * does, so they check the behaviour a player would see.
 */
public class FlatMonteCarloStrategyTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 240_000;

    private Game newGame(final boolean monteCarlo) {
        final Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = monteCarlo;
        game.AI_TIMEOUT = 10;
        game.getPlayers().get(1).setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        return game;
    }

    private static AiController brainOf(final Player p) {
        return ((PlayerControllerAi) p.getController()).getAi();
    }

    private void fill(final Player p, final String... names) {
        for (int i = 0; i < 10; i++) {
            for (final String n : names) {
                addCardToZone(n, p, ZoneType.Library);
            }
        }
    }

    /** What the AI does with priority right now: the host card of its next play, or null when it passes. */
    private static String nextPlay(final Player ai) {
        final List<SpellAbility> chosen = ai.getController() instanceof PlayerControllerAi
                ? ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay() : null;
        if (System.getProperty("flatmc.debug") != null) {
            System.out.println("   result: " + brainOf(ai).getLastFlatMonteCarloResult());
        }
        return chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
    }

    /** Like {@link #nextPlay}: the host card and how it is paid for, or null when the AI passes. */
    private static String nextPlayWithCost(final Player ai) {
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        return chosen == null || chosen.isEmpty() ? null
                : chosen.get(0).getHostCard().getName() + " [" + chosen.get(0).getPayCosts().getTotalMana() + " | " + chosen.get(0).getPayCosts() + "]";
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void cantripIsCastWithSpareMana() {
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        addCards("Island", 3, ai);
        addCardToZone("Opt", ai, ZoneType.Hand);
        fill(ai, "Island", "Grizzly Bears", "Air Elemental");
        fill(game.getPlayers().get(0), "Mountain");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        final String play = nextPlay(ai);
        System.out.println("cantripIsCastWithSpareMana -> " + play + " " + FlatMonteCarloStats.summary().split("\n")[1]);
        AssertJUnit.assertEquals("with three open Islands and nothing else to do the AI should cast Opt", "Opt", play);
    }

    // ------------------------------------------------------------------ helpers for the opponent's turn

    /** Makes the opponent (the other player) cast the card for real, leaving the spell on the stack. */
    private static void opponentCasts(final Game game, final Player opp, final String cardName) {
        opponentCasts(game, opp, cardName, null);
    }

    /** As above, aimed at the given target when the spell needs one. */
    private static void opponentCasts(final Game game, final Player opp, final String cardName, final forge.game.GameEntity target) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);
        final PlayerControllerAi pc = (PlayerControllerAi) opp.getController();
        for (final SpellAbility sa : forge.ai.ComputerUtilAbility.getSpellAbilities(forge.ai.ComputerUtilAbility.getAvailableCards(game, opp), opp)) {
            if (sa.getHostCard().getName().equals(cardName) && sa.isSpell()) {
                sa.setActivatingPlayer(opp);
                if (target != null && sa.usesTargeting()) {
                    sa.resetTargets();
                    sa.getTargets().add(target);
                }
                pc.playChosenSpellAbility(sa);
                return;
            }
        }
        throw new IllegalStateException("opponent cannot cast " + cardName);
    }

    private static void setPhase(final Game game, final PhaseType phase, final Player turnPlayer) {
        game.getPhaseHandler().devModeSet(phase, turnPlayer);
        game.getAction().checkStateEffects(true);
    }

    // ------------------------------------------------------------------ opponent's end step

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void drawsACardAtTheOpponentsEndStep() {
        final Game game = newGame(true);
        game.AI_FLAT_MONTE_CARLO_MAX = true; // a whole simulated turn per sample: generous budget, so a busy machine cannot starve it
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCardToZone("Opt", ai, ZoneType.Hand);
        fill(ai, "Island", "Grizzly Bears", "Air Elemental");
        fill(opp, "Mountain");
        setPhase(game, PhaseType.END_OF_TURN, opp);
        final String play = nextPlay(ai);
        System.out.println("drawsACardAtTheOpponentsEndStep -> " + play);
        AssertJUnit.assertEquals("unused mana at the opponent's end step should be turned into a card", "Opt", play);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void doesNotWasteTheCounterspellOnItsOwnEndStepDraw() {
        // Counterspell is the only instant: at the opponent's end step holding it is pointless (mana untaps), but
        // there is nothing else to do, and it must not be cast at nothing.
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        fill(ai, "Island");
        fill(opp, "Mountain");
        setPhase(game, PhaseType.END_OF_TURN, opp);
        final String play = nextPlay(ai);
        System.out.println("doesNotWasteTheCounterspell -> " + play);
        AssertJUnit.assertNull("a counterspell with nothing to counter is not cast", play);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void drawSpellIsPreferredOverPassingWithManaToSpare() {
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Island", 4, ai);
        addCardToZone("Brainstorm", ai, ZoneType.Hand);
        fill(ai, "Island", "Air Elemental");
        fill(opp, "Mountain");
        setPhase(game, PhaseType.END_OF_TURN, opp);
        final String play = nextPlay(ai);
        System.out.println("drawSpellIsPreferredOverPassing -> " + play);
        AssertJUnit.assertEquals("Brainstorm", play);
    }

    // ------------------------------------------------------------------ responses

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void countersABigSpell() {
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        fill(ai, "Island");
        addCards("Plains", 5, opp);
        addCardToZone("Serra Angel", opp, ZoneType.Hand);
        fill(opp, "Plains");
        opponentCasts(game, opp, "Serra Angel");
        AssertJUnit.assertFalse("the spell must be on the stack", game.getStack().isEmpty());
        final String play = nextPlay(ai);
        System.out.println("countersABigSpell -> " + play);
        AssertJUnit.assertEquals("Counterspell", play);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void drawsIntoTheCounterspellInResponseToAThreat() {
        // Five Islands, Brainstorm in hand, a library full of counterspells and Islands: cast Brainstorm first and
        // counter with what it finds. Neither line works with the heuristics alone.
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Island", 5, ai);
        addCardToZone("Brainstorm", ai, ZoneType.Hand);
        fill(ai, "Counterspell", "Counterspell", "Island");
        addCards("Plains", 5, opp);
        addCardToZone("Serra Angel", opp, ZoneType.Hand);
        fill(opp, "Plains");
        opponentCasts(game, opp, "Serra Angel");
        final String play = nextPlay(ai);
        System.out.println("drawsIntoTheCounterspell -> " + play);
        AssertJUnit.assertEquals("Brainstorm", play);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void answersLethalAttackWithRemoval() {
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        ai.setLife(3, null);
        addCards("Swamp", 2, ai);
        addCardToZone("Doom Blade", ai, ZoneType.Hand);
        fill(ai, "Swamp");
        final forge.game.card.Card attacker = addCard("Hill Giant", opp);
        attacker.setSickness(false);
        fill(opp, "Mountain");
        setPhase(game, PhaseType.COMBAT_DECLARE_ATTACKERS, opp);
        final forge.game.combat.Combat combat = new forge.game.combat.Combat(opp);
        combat.addAttacker(attacker, ai);
        game.getPhaseHandler().setCombat(combat);
        final String play = nextPlay(ai);
        System.out.println("answersLethalAttackWithRemoval -> " + play);
        AssertJUnit.assertEquals("facing lethal damage the AI must use its removal", "Doom Blade", play);
    }

    // ------------------------------------------------------------------ lands

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void playsTheUntappedLandWhenASpellNeedsTheMana() {
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        addCards("Island", 1, ai);
        addCardToZone("Tranquil Cove", ai, ZoneType.Hand);  // enters tapped
        addCardToZone("Island", ai, ZoneType.Hand);
        addCardToZone("Coral Merfolk", ai, ZoneType.Hand); // 1U 2/1
        fill(ai, "Island");
        fill(game.getPlayers().get(0), "Mountain");
        setPhase(game, PhaseType.MAIN1, ai);
        final String play = nextPlay(ai);
        System.out.println("playsTheUntappedLand -> " + play);
        AssertJUnit.assertEquals("the untapped Island lets the AI cast its two drop this turn", "Island", play);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void playsTheTappedLandWhenThereIsNothingToCast() {
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        addCards("Island", 2, ai);
        addCardToZone("Tranquil Cove", ai, ZoneType.Hand);
        addCardToZone("Island", ai, ZoneType.Hand);
        addCardToZone("Air Elemental", ai, ZoneType.Hand); // 5 mana: not castable this turn either way
        fill(ai, "Island");
        fill(game.getPlayers().get(0), "Mountain");
        setPhase(game, PhaseType.MAIN1, ai);
        final String play = nextPlay(ai);
        System.out.println("playsTheTappedLand -> " + play);
        AssertJUnit.assertEquals("with nothing to cast the tapped land costs nothing", "Tranquil Cove", play);
    }

    // ------------------------------------------------------------------ creatures and answers

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void killsTheThreatInsteadOfAddingAWeakBody() {
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Swamp", 4, ai);
        addCardToZone("Doom Blade", ai, ZoneType.Hand);
        addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
        fill(ai, "Swamp");
        addCard("Serra Angel", opp);
        fill(opp, "Plains");
        setPhase(game, PhaseType.MAIN1, ai);
        final String play = nextPlay(ai);
        System.out.println("killsTheThreat -> " + play);
        AssertJUnit.assertEquals("a 2/2 does not matter next to a 4/4 flyer: remove the flyer first", "Doom Blade", play);
    }

    // ------------------------------------------------------------------ holding up interaction

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void tapsOutWhenTheOpponentHasNoCardsToCounter() {
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        addCards("Island", 2, ai);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        addCardToZone("Merfolk of the Pearl Trident", ai, ZoneType.Hand);
        fill(ai, "Island");
        fill(game.getPlayers().get(0), "Mountain");
        game.getPlayers().get(0).getZone(ZoneType.Hand).removeAllCards(true);
        setPhase(game, PhaseType.MAIN2, ai);
        final String play = nextPlay(ai);
        System.out.println("tapsOutWhenTheOpponentHasNoCards -> " + play);
        AssertJUnit.assertEquals("a counterspell against an empty hand is dead mana: develop the board", "Merfolk of the Pearl Trident", play);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void spendsSpareManaOnACantripButKeepsTheCounterspellMana() {
        // Three Islands, Counterspell and Opt, a full-handed opponent: Opt costs one, which leaves UU for the counter.
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        addCardToZone("Opt", ai, ZoneType.Hand);
        fill(ai, "Island", "Grizzly Bears");
        fill(opp, "Mountain");
        for (int i = 0; i < 5; i++) {
            addCardToZone("Serra Angel", opp, ZoneType.Hand);
        }
        setPhase(game, PhaseType.MAIN2, ai);
        final String play = nextPlay(ai);
        System.out.println("spendsSpareMana -> " + play);
        AssertJUnit.assertEquals("Opt first (it still leaves the mana for Counterspell), never Counterspell itself", "Opt", play);
    }

    // ------------------------------------------------------------------ tempo at the end step

    @Test(timeOut = LONG_TIMEOUT_MS) // TODO: verify if this is always deterministic now with the updated patch
    public void bouncesABlockerAtTheEndStepToAttackIntoAnOpenBoard() {
        final Game game = newGame(true);
        // A whole simulated turn per sample is the heaviest rollout there is: use the generous desktop budget so that a
        // busy machine (parallel tests) cannot starve the search and make the AI fall back to passing.
        game.AI_FLAT_MONTE_CARLO_MAX = true;
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        final forge.game.card.Card mine = addCard("Air Elemental", ai);
        mine.setSickness(false);
        addCardToZone("Unsummon", ai, ZoneType.Hand);
        // The three drop uses all the mana of the next turn: bouncing at the end step is what makes both possible.
        addCardToZone("Wind Drake", ai, ZoneType.Hand);
        fill(ai, "Giant Growth"); // no land to draw: next turn has the same three mana
        final forge.game.card.Card blocker = addCard("Serra Angel", opp);
        blocker.setSickness(false);
        fill(opp, "Plains");
        setPhase(game, PhaseType.END_OF_TURN, opp);
        final String play = nextPlay(ai);
        System.out.println("bouncesABlockerAtTheEndStep -> " + play);
        AssertJUnit.assertEquals("bouncing their only flyer clears the way for the attack next turn", "Unsummon", play);
    }

    // ------------------------------------------------------------------ alternative costs

    /** The opponent casts the spell into the AI's open hand; the AI has the given lands, hand and life. */
    private String respondsTo(final boolean monteCarlo, final String oppSpell, final int oppLands, final String oppLandName,
                              final int life, final int islands, final String... hand) {
        final Game game = newGame(monteCarlo);
        game.AI_FLAT_MONTE_CARLO_MAX = monteCarlo; // the reactive search must not be starved by a busy machine
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        ai.setLife(life, null);
        if (islands > 0) {
            addCards("Island", islands, ai);
        }
        for (final String h : hand) {
            addCardToZone(h, ai, ZoneType.Hand);
        }
        fill(ai, "Island");
        addCards(oppLandName, oppLands, opp);
        addCardToZone(oppSpell, opp, ZoneType.Hand);
        fill(opp, oppLandName);
        opponentCasts(game, opp, oppSpell, ai);
        AssertJUnit.assertFalse("the spell must be on the stack", game.getStack().isEmpty());
        final String play = nextPlayWithCost(ai);
        System.out.println("respondsTo(" + oppSpell + ", life " + life + ", " + islands + " Islands, " + java.util.Arrays.toString(hand) + ") -> " + play);
        return play;
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void forceOfWillStopsLethalBurnForItsAlternativeCost() {
        // No mana at all, at 3 life: Lightning Bolt kills. The heuristics only counter spells worth three mana or more,
        // and the printed cost cannot be paid: only rollouts of the alternative cost (a life and a blue card) see the way out.
        final String play = respondsTo(true, "Lightning Bolt", 3, "Mountain", 3, 0, "Force of Will", "Brainstorm");
        AssertJUnit.assertNotNull("the AI must not let lethal burn resolve", play);
        AssertJUnit.assertTrue(play, play.startsWith("Force of Will [{0}"));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void keepsForceOfWillAndTheCardAgainstHarmlessBurn() {
        AssertJUnit.assertNull("two cards for a Bolt at 20 life is a bad trade",
                respondsTo(true, "Lightning Bolt", 3, "Mountain", 20, 0, "Force of Will", "Brainstorm"));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void forceOfWillCountersABigSpellWithoutMana() {
        final String play = respondsTo(true, "Serra Angel", 5, "Plains", 20, 0, "Force of Will", "Brainstorm");
        AssertJUnit.assertNotNull(play);
        AssertJUnit.assertTrue(play, play.startsWith("Force of Will"));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void paysManaForForceOfWillWhenItHasTheMana() {
        final String play = respondsTo(true, "Serra Angel", 5, "Plains", 20, 5, "Force of Will", "Brainstorm");
        AssertJUnit.assertNotNull(play);
        AssertJUnit.assertTrue("with five Islands there is no reason to also lose a card: " + play, play.startsWith("Force of Will [{3}{U}{U}"));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void dazeCountersLethalBurnWhenTheOpponentCannotPay() {
        // The opponent tapped its only Mountain for the Bolt: it cannot pay for Daze.
        final String play = respondsTo(true, "Lightning Bolt", 1, "Mountain", 3, 1, "Daze");
        AssertJUnit.assertNotNull(play);
        AssertJUnit.assertTrue(play, play.startsWith("Daze"));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void fireblastFinishesTheOpponentOffForItsAlternativeCost() {
        final String play = probeMain(true, 4, new String[] { "Mountain", "Mountain" }, new String[] { "Fireblast" });
        AssertJUnit.assertNotNull(play);
        AssertJUnit.assertTrue(play, play.startsWith("Fireblast"));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void keepsFireblastAndTheLandsWhenItIsNotLethal() {
        AssertJUnit.assertNull("sacrificing two lands for four damage at 20 life is a bad trade",
                probeMain(true, 20, new String[] { "Mountain", "Mountain" }, new String[] { "Fireblast" }));
    }

    private String probeMain(final boolean monteCarlo, final int oppLife, final String[] lands, final String[] hand) {
        final Game game = newGame(monteCarlo);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        opp.setLife(oppLife, null);
        for (final String l : lands) {
            addCard(l, ai).setSickness(false);
        }
        for (final String h : hand) {
            addCardToZone(h, ai, ZoneType.Hand);
        }
        fill(ai, "Island");
        fill(opp, "Plains");
        setPhase(game, PhaseType.MAIN2, ai);
        return nextPlayWithCost(ai);
    }

    // ------------------------------------------------------------------ lethal

    @Test(timeOut = LONG_TIMEOUT_MS) // TODO: verify if this is always deterministic now with the updated patch
    public void takesLethalDamageOverEverythingElse() {
        final Game game = newGame(true);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        opp.setLife(3, null);
        addCards("Mountain", 3, ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Divination", ai, ZoneType.Hand);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        fill(ai, "Mountain");
        fill(opp, "Plains");
        setPhase(game, PhaseType.MAIN1, ai);
        final String play = nextPlay(ai);
        System.out.println("takesLethalDamage -> " + play);
        AssertJUnit.assertEquals("three damage to a three life opponent wins the game", "Lightning Bolt", play);
    }
}
