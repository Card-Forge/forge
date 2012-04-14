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
import forge.gui.OverlayUtils;
import forge.gui.home.EMenuGroup;
import forge.gui.home.EMenuItem;
import forge.gui.home.ICSubmenu;
import forge.gui.home.IVSubmenu;
import forge.gui.home.StartButton;
import forge.gui.toolbox.DeckLister;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FOverlay;
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

    /** */
    private final JPanel pnl            = new JPanel();
    private final StartButton btnStart  = new StartButton();
    private final DeckLister lstHumanDecks = new DeckLister(GameType.Draft);
    private final JList lstAI           = new FList();
    private final JLabel btnBuildDeck   = new FLabel.Builder()
        .fontScaleAuto(false).fontSize(16)
        .opaque(true).hoverable(true).text("Start A New Draft").build();
    private final JLabel btnDirections = new FLabel.Builder()
        .fontScaleAuto(false).fontSize(16)
        .text("Click For Directions").fontAlign(SwingConstants.CENTER).build();

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        pnl.removeAll();
        pnl.setOpaque(false);
        pnl.setLayout(new MigLayout("insets 0, gap 0, hidemode 2"));

        lstAI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        btnStart.setEnabled(false);

        // Layout
        final JLabel lblHuman = new JLabel("Select your deck: ");
        lblHuman.setFont(FSkin.getBoldFont(16));
        lblHuman.setHorizontalAlignment(SwingConstants.CENTER);
        lblHuman.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        pnl.add(lblHuman, "w 60%!, gap 5% 5% 2% 2%");

        final JLabel lblAI = new JLabel("Who will you play?");
        lblAI.setFont(FSkin.getBoldFont(16));
        lblAI.setHorizontalAlignment(SwingConstants.CENTER);
        lblAI.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        pnl.add(lblAI, "w 25%!, gap 0 0 2% 2%, wrap");

        pnl.add(new FScrollPane(lstHumanDecks), "w 60%!, h 30%!, gap 5% 5% 2% 2%");

        pnl.add(new FScrollPane(lstAI), "w 25%!, h 37%!, gap 0 0 2% 0, span 1 2, wrap");

        pnl.add(btnBuildDeck, "w 60%!, h 5%!, gap 5% 5% 0 0, wrap");

        pnl.add(btnDirections, "alignx center, span 2 1, gap 5% 5% 5% 2%, wrap");

        pnl.add(btnStart, "gap 5% 5% 0 0, ax center, span 2 1, wrap");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SANCTIONED;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Draft Mode";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public String getItemEnum() {
        return EMenuItem.LIMITED_DRAFT.toString();
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getControl()
     */
    @Override
    public ICSubmenu getControl() {
        return CSubmenuDraft.SINGLETON_INSTANCE;
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
        final FOverlay overlay = OverlayUtils.genericOverlay();
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
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        final JButton btnCloseBig = new FButton("OK");
        btnCloseBig.setBounds(new Rectangle((w / 2 - 100), 510, 200, 30));
        btnCloseBig.addActionListener(new ActionListener() { @Override
            public void actionPerformed(ActionEvent arg0) { OverlayUtils.hideOverlay(); } });

        final FPanel pnl = new FPanel();
        pnl.setCornerDiameter(0);
        pnl.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));
        pnl.setLayout(new MigLayout("insets 0, gap 0"));
        pnl.add(tpnDirections, "w 90%!, h 90%!, gap 5% 0 5% 0");
        pnl.setBounds(new Rectangle((w / 2 - 250), 80, 500, 400));

        overlay.setLayout(null);
        overlay.add(btnCloseBig);
        overlay.add(pnl);
        OverlayUtils.showOverlay();
    }
}
