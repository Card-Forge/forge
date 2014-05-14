package forge.deck;

import forge.Forge;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.screens.FScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FOptionPane;
import forge.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class FDeckViewer extends FScreen {
    private static final float BUTTON_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.8f;
    private static final float PADDING = Utils.AVG_FINGER_HEIGHT * 0.1f;

    private final Deck deck;
    private final List<DeckSection> sections = new ArrayList<DeckSection>();
    private final CardManager cardManager;
    private DeckSection currentSection;

    private final FButton btnCopyToClipboard = new FButton("Copy to Clipboard");
    private final FButton btnChangeSection = new FButton("Change Section");

    public static void show(final Deck deck) {
        if (deck == null) { return; }

        Forge.openScreen(new FDeckViewer(deck));
    }

    private FDeckViewer(Deck deck0) {
        super(true, deck0.getName());
        deck = deck0;
        cardManager = new CardManager(false);
        cardManager.setPool(deck.getMain());

        for (Entry<DeckSection, CardPool> entry : deck) {
            sections.add(entry.getKey());
        }
        currentSection = DeckSection.Main;
        updateCaption();

        btnCopyToClipboard.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                copyToClipboard();
            }
        });
        if (sections.size() > 1) {
            btnChangeSection.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    changeSection();
                }
            });
        }
        else {
            btnChangeSection.setEnabled(false);
        }

        add(cardManager);
        add(btnCopyToClipboard);
        add(btnChangeSection);

        cardManager.setup(ItemManagerConfig.DECK_VIEWER);
    }

    private void changeSection() {
        int index = sections.indexOf(currentSection);
        index = (index + 1) % sections.size();
        currentSection = sections.get(index);
        cardManager.setPool(deck.get(currentSection));
        updateCaption();
    }

    private void updateCaption() {
        cardManager.setCaption(currentSection.name());
    }

    private void copyToClipboard() {
        final String nl = System.getProperty("line.separator");
        final StringBuilder deckList = new StringBuilder();
        final String dName = deck.getName();
        deckList.append(dName == null ? "" : dName + nl + nl);

        for (DeckSection s : DeckSection.values()){
            CardPool cp = deck.get(s);
            if (cp == null || cp.isEmpty()) {
                continue;
            }
            deckList.append(s.toString()).append(": ");
            if (s.isSingleCard()) {
                deckList.append(cp.get(0).getName()).append(nl);
            }
            else {
                deckList.append(nl);
                for (final Entry<PaperCard, Integer> ev : cp) {
                    deckList.append(ev.getValue()).append(" ").append(ev.getKey()).append(nl);
                }
            }
            deckList.append(nl);
        }

        Forge.getClipboard().setContents(deckList.toString());
        FOptionPane.showMessageDialog("Deck list for '" + deck.getName() + "' copied to clipboard.");
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        cardManager.setBounds(PADDING, startY, width - 2 * PADDING, height - BUTTON_HEIGHT - 2 * PADDING - startY);

        float y = height - BUTTON_HEIGHT - PADDING;
        float buttonWidth = (width - 3 * PADDING) / 2;
        btnCopyToClipboard.setBounds(PADDING, y, buttonWidth, BUTTON_HEIGHT);
        btnChangeSection.setBounds(buttonWidth + 2 * PADDING, y, buttonWidth, BUTTON_HEIGHT);
    }
}
