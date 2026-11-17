package forge.ai;

import org.testng.annotations.Test;

import forge.ai.simulation.FlatMonteCarloPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.cost.CostSacrifice;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Prints what the AI makes of Cabal Therapy and of the sacrifice cost of Wight of the Reliquary (lines starting with
 * DIAG). Asserts nothing: it is there to find out why a card is ignored or misplayed.
 */
public class FlatMonteCarloCardDiagnosticsTest extends AITest {

    private AiController controllerOf(final Player p) {
        final Object ctl = p.getController();
        if (ctl instanceof PlayerControllerAi) {
            return ((PlayerControllerAi) ctl).getAi();
        }
        System.out.println("DIAG the controller of the player is " + ctl.getClass());
        return null;
    }

    @Test
    public void printWhatTheAiThinksOfCabalTherapy() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Swamp", ai);
        final Card therapy = addCardToZone("Cabal Therapy", ai, ZoneType.Hand);
        addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        addCardToZone("Island", opp, ZoneType.Hand);
        final SpellAbility sa = therapy.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        final AiController ctrl = controllerOf(ai);
        String decision;
        try {
            decision = ctrl == null ? "no controller" : String.valueOf(ctrl.canPlaySa(sa));
        } catch (final RuntimeException e) {
            decision = "THREW " + e;
        }
        System.out.println("DIAG Cabal Therapy: api=" + sa.getApi()
                + ", sub=" + (sa.getSubAbility() == null ? null : sa.getSubAbility().getApi())
                + ", removeDeckAll=" + therapy.getRules().getAiHints().getRemAIDecks()
                + ", isCardRemAIDeck=" + ComputerUtilCard.isCardRemAIDeck(therapy)
                + ", discoverableByFlatMc=" + FlatMonteCarloPicker.isDiscoverable(sa, true)
                + ", canPlaySa=" + decision);
    }

    @Test
    public void printWhatTheAiSacrificesToWight() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card wight = addCard("Wight of the Reliquary", ai);
        wight.setSickness(false);
        addCard("Griselbrand", ai);
        addCard("Llanowar Elves", ai);
        addCard("Elvish Mystic", ai);
        addCardToZone("Forest", ai, ZoneType.Library);
        SpellAbility ability = null;
        for (final SpellAbility sa : wight.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getPayCosts() != null
                    && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
                ability = sa;
            }
        }
        if (ability == null) {
            System.out.println("DIAG Wight: no activated ability with a sacrifice cost found");
            return;
        }
        ability.setActivatingPlayer(ai);
        final String type = ability.getPayCosts().getCostPartByType(CostSacrifice.class).getType();
        final AiController ctrl = controllerOf(ai);
        String decision;
        try {
            decision = ctrl == null ? "no controller" : String.valueOf(ctrl.canPlaySa(ability));
        } catch (final RuntimeException e) {
            decision = "THREW " + e;
        }
        CardCollectionView chosen;
        try {
            chosen = ComputerUtil.chooseSacrificeType(ai, type, ability, ability.getTargetCard(), false, 1,
                    CardCollection.EMPTY);
        } catch (final RuntimeException e) {
            chosen = null;
            System.out.println("DIAG Wight: chooseSacrificeType threw " + e);
        }
        System.out.println("DIAG Wight: type=" + type + ", canPlaySa=" + decision + ", sacrifices=" + chosen
                + " (board: Griselbrand, Llanowar Elves, Elvish Mystic)");
    }
}