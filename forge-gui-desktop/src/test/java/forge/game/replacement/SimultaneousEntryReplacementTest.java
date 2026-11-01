package forge.game.replacement;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class SimultaneousEntryReplacementTest extends AITest {

    @Test
    public void checkLandDoesNotSeeAnIslandEnteringWithIt() {
        Card fortress = returnTogether("Island", "Glacial Fortress");

        AssertJUnit.assertTrue(fortress.isTapped());
    }

    @Test
    public void darkDepthsKeepsItsCountersWhenBloodMoonEntersWithIt() {
        Card depths = returnTogether("Blood Moon", "Dark Depths");

        AssertJUnit.assertEquals(10, depths.getCounters(CounterEnumType.ICE));
    }

    @Test
    public void darkDepthsKeepsItsCountersWhenSolemnityEntersWithIt() {
        Card depths = returnTogether("Solemnity", "Dark Depths");

        AssertJUnit.assertEquals(10, depths.getCounters(CounterEnumType.ICE));
    }

    @Test
    public void cloneCannotCopyAForestAnimatedByLifeAndLimbEnteringWithIt() {
        Card clone = returnTogether("Life and Limb", "Clone");

        AssertJUnit.assertEquals("Clone", clone.getName());
    }

    @Test
    public void lakeOfTheDeadCannotSacrificeAForestMadeASwampByUrborgEnteringWithIt() {
        Card lake = returnTogether("Urborg, Tomb of Yawgmoth", "Lake of the Dead");

        AssertJUnit.assertTrue(lake.isInZone(ZoneType.Graveyard));
    }

    @Test
    public void lichTakesYourLifeWhenPlatinumEmperionEntersWithIt() {
        Card lich = returnTogether("Platinum Emperion", "Lich");

        AssertJUnit.assertEquals(0, lich.getController().getLife());
    }

    @Test
    public void auraCanEnchantAnArtifactWhenGuardianBeastEntersWithIt() {
        Card aura = returnTogether("Guardian Beast", "Ensoul Artifact", "Sol Ring");

        AssertJUnit.assertTrue(aura.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void cloneCanCopyACreatureDevouredByAPermanentEnteringWithIt() {
        Card clone = returnTogether("Mycoloth", "Clone", "Grizzly Bears");

        AssertJUnit.assertEquals("Grizzly Bears", clone.getName());
        AssertJUnit.assertTrue(clone.isInZone(ZoneType.Battlefield));
    }

    private Card returnTogether(String movesFirst, String movesSecond, String... alreadyThere) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        Card host = addCard("Forest", p);
        for (String name : alreadyThere) {
            addCard(name, p);
        }
        addCardToZone(movesFirst, p, ZoneType.Graveyard);
        Card second = addCardToZone(movesSecond, p, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = AbilityFactory.getAbility("DB$ ChangeZoneAll | ChangeType$ Permanent.YouOwn | Origin$ Graveyard | Destination$ Battlefield", host);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);

        return game.getCardState(second);
    }
}
