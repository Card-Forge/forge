package forge.screens.deckeditor.views;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.deck.DeckBase;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.screens.deckeditor.controllers.CProbabilities;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;

/**
 * Assembles Swing components of deck editor analysis tab.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VProbabilities implements IVDoc<CProbabilities> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Draw Order");

    // Title labels
    private final FLabel lblReshuffle = new FLabel.Builder()
            .hoverable(true).text("CLICK HERE TO RE-SHUFFLE").tooltip("See a new sample shuffle")
            .fontSize(16).build();
    private final FLabel lblSampleHand = new FLabel.Builder().fontStyle(Font.BOLD)
            .fontSize(12).text("SAMPLE HAND").opaque(true).build();
    private final FLabel lblRemainingDraws = new FLabel.Builder().fontStyle(Font.BOLD)
            .fontSize(12).text("REMAINING DRAWS").opaque(true).build();
    // private final JLabel lblExplanation = new FLabel.Builder()
    //       .fontSize(11).text("XX % = frequency that card will appear at that position").build();

    // Layout containers
    private final JPanel pnlContent = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final FScrollPane scroller = new FScrollPane(pnlContent, false);
    private final JPanel pnlHand = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final JPanel pnlLibrary = new JPanel(new MigLayout("insets 0, gap 0, wrap"));

    //========== Constructor
    private VProbabilities() {
        pnlContent.setOpaque(false);
        pnlHand.setOpaque(false);
        pnlLibrary.setOpaque(false);
        scroller.getViewport().setBorder(null);

        lblSampleHand.setBorder(new FSkin.MatteSkinBorder(1, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        lblSampleHand.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        lblRemainingDraws.setBorder(new FSkin.MatteSkinBorder(1, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        lblRemainingDraws.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Core layout
        pnlContent.add(lblReshuffle, "w 96%!, h 29px!, gap 2% 0 5px 5px");
        pnlContent.add(lblSampleHand, "w 96%!, h 25px!, gap 2% 0 0 0");
        // pnlContent.add(lblExplanation, "w 96%!, h 25px!, gap 2% 0 0 0");
        pnlContent.add(pnlHand, "w 96%!, gap 2% 0 0 5px");
        pnlContent.add(lblRemainingDraws, "w 96%!, h 25px!, gap 2% 0 0 0");
        pnlContent.add(pnlLibrary, "w 96%!, gap 2% 0 5px 0");
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_PROBABILITIES;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CProbabilities getLayoutControl() {
        return CProbabilities.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0"));
        parentCell.getBody().add(scroller, "w 96%!, h 96%!, gap 2% 0 2% 0");
    }

    //========== Retrieval methods
    /** @return {@link javax.swing.JLabel} */
    public FLabel getLblReshuffle() {
        return lblReshuffle;
    }

    //========== Other methods
    /** @param shuffledVals &emsp; A map of card names and their positional probability. */
    public void rebuildLabels(final List<String> shuffledVals) {
        pnlHand.removeAll();
        pnlLibrary.removeAll();

        JLabel lbl;
        final String constraints = "w 96%, h 25px!, gap 2% 0 0 0";

        for (int i = 0; i < shuffledVals.size(); i++) {
            lbl = (i % 2 == 1 ? buildLabel(true) : buildLabel(false));
            lbl.setText(shuffledVals.get(i));

            if (i < 7) { pnlHand.add(lbl, constraints);  }
            else { pnlLibrary.add(lbl, constraints); }
        }

        pnlHand.validate();
        pnlLibrary.validate();
    }

    private static <T extends InventoryItem, TModel extends DeckBase> JLabel buildLabel(final boolean zebra) {
        final FLabel lbl = new FLabel.Builder().text("--")
                .fontAlign(SwingConstants.CENTER).fontSize(13)
                .build();

        lbl.addMouseListener(new MouseAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void mouseEntered(final MouseEvent e) {
                final ACEditorBase<T, TModel> ed = (ACEditorBase<T, TModel>)
                        CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

                final List<PaperCard> cards = (List<PaperCard>) ed.getDeckManager().getPool().toFlatList();
                final String name1 = lbl.getText();
                String name2;

                for (final PaperCard c : cards) {
                    name2 = c.getName();
                    if (name2.length() > name1.length()) { continue; }

                    if (name2.equals(name1.substring(0, name2.length()))) {
                        CDeckEditorUI.SINGLETON_INSTANCE.setCard(c);
                        break;
                    }
                }
            }
        });

        if (zebra) {
            lbl.setOpaque(true);
            lbl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        }

        return lbl;
    }
}
