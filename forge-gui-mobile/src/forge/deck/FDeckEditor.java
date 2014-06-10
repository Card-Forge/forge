package forge.deck;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Supplier;

import forge.Forge;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.deck.io.DeckPreferences;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.BoosterDraft;
import forge.model.FModel;
import forge.screens.TabPageScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextField;
import forge.util.Callback;
import forge.util.ItemPool;
import forge.util.Utils;
import forge.util.storage.IStorage;

public class FDeckEditor extends TabPageScreen<FDeckEditor> {
    public enum EditorType {
        Constructed(new DeckController<Deck>(FModel.getDecks().getConstructed(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        })),
        Draft(new DeckController<DeckGroup>(FModel.getDecks().getDraft(), new Supplier<DeckGroup>() {
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        })),
        Sealed(new DeckController<DeckGroup>(FModel.getDecks().getSealed(), new Supplier<DeckGroup>() {
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        })),
        Winston(new DeckController<DeckGroup>(FModel.getDecks().getWinston(), new Supplier<DeckGroup>() {
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        })),
        Commander(new DeckController<Deck>(FModel.getDecks().getCommander(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        })),
        Archenemy(new DeckController<Deck>(FModel.getDecks().getScheme(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        })),
        Planechase(new DeckController<Deck>(FModel.getDecks().getPlane(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        })),
        Vanguard(new DeckController<Deck>(FModel.getDecks().getConstructed(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        }));

        private final DeckController<? extends DeckBase> controller;

        public DeckController<? extends DeckBase> getController() {
            return controller;
        }

        private EditorType(DeckController<? extends DeckBase> controller0) {
            controller = controller0;
        }
    }

    private static DeckEditorPage[] getPages(EditorType editorType) {
        switch (editorType) {
        default:
        case Constructed:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new OptionsPage()
            };
        case Draft:
            return new DeckEditorPage[] {
                    new DraftPackPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.DRAFT_POOL),
                    new OptionsPage()
            };
        case Sealed:
            return new DeckEditorPage[] {
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.SEALED_POOL),
                    new OptionsPage()
            };
        case Commander:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Commander),
                    new OptionsPage()
            };
        case Archenemy:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Schemes),
                    new OptionsPage()
            };
        case Planechase:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Planes),
                    new OptionsPage()
            };
        case Vanguard:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Avatar),
                    new OptionsPage()
            };
        }
    }

    private final EditorType editorType;
    private Deck deck;
    private CatalogPage catalogPage;
    private DeckSectionPage mainDeckPage;
    private DeckSectionPage sideboardPage;
    private OptionsPage optionsPage;

    public FDeckEditor(EditorType editorType0, DeckProxy editDeck) {
        this(editorType0, editDeck.getName(), editDeck.getPath());
    }
    public FDeckEditor(EditorType editorType0, String editDeckName) {
        this(editorType0, editDeckName, "");
    }
    private FDeckEditor(EditorType editorType0, String editDeckName, String editDeckPath) {
        super(getPages(editorType0));

        editorType = editorType0;

        if (StringUtils.isEmpty(editDeckName)) {
            deck = new Deck();
            if (editorType == EditorType.Draft) {
                tabPages[3].hideTab(); //hide Options page while drafting
            }
        }
        else {
            if (editorType == EditorType.Draft) {
                tabPages[0].hideTab(); //hide Draft Pack page if editing existing draft deck
            }
            editorType.getController().load(editDeckPath, editDeckName);
            deck = editorType.getController().getDeck();
        }

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
            else if (tabPage instanceof OptionsPage) {
                optionsPage = (OptionsPage) tabPage;
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

    protected OptionsPage getOptionsPage() {
        return optionsPage;
    }

    protected BoosterDraft getDraft() {
        return null;
    }

    protected void save(final Callback<Boolean> callback) {
        final DeckController<?> controller = editorType.getController();
        final String name = getOptionsPage().txtName.getText();
        final String deckStr = DeckProxy.getDeckString(controller.getModelPath(), name);

        // Warn if no name
        if (StringUtils.isEmpty(name)) {
            FOptionPane.showErrorDialog("Please name your deck using the 'Name' box.", "Save Error!");
            if (callback != null) {
                callback.run(false);
            }
            return;
        }
        
        // Confirm if overwrite
        if (controller.fileExists(name)) {
            //prompt only if name was changed
            if (!StringUtils.equals(name, controller.getModelName())) {
                FOptionPane.showConfirmDialog("There is already a deck named '" + name + "'. Overwrite?",
                    "Overwrite Deck?", new Callback<Boolean>() {
                        @Override
                        public void run(Boolean result) {
                            if (result) {
                                controller.save();
                                afterSave(deckStr);
                            }
                            if (callback != null) {
                                callback.run(result);
                            }
                        }
                });
                return;
            }
            controller.save();
            afterSave(deckStr);
            if (callback != null) {
                callback.run(true);
            }
            return;
        }

        // Confirm if a new deck will be created
        FOptionPane.showConfirmDialog("This will create a new deck named '" +
                name + "'. Continue?", "Create Deck?", new Callback<Boolean>() {
                    @Override
                    public void run(Boolean result) {
                        if (result) {
                            controller.saveAs(name);
                            afterSave(deckStr);
                        }
                        if (callback != null) {
                            callback.run(result);
                        }
                    }
        });
    }

    private void afterSave(String deckStr) {
        if (editorType == EditorType.Constructed) {
            DeckPreferences.setCurrentDeck(deckStr);
        }
        deck = editorType.getController().getDeck();
    }

    @Override
    public void onClose(final Callback<Boolean> canCloseCallback) {
        if (editorType.getController().isSaved() || canCloseCallback == null) {
            super.onClose(canCloseCallback); //can skip prompt if draft saved
            return;
        }
        FOptionPane.showOptionDialog("Save changes to current deck?", "Save Changes?",
                FOptionPane.QUESTION_ICON, new String[] {"Save", "Don't Save", "Cancel"}, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == 0) {
                            save(canCloseCallback);
                        }
                        else if (result == 1) {
                            editorType.getController().reload(); //reload if not saving changes
                            canCloseCallback.run(true);
                        }
                        else {
                            canCloseCallback.run(false);
                        }
                    }
        });
    }

    protected static abstract class DeckEditorPage extends TabPage<FDeckEditor> {
        protected DeckEditorPage(String caption0, FImage icon0) {
            super(caption0, icon0);
        }

        protected abstract void initialize();
    }

    protected static abstract class CardManagerPage extends DeckEditorPage {
        protected final CardManager cardManager = add(new CardManager(false));

        protected CardManagerPage(ItemManagerConfig config, String caption0, FImage icon0) {
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
            parentScreen.getEditorType().getController().notifyModelChanged();
            updateCaption();
        }

        public void removeCard(PaperCard card) {
            cardManager.removeItem(card, 1);
            parentScreen.getEditorType().getController().notifyModelChanged();
            updateCaption();
        }

        protected void updateCaption() {
        }

        protected abstract void onCardActivated(PaperCard card);

        @Override
        protected void doLayout(float width, float height) {
            cardManager.setBounds(0, 0, width, height);
        }
    }

    protected static class CatalogPage extends CardManagerPage {
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

    protected static class DeckSectionPage extends CardManagerPage {
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
                parentScreen.getOptionsPage().showTab(); //show options page when finished drafting
                draft.finishedDrafting();
                parentScreen.save(null);
            }
        }
    }

    protected static class OptionsPage extends DeckEditorPage {
        private static final float PADDING = Utils.scaleMin(5);

        private final FLabel lblName = add(new FLabel.Builder().text("Name:").build());
        private final FTextField txtName = add(new FTextField());
        private final FLabel btnSave = add(new FLabel.ButtonBuilder().text("Save Deck").icon(FSkinImage.SAVE).build());
        private final FLabel btnAddLands = add(new FLabel.ButtonBuilder().text("Add Lands").icon(FSkinImage.LAND).build());
        private final FLabel btnDelete = add(new FLabel.ButtonBuilder().text("Delete Deck").icon(FSkinImage.DELETE).build());
        /*private final FLabel btnNew = add(new FLabel.ButtonBuilder().text("New Deck").icon(FSkinImage.NEW).build());
        private final FLabel btnOpen = add(new FLabel.ButtonBuilder().text("Open Deck").icon(FSkinImage.OPEN).build());
        private final FLabel btnSaveAs = add(new FLabel.ButtonBuilder().text("Save Deck As").icon(FSkinImage.SAVEAS).build());*/

        protected OptionsPage() {
            super("Options", FSkinImage.SETTINGS);
        }

        @Override
        protected void initialize() {
            txtName.setGhostText("[New Deck]");
            txtName.setText(parentScreen.getDeck().getName());
            txtName.setEnabled(false); //TODO: Allow editing for non-limited decks

            btnSave.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    parentScreen.save(null);
                }
            });
            btnAddLands.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                }
            });
            btnDelete.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    FOptionPane.showConfirmDialog(
                            "Are you sure you want to delete '" + parentScreen.getDeck().getName() + "'?",
                            "Delete Deck", "Delete", "Cancel", false, new Callback<Boolean>() {
                                @Override
                                public void run(Boolean result) {
                                    if (result) {
                                        parentScreen.getEditorType().getController().delete();
                                        Forge.back();
                                    }
                                }
                            });
                }
            });
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = PADDING;
            float y = PADDING;
            float w = width - 2 * PADDING;

            lblName.setBounds(x, y, lblName.getAutoSizeBounds().width, txtName.getHeight());
            txtName.setBounds(x + lblName.getWidth(), y, w - lblName.getWidth(), txtName.getHeight());
            y += txtName.getHeight() + PADDING;

            float buttonHeight = Utils.AVG_FINGER_HEIGHT;
            float dy = buttonHeight + PADDING;

            btnSave.setBounds(x, y, w, buttonHeight);
            y += dy;
            btnAddLands.setBounds(x, y, w, buttonHeight);
            y += dy;
            btnDelete.setBounds(x, y, w, buttonHeight);
            y += dy;
            /*btnNew.setBounds(x, y, w, buttonHeight);
            y += dy;
            btnOpen.setBounds(x, y, w, buttonHeight);
            y += dy;
            btnSaveAs.setBounds(x, y, w, buttonHeight);*/
        }
    }

    public static class DeckController<T extends DeckBase> {
        private T model;
        private boolean saved;
        private boolean modelInStorage;
        private final IStorage<T> rootFolder;
        private IStorage<T> currentFolder;
        private String modelPath;
        private final Supplier<T> newModelCreator;

        protected DeckController(final IStorage<T> folder0, final Supplier<T> newModelCreator0) {
            rootFolder = folder0;
            currentFolder = rootFolder;
            model = null;
            saved = true;
            modelInStorage = false;
            modelPath = "";
            newModelCreator = newModelCreator0;
        }

        public Deck getDeck() {
            if (model instanceof Deck) {
                return (Deck) model;
            }
            return ((DeckGroup) model).getHumanDeck();
        }

        public T getModel() {
            return model;
        }

        public String getModelPath() {
            return modelPath;
        }

        public void setModel(final T document) {
            setModel(document, false);
        }
        public void setModel(final T document, final boolean isStored) {
            modelInStorage = isStored;
            model = document;

            if (isStored) {
                if (isModelInSyncWithFolder()) {
                    setSaved(true);
                }
                else {
                    notifyModelChanged();
                }
            }
            else { //TODO: Make this smarter
                currentFolder = rootFolder;
                modelPath = "";
                setSaved(true);
            }
        }

        private boolean isModelInSyncWithFolder() {
            if (model.getName().isEmpty()) {
                return true;
            }

            final T modelStored = currentFolder.get(model.getName());
            // checks presence in dictionary only.
            if (modelStored == model) {
                return true;
            }
            if (modelStored == null) {
                return false;
            }

            return modelStored.equals(model);
        }

        public void notifyModelChanged() {
            if (saved) {
                setSaved(false);
            }
        }

        private void setSaved(boolean val) {
            saved = val;
        }

        public void reload() {
            String name = getModelName();
            if (name.isEmpty()) {
                newModel();
            }
            else {
                load(name);
            }
        }

        public void load(final String path, final String name) {
            if (StringUtils.isBlank(path)) {
                currentFolder = rootFolder;
            }
            else {
                currentFolder = rootFolder.tryGetFolder(path);
            }
            modelPath = path;
            load(name);
        }

        @SuppressWarnings("unchecked")
        private void load(final String name) {
            T newModel = currentFolder.get(name);
            if (newModel != null) {
                setModel((T) newModel.copyTo(name), true);
            }
            else {
                setSaved(true);
            }
        }

        @SuppressWarnings("unchecked")
        public void save() {
            if (model == null) {
                return;
            }

            // copy to new instance before adding to current folder so further changes are auto-saved
            currentFolder.add((T) model.copyTo(model.getName()));
            modelInStorage = true;
            setSaved(true);
        }

        @SuppressWarnings("unchecked")
        public void saveAs(final String name0) {
            model = (T)model.copyTo(name0);
            modelInStorage = false;
            save();
        }

        public boolean isSaved() {
            return saved;
        }

        public boolean fileExists(final String deckName) {
            return currentFolder.contains(deckName);
        }

        public void importDeck(final T newDeck) {
            setModel(newDeck);
        }

        public void refreshModel() {
            if (model == null) {
                newModel();
            }
            else {
                setModel(model, modelInStorage);
            }
        }

        public void newModel() {
            model = newModelCreator.get();
            setSaved(true);
        }

        public String getModelName() {
            return model != null ? model.getName() : "";
        }

        public boolean delete() {
            if (model == null) { return false; }
            currentFolder.delete(model.getName());
            setModel(null);
            return true;
        }
    }
}
