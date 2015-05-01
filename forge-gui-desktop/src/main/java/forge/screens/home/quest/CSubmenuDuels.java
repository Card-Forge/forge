package forge.screens.home.quest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.quest.QuestEventDuel;
import forge.quest.QuestUtil;
import forge.quest.bazaar.QuestPetController;
import forge.toolbox.JXButtonPanel;

/**
 * Controls the quest duels submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuDuels implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#initialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

        view.getBtnSpellShop().setCommand(
                new UiCommand() { @Override
                    public void run() { QuestUtil.showSpellShop(); } });

        view.getBtnBazaar().setCommand(
                new UiCommand() { @Override
                    public void run() { QuestUtil.showBazaar(); } });

        view.getBtnTravel().setCommand(
                new UiCommand() { @Override
                    public void run() { QuestUtil.travelWorld(); CSubmenuDuels.this.update(); } });

        view.getBtnUnlock().setCommand(
                new UiCommand() { @Override
                    public void run() { QuestUtil.chooseAndUnlockEdition(); CSubmenuDuels.this.update(); } });

        view.getBtnStart().addActionListener(
                new ActionListener() { @Override
                    public void actionPerformed(final ActionEvent e) { QuestUtil.startGame(); } });

        final QuestController quest = FModel.getQuest();
        view.getCbPlant().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                quest.selectPet(0, view.getCbPlant().isSelected() ? "Plant" : null);
                quest.save();
            }
        });

        view.getCbCharm().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                quest.setCharmState(view.getCbCharm().isSelected());
                quest.save();
            }
        });

        view.getCbxPet().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final int slot = 1;
                final int index = view.getCbxPet().getSelectedIndex();
                final List<QuestPetController> pets = quest.getPetsStorage().getAvaliablePets(slot, quest.getAssets());
                final String petName = index <= 0 || index > pets.size() ? null : pets.get(index - 1).getName();
                quest.selectPet(slot, petName);
                quest.save();
            }
        });

        view.getBtnRandomOpponent().setCommand(new UiCommand() {
            @Override
            public void run() {
                if (QuestUtil.canStartGame()) {
                    FModel.getQuest().getDuelsManager().randomizeOpponents();
                    final List<QuestEventDuel> duels = FModel.getQuest().getDuelsManager().generateDuels();
                    QuestUtil.setEvent(duels.get((int) (Math.random() * duels.size())));
                    QuestUtil.startGame();
                }
            }
        });

    }

    private final KeyAdapter _startOnEnter = new KeyAdapter() {
        @Override
        public void keyPressed(final KeyEvent e) {
            if (KeyEvent.VK_ENTER == e.getKeyChar()) {
                VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().doClick();
            }
        }
    };
    private final MouseAdapter _startOnDblClick = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
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
        QuestUtil.updateQuestView(VSubmenuDuels.SINGLETON_INSTANCE);

        final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

        if (FModel.getQuest().getAchievements() != null) {
            view.getLblTitle().setText("Duels: " + FModel.getQuest().getRank());

            view.getPnlDuels().removeAll();
            final List<QuestEventDuel> duels = FModel.getQuest().getDuelsManager().generateDuels();

            final JXButtonPanel grpPanel = new JXButtonPanel();

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
                grpPanel.add(temp, rad, "w 100%!, h 135px!, gapy 15px");
            }
            view.getPnlDuels().add(grpPanel, "w 100%!");
        }
    }

}
