package forge.screens.home.quest;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import forge.Singletons;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestEventChallenge;
import forge.gamemodes.quest.QuestUtil;
import forge.gamemodes.quest.bazaar.QuestItemType;
import forge.gamemodes.quest.bazaar.QuestPetController;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;
import forge.model.FModel;
import forge.toolbox.FLabel;
import forge.toolbox.JXButtonPanel;
import forge.util.Localizer;

/**
 * Controls the quest challenges submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuChallenges implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#initialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        final VSubmenuChallenges view = VSubmenuChallenges.SINGLETON_INSTANCE;

        view.getBtnSpellShop().setCommand((UiCommand) QuestUtil::showSpellShop);

        view.getBtnBazaar().setCommand((UiCommand) QuestUtil::showBazaar);

        view.getBtnUnlock().setCommand(
                (UiCommand) () -> { QuestUtil.chooseAndUnlockEdition(); CSubmenuChallenges.this.update(); });

        view.getBtnTravel().setCommand(
                (UiCommand) () -> { QuestUtil.travelWorld(); CSubmenuChallenges.this.update(); });

        view.getBtnStart().addActionListener(e -> QuestUtil.startGame());

        view.getLblZep().setCommand(
                (UiCommand) () -> {
                    final Localizer localizer = Localizer.getInstance();
                    if (!QuestUtil.checkActiveQuest(localizer.getMessage("lblLaunchaZeppelin"))) {
                        return;
                    }
                    FModel.getQuest().getAchievements().setCurrentChallenges(null);
                    FModel.getQuest().getAssets().setItemLevel(QuestItemType.ZEPPELIN, 2);
                    update();
                });

        final QuestController quest = FModel.getQuest();
        view.getCbPlant().addActionListener(arg0 -> {
            // This can't be translated. As the English string "Plant" is used to find the Plant pet.
            quest.selectPet(0, view.getCbPlant().isSelected() ? "Plant" : null);
            quest.save();
        });

        view.getCbxPet().addActionListener(arg0 -> {
            final int slot = 1;
            final int index = view.getCbxPet().getSelectedIndex();
            final List<QuestPetController> pets = quest.getPetsStorage().getAvaliablePets(slot, quest.getAssets());
            final String petName = index <= 0 || index > pets.size() ? null : pets.get(index - 1).getName();
            quest.selectPet(slot, petName);
            quest.save();
        });

        view.getCbxMatchLength().addActionListener(arg0 -> {
            String match = view.getCbxMatchLength().getSelectedItem();
            if (match != null) {
                quest.setMatchLength(match.substring(match.length() - 1));
                quest.save();
            }
        });
    }

    private final KeyAdapter _startOnEnter = new KeyAdapter() {
        @Override
        public void keyPressed(final KeyEvent e) {
            if (KeyEvent.VK_ENTER == e.getKeyChar()) {
                VSubmenuChallenges.SINGLETON_INSTANCE.getBtnStart().doClick();
            }
        }
    };
    private final MouseAdapter _startOnDblClick = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (MouseEvent.BUTTON1 == e.getButton() && 2 == e.getClickCount()) {
                VSubmenuChallenges.SINGLETON_INSTANCE.getBtnStart().doClick();
            }
        }
    };

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        QuestUtil.updateQuestView(VSubmenuChallenges.SINGLETON_INSTANCE);

        final VSubmenuChallenges view = VSubmenuChallenges.SINGLETON_INSTANCE;
        final QuestController qCtrl = FModel.getQuest();

        if (qCtrl.getAchievements() == null) {
            return;
        }
        final Localizer localizer = Localizer.getInstance();
        view.getLblTitle().setText(localizer.getMessage("lblChallenges") +": " + qCtrl.getRank());

        view.getPnlChallenges().removeAll();
        qCtrl.regenerateChallenges();
        final List<QuestEventChallenge> challenges = new ArrayList<>();
        for(final Object id : qCtrl.getAchievements().getCurrentChallenges()) {
            challenges.add(qCtrl.getChallenges().get(id.toString()));
        }

        final JXButtonPanel grpPanel = new JXButtonPanel();

        StringBuilder sb = new StringBuilder();
        sb.append(localizer.getMessage("lblMatchBestof")).append(" ").append(FModel.getQuest().getMatchLength());
        view.getCbxMatchLength().setSelectedItem(sb.toString());

        boolean haveAnyChallenges = true;
        for (final QuestEventChallenge qc : challenges) {
            final PnlEvent temp = new PnlEvent(qc);
            final JRadioButton rad = temp.getRad();
            if (haveAnyChallenges) {
                rad.setSelected(true);
                SwingUtilities.invokeLater(rad::requestFocusInWindow);
                haveAnyChallenges = false;
            }
            rad.addKeyListener(_startOnEnter);
            rad.addMouseListener(_startOnDblClick);
            grpPanel.add(temp, rad, "w 100%!, h 135px!, gapy 15px");
        }
        view.getPnlChallenges().add(grpPanel, "w 100%!");

        if (!haveAnyChallenges) {
            final FLabel lbl = new FLabel.Builder()
            .text(VSubmenuChallenges.SINGLETON_INSTANCE.getLblNextChallengeInWins().getText())
            .fontAlign(SwingConstants.CENTER).build();
            lbl.setForeground(Color.red);
            lbl.setBackground(Color.white);
            lbl.setBorder(new EmptyBorder(10, 10, 10, 10));
            lbl.setOpaque(true);
            view.getPnlChallenges().add(lbl, "w 50%!, h 30px!, gap 25% 0 50px 0");
            SwingUtilities.invokeLater(() -> view.getBtnTravel().requestFocusInWindow());
        }

        Singletons.getView().getFrame().validate();

    }

}
