package forge.deck;

import java.util.ArrayList;

import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.itemmanager.CardManager;
import forge.screens.TabPageScreen;

public class FDeckEditor extends TabPageScreen<FDeckEditor> {
    @SuppressWarnings("unchecked")
    private static TabPage<FDeckEditor>[] getPages() {
        ArrayList<TabPage<FDeckEditor>> pages = new ArrayList<TabPage<FDeckEditor>>();

        return (TabPage<FDeckEditor>[])pages.toArray();
    }

    private Deck deck;

    public FDeckEditor() {
        this(null);
    }
    public FDeckEditor(Deck deck0) {
        super(getPages());
        deck = deck0;
    }

    public Deck getDeck() {
        return deck;
    }

    private static class DeckSectionPage extends TabPage<FDeckEditor> {
        private final DeckSection deckSection;
        private final String captionPrefix;
        private final CardManager cardManager = new CardManager(false);

        protected DeckSectionPage(DeckSection deckSection0) {
            super(null, null);
            deckSection = deckSection0;
            switch (deckSection) {
            case Main:
                captionPrefix = "Main";
                icon = FSkinImage.DECKLIST;
                break;
            case Sideboard:
                captionPrefix = "Side";
                icon = FSkinImage.FLASHBACK;
                break;
            case Commander:
                captionPrefix = "Commander";
                icon = FSkinImage.PLANESWALKER;
                break;
            case Avatar:
                captionPrefix = "Avatar";
                icon = new FTextureRegionImage(FSkin.getAvatars().get(0));
                break;
            case Planes:
                captionPrefix = "Planes";
                icon = FSkinImage.CHAOS;
                break;
            case Schemes:
                captionPrefix = "Schemes";
                icon = FSkinImage.POISON;
                break;
            default:
                captionPrefix = "";
                break;
            }
            updateCaption();
        }

        private void updateCaption() {
            caption = captionPrefix + " (" + parentScreen.getDeck().getOrCreate(deckSection).countAll() + ")";
        }

        @Override
        protected void doLayout(float width, float height) {
            cardManager.setBounds(0, 0, width, height);
        }
    }
}
