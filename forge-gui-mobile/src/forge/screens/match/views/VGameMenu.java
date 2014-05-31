package forge.screens.match.views;

import forge.assets.FSkinImage;
import forge.deck.FDeckViewer;
import forge.game.player.RegisteredPlayer;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.player.GamePlayerUtil;
import forge.screens.match.FControl;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class VGameMenu extends FDropDownMenu {
    @Override
    protected void buildMenu() {
        addItem(new FMenuItem("Undo", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FControl.undoLastAction();
            }
        }));
        addItem(new FMenuItem("Concede", FSkinImage.CONCEDE, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FControl.concede();
            }
        }));
        addItem(new FMenuItem("End Turn", FSkinImage.ENDTURN, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FControl.endCurrentTurn();
            }
        }));
        addItem(new FMenuItem("Alpha Strike", FSkinImage.ALPHASTRIKE, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FControl.alphaStrike();
            }
        }));
        addItem(new FMenuItem("Deck List", FSkinImage.DECKLIST, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                RegisteredPlayer player = GamePlayerUtil.getGuiRegisteredPlayer(FControl.getGame());
                if (player != null) {
                    FDeckViewer.show(player.getDeck());
                }
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
