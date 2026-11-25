package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class MobilizeZeroTest extends AITest {

    @Test
    public void mobilizeZeroStillTriggersAtEndStep() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card avenger = addCard("Avenger of the Fallen", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Graveyard).isEmpty());

        SpellAbility mobilize = null;
        for (Trigger t : avenger.getTriggers()) {
            SpellAbility sa = t.getOverridingAbility();
            if (sa != null && sa.hasParam("AtEOT")) {
                mobilize = sa;
            }
        }
        AssertJUnit.assertNotNull(mobilize);
        mobilize.setActivatingPlayer(p);
        AbilityUtils.resolve(mobilize);
        AssertJUnit.assertEquals(0, game.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).count());

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, p);
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(p), false);
        AssertJUnit.assertTrue(game.getStack().hasSimultaneousStackEntries());
    }
}
