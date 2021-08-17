package forge.adventure.libgdxgui.screens.match.views;

import forge.adventure.libgdxgui.Forge;
import forge.adventure.libgdxgui.assets.FSkinImage;
import forge.deck.Deck;
import forge.adventure.libgdxgui.deck.FDeckViewer;
import forge.game.player.Player;
import forge.adventure.libgdxgui.menu.FDropDownMenu;
import forge.adventure.libgdxgui.menu.FMenuItem;
import forge.adventure.libgdxgui.screens.match.MatchController;
import forge.adventure.libgdxgui.screens.settings.SettingsScreen;
import forge.adventure.libgdxgui.toolbox.FEvent;
import forge.adventure.libgdxgui.toolbox.FEvent.FEventHandler;
import forge.adventure.libgdxgui.toolbox.FOptionPane;
import forge.util.Localizer;
import forge.util.ThreadUtil;

public class VGameMenu extends FDropDownMenu {
    public VGameMenu() {
    }

    @Override
    protected void buildMenu() {
        final Localizer localizer = Localizer.getInstance();

        addItem(new FMenuItem(MatchController.instance.getConcedeCaption(), FSkinImage.CONCEDE, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.concede();
                    }
                });
            }
        }));
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
        addItem(new FMenuItem(localizer.getMessage("lblDeckList"), FSkinImage.DECKLIST, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                //pause game when spectating AI Match
                if (!MatchController.instance.hasLocalPlayers()) {
                    if(!MatchController.instance.isGamePaused())
                        MatchController.instance.pauseMatch();
                }

                final Player player = MatchController.getHostedMatch().getGame().getPhaseHandler().getPlayerTurn();
                if (player != null) {
                    final Deck deck = player.getRegisteredPlayer().getDeck();
                    if (deck != null) {
                        FDeckViewer.show(deck);
                        return;
                    }
                }
                FOptionPane.showMessageDialog(localizer.getMessage("lblNoPlayerPriorityNoDeckListViewed"));
            }
        }));
        addItem(new FMenuItem(localizer.getMessage("lblAutoYields"), Forge.hdbuttons ? FSkinImage.HDYIELD : FSkinImage.WARNING, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                final boolean autoYieldsDisabled = MatchController.instance.getDisableAutoYields();
                final VAutoYields autoYields = new VAutoYields() {
                    @Override
                    public void setVisible(boolean b0) {
                        super.setVisible(b0);
                        if (!b0) {
                            if (autoYieldsDisabled && !MatchController.instance.getDisableAutoYields()) {
                                //if re-enabling auto-yields, auto-yield to current ability on stack if applicable
                                final String key = MatchController.instance.getGameView().peekStack().getKey();
                                final boolean autoYield = MatchController.instance.shouldAutoYield(key);
                                MatchController.instance.setShouldAutoYield(key, !autoYield);
                                if (!autoYield && MatchController.instance.shouldAutoYield(key)) {
                                    //auto-pass priority if ability is on top of stack
                                    MatchController.instance.getGameController().passPriority();
                                }
                            }
                        }
                    }
                };
                autoYields.show();
            }
        }));
        addItem(new FMenuItem(localizer.getMessage("lblSettings"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                //pause game when spectating AI Match
                if (!MatchController.instance.hasLocalPlayers()) {
                    if(!MatchController.instance.isGamePaused())
                        MatchController.instance.pauseMatch();
                }
                SettingsScreen.show(false);
            }
        }));
        addItem(new FMenuItem(localizer.getMessage("lblShowWinLoseOverlay"), FSkinImage.ENDTURN, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                MatchController.instance.showWinlose();
            }
        }));
    }
}
