package forge.game.trigger;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class SimultaneousEntryTriggerTest extends AITest {

    @Test
    public void landEnteringAlongsideYavimayaIsAForest() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        addCard("Roiling Canopy", p);
        addCards("Forest", 5, p);
        addCard("Grizzly Bears", p);
        addCardToZone("Wastes", p, ZoneType.Graveyard);
        addCardToZone("Yavimaya, Cradle of Growth", p, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = addCardToZone("Splendid Reclamation", p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();

        AssertJUnit.assertEquals(2, game.getStack().size());
    }
}
