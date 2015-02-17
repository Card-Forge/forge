package forge.screens.match.views;

import forge.Forge;
import forge.assets.FSkinImage;
import forge.deck.Deck;
import forge.deck.FDeckViewer;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
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
        
        addItem(new FMenuItem("Concede", FSkinImage.CONCEDE, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().concede();
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
        addItem(new FMenuItem("Deck List", FSkinImage.DECKLIST, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                final Deck deck = Forge.hostedMatch.getGame().getPhaseHandler().getPlayerTurn().getRegisteredPlayer().getDeck();
                if (deck != null) {
                    FDeckViewer.show(deck);
                }
            }
        }));
        addItem(new FMenuItem("Auto-Yields", FSkinImage.WARNING, new FEventHandler() {
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
        addItem(new FMenuItem("Settings", FSkinImage.SETTINGS, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                SettingsScreen.show();
            }
        }));
    }
}
