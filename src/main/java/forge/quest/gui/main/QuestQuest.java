package forge.quest.gui.main;

import forge.AllZone;
import forge.Quest_Assignment;
import forge.ReadQuest_Assignment;
import forge.gui.GuiUtils;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>QuestQuest class.</p>
 * 
 * Manages QuestSelectablePanel instances for "quest" style matches.
 *
 * @author Forge
 * @version $Id$
 */
public class QuestQuest extends QuestSelectablePanel {
    /** Constant <code>serialVersionUID=-162817410327650160L</code> */
    private static final long serialVersionUID = -162817410327650160L;

    Quest_Assignment assignment;

    /**
     * <p>Constructor for QuestQuest.</p>
     *
     * @param assignment a {@link forge.Quest_Assignment} object.
     */
    public QuestQuest(Quest_Assignment assignment) {
        super(assignment.getName(), assignment.getDifficulty(), assignment.getDesc(), GuiUtils.getIconFromFile(assignment.getIconName()));
        this.assignment = assignment;

        JLabel repeatabilityLabel;
        if (assignment.isRepeatable()) {
            repeatabilityLabel = new JLabel("This quest is repeatable");
        } else {
            repeatabilityLabel = new JLabel("This quest is not repeatable");
        }

        GuiUtils.addGap(centerPanel);
        this.centerPanel.add(repeatabilityLabel);
    }

    /**
     * <p>getQuests.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public static List<QuestQuest> getQuests() {
        List<QuestQuest> quests = new ArrayList<QuestQuest>();

        List<Quest_Assignment> questList = readQuests();

        for (Quest_Assignment assignment : questList) {
            quests.add(new QuestQuest(assignment));
        }
        return quests;
    }

    /**
     * <p>readQuests.</p>
     *
     * @return a {@link java.util.List} object.
     */
    private static List<Quest_Assignment> readQuests() {
        forge.quest.data.QuestData questData = AllZone.getQuestData();
        ReadQuest_Assignment read = new ReadQuest_Assignment(ForgeProps.getFile(NewConstants.QUEST.QUESTS), questData);
        read.run();

        List<Quest_Assignment> questsToDisplay = new ArrayList<Quest_Assignment>();

        if (questData.getAvailableQuests() != null && questData.getAvailableQuests().size() > 0) {
            List<Quest_Assignment> availableQuests = read.getQuestsByIds(questData.getAvailableQuests());
            questsToDisplay = availableQuests;

        } else {
            List<Quest_Assignment> allAvailableQuests = read.getQuests();

            List<Integer> availableInts = new ArrayList<Integer>();

            int maxQuests = questData.getWin() / 10;
            if (maxQuests > 5) {
                maxQuests = 5;
            }
            if (allAvailableQuests.size() < maxQuests) {
                maxQuests = allAvailableQuests.size();
            }

            Collections.shuffle(allAvailableQuests);

            for (int i = 0; i < maxQuests; i++) {
                Quest_Assignment qa = allAvailableQuests.get(i);

                availableInts.add(qa.getId());
                questsToDisplay.add(qa);
            }
            questData.setAvailableQuests(availableInts);
            questData.saveData();
        }//else
        return questsToDisplay;
    }

    /**
     * <p>getQuestAssignment.</p>
     *
     * @return a {@link forge.Quest_Assignment} object.
     */
    public Quest_Assignment getQuestAssignment() {
        return assignment;
    }
}
