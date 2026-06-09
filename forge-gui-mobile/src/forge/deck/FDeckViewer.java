package forge.deck;

import java.util.Map.Entry;
import java.util.regex.Pattern;

import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.assets.FImage;
import forge.assets.FSkinImage;
import forge.assets.ImageCache;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.ItemFilter;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.screens.FScreen;
import forge.screens.match.MatchController;
import forge.toolbox.FOptionPane;
import forge.util.ItemPool;
import org.apache.commons.lang3.StringUtils;

public class FDeckViewer extends FScreen {
    private static FDeckViewer deckViewer;
    private static FPopupMenu menu = new FPopupMenu() {
        @Override
        protected void buildMenu() {
            Deck deck = deckViewer.deck;
            for (Entry<DeckSection, CardPool> entry : deck) {
                final DeckSection section = entry.getKey();
                final CardPool pool = entry.getValue();
                int count = pool.countAll();
                if (count == 0) { continue; }

                final String captionPrefix = section.getLocalizedName();
                final FImage icon = FDeckEditor.iconFromDeckSection(section);

                FMenuItem item = new FMenuItem(captionPrefix + " (" + count + ")", icon, e -> deckViewer.setCurrentSection(section));
                if (section == deckViewer.currentSection) {
                    item.setSelected(true);
                }
                addItem(item);
            }
            addItem(new FMenuItem(Forge.getLocalizer().getMessage("btnCopyToClipboard"), Forge.hdbuttons ? FSkinImage.HDEXPORT : FSkinImage.BLANK, e -> copyDeckToClipboard(deckViewer.deck)));
        }
    };

    public static void copyDeckToClipboard(Deck deck) {
        Forge.getClipboard().setContents(deck.generateTextExport());
        FOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblDeckListCopiedClipboard", deck.getName()));
    }

    public static void copyCollectionToClipboard(CardPool pool) {
        ItemPool<PaperCard> autoSellCards = AdventurePlayer.current().getAutoSellCards();
        CardPool playedCards = pool.getFilteredPool(card -> !autoSellCards.contains(card));

        final String nl = System.lineSeparator();
        final StringBuilder collectionList = new StringBuilder();
        collectionList.append("\"Count\",\"Name\",\"Edition\",\"Collector Number\",\"Foil\"").append(nl);
        Pattern regexQuote = Pattern.compile("\"");
        Pattern regexEdPlst = Pattern.compile("PLIST|MB1");
        Pattern regexEdNem = Pattern.compile("NMS");
        Pattern regexEdP02 = Pattern.compile("PO2");

        for (final Entry<PaperCard, Integer> entry : playedCards) {
            PaperCard card = entry.getKey();
            if (!card.isVeryBasicLand()) {
                Integer count = entry.getValue();
                String cleanCardName = regexQuote.matcher(card.getCardName()).replaceAll("\"\"");
                // Moxfield import will choke on accented characters so replace them with ASCII equivalents
                cleanCardName = StringUtils.stripAccents(cleanCardName);
                String cleanCardEdition = regexEdPlst.matcher(card.getEdition()).replaceAll("PLST");
                cleanCardEdition = regexEdNem.matcher(cleanCardEdition).replaceAll("NEM");
                cleanCardEdition = regexEdP02.matcher(cleanCardEdition).replaceAll("P02");
                String cardLine = "\"" + count + "\",\"" + cleanCardName + "\",\"" + cleanCardEdition + "\",\"" + card.getCollectorNumber() + "\",\"" + (card.isFoil() ? "foil" : "") + "\"" + nl;
                collectionList.append(cardLine);
            }
        }

        Forge.getClipboard().setContents(collectionList.toString());
        FOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblCollectionCopiedClipboard"));
    }

    private final Deck deck;
    private final CardManager cardManager;
    private DeckSection currentSection;

    public static void show(final Deck deck0) {
        show(deck0, false, false);
    }
    public static void show(final Deck deck0, boolean noPreload) {
        show(deck0, noPreload, false);
    }
    public static void show(final Deck deck0, boolean noPreload, boolean showRanking) {
        if (deck0 == null) { return; }

        if (!noPreload){
            /*preload deck to cache*/
            ImageCache.getInstance().preloadCache(deck0);
        }

        deckViewer = new FDeckViewer(deck0, showRanking);
        deckViewer.setRotate180(MatchController.getView() != null && MatchController.getView().isTopHumanPlayerActive());
        Forge.openScreen(deckViewer);
    }

    private FDeckViewer(Deck deck0, boolean showRanking) {
        super(new MenuHeader(deck0.getName(), menu) {
            @Override
            protected boolean displaySidebarForLandscapeMode() {
                return false;
            }
        });
        deck = deck0;
        cardManager = new CardManager(false);
        cardManager.setPool(deck.getMain());
        cardManager.setShowRanking(showRanking);

        currentSection = DeckSection.Main;
        updateCaption();

        add(cardManager);

        cardManager.setup(ItemManagerConfig.DECK_VIEWER);
    }

    private void setCurrentSection(DeckSection currentSection0) {
        if (currentSection == currentSection0) { return; }
        currentSection = currentSection0;
        cardManager.setPool(deck.get(currentSection));
        updateCaption();
    }

    private void updateCaption() {
        cardManager.setCaption(currentSection.name());
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = 0;
        if (Forge.isLandscapeMode()) { //add some horizontal padding in landscape mode
            x = ItemFilter.PADDING;
            width -= 2 * x;
        }
        cardManager.setBounds(x, startY, width, height - startY);
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null; //never use backdrop for editor
    }
}
