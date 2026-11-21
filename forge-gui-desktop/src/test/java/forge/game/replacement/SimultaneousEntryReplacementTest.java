package forge.game.replacement;

import forge.ai.AITest;
import forge.ai.AiCostDecision;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.cost.Cost;
import forge.game.cost.CostPayment;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;
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
    public void lifeLandSeesLifeBeforeShockLandPaysEnteringWithIt() {
        Card shack = returnTogether("Breeding Pool", "Lakeside Shack");

        AssertJUnit.assertTrue(shack.isTapped());
    }

    private Card returnTogether(String movesFirst, String movesSecond) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        p.dangerouslySetController(new ShockLandPayer(game, p));
        p.setLife(14, null);
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

    // the default AI only pays for a shock land when it plans to cast a spell with it
    private static class ShockLandPayer extends PlayerControllerAi {
        ShockLandPayer(Game game, Player p) {
            super(game, p, p.getLobbyPlayer());
        }

        @Override
        public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, FCollectionView<Player> allPayers) {
            return new CostPayment(cost, sa).payComputerCosts(new AiCostDecision(player, sa, true));
        }
    }
}
