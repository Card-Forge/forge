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

import javax.swing.JLabel;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.itemmanager.CardManager;
import forge.gui.toolbox.itemmanager.ItemManagerContainer;
import forge.gui.toolbox.itemmanager.views.ItemCellRenderer;
import forge.gui.toolbox.itemmanager.views.ItemColumn;
import forge.gui.toolbox.itemmanager.views.SColumnUtil;
import forge.gui.toolbox.itemmanager.views.ItemColumn.ColumnDef;
import forge.item.PaperCard;
import forge.view.FDialog;

@SuppressWarnings("serial")
public class FDeckViewer extends FDialog {
    private final Deck deck;
    private final List<DeckSection> sections = new ArrayList<DeckSection>();
    private final CardManager cardManager;
    private DeckSection currentSection;

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
        this.cardManager = new CardManager(false);
        this.cardManager.setPool(deck.getMain());
        this.setDefaultFocus(this.cardManager.getTable().getComponent());

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

        final int width = 700;
        final int height = 600;
        this.setPreferredSize(new Dimension(width, height));
        this.setSize(width, height);

        this.add(new ItemManagerContainer(this.cardManager), "w 100%, pushy, growy, spanx 4, gapbottom 10px, wrap");
        this.add(this.btnCopyToClipboard, "w 200px!, h 26px!, gapright 5px");
        this.add(this.btnChangeSection, "w 200px!, h 26px!");
        this.add(new JLabel(), "pushx, growx");
        this.add(this.btnClose, "w 120px!, h 26px!");

        Map<ColumnDef, ItemColumn> columns = SColumnUtil.getDeckDefaultColumns();
        columns.get(ColumnDef.DECK_QUANTITY).setCellRenderer(new ItemCellRenderer()); //prevent displaying +/- buttons
        this.cardManager.getTable().setup(columns);
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
