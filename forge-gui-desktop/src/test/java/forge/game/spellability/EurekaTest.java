package forge.game.spellability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class EurekaTest extends AITest {

    @Test(timeOut = 30000)
    public void eurekaEndsOnceTheOnlyCardLeftIsAnAuraWithNothingToEnchant() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        Card ring = addCardToZone("Sol Ring", p, ZoneType.Hand);
        Card growth = addCardToZone("Wild Growth", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = addCardToZone("Eureka", p, ZoneType.Graveyard).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);

        AssertJUnit.assertTrue(game.getCardState(ring).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(game.getCardState(growth).isInZone(ZoneType.Hand));
    }
}
