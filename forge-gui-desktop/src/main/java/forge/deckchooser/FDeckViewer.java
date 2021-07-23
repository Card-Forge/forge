package forge.deckchooser;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.card.CardView;
import forge.gui.CardDetailPanel;
import forge.gui.CardPicturePanel;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerContainer;
import forge.itemmanager.ItemManagerModel;
import forge.itemmanager.views.ImageView;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;
import forge.view.FDialog;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

@SuppressWarnings("serial")
public class FDeckViewer extends FDialog {
    private final Deck deck;
    private final List<DeckSection> sections = new ArrayList<>();
    private final CardManager cardManager;
    private DeckSection currentSection;

    private final CardDetailPanel cardDetail = new CardDetailPanel();
    private final CardPicturePanel cardPicture = new CardPicturePanel();
    private final FButton btnCopyToClipboard = new FButton(Localizer.getInstance().getMessage("btnCopyToClipboard"));
    private final FButton btnChangeSection = new FButton(Localizer.getInstance().getMessage("lblChangeSection"));
    private final FButton btnClose = new FButton(Localizer.getInstance().getMessage("lblClose"));

    public static void show(final Deck deck) {
        if (deck == null) { return; }

        final FDeckViewer deckViewer = new FDeckViewer(deck);
        deckViewer.setVisible(true);
        deckViewer.dispose();
    }

    private FDeckViewer(final Deck deck0) {
        this.deck = deck0;
        this.setTitle(deck.getName());
        this.cardManager = new CardManager(null, false, false) {
            @Override //show hovered card in Image View in dialog instead of main Detail/Picture panes
            protected ImageView<PaperCard> createImageView(final ItemManagerModel<PaperCard> model0) {
                return new ImageView<PaperCard>(this, model0) {
                    @Override
                    protected void showHoveredItem(PaperCard item) {
                        final CardView card = CardView.getCardForUi(item);
                        if (card == null) { return; }

                        cardDetail.setCard(card);
                        cardPicture.setCard(card.getCurrentState());
                    }
                };
            }
        };
        this.cardManager.setPool(deck.getMain());
        this.cardManager.addSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                final IPaperCard paperCard = cardManager.getSelectedItem();
                if (paperCard == null) { return; }

                final CardView card = CardView.getCardForUi(paperCard);
                if (card == null) { return; }

                cardDetail.setCard(card);
                cardPicture.setCard(card.getCurrentState());
            }
        });

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

        final int width;
        final int height;
        if(FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_SMALL_DECK_VIEWER)){
            width = 800;
            height = 600;
        }
        else {
            GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
            width = (int)(gd.getDisplayMode().getWidth() * 0.7);
            height = (int)(gd.getDisplayMode().getHeight() * 0.8);
        }

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

        this.cardManager.setup(ItemManagerConfig.DECK_VIEWER);
        this.setDefaultFocus(this.cardManager.getCurrentView().getComponent());
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
        String cardName;
        SortedMap<String, Integer> sectionCards;
        deckList.append(dName == null ? "" : dName + nl + nl);

        for (DeckSection s : DeckSection.values()){
            CardPool cp = deck.get(s);
            if (cp == null || cp.isEmpty()) {
                continue;
            }
            deckList.append(s.toString()).append(": ");
            sectionCards = new TreeMap<>();
            deckList.append(nl);
            for (final Entry<PaperCard, Integer> ev : cp) {
                cardName = ev.getKey().toString();
                if (sectionCards.containsKey(cardName)) {
                    sectionCards.put(cardName, (int)sectionCards.get(cardName) + ev.getValue());
                }
                else {
                    sectionCards.put(cardName, ev.getValue());
                }
            }
            for (final Entry<String, Integer> ev: sectionCards.entrySet()) {
                deckList.append(ev.getValue()).append(" ").append(ev.getKey()).append(nl);
            }
            deckList.append(nl);
        }

        final StringSelection ss = new StringSelection(deckList.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
        FOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblDeckListCopiedClipboard", deck.getName()));
    }
}
