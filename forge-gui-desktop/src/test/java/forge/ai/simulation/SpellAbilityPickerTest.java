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
}
