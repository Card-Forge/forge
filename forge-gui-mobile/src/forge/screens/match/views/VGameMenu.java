package forge.screens.match.views;

import forge.Forge;
import forge.assets.FSkinImage;
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

    private static boolean isExperimentalYieldEnabled() {
        return FModel.getPreferences().getPrefBoolean(FPref.YIELD_EXPERIMENTAL_OPTIONS);
    }

    @Override
    protected void buildMenu() {

        addItem(new FMenuItem(MatchController.instance.getConcedeCaption(), FSkinImage.CONCEDE, e ->
                ThreadUtil.invokeInGameThread(MatchController.instance::concede)
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
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblAutoYields"), Forge.hdbuttons ? FSkinImage.HDYIELD : FSkinImage.WARNING, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                final boolean autoYieldsDisabled = MatchController.instance.getGameController().getDisableAutoYields();
                final VAutoYields autoYields = new VAutoYields() {
                    @Override
                    public void setVisible(boolean b0) {
                        super.setVisible(b0);
                        if (!b0) {
                            if (autoYieldsDisabled && !MatchController.instance.getGameController().getDisableAutoYields()) {
                                //if re-enabling auto-yields, auto-yield to current ability on stack if applicable
                                if (MatchController.instance.getGameView().peekStack() != null) {
                                    final String key = MatchController.instance.getGameView().peekStack().getKey();
                                    final boolean autoYield = MatchController.instance.getGameController().shouldAutoYield(key);
                                    boolean abilityScope = !forge.localinstance.properties.ForgeConstants.AUTO_YIELD_PER_CARD.equals(
                                            forge.model.FModel.getPreferences().getPref(forge.localinstance.properties.ForgePreferences.FPref.UI_AUTO_YIELD_MODE));
                                    MatchController.instance.getGameController().setShouldAutoYield(key, !autoYield, abilityScope);
                                    if (!autoYield && MatchController.instance.getGameController().shouldAutoYield(key)) {
                                        //auto-pass priority if ability is on top of stack
                                        MatchController.instance.getGameController().passPriority();
                                    }
                                }
                            }
                        }
                    }
                };
                autoYields.show();
            }
        }));

        if (isExperimentalYieldEnabled()) {
            addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblYieldOptions"),
                    Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS,
                    e -> new VYieldOptions().show()));

            boolean autoPassOn = FModel.getPreferences().getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS);
            String autoPassLabel = Forge.getLocalizer().getMessage(autoPassOn ? "lblYieldBtnAutoPassOn" : "lblYieldBtnAutoPass");
            addItem(new FMenuItem(autoPassLabel,
                    Forge.hdbuttons ? FSkinImage.HDYIELD : FSkinImage.WARNING,
                    e -> {
                        boolean newVal = !FModel.getPreferences().getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS);
                        FModel.getPreferences().setPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, newVal);
                        FModel.getPreferences().save();
                        MatchController.instance.getGameController().setYieldInterruptPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, newVal);
                        if (newVal) {
                            MatchController.instance.getGameController().selectButtonOk();
                        }
                    }));
        }

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
