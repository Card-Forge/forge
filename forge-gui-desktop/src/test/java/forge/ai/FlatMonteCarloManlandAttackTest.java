package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.AnimateAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * A creature land (Mutavault) and Flat Monte Carlo.
 *
 * <p>Report: on its own turn the AI paid {1} to animate Mutavault, the attack arrows to the opponent showed up in the
 * declare attackers step, and then the land stayed home: the mana was wasted. Cause: the animation is approved by the
 * heuristics (AnimateAi: "this creature would attack"), but the declaration that follows is made by the full attack
 * controller and then by the Monte Carlo attack search, whose "do not attack" candidate is judged with the mana already
 * spent. Either could talk the AI out of the attack the animation was bought for.
 *
 * <p>These tests play the AI's turn through the real phase handler, so the animation and the declaration are the real
 * ones. Reading a failure: "attacked" means the land was animated and the attack was withdrawn (the bug); "wasted" means
 * the AI spent mana on a land that had no business attacking.
 */
public class FlatMonteCarloManlandAttackTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 240_000;
    private static final int MAX_STEPS = 400;

    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_FLAT_MONTE_CARLO_MAX = true; // a generous budget: the tests are about the choice, not about speed
        game.AI_TIMEOUT = 20;
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
    }

    private Card readyVault() {
        final Card vault = addCard("Mutavault", ai);
        vault.setSickness(false);
        return vault;
    }

    private void startAiTurn() {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
    }

    /**
     * Plays the AI's turn step by step (priority, animation, declaration, blocks) and returns the combat as it stands
     * once the blockers are declared, or null when the AI reached its second main phase without any combat.
     */
    private Combat playToBlocks() {
        final PhaseHandler ph = game.getPhaseHandler();
        for (int i = 0; i < MAX_STEPS && !game.isGameOver(); i++) {
            ph.mainLoopStep();
            if (ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS) && game.getCombat() != null
                    && !game.getCombat().getAttackers().isEmpty()) {
                return game.getCombat();
            }
            if (ph.is(PhaseType.MAIN2) || !ph.isPlayerTurn(ai)) {
                return null;
            }
        }
        return null;
    }

    /** Safe attack: the opponent has nothing to block with, so the animation pays off at once. */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void animatedManlandAttacksAnEmptyBoard() {
        setUp();
        addCard("Mountain", ai);
        final Card vault = readyVault();
        startAiTurn();

        final Combat combat = playToBlocks();
        AssertJUnit.assertNotNull("the AI neither animated Mutavault nor attacked an empty board", combat);
        AssertJUnit.assertTrue("Mutavault was animated and then withdrawn from the attack (wasted {1}): "
                + combat.getAttackers(), combat.isAttacking(vault));
        AssertJUnit.assertEquals("it attacks the opponent", opp, combat.getDefenderPlayerByAttacker(vault));
        AssertJUnit.assertFalse("nobody can block it", combat.isBlocked(vault));
    }

    /** The mana must not be spent when the land has no business in combat: a 6/4 blocker eats a 2/2 Mutavault for free. */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void manlandIsNotAnimatedIntoABigBlocker() {
        setUp();
        final Card mountain = addCard("Mountain", ai);
        final Card vault = readyVault();
        addCard("Craw Wurm", opp).setSickness(false);
        startAiTurn();

        final Combat combat = playToBlocks();
        AssertJUnit.assertTrue("a 2/2 land went into a 6/4: " + (combat == null ? "" : combat.getAttackers()),
                combat == null || !combat.isAttacking(vault));
        AssertJUnit.assertFalse("wasted: the AI tapped a land to animate Mutavault and then did not attack",
                mountain.isTapped() && !vault.isTapped());
        AssertJUnit.assertFalse("Mutavault is still an ordinary, untapped land", vault.isCreature() && !vault.isTapped());
    }

    /**
     * Whatever the AI decides about a land that can attack, the two halves of the decision agree: if it paid to animate
     * Mutavault (the Mountain is tapped and the land is a creature or has been tapped by attacking), then it attacked.
     */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void animationAndAttackAreOneDecision() {
        setUp();
        final Card mountain = addCard("Mountain", ai);
        final Card vault = readyVault();
        addCard("Grizzly Bears", opp).setSickness(false);
        startAiTurn();

        final Combat combat = playToBlocks();
        final boolean paidToAnimate = mountain.isTapped();
        final boolean attacked = combat != null && combat.isAttacking(vault);
        AssertJUnit.assertEquals("animated (Mountain tapped: " + paidToAnimate + ") but attacked: " + attacked
                + " -- mana spent without an attack, or an attack that was never paid for", paidToAnimate, attacked);
    }

    /**
     * A trick in hand: AnimateAi does not know about tricks, so an even trade (2/2 land into a 2/2) is not worth {1} and
     * the AI may leave the land alone. What must hold either way: no mana is spent for nothing, and when the land does
     * attack, the opponent is offered the block (the bait).
     */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void manlandWithATrickIsEitherSkippedOrBaitsABlock() {
        setUp();
        final Card forest1 = addCard("Forest", ai);
        final Card forest2 = addCard("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card vault = readyVault();
        final Card bears = addCard("Grizzly Bears", opp);
        bears.setSickness(false);
        startAiTurn();

        final Combat combat = playToBlocks();
        if (combat == null) {
            AssertJUnit.assertFalse("animated Mutavault and then did not attack: wasted mana",
                    forest1.isTapped() || forest2.isTapped());
            return;
        }
        AssertJUnit.assertTrue("Mutavault was animated and withdrawn from the attack: " + combat.getAttackers(),
                combat.isAttacking(vault));
        if (!combat.isBlocked(vault)) {
            return; // not blocked: two free damage, a fine outcome (the opponent is not obliged to take the bait)
        }
        // blocked: the trick has to win the fight, the land survives and the Bears do not
        final PhaseHandler ph = game.getPhaseHandler();
        for (int i = 0; i < MAX_STEPS && !game.isGameOver() && !ph.is(PhaseType.MAIN2); i++) {
            ph.mainLoopStep();
        }
        AssertJUnit.assertTrue("Mutavault was blocked and the AI did not win the fight with Giant Growth",
                vault.isInZone(ZoneType.Battlefield) && !bears.isInZone(ZoneType.Battlefield));
    }

    // ------------------------------------------------------------------------------------------------ bait with a trick

    private SpellAbility animateOf(final Card land) {
        for (final SpellAbility sa : land.getSpellAbilities()) {
            if (sa.isAbility() && sa.getApi() == ApiType.Animate) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no animate ability on " + land);
    }

    /** Mutavault 2/2 + Giant Growth (+3/+3) against one Grizzly Bears 2/2: the pumped land wins the fight. */
    @Test
    public void trickThatWinsTheFightMakesTheManlandWorthAnimating() {
        setUp();
        addCard("Forest", ai);
        addCard("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final SpellAbility animate = animateOf(readyVault());
        addCard("Grizzly Bears", opp).setSickness(false);
        startAiTurn();
        AssertJUnit.assertTrue("{1} to animate, {G} left for Giant Growth, and the pumped 5/5 beats the Bears",
                AnimateAi.hasBackingTrick(ai, animate));
    }

    @Test
    public void noTrickNoBait() {
        setUp();
        addCard("Forest", ai);
        addCard("Forest", ai);
        final SpellAbility animate = animateOf(readyVault());
        addCard("Grizzly Bears", opp).setSickness(false);
        startAiTurn();
        AssertJUnit.assertFalse("nothing in hand: an even trade is not worth the mana", AnimateAi.hasBackingTrick(ai, animate));
    }

    @Test
    public void noManaLeftForTheTrickNoBait() {
        setUp();
        addCard("Forest", ai); // pays for the animation, nothing is left for the trick (the land attacks)
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final SpellAbility animate = animateOf(readyVault());
        addCard("Grizzly Bears", opp).setSickness(false);
        startAiTurn();
        AssertJUnit.assertFalse("animating would leave no mana for the trick", AnimateAi.hasBackingTrick(ai, animate));
    }

    @Test
    public void trickThatDoesNotWinTheFightNoBait() {
        setUp();
        addCard("Forest", ai);
        addCard("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final SpellAbility animate = animateOf(readyVault());
        addCard("Craw Wurm", opp).setSickness(false); // 6/4: the pumped 5/5 kills it but dies too
        startAiTurn();
        AssertJUnit.assertFalse("a 5/5 land still dies to a 6/4", AnimateAi.hasBackingTrick(ai, animate));
    }

    @Test
    public void twoPossibleBlockersNoBait() {
        setUp();
        addCard("Forest", ai);
        addCard("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final SpellAbility animate = animateOf(readyVault());
        addCard("Grizzly Bears", opp).setSickness(false);
        addCard("Grizzly Bears", opp).setSickness(false);
        startAiTurn();
        AssertJUnit.assertFalse("a double block beats the trick", AnimateAi.hasBackingTrick(ai, animate));
    }
}
