package forge.gui.home.quest;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.game.GameType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.toolbox.DeckLister;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FScrollPane;

/** 
 * Assembles Swing components of quest decks submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuQuestDecks implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Constructed Mode");

    /** */
    private final JPanel pnl = new JPanel();
    private final DeckLister lstDecks = new DeckLister(GameType.Quest);
    private final FLabel btnNewDeck = new FLabel.Builder().opaque(true)
            .hoverable(true).text("Build a New Deck").fontSize(18).build();

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        final FScrollPane scr = new FScrollPane(lstDecks);
        final JPanel pnlDecks = new JPanel();
        scr.setBorder(null);
        scr.getViewport().setBorder(null);

        pnlDecks.setOpaque(false);
        pnlDecks.setLayout(new MigLayout("insets 0, wrap, alignx center, wrap"));

        pnlDecks.add(scr, "w 90%!, h 350px!");
        pnlDecks.add(btnNewDeck, "w 40%!, h 35px!, gap 25%! 0 20px 0");

        pnl.removeAll();
        pnl.setOpaque(false);
        pnl.setLayout(new MigLayout("insets 0"));
        pnl.add(pnlDecks, "w 90%!, gap 5% 0 5% 0");

        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0"));
        parentCell.getBody().add(pnl, "w 98%!, h 98%!, gap 1% 0 1% 0");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.QUEST;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Quest Decks";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_QUESTDECKS;
    }

    /** @return {@link forge.gui.toolbox.DeckLister} */
    public DeckLister getLstDecks() {
        return this.lstDecks;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getBtnNewDeck() {
        return this.btnNewDeck;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_QUESTDECKS;
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
    public ICDoc getLayoutControl() {
        return CSubmenuQuestDecks.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }
}
