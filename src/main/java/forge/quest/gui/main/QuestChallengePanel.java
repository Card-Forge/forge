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

        GuiUtils.addGap(super.rootPanel, 7);

        if (q.getRepeatable()) {
            repeatabilityLabel = new JLabel("This challenge is repeatable");
        } else {
            repeatabilityLabel = new JLabel("This challenge is not repeatable");
        }

        super.rootPanel.add(repeatabilityLabel);

    }
}
