package forge.gui.home.quest;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.game.GameType;
import forge.gui.home.EMenuGroup;
import forge.gui.home.EMenuItem;
import forge.gui.home.ICSubmenu;
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
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.QUEST;
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
        return "Quest Decks";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public String getItemEnum() {
        return EMenuItem.QUEST_DECKS.toString();
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getControl()
     */
    @Override
    public ICSubmenu getControl() {
        return CSubmenuQuestDecks.SINGLETON_INSTANCE;
    }

    /** @return {@link forge.gui.toolbox.DeckLister} */
    public DeckLister getLstDecks() {
        return this.lstDecks;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getBtnNewDeck() {
        return this.btnNewDeck;
    }
}
