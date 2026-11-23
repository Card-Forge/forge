package forge.screens.rogue;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.FDeckEditor;
import forge.deck.FDeckViewer;
import forge.game.GameType;
import forge.gamemodes.rogue.RogueDeckEdits;
import forge.gamemodes.rogue.RogueRun;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.localinstance.achievements.RogueCommanderAchievements;
import forge.menu.FDropDownMenu;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.ItemPool;
import forge.util.Localizer;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * The run's deck in mobile's own deck editor. Only the run's basic lands can be added; removing a non-basic card
 * costs a removal credit, all or nothing, and Undo gives it back. Every change is saved at once.
 */
public class RogueDeckEditor extends FDeckEditor {
    private final RogueRun run;
    private final RogueDeckEdits edits;

    /** The editor for the run in memory; while a question is open the deck can only be looked at. */
    public static void openForRun() {
        RogueRun run = RogueController.get().flow().run();
        if (run == null) {
            return;
        }
        if (RogueController.get().busy()) {
            FDeckViewer.show(run.getCurrentDeck());
        } else {
            Forge.openScreen(open(run));
        }
    }

    public static RogueDeckEditor open(RogueRun run) {
        RogueDeckEdits edits = new RogueDeckEdits(run);
        Controller controller = new Controller(run);
        Holder holder = new Holder();
        RogueDeckEditor editor = new RogueDeckEditor(run, edits, controller, holder);
        holder.editor = editor;
        return editor;
    }

    /** The pages are made before the editor exists, so they reach it through this. */
    private static final class Holder {
        private RogueDeckEditor editor;
    }

    private RogueDeckEditor(RogueRun run, RogueDeckEdits edits, Controller controller, Holder holder) {
        super(config(run, controller, holder), controller);
        this.run = run;
        this.edits = edits;
        refreshHeader();
    }

    /** The header shows the removal credits left and Undo, beside the deck's name. */
    @Override
    protected DeckHeader initDeckHeader() {
        return add(new RogueHeader());
    }

    private final class RogueHeader extends DeckHeader {
        private final FLabel credits = add(new FLabel.Builder().icon(FSkinImage.CARD_IMAGE).font(FSkinFont.get(16)).build());
        private final FLabel undo = add(new FLabel.ButtonBuilder().text(Localizer.getInstance().getMessage("lblUndo"))
            .font(FSkinFont.get(14)).command(e -> undoLast()).build());

        @Override
        protected List<FDisplayObject> layoutHeaderElements(float height, float availableWidth) {
            List<FDisplayObject> out = super.layoutHeaderElements(height, availableWidth);
            undo.setSize(height * 2, height);
            out.add(undo);
            credits.setSize(height * 1.6f, height);
            out.add(credits);
            return out;
        }
    }

    private static DeckEditorConfig config(RogueRun run, Controller controller, Holder holder) {
        return new DeckEditorConfig() {
            @Override
            public GameType getGameType() {
                return GameType.Commander;
            }

            @Override
            public ItemPool<PaperCard> getCardPool() {
                ItemPool<PaperCard> pool = new ItemPool<>(PaperCard.class);
                pool.addAllFlat(run.basicLands());
                return pool;
            }

            @Override
            protected IDeckController getController() {
                return controller;
            }

            @Override
            public DeckSection[] getExtraSections() {
                return new DeckSection[0];
            }

            @Override
            public DeckSection[] getPrimarySections() {
                return new DeckSection[]{DeckSection.Main, DeckSection.Commander};
            }

            @Override
            protected DeckEditorPage[] getInitialPages() {
                Localizer localizer = Localizer.getInstance();
                return new DeckEditorPage[]{
                    new LandsPage(holder, localizer.getMessage("lblRogueBasicLands")),
                    new MainPage(holder),
                    new CommanderPage()
                };
            }
        };
    }

    private void refreshHeader() {
        RogueHeader header = (RogueHeader) deckHeader;
        header.credits.setText(String.valueOf(run.getRemovalCredits()));
        header.undo.setEnabled(edits.canUndo());
        header.revalidate();
    }

    private MainPage mainPage() {
        for (Object page : tabPages) {
            if (page instanceof MainPage main) {
                return main;
            }
        }
        return null;
    }

    private void undoLast() {
        MainPage main = mainPage();
        edits.undo(edit -> {
            ItemPool<PaperCard> pool = new ItemPool<>(PaperCard.class);
            for (Entry<PaperCard, Integer> entry : edit.cards()) {
                pool.add(entry.getKey(), entry.getValue());
            }
            if (edit.removal()) {
                main.addPool(pool);
            } else {
                main.removePool(pool);
            }
        });
        refreshHeader();
    }

    /** Only the run's own basic lands can be added, from their page. */
    @Override
    protected boolean allowAddBasic() {
        return false;
    }

    /** Leaving checks the run's achievements, on the flow thread as anything that can announce must be. */
    @Override
    public void onClose(Consumer<Boolean> canCloseCallback) {
        RogueController.get().submitIfIdle(() -> RogueCommanderAchievements.getInstance().evaluateRunAchievements(run));
        if (canCloseCallback != null) {
            canCloseCallback.accept(true);
        }
    }

    private static final class LandsPage extends CatalogPage {
        private final Holder holder;

        LandsPage(Holder holder, String caption) {
            super(new CardManager(false), ItemManagerConfig.CARD_CATALOG, caption, FSkinImage.LAND);
            this.holder = holder;
        }

        @Override
        public void moveCard(PaperCard card, CardManagerPage destination, int qty) {
            super.moveCard(card, destination, qty);
            holder.editor.edits.added(List.of(new AbstractMap.SimpleImmutableEntry<>(card, qty)));
            holder.editor.refreshHeader();
        }

        @Override
        public void moveCards(Iterable<Entry<PaperCard, Integer>> cards, CardManagerPage destination) {
            super.moveCards(cards, destination);
            holder.editor.edits.added(cards);
            holder.editor.refreshHeader();
        }

        @Override
        protected void addCommanderItems(FDropDownMenu menu, PaperCard card) {
        }
    }

    private static final class MainPage extends DeckSectionPage {
        private final Holder holder;

        MainPage(Holder holder) {
            super(new CardManager(false), DeckSection.Main, ItemManagerConfig.DECK_EDITOR);
            this.holder = holder;
        }

        void addPool(ItemPool<PaperCard> pool) {
            cardManager.addItems(pool);
            parentScreen.getDeckController().notifyModelChanged();
            updateCaption();
        }

        void removePool(ItemPool<PaperCard> pool) {
            cardManager.removeItems(pool);
            parentScreen.getDeckController().notifyModelChanged();
            updateCaption();
        }

        @Override
        public void moveCard(PaperCard card, CardManagerPage destination, int qty) {
            int amount = Math.min(cardManager.getItemCount(card), qty);
            RogueDeckEditor editor = holder.editor;
            String refusal = editor.edits.remove(List.of(new AbstractMap.SimpleImmutableEntry<>(card, amount)),
                () -> super.moveCard(card, destination, amount));
            if (refusal != null) {
                FOptionPane.showMessageDialog(refusal, Localizer.getInstance().getMessage("lblRogueRemovalLimitReached"),
                    FOptionPane.WARNING_ICON);
            }
            editor.refreshHeader();
        }

        @Override
        public void moveCards(Iterable<Entry<PaperCard, Integer>> cards, CardManagerPage destination) {
            RogueDeckEditor editor = holder.editor;
            String refusal = editor.edits.remove(cards, () -> super.moveCards(cards, destination));
            if (refusal != null) {
                FOptionPane.showMessageDialog(refusal, Localizer.getInstance().getMessage("lblRogueRemovalLimitReached"),
                    FOptionPane.WARNING_ICON);
            }
            editor.refreshHeader();
        }

        /** The run's commander cannot be changed. */
        @Override
        protected void addCommanderItems(FDropDownMenu menu, PaperCard card) {
        }
    }

    /** The commander cannot be changed. */
    private static final class CommanderPage extends DeckSectionPage {
        CommanderPage() {
            super(new CardManager(false), DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION);
        }

        @Override
        protected void buildMenu(FDropDownMenu menu, PaperCard card) {
        }

        @Override
        protected void onCardActivated(PaperCard card) {
        }
    }

    /** The deck is the run's own, so the editor's changes are the run's; saving is the edits' job. */
    private static final class Controller implements IDeckController {
        private final RogueRun run;
        private FDeckEditor editor;

        Controller(RogueRun run) {
            this.run = run;
        }

        @Override
        public void setEditor(FDeckEditor editor) {
            this.editor = editor;
            if (editor != null) {
                editor.notifyNewControllerModel();
            }
        }

        @Override
        public void setDeck(Deck deck) {
        }

        @Override
        public Deck getDeck() {
            return run.getCurrentDeck();
        }

        @Override
        public void newDeck() {
        }

        @Override
        public String getDeckDisplayName() {
            return run.getCurrentCommanderName();
        }

        @Override
        public void notifyModelChanged() {
        }

        @Override
        public void exitWithoutSaving() {
        }
    }
}
