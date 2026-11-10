package forge.game.trigger;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
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

    @Test
    public void kodamaSeesMoxDiamondThatEntersWithItButMovesFirst() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        addCardToZone("Island", p, ZoneType.Hand);
        addCardToZone("Mox Diamond", p, ZoneType.Graveyard);
        addCardToZone("Kodama of the East Tree", p, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = AbilityFactory.getAbility("DB$ ChangeZoneAll | ChangeType$ Permanent.YouOwn | Origin$ Graveyard | Destination$ Battlefield", addCard("Forest", p));
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();

        AssertJUnit.assertEquals(1, game.getStack().size());
        AssertJUnit.assertEquals("Kodama of the East Tree", game.getStack().peekAbility().getHostCard().getName());
    }
}
