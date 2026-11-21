package forge.ai;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.PlaySpellAbility;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static org.testng.AssertJUnit.*;

public class CastRestrictionTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 120_000;

    private Game newFlatMcGame() {
        final Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_FLAT_MONTE_CARLO_MAX = true;
        game.AI_TIMEOUT = 20;

        final Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        game.getPlayers().get(0).setTeam(1);

        final AiController brain = ((PlayerControllerAi) ai.getController()).getAi();
        brain.setFlatMonteCarloOverride(true);
        assertTrue("FlatMC must be enabled for this regression test",
                brain.flatMonteCarloActive());

        return game;
    }

    private SpellAbility getSpell(final Card card, final Player caster) {
        for (final SpellAbility sa : card.getSpellAbilities()) {
            if (sa.isSpell()) {
                sa.setActivatingPlayer(caster);
                return sa;
            }
        }
        fail("No spell ability found for " + card.getName());
        return null;
    }

    /**
     * Gives the AI a genuinely castable, potentially lethal spell. Keeping the opponent at three life means
     * Lightning Bolt is a play the AI should make when no restriction prevents it.
     */
    private Card giveAiLightningBolt(final Game game) {
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);

        opponent.setLife(3, null);
        addCard("Mountain", ai);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);

        // Keep both libraries populated for the real phase loop and any FlatMC game copies.
        for (int i = 0; i < 10; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
            addCardToZone("Plains", opponent, ZoneType.Library);
        }
        return bolt;
    }

    /**
     * Run the AI's real main-phase decision loop with FlatMC enabled. The assertion is against the actual card
     * zone after the AI has received priority, not against a static-ability helper.
     */
    private void assertAiDoesNotCast(final Game game, final Player ai, final Card prohibitedSpell) {
        final AiController brain = ((PlayerControllerAi) ai.getController()).getAi();
        assertTrue("The test must exercise the FlatMC-enabled AI", brain.flatMonteCarloActive());

        game.getAction().checkStateEffects(true);

        // Check the actual AI/FlatMC choice as well as the result of the real turn. Otherwise an illegal choice
        // rejected only later by the rules engine could leave the card in hand and make this test pass falsely.
        final List<SpellAbility> chosen = ai.getController().chooseSpellAbilityToPlay();
        final boolean selectedProhibitedSpell = chosen != null && chosen.stream()
                .anyMatch(sa -> sa.isSpell() && sa.getHostCard() == prohibitedSpell);
        assertFalse("FlatMC must not select prohibited spell " + prohibitedSpell.getName(),
                selectedProhibitedSpell);

        playUntilNextTurn(game);

        assertTrue("AI illegally cast " + prohibitedSpell.getName()
                        + " despite the active casting restriction",
                prohibitedSpell.isInZone(ZoneType.Hand));
        assertTrue("The AI's main phase should leave no spell on the stack", game.getStack().isEmpty());
    }

    private void castAndResolveOrimsChant(final Game game, final Player caster, final Player target) {
        addCard("Plains", caster);
        final Card chant = addCardToZone("Orim's Chant", caster, ZoneType.Hand);
        final SpellAbility chantSpell = getSpell(chant, caster);

        // Model the active player passing priority to the opponent so the opponent can cast the instant.
        game.getPhaseHandler().setPriority(caster);

        // Cast the real card through the normal spell-play pipeline. The casting player's AI chooses the opponent
        // as the target; resolution creates the temporary CantBeCast effect from Orim's Chant's card script.
        assertTrue("Orim's Chant should be legally castable", chantSpell.canPlay());
        assertTrue("Orim's Chant should be put on the stack",
                PlaySpellAbility.playSpellAbility(caster.getController(), caster, chantSpell));
        assertFalse("Orim's Chant must have been cast", game.getStack().isEmpty());
        assertTrue("Orim's Chant must target the AI whose turn is in progress",
                chantSpell.getTargets().contains(target));

        int resolveLimit = 4;
        while (!game.getStack().isEmpty() && resolveLimit-- > 0) {
            game.getStack().resolveStack();
        }
        assertTrue("Orim's Chant and any resulting triggers should resolve",
                game.getStack().isEmpty());
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcAiCastsLightningBoltWhenNoRestrictionApplies() {
        final Game game = newFlatMcGame();
        final Card bolt = giveAiLightningBolt(game);

        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        assertFalse("Control case: the AI should cast its lethal Lightning Bolt",
                bolt.isInZone(ZoneType.Hand));
        assertEquals("Lightning Bolt should leave the hand",
                0, countCardsWithName(game, "Lightning Bolt", ZoneType.Hand));
        assertEquals("Lightning Bolt should resolve into the graveyard.\n"
                        + gameStateToString(game),
                1, countCardsWithName(game, "Lightning Bolt", ZoneType.Graveyard));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcAiDoesNotCastRedSpellWhileIonaIsOnBattlefield() {
        final Game game = newFlatMcGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);

        final Card iona = addCard("Iona, Shield of Emeria", opponent);
        iona.setChosenColors(Collections.singletonList("Red"));

        final Card bolt = giveAiLightningBolt(game);
        assertAiDoesNotCast(game, ai, bolt);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcAiDoesNotCastNamedSpellWhileMeddlingMageIsOnBattlefield() {
        final Game game = newFlatMcGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);

        final Card mage = addCard("Meddling Mage", opponent);
        mage.setNamedCards(Collections.singletonList("Lightning Bolt"));

        final Card bolt = giveAiLightningBolt(game);
        assertAiDoesNotCast(game, ai, bolt);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcAiDoesNotCastSpellsAfterOrimsChantResolvesAgainstIt() {
        final Game game = newFlatMcGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);

        final Card bolt = giveAiLightningBolt(game);
        castAndResolveOrimsChant(game, opponent, ai);

        assertAiDoesNotCast(game, ai, bolt);
    }
}
