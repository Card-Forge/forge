package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class EmptyDelayedTriggerTest extends AITest {

    private static SpellAbility myriadAbility(Card card) {
        for (Trigger t : card.getTriggers()) {
            SpellAbility sa = t.getOverridingAbility();
            if (sa != null && "ExileCombat".equals(sa.getParam("AtEOT"))) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void myriadWithNoTokenCreatesNoEndOfCombatTrigger() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card banshee = addCard("Banshee of the Dread Choir", p);
        game.getAction().checkStateEffects(true);

        Combat combat = new Combat(p);
        combat.addAttacker(banshee, opp);
        game.getPhaseHandler().setCombat(combat);

        SpellAbility myriad = myriadAbility(banshee);
        AssertJUnit.assertNotNull(myriad);
        myriad.setActivatingPlayer(p);
        AbilityUtils.resolve(myriad);
        AssertJUnit.assertEquals(0, game.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).count());

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_END, p);
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(p), false);
        AssertJUnit.assertFalse(game.getStack().hasSimultaneousStackEntries());
    }
}
