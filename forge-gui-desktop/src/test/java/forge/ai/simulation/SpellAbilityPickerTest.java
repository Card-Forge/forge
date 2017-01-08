package forge.ai.simulation;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SpellAbilityPickerTest extends SimulationTestCase {
    public void testPickingLethalDamage() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);
        opponent.setLife(2, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        assertNotNull(sa);
        assertNull(sa.getTargetCard());
        assertEquals(opponent, sa.getTargets().getFirstTargetedPlayer());
    }

    public void testPickingKillingCreature() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        Card bearCard = addCard("Runeclaw Bear", opponent);
        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        assertNotNull(sa);
        assertEquals(bearCard, sa.getTargetCard());
        assertNull(sa.getTargets().getFirstTargetedPlayer());
    }

    public void testSequenceStartingWithPlayingLand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card mountain = addCardToZone("Mountain", p, ZoneType.Hand);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);
        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        assertEquals(game.PLAY_LAND_SURROGATE, sa);
        assertEquals(mountain, sa.getHostCard());

        Plan plan = picker.getPlan();
        assertEquals(2, plan.getDecisions().size());
        assertEquals("Play land Mountain", plan.getDecisions().get(0).saRef.toString());
        assertEquals("Shock deals 2 damage to target creature or player.", plan.getDecisions().get(1).saRef.toString());
        assertTrue(plan.getDecisions().get(1).targets.toString().contains("Runeclaw Bear"));
    }

    public void testMultipleModes() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        addCard("Mountain", p);
        addCard("Mountain", p);
        Card spell = addCardToZone("Fiery Confluence", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);
        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // Expected: 2x 1 damage to each creature, 1x 2 damage to each opponent.
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        assertEquals(spell.getSpellAbilities().get(0), sa);

        String dmgCreaturesStr = "Fiery Confluence deals 1 damage to each creature.";
        String dmgOppStr = "Fiery Confluence deals 2 damage to each opponent.";
        String expected = "Fiery Confluence -> " + dmgCreaturesStr + " " + dmgCreaturesStr + " " + dmgOppStr;
        assertEquals(expected, picker.getPlan().getDecisions().get(0).modesStr);
    }

    public void testMultipleModes2() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        addCard("Mountain", p);
        addCard("Mountain", p);
        Card spell = addCardToZone("Fiery Confluence", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);
        opponent.setLife(6, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // Expected: 3x 2 damage to each opponent.
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        assertEquals(spell.getSpellAbilities().get(0), sa);

        String dmgOppStr = "Fiery Confluence deals 2 damage to each opponent.";
        String expected = "Fiery Confluence -> " + dmgOppStr + " " + dmgOppStr + " " + dmgOppStr;
        assertEquals(expected, picker.getPlan().getDecisions().get(0).modesStr);
    }
}
