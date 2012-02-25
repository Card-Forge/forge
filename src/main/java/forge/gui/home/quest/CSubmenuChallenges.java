package forge.gui.home.quest;

import java.awt.Color;
import java.util.List;

import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.gui.home.ICSubmenu;
import forge.gui.home.quest.SubmenuQuestUtil.SelectablePanel;
import forge.quest.data.QuestChallenge;
import forge.view.toolbox.FLabel;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CSubmenuChallenges implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getMenuCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#initialize()
     */
    @Override
    public void initialize() {
        /// TEMPORARY
        VSubmenuDuels.SINGLETON_INSTANCE.populate();
        CSubmenuDuels.SINGLETON_INSTANCE.update();
        /////////////

        VSubmenuChallenges.SINGLETON_INSTANCE.populate();
        CSubmenuChallenges.SINGLETON_INSTANCE.update();
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        SubmenuQuestUtil.updateStatsAndPet();

        final VSubmenuChallenges view = VSubmenuChallenges.SINGLETON_INSTANCE;
        VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().setEnabled(false);

        if (AllZone.getQuestData() != null) {
            VSubmenuDuels.SINGLETON_INSTANCE.getLblTitle().setText("Challenges: " + AllZone.getQuestData().getRank());

            view.getPnlChallenges().removeAll();
            final List<QuestChallenge> challenges =
                    Singletons.getModel().getQuestEventManager().generateChallenges(AllZone.getQuestData());

            for (final QuestChallenge c : challenges) {
                final SelectablePanel temp = new SelectablePanel(c);
                view.getPnlChallenges().add(temp, "w 96%!, h 86px!, gap 2% 0 5px 5px");
            }

            if (challenges.size() == 0) {
                final FLabel lbl = new FLabel.Builder()
                    .text(VSubmenuDuels.SINGLETON_INSTANCE.getLblNextChallengeInWins().getText())
                    .fontAlign(SwingConstants.CENTER).build();
                lbl.setForeground(Color.red);
                lbl.setBackground(Color.white);
                lbl.setBorder(new EmptyBorder(10, 10, 10, 10));
                lbl.setOpaque(true);
                view.getPnlChallenges().add(lbl, "w 50%!, h 30px!, gap 25% 0 50px 0");
            }
        }
    }
}
