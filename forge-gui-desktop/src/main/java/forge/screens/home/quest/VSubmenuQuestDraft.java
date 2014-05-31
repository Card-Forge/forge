package forge.screens.home.quest;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.assets.FSkinProp;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.LblHeader;
import forge.screens.home.StartButton;
import forge.screens.home.VHomeUI;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPanel;
import forge.toolbox.FSkin;

/**
 * Assembles Swing components of quest draft submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuQuestDraft implements IVSubmenu<CSubmenuQuestDraft> {
    
    SINGLETON_INSTANCE;
    
    protected static enum Mode {
        SELECT_TOURNAMENT,
        PREPARE_DECK,
        TOURNAMENT_ACTIVE
    }
    
    private final DragTab tab = new DragTab("Tournaments");
    
    private final LblHeader lblTitle = new LblHeader("Quest Mode: Draft Tournament");
    
    private final FLabel lblCredits = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COINSTACK))
        .iconScaleFactor(0.75)
        .fontSize(13).build();
    
    private final FScrollPanel pnlTournaments = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap, ax center"), true,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    
    private final JLabel lblInfo = new FLabel.Builder().text("Select a tournament to join:")
        .fontStyle(Font.BOLD).fontSize(16)
        .fontAlign(SwingConstants.LEFT).build();
    
    private final StartButton btnStartDraft  = new StartButton();
    private final StartButton btnStartTournament  = new StartButton();
    private final StartButton btnStartMatch  = new StartButton();
    
    private final FLabel btnEditDeck = new FLabel.ButtonBuilder().text("Edit Deck").fontSize(24).build();
    private final FLabel btnLeaveTournament = new FLabel.ButtonBuilder().text("Leave Tournament").fontSize(14).build();
    
    private final JLabel lblsStandings[] = new JLabel[15];
    
    private Mode mode = Mode.SELECT_TOURNAMENT;
    
    private DragCell parentCell;
    
    private VSubmenuQuestDraft() {
        for (int i = 0; i < 15; i++) {
            lblsStandings[i] = new FLabel.Builder().text("Standing Slot: " + i)
                    .fontStyle(Font.BOLD).fontSize(14)
                    .fontAlign(SwingConstants.LEFT).build();
        }
    }
    
    public LblHeader getLblTitle() {
        return lblTitle;
    }
    
    public FLabel getLblCredits() {
        return lblCredits;
    }
    
    public FScrollPanel getPnlTournaments() {
        return pnlTournaments;
    }
    
    public StartButton getBtnStartDraft() {
        return btnStartDraft;
    }
    
    public StartButton getBtnStartTournament() {
        return btnStartTournament;
    }
    
    public StartButton getBtnStartMatch() {
        return btnStartMatch;
    }
    
    public FLabel getBtnEditDeck() {
        return btnEditDeck;
    }
    
    public FLabel getBtnLeaveTournament() {
        return btnLeaveTournament;
    }
    
    public JLabel[] getLblsStandings() {
        return lblsStandings;
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    
    public Mode getMode() {
        return mode;
    }
    
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_QUESTDRAFTS;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CSubmenuQuestDraft getLayoutControl() {
        return CSubmenuQuestDraft.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.QUEST;
    }

    @Override
    public String getMenuTitle() {
        return "Tournaments";
    }

    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_QUESTDRAFTS;
    }

    @Override
    public void populate() {
        
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        
        switch (mode) {

            case SELECT_TOURNAMENT:
                populateSelectTournament();
                break;
            case PREPARE_DECK:
                populatePrepareDeck();
                break;
            case TOURNAMENT_ACTIVE:
                populateTournamentActive();
                break;
            default:
                break;
        
        }
        
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
        
    }
    
    private void populateSelectTournament() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, ax right, wrap"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 35px, ax right");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblCredits, "h 25px!, gap 0 0 30px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblInfo, "h 30px!, gap 0 0 5px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlTournaments, "w 95%, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStartDraft, "gap 0 6% 30px 30px, ax center");
    }
    
    private void populatePrepareDeck() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, ax center, wrap"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 20% 0 15px 35px, ax right");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnEditDeck, "w 150px, h 50px, gap 0 0 35% 0, ax center");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStartTournament, "gap 0 0 30px 0, ax center");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnLeaveTournament, "w 100px, h 35px, gap 0 0 100px 0, ax center");
    }
    
    private void populateTournamentActive() {
        
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, ax center, wrap"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 20% 0 15px 35px, ax right");
        
        FScrollPanel panel = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap 4, ax center"), true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        String constraints = "w 200px!, h 30px!, ay center";
        String groupingGap = ", gap 0 0 0 50px";
        
        panel.add(lblsStandings[0], constraints);
        panel.add(lblsStandings[8], constraints + ", span 1 2");
        panel.add(lblsStandings[12], constraints + ", span 1 4");
        panel.add(lblsStandings[14], constraints + ", span 1 8");
        panel.add(lblsStandings[1], constraints + groupingGap);
        panel.add(lblsStandings[2], constraints);
        panel.add(lblsStandings[9], constraints + ", span 1 2");
        panel.add(lblsStandings[3], constraints + groupingGap);
        panel.add(lblsStandings[4], constraints);
        panel.add(lblsStandings[10], constraints + ", span 1 2");
        panel.add(lblsStandings[13], constraints + ", span 1 4");
        panel.add(lblsStandings[5], constraints + groupingGap);
        panel.add(lblsStandings[6], constraints);
        panel.add(lblsStandings[11], constraints + ", span 1 2");
        panel.add(lblsStandings[7], constraints);
        
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(panel, "gap 0 0 30px 0, ax center");
        
        /*for (JLabel label : lblsStandings) {
            VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(label, "h 30px!, gap 0 0 5px");
        }*/
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStartMatch, "gap 0 0 30px 0, ax center");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnLeaveTournament, "w 120px, h 35px, gap 0 0 100px 0, ax center");
    }
    
}
