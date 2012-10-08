package forge.gui.home.sanctioned;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import forge.game.GameType;
import forge.gui.SOverlayUtils;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.StartButton;
import forge.gui.toolbox.DeckLister;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of draft submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuDraft implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Draft Mode");

    /** */
    private final FLabel lblTitle = new FLabel.Builder()
        .text("Sanctioned Format: Draft").fontAlign(SwingConstants.CENTER)
        .fontSize(16).opaque(true).build();

    private final StartButton btnStart  = new StartButton();

    private final DeckLister lstHumanDecks = new DeckLister(GameType.Draft);
    private final JList lstAI = new FList();

    private final JLabel lblAI = new FLabel.Builder()
        .text("Who will you play?").fontSize(16).fontAlign(SwingConstants.CENTER).build();

    private final JLabel lblHuman = new FLabel.Builder()
        .text("Select your deck:").fontSize(16).fontAlign(SwingConstants.CENTER).build();
    private final JLabel btnPlayThisOpponent = new FLabel.Builder()
        .fontSize(16).opaque(true).hoverable(true).text("Play this opponent").build();
    private final JLabel btnBuildDeck = new FLabel.Builder()
        .fontSize(16).opaque(true).hoverable(true).text("Start A New Draft").build();
    private final JLabel btnDirections = new FLabel.Builder()
        .fontSize(16).opaque(true).hoverable(true).text("How To Play").build();

    /**
     * Constructor.
     */
    private VSubmenuDraft() {
        lstAI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        btnStart.setEnabled(false);

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SANCTIONED;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Draft Mode";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_DRAFT;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnDirections() {
        return this.btnDirections;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnBuildDeck() {
        return this.btnBuildDeck;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link javax.swing.JButton} */
    public JLabel getBtnPlayThisOpponent() {
        return this.btnPlayThisOpponent;
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstAIDecks() {
        return lstAI;
    }

    /** @return {@link forge.gui.toolbox.DeckLister} */
    public DeckLister getLstHumanDecks() {
        return lstHumanDecks;
    }

    /** */
    public void showDirections() {
        final JPanel overlay = SOverlayUtils.genericOverlay();
        final int w = overlay.getWidth();

        final String instructions = "BOOSTER DRAFT MODE INSTRUCTIONS"
                + "\r\n\r\n"
                + "In a booster draft, several players (usually eight) are seated "
                + "around a table and each player is given three booster packs."
                + "\r\n\r\n"
                + "Each player opens a pack, selects a card from it and passes the remaining "
                + "cards to his or her left. Each player then selects one of the 14 remaining "
                + "cards from the pack that was just passed to him or her, and passes the "
                + "remaining cards to the left again. This continues until all of the cards "
                + "are depleted. The process is repeated with the second and third packs, "
                + "except that the cards are passed to the right in the second pack."
                + "\r\n\r\n"
                + "Players then build decks out of any of the cards that they selected "
                + "during the drafting and add as many basic lands as they want."
                + "\r\n\r\n"
                + "Credit: Wikipedia";

        // Init directions text pane
        final JTextPane tpnDirections = new JTextPane();
        tpnDirections.setOpaque(false);
        tpnDirections.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tpnDirections.setFont(FSkin.getFont(15));
        tpnDirections.setAlignmentX(SwingConstants.CENTER);
        tpnDirections.setFocusable(false);
        tpnDirections.setEditable(false);
        tpnDirections.setBorder(null);
        tpnDirections.setText(instructions);

        final StyledDocument doc = tpnDirections.getStyledDocument();
        final SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        final JButton btnCloseBig = new FButton("OK");
        btnCloseBig.setBounds(new Rectangle((w / 2 - 100), 510, 200, 30));
        btnCloseBig.addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) { SOverlayUtils.hideOverlay(); } });

        final FPanel pnl = new FPanel();
        pnl.setCornerDiameter(0);
        pnl.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));
        pnl.setLayout(new MigLayout("insets 0, gap 0"));
        pnl.add(tpnDirections, "w 90%!, h 90%!, gap 5% 0 5% 0");
        pnl.setBounds(new Rectangle((w / 2 - 250), 80, 500, 400));

        overlay.setLayout(null);
        overlay.add(btnCloseBig);
        overlay.add(pnl);
        SOverlayUtils.showOverlay();
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, wrap 2, hidemode 2"));
        parentCell.getBody().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px, span 2");

        parentCell.getBody().add(lblHuman, "w 60%!, gap 1% 8% 0 15px");
        parentCell.getBody().add(lblAI, "w 30%!, gap 0 0 0 15px");

        parentCell.getBody().add(new FScrollPane(lstHumanDecks), "w 60%!, gap 1% 8% 0 0, pushy, growy");
        parentCell.getBody().add(new FScrollPane(lstAI), "w 30%!, pushy, growy");

        parentCell.getBody().add(btnBuildDeck, "w 60%!, h 30px!, gap 1% 5% 10px 0");
        parentCell.getBody().add(btnPlayThisOpponent, "w 30%!, h 30px!, gap 0 0 10px 0");

        parentCell.getBody().add(btnDirections, "w 200px!, h 30px!, gap 1% 0 20px 0, span 2, ax center");
        parentCell.getBody().add(btnStart, "w 98%!, gap 1% 0 50px 50px, ax center, span 2");
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_DRAFT;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
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
        return parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public ICDoc getLayoutControl() {
        return CSubmenuDraft.SINGLETON_INSTANCE;
    }
}
