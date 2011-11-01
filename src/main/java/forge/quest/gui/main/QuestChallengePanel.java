package forge.quest.gui.main;

import javax.swing.JLabel;

import forge.gui.GuiUtils;

/**
 * <p>
 * QuestQuestPanel.
 * </p>
 * VIEW - Creates a QuestSelectablePanel instance for a "quest" style event.
 */

@SuppressWarnings("serial")
public class QuestChallengePanel extends QuestSelectablePanel {

    private JLabel repeatabilityLabel;

    /**
     * <p>
     * QuestChallengePanel.
     * </p>
     * Constructor, using challenge data instance.
     * 
     * @param q
     *            the q
     */
    public QuestChallengePanel(final QuestChallenge q) {
        super(q);

        GuiUtils.addGap(super.getRootPanel(), 7);

        if (q.getRepeatable()) {
            this.repeatabilityLabel = new JLabel("This challenge is repeatable");
        } else {
            this.repeatabilityLabel = new JLabel("This challenge is not repeatable");
        }

        super.getRootPanel().add(this.repeatabilityLabel);

    }
}
