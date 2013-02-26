package forge.gui.home.quest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import forge.Command;
import forge.Singletons;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.CHomeUI;
import forge.gui.toolbox.JXButtonPanel;
import forge.quest.QuestController;
import forge.quest.QuestEventDuel;
import forge.quest.bazaar.QuestPetController;

/** 
 * Controls the quest duels submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuDuels implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#initialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

        view.getBtnSpellShop().setCommand(
                new Command() { @Override
                    public void execute() { SSubmenuQuestUtil.showSpellShop(); } });

        view.getBtnBazaar().setCommand(
                new Command() { @Override
                    public void execute() { SSubmenuQuestUtil.showBazaar(); } });

        view.getBtnTravel().setCommand(
                new Command() { @Override
                    public void execute() { SSubmenuQuestUtil.travelWorld(); CSubmenuDuels.this.update(); } });

        view.getBtnUnlock().setCommand(
                new Command() { @Override
                    public void execute() { SSubmenuQuestUtil.chooseAndUnlockEdition(); CSubmenuDuels.this.update(); } });

        view.getBtnStart().addActionListener(
                new ActionListener() { @Override
            public void actionPerformed(final ActionEvent e) { SSubmenuQuestUtil.startGame(); } });

        final QuestController quest = Singletons.getModel().getQuest();
        view.getCbPlant().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                quest.selectPet(0, view.getCbPlant().isSelected() ? "Plant" : null);
                quest.save();
            }
        });

        view.getCbxPet().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final int slot = 1;
                final int index = view.getCbxPet().getSelectedIndex();
                List<QuestPetController> pets = quest.getPetsStorage().getAvaliablePets(slot, quest.getAssets());
                String petName = index <= 0 || index > pets.size() ? null : pets.get(index - 1).getName();
                quest.selectPet(slot, petName);
                quest.save();
            }
        });
    }

    private final KeyAdapter _startOnEnter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (KeyEvent.VK_ENTER == e.getKeyChar()) {
                VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().doClick();
            }
        }
    };
    private final MouseAdapter _startOnDblClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (MouseEvent.BUTTON1 == e.getButton() && 2 == e.getClickCount()) {
                VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().doClick();
            }
        }
    };
    
    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        SSubmenuQuestUtil.updateQuestView(VSubmenuDuels.SINGLETON_INSTANCE);

        final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

        if (Singletons.getModel().getQuest().getAchievements() != null) {
            view.getLblTitle().setText("Duels: " + Singletons.getModel().getQuest().getRank());

            view.getPnlDuels().removeAll();
            final List<QuestEventDuel> duels = Singletons.getModel().getQuest().getDuelsManager().generateDuels();

            JXButtonPanel grpPanel = new JXButtonPanel();

            for (int i = 0; i < duels.size(); i++) {
                final PnlEvent temp = new PnlEvent(duels.get(i));
                final JRadioButton rad = temp.getRad();
                if (i == 0) {
                    rad.setSelected(true);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() { rad.requestFocusInWindow(); }
                    });
                }
                rad.addKeyListener(_startOnEnter);
                rad.addMouseListener(_startOnDblClick);
                grpPanel.add(temp, rad, "w 100%!, h 135px!, gap 2% 0 15px 15px");
            }
            view.getPnlDuels().add(grpPanel, "w 96%!");
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @SuppressWarnings("serial")
    @Override
    public Command getCommandOnSelect() {
        final QuestController qc = Singletons.getModel().getQuest();
        return new Command() {
            @Override
            public void execute() {
                if (qc.getAchievements() == null) {
                    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_QUESTDATA);
                }
            }
        };
    }
}
