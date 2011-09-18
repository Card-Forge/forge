package forge.quest.gui.main;

//import javax.swing.JLabel;

/** 
 * <p>QuestQuestPanel.</p>
 *  VIEW - Creates a QuestSelectablePanel instance for a "quest" style event.
 */

@SuppressWarnings("serial")
public class QuestChallengePanel extends QuestSelectablePanel {
    
    //private JLabel repeatabilityLabel;
    
    /** <p>QuestChallengePanel.</p>
     * Constructor, using challenge data instance.
     * 
     * @param {@link forge.quest.gui.main.QuestChallenge}
     */
    public QuestChallengePanel(QuestChallenge q) {
        super(q);
        
        // Repeatability is currently meaningless.
        // Can be added here later if necessary.
        /* 
         * if (q.getRepeatable()) {
            repeatabilityLabel = new JLabel("This challenge is repeatable");
        } else {
            repeatabilityLabel = new JLabel("This challenge is not repeatable");
        }
        
        super.rootPanel.add(repeatabilityLabel);
        */
    }
}
