package forge.quest.gui.main;

//import javax.swing.JLabel;

/** 
 * <p>QuestQuestPanel.</p>
 *  VIEW - Creates a QuestSelectablePanel instance for a "quest" style event.
 */

@SuppressWarnings("serial")
public class QuestQuestPanel extends QuestSelectablePanel {
    
    //private JLabel repeatabilityLabel;
    
    /** <p>QuestQuestPanel.</p>
     * Constructor, using quest data instance.
     * 
     * @param {@link forge.quest.gui.main.QuestDuel}
     */
    public QuestQuestPanel(QuestQuest q) {
        super(q);
        
        // Repeatability is currently meaningless.
        // Can be added here later if necessary.
        /* 
         * if (q.getRepeatable()) {
            repeatabilityLabel = new JLabel("This quest is repeatable");
        } else {
            repeatabilityLabel = new JLabel("This quest is not repeatable");
        }
        
        super.rootPanel.add(repeatabilityLabel);
        */
    }
}
