package forge.screens.match.views;

import forge.Forge;
import forge.assets.FSkinImage;
import forge.gamemodes.match.DrawOfferMessage;
import forge.gamemodes.match.YieldController;
import forge.gamemodes.match.YieldUpdate;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.ThreadUtil;

public class VGameMenu extends FDropDownMenu {
    public VGameMenu() {
    }

    @Override
    protected void buildMenu() {
        addItem(new FMenuItem(MatchController.instance.getConcedeCaption(), FSkinImage.CONCEDE, e ->
                ThreadUtil.invokeInGameThread(MatchController.instance::concede)
        ));
        // Called directly, not via invokeInGameThread like concede: the offer must register even while
        // the game thread is busy (a frozen thread would never drain a queued task). It's applied
        // off-thread like concede, safe because a human acts only while the game thread is parked at priority.
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblOfferDraw"), FSkinImage.OFFERDRAW, e ->
                MatchController.instance.getGameController().drawOfferAction(DrawOfferMessage.Action.OFFER)
        ));
        /*addItem(new FMenuItem("Save Game", FSkinImage.SAVE, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GameStateSerializer.saveGameState(MatchUtil.getGame(), ForgeConstants.USER_GAMES_DIR + "GameSave.txt");
                FOptionPane.showMessageDialog("Game saved successfully.", "Save Game", FOptionPane.INFORMATION_ICON);
            }
        }));
        addItem(new FMenuItem("Load Game", FSkinImage.OPEN, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GameStateDeserializer.loadGameState(MatchUtil.getGame(), ForgeConstants.USER_GAMES_DIR + "GameSave.txt");
            }
        }));*/
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblAutoYieldsAndTriggers"), Forge.hdbuttons ? FSkinImage.HDYIELD : FSkinImage.WARNING, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                final boolean autoYieldsDisabled = MatchController.instance.getGameController().getYieldController().getDisableAutoYields();
                final VAutoYieldsAndTriggers dialog = new VAutoYieldsAndTriggers() {
                    @Override
                    public void setVisible(boolean b0) {
                        super.setVisible(b0);
                        if (!b0 && MatchController.instance.getGameView().peekStack() != null
                                && autoYieldsDisabled && !MatchController.instance.getGameController().getYieldController().getDisableAutoYields()) {
                            //if re-enabling auto-yields, auto-yield to current ability on stack if applicable
                            final String key = MatchController.instance.getGameView().peekStack().getKey();
                            final boolean autoYield = MatchController.instance.getGameController().shouldAutoYield(key);
                            boolean abilityScope = MatchController.instance.getGameController().getYieldController().isAbilityScope();
                            MatchController.instance.getGameController().setShouldAutoYield(key, !autoYield, abilityScope);
                            if (!autoYield && MatchController.instance.getGameController().shouldAutoYield(key)) {
                                //auto-pass priority if ability is on top of stack
                                MatchController.instance.getGameController().passPriority();
                            }
                        }
                    }
                };
                dialog.show();
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblYieldSettings"), Forge.hdbuttons ? FSkinImage.HDYIELD : FSkinImage.WARNING, e -> {
            new VYieldOptions().show();
        }));
        String autoPassLabel = Forge.getLocalizer().getMessage(FModel.getPreferences().getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS) ? "lblYieldBtnAutoPassOn" : "lblYieldBtnAutoPass");
        addItem(new FMenuItem(autoPassLabel, Forge.hdbuttons ? FSkinImage.HDYIELD : FSkinImage.WARNING, e -> {
            YieldController.toggleAutoPassNoActions(MatchController.instance.getGameController());
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblResetSavedAbilityOrders"), FSkinImage.WARNING, e ->
                MatchController.instance.getGameController().sendYieldUpdate(new YieldUpdate.ClearAbilityOrders())
        ));
        if (!Forge.isMobileAdventureMode) {
            addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblSettings"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS, e -> {
                //pause game when spectating AI Match
                if (!MatchController.instance.hasLocalPlayers()) {
                    if(!MatchController.instance.isGamePaused())
                        MatchController.instance.pauseMatch();
                }
                SettingsScreen.show(false);
            }));
            addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblShowWinLoseOverlay"), FSkinImage.ENDTURN, e ->
                    MatchController.instance.showWinlose()
            ));
        }
    }
}
