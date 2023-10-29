package forge.adventure.editor;

import forge.adventure.data.*;
import forge.adventure.util.AdventureQuestController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestStageEdit extends FormPanel {
    private boolean updating=false;
    AdventureQuestStage currentData;
    AdventureQuestData currentQuestData;
    public JTextField name=new JTextField("", 25);
    public JTextField description=new JTextField("", 25);
    public TextListEdit itemNames =new TextListEdit();
    public TextListEdit spriteNames =new TextListEdit();
    public TextListEdit equipNames =new TextListEdit();
    public TextListEdit prerequisites =new TextListEdit(currentQuestData!=null?(String[])Arrays.stream(currentQuestData.stages).filter(q -> !q.equals(currentData)).toArray():new String[]{}); //May not be the right way to do this, will come back to it.

    JTabbedPane tabs =new JTabbedPane();
    DialogEditor prologueEditor = new DialogEditor();
    DialogEditor epilogueEditor = new DialogEditor();

    public QuestStageEdit()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(getInfoTab());
        add(tabs);
        tabs.add("Objective", getObjectiveTab());
        tabs.add("Prologue",getPrologueTab());
        tabs.add("Epilogue",getEpilogueTab());
        tabs.add("Prerequisites",getPrereqTab());

        //temp
        nyi.setForeground(Color.red);
        //

        addListeners();
    }

    public JPanel getInfoTab(){
        JPanel infoTab = new JPanel();
        FormPanel center=new FormPanel();
        center.add("name:",name);
        center.add("description:",description);
        name.setSize(400, name.getHeight());
        description.setSize(400, description.getHeight());
        infoTab.add(center);
        name.getDocument().addDocumentListener(new DocumentChangeListener(QuestStageEdit.this::updateStage));
        description.getDocument().addDocumentListener(new DocumentChangeListener(QuestStageEdit.this::updateStage));
        prerequisites.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(QuestStageEdit.this::updateStage));
        return infoTab;
    }

    public JPanel getPrologueTab(){
        JPanel prologueTab = new JPanel();
        prologueTab.setLayout(new BoxLayout(prologueTab, BoxLayout.Y_AXIS));
        FormPanel center = new FormPanel();
        center.add(prologueEditor);
        prologueTab.add(center);
        return prologueTab;
    }

    public JPanel getEpilogueTab(){
        JPanel epilogueTab = new JPanel();
        epilogueTab.setLayout(new BoxLayout(epilogueTab, BoxLayout.Y_AXIS));
        FormPanel center = new FormPanel();
        center.add(epilogueEditor);
        epilogueTab.add(center);
        return epilogueTab;
    }

    private JComboBox<AdventureQuestController.ObjectiveTypes> objectiveType;

    private final JLabel nyi = new JLabel("Not yet implemented");
    private final JTextField deliveryItem=new JTextField(25);
    private final JTextField mapFlag = new JTextField(25);

    private Box mapFlagGroup;
    private final JSpinner flagSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
    private Box flagValueGroup;
    private final JCheckBox anyPOI = new JCheckBox("Any Point of Interest matching");
    private final JCheckBox here = new JCheckBox("Use current map instead of selecting tags");
    private final JCheckBox mixedEnemies = new JCheckBox("Mixture of enemy types matching");
    private final JLabel count1Description = new JLabel("(Count 1 description)");
    private final JLabel count2Description = new JLabel("(Count 2 description)");
    private final JLabel count3Description = new JLabel("(Count 3 description)");
    private final JSpinner count1Spinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
    private final JSpinner count2Spinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
    private final JSpinner count3Spinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));

    private final JLabel arenaLabel = new JLabel("Enter the arena and prove your worth. (Note: Please be sure the PoIs selected have an arena)");
    private final JLabel clearLabel = new JLabel("Clear all enemies from the target area.");
    private final JLabel defeatLabel = new JLabel("Defeat a number of enemies of the indicated type.");
    private final JLabel deliveryLabel = new JLabel("Travel to the given destination to deliver an item (not tracked in inventory).");
    private final JLabel escortLabel = new JLabel("Protect your target as they travel to their destination.");
    private final JLabel fetchLabel = new JLabel("Obtain the requested items (not tracked in inventory).");
    private final JLabel findLabel = new JLabel("Locate the and enter a PoI.");
    private final JLabel gatherLabel = new JLabel("Have the requested item in your inventory (tracked in inventory)");
    private final JLabel giveLabel = new JLabel("Have the requested items removed from your inventory.");
    private final JLabel haveReputationLabel = new JLabel("Have a minimum reputation in the selected PoI (and enter it)");
    private final JLabel huntLabel = new JLabel("Track down and defeat your target (on the overworld map).");
    private final JLabel leaveLabel = new JLabel("Exit the current PoI and return to the overworld map.");
    private final JLabel noneLabel = new JLabel("No visible objective. Use in coordination with hidden parallel objectives to track when to progress");
    private final JLabel patrolLabel = new JLabel("Get close to generated coordinates before starting your next objective");
    private final JLabel rescueLabel = new JLabel("Reach and rescue the target");
    private final JLabel siegeLabel = new JLabel("Travel to the target location and defeat enemies attacking it");
    private final JLabel mapFlagLabel = new JLabel("Have a map flag set to a minimum value");
    private final JLabel questFlagLabel = new JLabel("Have a global quest flag set to a minimum value");
    private final JLabel travelLabel = new JLabel("Travel to the given destination.");
    private final JLabel useLabel = new JLabel("Use the indicated item from your inventory.");
    private JTabbedPane poiPane = new JTabbedPane();
    private final QuestTagSelector poiSelector = new QuestTagSelector("Destination Tags", false,true);
    private final QuestTagSelector enemySelector = new QuestTagSelector("Enemy Tags", true,false);
    private final JTextField poiTokenInput = new JTextField(25);

    private final JLabel poiTokenLabel = new JLabel();
    private final JLabel poiTokenDescription = new JLabel(
                 "At the bottom of many objectives involving a PoI, you will see a text field in the format of '$poi_#'." +
                 "Enter that tag here to ignore the PoI tag selector of this stage and instead use the same PoI that was selected " +
                 "for that stage as the target PoI for this one as well.");

    private void hideAllControls(){
        arenaLabel.setVisible(false);
        clearLabel.setVisible(false);
        deliveryLabel.setVisible(false);
        defeatLabel.setVisible(false);
        escortLabel.setVisible(false);
        fetchLabel.setVisible(false);
        findLabel.setVisible(false);
        gatherLabel.setVisible(false);
        giveLabel.setVisible(false);
        haveReputationLabel.setVisible(false);
        huntLabel.setVisible(false);
        leaveLabel.setVisible(false);
        noneLabel.setVisible(false);
        patrolLabel.setVisible(false);
        rescueLabel.setVisible(false);
        siegeLabel.setVisible(false);
        mapFlagLabel.setVisible(false);
        questFlagLabel.setVisible(false);
        travelLabel.setVisible(false);
        useLabel.setVisible(false);
        deliveryItem.setVisible(false);
        mapFlagGroup.setVisible(false);
        flagValueGroup.setVisible(false);
        anyPOI.setVisible(false);
        here.setVisible(false);
        mixedEnemies.setVisible(false);
        enemySelector.setVisible(false);
        count1Description.setVisible(false);
        count2Description.setVisible(false);
        count3Description.setVisible(false);
        count1Spinner.setVisible(false);
        count2Spinner.setVisible(false);
        count3Spinner.setVisible(false);
        poiPane.setVisible(false);
        poiTokenLabel.setVisible(false);
        nyi.setVisible(false);
    }
    private void switchPanels(){
        hideAllControls();
        if (objectiveType.getSelectedItem() == null){
            return;
        }
        switch(objectiveType.getSelectedItem().toString()){
            case "Arena":
                arenaLabel.setVisible(true);
                poiPane.setVisible(true);
                anyPOI.setVisible(true);
                here.setVisible(true);
                poiTokenLabel.setVisible(true);
                break;
            case "Clear":
                clearLabel.setVisible(true);
                nyi.setVisible(true);
                poiPane.setVisible(true);
                count1Description.setText("Target candidate percentile");
                count1Description.setVisible(true);
                count1Spinner.setVisible(true);
                count2Description.setText("Percent variance");
                count2Description.setVisible(true);
                count2Spinner.setVisible(true);
                anyPOI.setVisible(true);
                here.setVisible(true);
                poiTokenLabel.setVisible(true);
                break;
            case "Defeat":
                defeatLabel.setVisible(true);
                mixedEnemies.setVisible(true);
                count1Description.setText("Number to defeat");
                count1Description.setVisible(true);
                count1Spinner.setVisible(true);
                count2Description.setText("Maximum losses");
                count2Description.setVisible(true);
                count2Spinner.setVisible(true);
                enemySelector.setVisible(true);
                break;
            case "Delivery":
                deliveryLabel.setVisible(true);
                nyi.setVisible(true);
                poiPane.setVisible(true);
                anyPOI.setVisible(true);
                here.setVisible(true);
                deliveryItem.setVisible(true);
                poiTokenLabel.setVisible(true);
                break;
            case "Escort":
                escortLabel.setVisible(true);
                nyi.setVisible(true);
                poiPane.setVisible(true);
                here.setVisible(true);
                spriteNames.setVisible(true);
                poiTokenLabel.setVisible(true);
                break;
            case "Fetch":
                fetchLabel.setVisible(true);
                nyi.setVisible(true);
                poiPane.setVisible(true);
                anyPOI.setVisible(true);
                here.setVisible(true);
                deliveryItem.setVisible(true);
                enemySelector.setVisible(true);
                poiTokenLabel.setVisible(true);
                break;
            case "Find":
                findLabel.setVisible(true);
                nyi.setVisible(true);
                poiPane.setVisible(true);
                anyPOI.setVisible(true);
                poiTokenLabel.setVisible(true);
                break;
            case "Gather":
                gatherLabel.setVisible(true);
                gatherLabel.setVisible(true);
                nyi.setVisible(true);
                itemNames.setVisible(true);
                break;
            case "Give":
                giveLabel.setVisible(true);
                nyi.setVisible(true);
                itemNames.setVisible(true);
                poiPane.setVisible(true);
                poiTokenLabel.setVisible(true);
                here.setVisible(true);
                break;
            case "HaveReputation":
                haveReputationLabel.setVisible(true);
                poiPane.setVisible(true);
                poiTokenLabel.setVisible(true);
                here.setVisible(true);
                count1Description.setText("Minimum reputation needed");
                count1Description.setVisible(true);
                count1Spinner.setVisible(true);
                count1Spinner.setVisible(true);
                break;
            case "Hunt":
                huntLabel.setVisible(true);
                enemySelector.setVisible(true);
                count1Description.setText("Lifespan (seconds to complete hunt before despawn)");
                count1Description.setVisible(true);
                count1Spinner.setVisible(true);
                count1Spinner.setVisible(true);
                break;
            case "Leave":
                leaveLabel.setVisible(true);
                break;
            case "None":
                noneLabel.setVisible(true);
                nyi.setVisible(true);
                break;
            case "Patrol":
                patrolLabel.setVisible(true);
                nyi.setVisible(true);
                break;
            case "Rescue":
                rescueLabel.setVisible(true);
                nyi.setVisible(true);
                poiPane.setVisible(true);
                anyPOI.setVisible(true);
                poiTokenLabel.setVisible(true);
                here.setVisible(true);
                spriteNames.setVisible(true);
                enemySelector.setVisible(true);
                break;
            case "Siege":
                siegeLabel.setVisible(true);
                nyi.setVisible(true);
                poiPane.setVisible(true);
                poiTokenLabel.setVisible(true);
                anyPOI.setVisible(true);
                here.setVisible(true);
                enemySelector.setVisible(true);
                mixedEnemies.setVisible(true);
                break;
            case "MapFlag":
                mapFlagLabel.setVisible(true);
                poiPane.setVisible(true);
                here.setVisible(true);
                anyPOI.setVisible(true);
                mapFlagGroup.setVisible(true);
                flagValueGroup.setVisible(true);
                break;
            case "QuestFlag":
                questFlagLabel.setVisible(true);
                mapFlagGroup.setVisible(true);
                flagValueGroup.setVisible(true);
                break;
            case "Travel":
                travelLabel.setVisible(true);
                poiPane.setVisible(true);
                poiTokenLabel.setVisible(true);
                poiTokenInput.setVisible(true);
                anyPOI.setVisible(true);
                here.setVisible(true);
                count1Description.setText("Target % of possible distances");
                count1Description.setVisible(true);
                count1Spinner.setVisible(true);
                count2Description.setText("Plus or minus %");
                count2Description.setVisible(true);
                count2Spinner.setVisible(true);
                break;
            case "Use":
                useLabel.setVisible(true);
                nyi.setVisible(true);
                itemNames.setVisible(true);
                poiPane.setVisible(true);
                anyPOI.setVisible(true);
                here.setVisible(true);
                poiTokenLabel.setVisible(true);
                here.setVisible(true);
                break;
        }
    }

    private void changeObjective(){
        if (objectiveType.getSelectedItem() != null)
            currentData.objective = AdventureQuestController.ObjectiveTypes.valueOf(objectiveType.getSelectedItem().toString());
        switchPanels();
    }

    private JPanel getObjectiveTab(){
        objectiveType = new JComboBox<>(AdventureQuestController.ObjectiveTypes.values());
        objectiveType.addActionListener( e -> changeObjective());

        JPanel objectiveTab = new JPanel();
        JScrollPane scrollPane = new JScrollPane();
        objectiveTab.add(scrollPane);
        FormPanel center=new FormPanel();
        center.add(objectiveType);
        scrollPane.add(center);

        mapFlagGroup = new Box(BoxLayout.Y_AXIS);
        mapFlagGroup.add(new JLabel("Map flag to check"));
        mapFlagGroup.add(mapFlag);

        flagValueGroup = new Box(BoxLayout.Y_AXIS);
        flagValueGroup.add(new JLabel("Flag value to check"));
        flagValueGroup.add(flagSpinner);

        JPanel poiSelectorPane = new JPanel();
        poiSelectorPane.setLayout(new BorderLayout());
        poiSelectorPane.add(poiSelector, BorderLayout.CENTER);
        JPanel poiTokenPanel = new JPanel();

        JPanel tokenPanel = new JPanel();
        tokenPanel.add(new JLabel("Token to use:"));
        tokenPanel.add(poiTokenInput);
        tokenPanel.setBorder(new EmptyBorder(10, 10, 30, 10));

        poiTokenPanel.add(poiTokenDescription);
        poiTokenPanel.add(tokenPanel);
        poiTokenPanel.add(here);

        GroupLayout poiTokenLayout =  new GroupLayout(poiTokenPanel);


        poiTokenLayout.setHorizontalGroup(
            poiTokenLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(poiTokenDescription)
                            .addComponent(tokenPanel,GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE)
                            .addComponent(here));

        poiTokenLayout.setVerticalGroup(
                poiTokenLayout.createSequentialGroup()
                        .addComponent(poiTokenDescription)
                        .addComponent(tokenPanel,GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)
                        .addComponent(here));

        poiTokenPanel.setLayout(poiTokenLayout);

        poiPane.add("Specific PoI", poiTokenPanel);
        poiPane.add("Tag Selector", poiSelectorPane);
        poiPane.setPreferredSize(new Dimension(0,200));


        center.add(arenaLabel);
        center.add(clearLabel);
        center.add(defeatLabel);
        center.add(deliveryLabel);
        center.add(escortLabel);
        center.add(fetchLabel);
        center.add(findLabel);
        center.add(gatherLabel);
        center.add(giveLabel);
        center.add(haveReputationLabel);
        center.add(huntLabel);
        center.add(leaveLabel);
        center.add(noneLabel);
        center.add(patrolLabel);
        center.add(rescueLabel);
        center.add(siegeLabel);
        center.add(mapFlagLabel);
        center.add(questFlagLabel);
        center.add(travelLabel);
        center.add(useLabel);
        center.add(nyi);
        center.add(deliveryItem);
        center.add(mapFlagGroup);
        center.add(flagValueGroup);
        center.add(anyPOI);
        center.add(mixedEnemies);
        center.add(enemySelector);
        center.add(count1Description);
        center.add(count1Spinner);
        center.add(count2Description);
        center.add(count2Spinner);
        center.add(count3Description);
        center.add(count3Spinner);
        center.add(poiPane);
        center.add(poiTokenLabel);

        switchPanels();

        poiSelector.selectedItems.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                rebuildPOIList();
                rebuildEnemyList();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                rebuildPOIList();
                rebuildEnemyList();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                rebuildPOIList();
                rebuildEnemyList();
            }
        });

        return center;
    }

    private void rebuildPOIList(){
        List<String> currentList = new ArrayList<>();

        for(int i = 0; i< poiSelector.selectedItems.getSize(); i++){
            currentList.add(poiSelector.selectedItems.getElementAt(i));
        }
        currentData.POITags = currentList;
    }

    private void rebuildEnemyList(){
        List<String> currentList = new ArrayList<>();

        for(int i = 0; i< enemySelector.selectedItems.getSize(); i++){
            currentList.add(enemySelector.selectedItems.getElementAt(i));
        }
        currentData.enemyTags = currentList;
    }

    private JPanel getPrereqTab(){
        JPanel prereqTab = new JPanel();
        prereqTab.add(new JLabel("Insert Prereq data here"));

        return prereqTab;
    }

    private void refresh() {

        if(currentData==null)
        {
            return;
        }
        setEnabled(false);
        updating=true;
        objectiveType.setSelectedItem(currentData.objective);
        if (objectiveType.getSelectedItem() != null)
            currentData.objective = AdventureQuestController.ObjectiveTypes.valueOf(objectiveType.getSelectedItem().toString()); //Ensuring this gets initialized on new

        name.setText(currentData.name);
        description.setText(currentData.description);
        deliveryItem.setText(currentData.deliveryItem);

        if (currentData.enemyTags != null){
            DefaultListModel<String> selectedEnemies = new DefaultListModel<>();
            for (int i = 0; i < currentData.enemyTags.size(); i++) {
                selectedEnemies.add(i, currentData.enemyTags.get(i));
            }
            enemySelector.load(selectedEnemies);
        }
        if (currentData.POITags != null){
            DefaultListModel<String> selectedPOI = new DefaultListModel<>();
            for (int i = 0; i < currentData.POITags.size(); i++) {
                selectedPOI.add(i, currentData.POITags.get(i));
            }
            poiSelector.load(selectedPOI);
        }

        itemNames.setText(currentData.itemNames);
        equipNames.setText(currentData.equipNames);
        prologueEditor.loadData(currentData.prologue);
        epilogueEditor.loadData(currentData.epilogue);


        if (currentData.POITags != null){
            DefaultListModel<String> selectedPOI = new DefaultListModel<>();
            for (int i = 0; i < currentData.POITags.size(); i++) {
                selectedPOI.add(i, currentData.POITags.get(i));
            }
            poiSelector.load(selectedPOI);
        }
        here.getModel().setSelected(currentData.here);
        poiTokenInput.setText(currentData.POIToken);
        poiTokenLabel.setText( "To reference this point of interest: $(poi_" + currentData.id + ")");

        ArrayList<String> temp = new ArrayList<>();
        for (AdventureQuestStage stage : currentQuestData.stages){
            if (stage.equals(currentData))
                continue;
            temp.add(stage.name);
        }
        prerequisites.setOptions(temp);

        count1Spinner.getModel().setValue(currentData.count1);
        count2Spinner.getModel().setValue(currentData.count2);
        count3Spinner.getModel().setValue(currentData.count3);

        updating=false;
        setEnabled(true);
    }
    public void updateStage()
    {
        if(currentData==null||updating)
            return;

        currentData.name=name.getText();
        currentData.description= description.getText();
        currentData.prologue = prologueEditor.getDialogData();
        currentData.epilogue = epilogueEditor.getDialogData();
        currentData.deliveryItem = deliveryItem.getText();
        currentData.itemNames = itemNames.getList()==null?new ArrayList<>():Arrays.asList(itemNames.getList());
        currentData.equipNames = equipNames.getList()==null?new ArrayList<>():Arrays.asList(equipNames.getList());
        currentData.anyPOI = anyPOI.getModel().isSelected();
        currentData.mapFlag = mapFlag.getText();
        currentData.mapFlagValue = Integer.parseInt(flagSpinner.getModel().getValue().toString());
        currentData.count1 = Integer.parseInt(count1Spinner.getModel().getValue().toString());
        currentData.count2 = Integer.parseInt(count2Spinner.getModel().getValue().toString());
        currentData.count3 = Integer.parseInt(count3Spinner.getModel().getValue().toString());
        currentData.mixedEnemies = mixedEnemies.getModel().isSelected();
        currentData.here = here.getModel().isSelected();
        currentData.POIToken = poiTokenInput.getText();

        rebuildPOIList();
        rebuildEnemyList();
        emitChanged();
    }
    public void setCurrentStage(AdventureQuestStage stageData, AdventureQuestData data) {
        if (stageData == null)
            stageData = new AdventureQuestStage();
        if (data == null)
            data = new AdventureQuestData();
        currentData =stageData;
        currentQuestData=data;
        setVisible(true);
        refresh();
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }
    protected void emitChanged() {
        ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
        if (listeners != null && listeners.length > 0) {
            ChangeEvent evt = new ChangeEvent(this);
            for (ChangeListener listener : listeners) {
                listener.stateChanged(evt);
            }
        }
    }

    private void addListeners(){
        deliveryItem.getDocument().addDocumentListener(new DocumentChangeListener(QuestStageEdit.this::updateStage));
        mapFlag.getDocument().addDocumentListener(new DocumentChangeListener(QuestStageEdit.this::updateStage));
        flagSpinner.getModel().addChangeListener(q -> QuestStageEdit.this.updateStage());
        count1Spinner.getModel().addChangeListener(q -> QuestStageEdit.this.updateStage());
        count2Spinner.getModel().addChangeListener(q -> QuestStageEdit.this.updateStage());
        count3Spinner.getModel().addChangeListener(q -> QuestStageEdit.this.updateStage());
        mixedEnemies.getModel().addChangeListener(q -> QuestStageEdit.this.updateStage());
        here.getModel().addChangeListener(q -> QuestStageEdit.this.updateStage());
        anyPOI.getModel().addChangeListener(q -> QuestStageEdit.this.updateStage());
        deliveryItem.getDocument().addDocumentListener(new DocumentChangeListener(QuestStageEdit.this::updateStage));
        mapFlag.getDocument().addDocumentListener(new DocumentChangeListener(QuestStageEdit.this::updateStage));
        prologueEditor.addChangeListener(q -> QuestStageEdit.this.updateStage());
        epilogueEditor.addChangeListener(q -> QuestStageEdit.this.updateStage());
    }
}
