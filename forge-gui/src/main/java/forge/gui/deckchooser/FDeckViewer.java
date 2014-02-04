package forge.gui.deckchooser;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.card.Card;
import forge.gui.CardDetailPanel;
import forge.gui.CardPicturePanel;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.itemmanager.CardManager;
import forge.gui.toolbox.itemmanager.ItemManagerContainer;
import forge.gui.toolbox.itemmanager.ItemManagerModel;
import forge.gui.toolbox.itemmanager.views.GroupDef;
import forge.gui.toolbox.itemmanager.views.ImageView;
import forge.gui.toolbox.itemmanager.views.ItemColumn;
import forge.gui.toolbox.itemmanager.views.SColumnUtil;
import forge.gui.toolbox.itemmanager.views.ColumnDef;
import forge.item.PaperCard;
import forge.view.FDialog;

@SuppressWarnings("serial")
public class FDeckViewer extends FDialog {
    private final Deck deck;
    private final List<DeckSection> sections = new ArrayList<DeckSection>();
    private final CardManager cardManager;
    private DeckSection currentSection;

    private final CardDetailPanel cardDetail = new CardDetailPanel(null);
    private final CardPicturePanel cardPicture = new CardPicturePanel();
    private final FButton btnCopyToClipboard = new FButton("Copy to Clipboard");
    private final FButton btnChangeSection = new FButton("Change Section");
    private final FButton btnClose = new FButton("Close");

    public static void show(final Deck deck) {
        if (deck == null) { return; }

        FDeckViewer deckViewer = new FDeckViewer(deck);
        deckViewer.setVisible(true);
        deckViewer.dispose();
    }

    private FDeckViewer(Deck deck0) {
        this.deck = deck0;
        this.setTitle(deck.getName());
        this.cardManager = new CardManager(false) {
            @Override //show hovered card in Image View in dialog instead of main Detail/Picture panes
            protected ImageView<PaperCard> createImageView(final ItemManagerModel<PaperCard> model0) {
                return new ImageView<PaperCard>(this, model0) {
                    @Override
                    protected void showHoveredItem(PaperCard item) {
                        Card card = Card.getCardForUi(item);
                        if (card == null) { return; }

                        cardDetail.setCard(card);
                        cardPicture.setCard(card);
                    }
                };
            }
        };
        this.cardManager.setPool(deck.getMain());
        this.cardManager.addSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                PaperCard paperCard = cardManager.getSelectedItem();
                if (paperCard == null) { return; }

                Card card = Card.getCardForUi(paperCard);
                if (card == null) { return; }

                cardDetail.setCard(card);
                cardPicture.setCard(card);
            }
        });
        this.setDefaultFocus(this.cardManager.getCurrentView().getComponent());

        for (Entry<DeckSection, CardPool> entry : deck) {
            this.sections.add(entry.getKey());
        }
        this.currentSection = DeckSection.Main;
        updateCaption();

        this.btnCopyToClipboard.setFocusable(false);
        this.btnCopyToClipboard.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                FDeckViewer.this.copyToClipboard();
            }
        });
        this.btnChangeSection.setFocusable(false);
        if (this.sections.size() > 1) {
            this.btnChangeSection.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    FDeckViewer.this.changeSection();
                }
            });
        }
        else {
            this.btnChangeSection.setEnabled(false);
        }
        this.btnClose.setFocusable(false);
        this.btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                FDeckViewer.this.setVisible(false);
            }
        });

        final int width = 800;
        final int height = 600;
        this.setPreferredSize(new Dimension(width, height));
        this.setSize(width, height);

        this.cardPicture.setOpaque(false);

        JPanel cardPanel = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
        cardPanel.setOpaque(false);
        cardPanel.add(this.cardDetail, "w 225px, h 240px, gapbottom 10px");
        cardPanel.add(this.cardPicture, "w 225px, h 350px, gapbottom 10px");

        JPanel buttonPanel = new JPanel(new MigLayout("insets 0, gap 0"));
        buttonPanel.setOpaque(false);
        buttonPanel.add(this.btnCopyToClipboard, "w 200px!, h 26px!, gapright 5px");
        buttonPanel.add(this.btnChangeSection, "w 200px!, h 26px!");

        this.add(new ItemManagerContainer(this.cardManager), "push, grow, gapright 10px, gapbottom 10px");
        this.add(cardPanel, "wrap");
        this.add(buttonPanel);
        this.add(this.btnClose, "w 120px!, h 26px!, ax right");

        Map<ColumnDef, ItemColumn> columns = SColumnUtil.getDeckDefaultColumns();
        columns.remove(ColumnDef.DECK_QUANTITY); //prevent displaying +/- buttons
        ItemColumn qtyColumn = new ItemColumn(ColumnDef.QUANTITY);
        qtyColumn.setIndex(0);
        columns.put(ColumnDef.QUANTITY, qtyColumn);
        this.cardManager.setup(columns, GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC);
    }

    private void changeSection() {
        int index = sections.indexOf(currentSection);
        index = (index + 1) % sections.size();
        currentSection = sections.get(index);
        this.cardManager.setPool(this.deck.get(currentSection));
        updateCaption();
    }

    private void updateCaption() {
        this.cardManager.setCaption(deck.getName() + " - " + currentSection.name());
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

        final StringSelection ss = new StringSelection(deckList.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
        FOptionPane.showMessageDialog("Deck list for '" + deck.getName() + "' copied to clipboard.");
    }
}
