package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMonteCarloPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Answers to the opponent's removal or burn that is on the stack: regeneration, protection (see
 * {@link FlatMonteCarloPicker#isSavingPlay}). Rules first, then the AI's own decision with the spell on the stack.
 */
public class FlatMonteCarloSavingTest extends AITest {
    private static final int LONG = 240_000;

    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
    }

    private Card ready(final String name, final Player owner) {
        final Card c = addCardToZone(name, owner, ZoneType.Battlefield);
        c.setSickness(false);
        return c;
    }

    private SpellAbility activated(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no activated ability on " + c);
    }

    /** The opponent has cast Lightning Bolt at the target, and it waits on the stack. */
    private void boltAt(final Card target) {
        final Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        final SpellAbility sa = bolt.getFirstSpellAbility();
        sa.setActivatingPlayer(opp);
        final Card onStack = game.getAction().moveToStack(bolt, sa);
        sa.setHostCard(onStack);
        sa.getTargets().add(target);
        game.getStack().add(sa);
    }

    @Test
    public void regenerationIsForTheCreatureTheBoltIsAimedAt() {
        setUp();
        addCards("Swamp", 1, ai);
        final Card skeletons = ready("Drudge Skeletons", ai);
        boltAt(skeletons);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isSavingPlay(ai, activated(skeletons)));
    }

    @Test
    public void regenerationIsNotForAnotherCreature() {
        setUp();
        addCards("Swamp", 1, ai);
        final Card skeletons = ready("Drudge Skeletons", ai);
        boltAt(ready("Grizzly Bears", ai));
        AssertJUnit.assertFalse("the Bolt is not aimed at the Skeletons", FlatMonteCarloPicker.isSavingPlay(ai, activated(skeletons)));
    }

    @Test
    public void nothingIsSavedWithAnEmptyStack() {
        setUp();
        addCards("Swamp", 1, ai);
        AssertJUnit.assertFalse(FlatMonteCarloPicker.isSavingPlay(ai, activated(ready("Drudge Skeletons", ai))));
    }

    @Test
    public void protectionIsAimedAtTheThreatenedCreature() {
        setUp();
        final Card mother = ready("Mother of Runes", ai);
        final Card bears = ready("Grizzly Bears", ai);
        ready("Hill Giant", ai);
        boltAt(bears);
        final SpellAbility protect = activated(mother);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isSavingPlay(ai, protect));
        AssertJUnit.assertEquals(bears, FlatMonteCarloPicker.savingTarget(ai, protect));
    }

    @Test(timeOut = LONG)
    public void theSkeletonsRegenerateInResponseToTheBolt() {
        setUp();
        addCards("Swamp", 1, ai);
        final Card skeletons = ready("Drudge Skeletons", ai);
        boltAt(skeletons);
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        AssertJUnit.assertTrue("{B}: regenerate saves the creature (the AI chose " + chosen + ")",
                chosen != null && !chosen.isEmpty() && chosen.get(0).getHostCard().equals(skeletons));
    }

    @Test(timeOut = LONG)
    public void theSkeletonsDoNotRegenerateForAnotherCreature() {
        setUp();
        addCards("Swamp", 1, ai);
        final Card skeletons = ready("Drudge Skeletons", ai);
        boltAt(ready("Grizzly Bears", ai));
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        AssertJUnit.assertFalse("the mana is wasted on a creature that is not threatened",
                chosen != null && !chosen.isEmpty() && chosen.get(0).getHostCard().equals(skeletons));
    }
}
