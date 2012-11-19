package forge.gui.home.variant;

import java.util.Arrays;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import net.miginfocom.swing.MigLayout;
import forge.card.CardCoreType;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.game.player.PlayerType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.LblHeader;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FDeckChooser;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FSkin;
import forge.item.CardDb;
import forge.item.CardPrinted;

/** 
 * Assembles Swing components of constructed submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuVanguard implements IVSubmenu<CSubmenuVanguard> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Vanguard Mode");

    /** */
    private final LblHeader lblTitle = new LblHeader("Variant: Vanguard");

    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));

    private final StartButton btnStart  = new StartButton();

    private final JCheckBox cbSingletons = new FCheckBox("Singleton Mode");
    private final JCheckBox cbArtifacts = new FCheckBox("Remove Artifacts");
    private final JCheckBox cbRemoveSmall = new FCheckBox("Remove Small Creatures");

    private final FDeckChooser dcHuman = new FDeckChooser("Select your deck:", PlayerType.HUMAN);
    private final FDeckChooser dcAi = new FDeckChooser("Select AI deck:", PlayerType.COMPUTER);
    
    private final FList avHuman = new FList();
    private final FList avAi = new FList();
    private final JScrollPane scrHuman = new JScrollPane(avHuman, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private final JScrollPane scrAi = new JScrollPane(avAi, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    
    private final Predicate<CardPrinted> avatarTypePred = CardPrinted.Predicates.type("Vanguard");
    private final Iterable<CardPrinted> allAvatars = Iterables.filter(CardDb.instance().getAllCards(), avatarTypePred);

    private final FLabel lblAvatarHuman = new FLabel.Builder()
    .text("Human avatar:")
    .fontAlign(SwingConstants.CENTER)
    .build();
    private final FLabel lblAvatarAi = new FLabel.Builder()
    .text("AI Avatar:")
    .fontAlign(SwingConstants.CENTER)
    .build();
    
    private VSubmenuVanguard() {

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        Predicate<CardPrinted> typePred = CardPrinted.Predicates.type("Vanguard");
        
        Vector<Object> listData = new Vector<Object>();
        listData.add("Random");
        for(CardPrinted cp : Iterables.filter(CardDb.instance().getAllCards(), typePred))
        {
            listData.add(cp);
        }
        
        avHuman.setListData(listData);
        avAi.setListData(listData);
        avHuman.setSelectedIndex(0);
        avAi.setSelectedIndex(0);
        
        final String strCheckboxConstraints = "w 200px!, h 30px!, gap 0 20px 0 0";
        pnlStart.setOpaque(false);
        pnlStart.add(cbSingletons, strCheckboxConstraints);
        pnlStart.add(btnStart, "span 1 3, growx, pushx, align center");
        pnlStart.add(cbArtifacts, strCheckboxConstraints);
        pnlStart.add(cbRemoveSmall, strCheckboxConstraints);
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getGroupEnum()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.VARIANT;
    }

    public final FDeckChooser getDcHuman() {
        return dcHuman;
    }

    public final FDeckChooser getDcAi() {
        return dcAi;
    }
    
    
    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Vanguard";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_VANGUARD;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap 2, ax right"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 15px, span 2, ax right");
        
        dcAi.populate();
        dcHuman.populate();
        
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(dcAi, "w 44%!, gap 0 0 20px 20px, growy, pushy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(dcHuman, "w 44%!, gap 4% 4% 20px 20px, growy, pushy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblAvatarAi, "w 44%!, gap 0 0 0px 0px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblAvatarHuman, "w 44%!, gap 4% 4% 0px 0px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrAi, "w 44%!, gap 0 0 20px 20px, growy, pushy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrHuman, "w 44%!, gap 4% 4% 20px 20px, growy, pushy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStart, "span 2, gap 0 0 50px 50px, ax center");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
    }


    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }
    

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbSingletons() {
        return cbSingletons;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbArtifacts() {
        return cbArtifacts;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRemoveSmall() {
        return cbRemoveSmall;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_VANGUARD;
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
    public CSubmenuVanguard getLayoutControl() {
        return CSubmenuVanguard.SINGLETON_INSTANCE;
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

    /**
     * @return the avHuman
     */
    public FList getAvHuman() {
        return avHuman;
    }

    /**
     * @return the avAi
     */
    public FList getAvAi() {
        return avAi;
    }

    /**
     * @return the allAvatars
     */
    public Iterable<CardPrinted> getAllAvatars() {
        return allAvatars;
    }
}
