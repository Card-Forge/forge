package forge.screens.quest;

import forge.assets.FSkinImage;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.screens.FScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public abstract class QuestScreen extends FScreen {
    protected QuestScreen(String headerCaption) {
        super(headerCaption, new FPopupMenu() {
            @Override
            protected void buildMenu() {
                addItem(new FMenuItem("Duels", FSkinImage.QUEST_GEAR, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                    }
                }));
                addItem(new FMenuItem("Challenges", FSkinImage.QUEST_HEART, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                    }
                }));
                addItem(new FMenuItem("Tournaments", FSkinImage.PACK, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                    }
                }));
                addItem(new FMenuItem("Travel", FSkinImage.QUEST_MAP, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                    }
                }));
                addItem(new FMenuItem("Spell Shop", FSkinImage.QUEST_BOOK, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                    }
                }));
                addItem(new FMenuItem("Bazaar", FSkinImage.QUEST_BOTTLES, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                    }
                }));
                addItem(new FMenuItem("Change Deck", FSkinImage.DECKLIST, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                    }
                }));
                addItem(new FMenuItem("New Quest", FSkinImage.NEW, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                    }
                }));
                addItem(new FMenuItem("Load Quest", FSkinImage.OPEN, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                    }
                }));
                addItem(new FMenuItem("Preferences", FSkinImage.SETTINGS, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                    }
                }));
            }
        });
    }
}
