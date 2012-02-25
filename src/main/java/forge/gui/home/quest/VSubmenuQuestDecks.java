package forge.gui.home.quest;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.game.GameType;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.view.toolbox.DeckLister;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FScrollPane;

/** 
 * Singleton instance of "Colors" submenu in "Constructed" group.
 *
 */
public enum VSubmenuQuestDecks implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /** */
    private final JPanel pnl = new JPanel();
    private final DeckLister lstDecks = new DeckLister(GameType.Quest);
    private final FLabel btnNewDeck = new FLabel.Builder().opaque(true)
            .hoverable(true).text("Build a New Deck").fontScaleAuto(false).fontSize(18).build();

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
    public EMenuGroup getGroup() {
        return EMenuGroup.QUEST;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }

    /** @return {@link forge.view.toolbox.DeckLister} */
    public DeckLister getLstDecks() {
        return this.lstDecks;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnNewDeck() {
        return this.btnNewDeck;
    }
}
