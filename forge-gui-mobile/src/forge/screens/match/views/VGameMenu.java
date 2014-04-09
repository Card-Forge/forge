package forge.screens.match.views;

import forge.Forge;
import forge.assets.FSkinImage;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.screens.match.FControl;
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
            }
        }));
        addItem(new FMenuItem("Settings", FSkinImage.SETTINGS, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(new forge.screens.settings.SettingsScreen());
            }
        }));
    }
}
