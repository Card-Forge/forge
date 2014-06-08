package forge.deck;

import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.BoosterDraft;
import forge.model.FModel;
import forge.screens.TabPageScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.ItemPool;

public class FDeckEditor extends TabPageScreen<FDeckEditor> {
    public enum EditorType {
        Constructed,
        Draft,
        Sealed,
        Commander,
        Archenemy,
        Planechase,
        Vanguard,
    }

    private static DeckEditorPage[] getPages(EditorType editorType) {
        switch (editorType) {
        default:
        case Constructed:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard)
            };
        case Draft:
            return new DeckEditorPage[] {
                    new DraftPackPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.DRAFT_POOL)
            };
        case Sealed:
            return new DeckEditorPage[] {
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.SEALED_POOL)
            };
        case Commander:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Commander)
            };
        case Archenemy:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Schemes)
            };
        case Planechase:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Planes)
            };
        case Vanguard:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Avatar)
            };
        }
    }

    private final EditorType editorType;
    private final Deck deck;
    private CatalogPage catalogPage;
    private DeckSectionPage mainDeckPage;
    private DeckSectionPage sideboardPage;

    public FDeckEditor(EditorType editorType0) {
        this(editorType0, null);
    }
    public FDeckEditor(EditorType editorType0, Deck deck0) {
        super(getPages(editorType0));
        editorType = editorType0;
        if (deck0 == null) {
            deck0 = new Deck();
        }
        else {
            if (editorType == EditorType.Draft) {
                tabPages[0].hideTab(); //hide Draft Pack page if editing existing draft deck
            }
        }
        deck = deck0;

        //cache specific pages and initialize all pages after fields set
        for (int i = 0; i < tabPages.length; i++) {
            DeckEditorPage tabPage = (DeckEditorPage) tabPages[i];
            if (tabPage instanceof CatalogPage) {
                catalogPage = (CatalogPage) tabPage;
            }
            else if (tabPage instanceof DeckSectionPage) {
                DeckSectionPage deckSectionPage = (DeckSectionPage) tabPage;
                if (deckSectionPage.deckSection == DeckSection.Main) {
                    mainDeckPage = deckSectionPage;
                }
                else if (deckSectionPage.deckSection == DeckSection.Sideboard) {
                    sideboardPage = deckSectionPage;
                }
            }
            tabPage.initialize();
        }

        //if opening brand new sealed deck, show sideboard (card pool) by default
        if (editorType == EditorType.Sealed && deck.getMain().isEmpty()) {
            setSelectedPage(sideboardPage);
        }
    }

    public EditorType getEditorType() {
        return editorType;
    }

    public Deck getDeck() {
        return deck;
    }

    protected CatalogPage getCatalogPage() {
        return catalogPage;
    }

    protected DeckSectionPage getMainDeckPage() {
        return mainDeckPage;
    }

    protected DeckSectionPage getSideboardPage() {
        return sideboardPage;
    }

    protected BoosterDraft getDraft() {
        return null;
    }

    protected void save() {
    }

    protected static abstract class DeckEditorPage extends TabPage<FDeckEditor> {
        protected final CardManager cardManager = add(new CardManager(false));

        protected DeckEditorPage(ItemManagerConfig config, String caption0, FImage icon0) {
            super(caption0, icon0);
            cardManager.setup(config);
            cardManager.setItemActivateHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    onCardActivated(cardManager.getSelectedItem());
                }
            });
        }

        public void addCard(PaperCard card) {
            cardManager.addItem(card, 1);
            updateCaption();
        }

        public void removeCard(PaperCard card) {
            cardManager.removeItem(card, 1);
            updateCaption();
        }

        protected void updateCaption() {
        }

        protected abstract void initialize();
        protected abstract void onCardActivated(PaperCard card);

        @Override
        protected void doLayout(float width, float height) {
            cardManager.setBounds(0, 0, width, height);
        }
    }

    protected static class CatalogPage extends DeckEditorPage {
        protected CatalogPage() {
            this(ItemManagerConfig.CARD_CATALOG, "Catalog", FSkinImage.FOLDER);
        }
        protected CatalogPage(ItemManagerConfig config, String caption0, FImage icon0) {
            super(config, caption0, icon0);
        }

        @Override
        protected void initialize() {
            cardManager.setCaption(getItemManagerCaption());
            refresh();
        }

        protected String getItemManagerCaption() {
            return "Catalog";
        }

        public void refresh() {
            ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(), PaperCard.class);
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            parentScreen.getMainDeckPage().addCard(card);
        }
    }

    protected static class DeckSectionPage extends DeckEditorPage {
        private final String captionPrefix;
        private final DeckSection deckSection;

        protected DeckSectionPage(DeckSection deckSection0) {
            this(deckSection0, ItemManagerConfig.DECK_EDITOR);
        }
        protected DeckSectionPage(DeckSection deckSection0, ItemManagerConfig config) {
            super(config, null, null);

            deckSection = deckSection0;
            switch (deckSection) {
            default:
            case Main:
                captionPrefix = "Main";
                cardManager.setCaption("Main Deck");
                icon = FSkinImage.DECKLIST;
                break;
            case Sideboard:
                captionPrefix = "Side";
                cardManager.setCaption("Sideboard");
                icon = FSkinImage.FLASHBACK;
                break;
            case Commander:
                captionPrefix = "Commander";
                cardManager.setCaption("Commander");
                icon = FSkinImage.PLANESWALKER;
                break;
            case Avatar:
                captionPrefix = "Avatar";
                cardManager.setCaption("Avatar");
                icon = new FTextureRegionImage(FSkin.getAvatars().get(0));
                break;
            case Planes:
                captionPrefix = "Planes";
                cardManager.setCaption("Planes");
                icon = FSkinImage.CHAOS;
                break;
            case Schemes:
                captionPrefix = "Schemes";
                cardManager.setCaption("Schemes");
                icon = FSkinImage.POISON;
                break;
            }
        }

        @Override
        protected void initialize() {
            cardManager.setPool(parentScreen.getDeck().getOrCreate(deckSection));
            updateCaption();
        }

        @Override
        protected void updateCaption() {
            caption = captionPrefix + " (" + parentScreen.getDeck().get(deckSection).countAll() + ")";
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            switch (deckSection) {
            case Main:
                removeCard(card);
                switch (parentScreen.getEditorType()) {
                case Draft:
                case Sealed:
                    parentScreen.getSideboardPage().addCard(card);
                    break;
                default:
                    if (parentScreen.getCatalogPage() != null) {
                        parentScreen.getCatalogPage().addCard(card);
                    }
                    break;
                }
                break;
            case Sideboard:
                removeCard(card);
                parentScreen.getMainDeckPage().addCard(card);
                break;
            default:
                break;
            }
        }
    }

    private static class DraftPackPage extends CatalogPage {
        protected DraftPackPage() {
            super(ItemManagerConfig.DRAFT_PACK, "Pack 1", FSkinImage.PACK);

            //hide filters and options panel so more of pack is visible by default
            cardManager.setHideViewOptions(1, true);
            cardManager.setAlwaysNonUnique(true);
        }

        protected String getItemManagerCaption() {
            return "Cards";
        }

        @Override
        public void refresh() {
            BoosterDraft draft = parentScreen.getDraft();
            if (draft == null) { return; }

            CardPool pool = draft.nextChoice();
            int packNumber = draft.getCurrentBoosterIndex() + 1;
            caption = "Pack " + packNumber;
            cardManager.setPool(pool);
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            super.onCardActivated(card);

            BoosterDraft draft = parentScreen.getDraft();
            draft.setChoice(card);

            if (draft.hasNextChoice()) {
                refresh();
            }
            else {
                hideTab(); //hide this tab page when finished drafting
                draft.finishedDrafting();
                parentScreen.save();
            }
        }
    }
}
