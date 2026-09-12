package forge.screens.match.views;

import forge.Forge;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.screens.match.MatchController;
import forge.util.ThreadUtil;

public class VDevMenu extends FDropDownMenu {
    @Override
    protected void buildMenu() {
        //must invoke all these in game thread since they may require synchronous user input
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblGenerateMana"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().generateMana())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblTutor"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().tutorForCard())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRollbackPhase"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().rollbackPhase())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCastSpellOrPlayLand"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().castASpell())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCardToHand"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().addCardToHand())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCardToBattlefield"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().addCardToBattlefield())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblTokenToBattlefield"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().addTokenToBattlefield())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCardToLibrary"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().addCardToLibrary())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCardToGraveyard"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().addCardToGraveyard())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCardToExile"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().addCardToExile())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRepeatAddCard"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().repeatLastAddition())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblExileFromHand"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().exileCardsFromHand())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblExileFromPlay"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().exileCardsFromBattlefield())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRemoveFromGame"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().removeCardsFromGame())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblSetLife"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().setPlayerLife())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblWinGame"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().winGame())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblSetupGame"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().setupGameState())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblDumpGame"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().dumpGameState())
        ));

        final boolean unlimitedLands = MatchController.instance.getGameController().canPlayUnlimitedLands();
        addItem(new FCheckBoxMenuItem(Forge.getLocalizer().getMessage("lblUnlimitedLands"), unlimitedLands, e ->
                MatchController.instance.getGameController().cheat().setCanPlayUnlimitedLands(!unlimitedLands)
        ));
        final boolean viewAll = MatchController.instance.getGameController().mayLookAtAllCards();
        addItem(new FCheckBoxMenuItem(Forge.getLocalizer().getMessage("lblViewAll"), viewAll, e ->
                MatchController.instance.getGameController().cheat().setViewAllCards(!viewAll)
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblAddCounterPermanent"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().addCountersToPermanent())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblSubCounterPermanent"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().removeCountersFromPermanent())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblTapPermanent"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().tapPermanents())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblUntapPermanent"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().untapPermanents())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRiggedRoll"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().riggedPlanarRoll())
        ));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblWalkTo"), e ->
                ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().cheat().planeswalkTo())
        ));
    }
}
