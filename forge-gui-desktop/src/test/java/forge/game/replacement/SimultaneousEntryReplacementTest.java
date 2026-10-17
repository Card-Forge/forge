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
    public void cloneCannotCopyAForestAnimatedByLifeAndLimbEnteringWithIt() {
        Card clone = returnTogether("Life and Limb", "Clone");

        AssertJUnit.assertEquals("Clone", clone.getName());
    }

    private Card returnTogether(String movesFirst, String movesSecond) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        Card host = addCard("Forest", p);
        addCardToZone(movesFirst, p, ZoneType.Graveyard);
        Card second = addCardToZone(movesSecond, p, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = AbilityFactory.getAbility("DB$ ChangeZoneAll | ChangeType$ Permanent.YouOwn | Origin$ Graveyard | Destination$ Battlefield", host);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);

        return game.getCardState(second);
    }
}
