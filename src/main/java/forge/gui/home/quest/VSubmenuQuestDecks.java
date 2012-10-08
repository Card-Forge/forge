package forge.gui.home.quest;

import javax.swing.SwingConstants;

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
import forge.gui.toolbox.FSkin;

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
    private final FLabel lblTitle = new FLabel.Builder()
        .text("Quest Decks").fontAlign(SwingConstants.CENTER)
        .opaque(true).fontSize(16).build();

    private final DeckLister lstDecks = new DeckLister(GameType.Quest);
    private final FLabel btnNewDeck = new FLabel.Builder().opaque(true)
            .hoverable(true).text("Build a New Deck").fontSize(16).build();

    private final FScrollPane scr = new FScrollPane(lstDecks);

    /**
     * Constructor.
     */
    private VSubmenuQuestDecks() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        scr.setBorder(null);
        scr.getViewport().setBorder(null);
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, wrap"));
        parentCell.getBody().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        parentCell.getBody().add(scr, "w 98%!, growy, pushy, gap 1% 0 0 0");
        parentCell.getBody().add(btnNewDeck, "w 300px!, h 30px!, gap 0 0 50px 50px, ax center");
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
