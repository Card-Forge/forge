package forge.adventure.editor;

import forge.adventure.data.AdventureQuestData;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Enumeration;

public class QuestEdit extends FormPanel {
    AdventureQuestData currentData;
    //public JSpinner spawnWeight= new JSpinner(new SpinnerNumberModel(0.0f, 0.f, 1f, 0.1f));
    public JLabel id = new JLabel();
    public JTextField name=new JTextField();
    public JTextField description=new JTextField();
    public JTextField synopsis=new JTextField();
    public JCheckBox storyQuest = new JCheckBox();
    public JTextField rewardDescription=new JTextField();
    public QuestStageEditor stages =new QuestStageEditor();


    JTabbedPane tabs =new JTabbedPane();
    public DialogEditor prologueEditor =new DialogEditor();
    public DialogEditor epilogueEditor =new DialogEditor();
    public DialogEditor offerEditor = new DialogEditor();
    public DialogEditor failureEditor = new DialogEditor();
    public DialogEditor declineEditor = new DialogEditor();
    private boolean updating=false;

    JTextField manualEntry = new JTextField(20);
    DefaultListModel<String> existingModel = new DefaultListModel<>();
    DefaultListModel<String> selectedTagModel = new DefaultListModel<>();
    JList<String> existingTags;
    JList<String> selectedTags;
    JPanel tags = new JPanel();

    public QuestEdit()
    {

        FormPanel center=new FormPanel() {  };

        center.add("Quest ID:", id);
        center.add("Name:",name);
        //center.add("Synopsis (dev mode):",synopsis);
        center.add("Description:",description);
        center.add("Reward Description:",rewardDescription);
        center.add("Storyline Quest", storyQuest);

        add(center);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(tabs);
        tabs.add("Quest Stages", getStagesTab());
        tabs.add("Offer Dialog",getOfferTab());
        tabs.add("Prologue",getPrologueTab());
        tabs.add("Epilogue",getEpilogueTab());
        tabs.add("Failure Dialog", getFailureTab());
        tabs.add("Decline Dialog",getDeclineTab());
        tabs.add("Quest Sources", getSourcesTab());



        existingTags = new JList<>();


        existingTags.getInputMap(JList.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"), "addSelected");
        existingTags.getActionMap().put("addSelected", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addSelected();

                int index = existingTags.getSelectedIndex();

                String selectedItem = existingTags.getSelectedValue();

                if (selectedItem != null) {
                    selectedTagModel.addElement(selectedItem);
                    existingTags.grabFocus();
                    existingTags.setSelectedIndex(index<existingModel.size()?index:index-1);

                }
            }
        });



        existingModel = QuestController.getInstance().getSourceTags();
        existingTags.setModel(existingModel);




        selectedTags = new JList<>();
        selectedTagModel = new DefaultListModel<>();
        selectedTags.setModel(selectedTagModel);

        selectedTags.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                doUpdate();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                doUpdate();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                doUpdate();
            }
        });

        JButton select = new JButton("Select");
        select.addActionListener(q -> addSelected());
        JButton add = new JButton("Manual Add");
        add.addActionListener(q -> manualAdd(selectedTagModel));
        JButton remove = new JButton("Remove Item");
        remove.addActionListener(q -> removeSelected());

        tags.setLayout(new BorderLayout());

        JPanel left = new JPanel();
        left.setLayout(new BorderLayout());
        left.add(new JLabel("Tags already in use"), BorderLayout.NORTH);
        JScrollPane listScroller = new JScrollPane(existingTags);
        listScroller.setMinimumSize(new Dimension(400, 800));
        left.add(listScroller, BorderLayout.CENTER);
        tags.add(left, BorderLayout.WEST);

        FormPanel tagEdit = new FormPanel();
        tagEdit.setLayout(new BorderLayout());

        FormPanel mappedTags = new FormPanel();
        mappedTags.setLayout(new BorderLayout());
        mappedTags.add(new JLabel("Tags Mapped to this object"), BorderLayout.NORTH);
        JScrollPane listScroller2 = new JScrollPane(selectedTags);
        listScroller2.setMinimumSize(new Dimension(400, 800));
        mappedTags.add(listScroller2, BorderLayout.CENTER);
        tagEdit.add(mappedTags,BorderLayout.EAST);

        JPanel controlPanel = new JPanel();

        controlPanel.add(select);
        controlPanel.add(add);
        controlPanel.add(manualEntry);

        manualEntry.getInputMap(JList.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"), "addTyped");
        manualEntry.getActionMap().put("addTyped", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!manualEntry.getText().trim().isEmpty()) {
                    manualAdd(selectedTagModel);
                    manualEntry.grabFocus();
                }
            }
        });


        controlPanel.add(remove);
        tagEdit.add(controlPanel, BorderLayout.CENTER);
        tags.add(tagEdit,BorderLayout.CENTER);

        JTextArea right1 = new JTextArea("This is really just to pad some space\n" +
                "but also to explain the use of tags.\n" +
                "Rather than adding 100's of object names\n" +
                "to every quest definition, instead we will\n"+
                "categorize enemies and points of interest with\n"+
                "tags and reference those categories in quests");
        right1.setEnabled(false);
        tags.add(right1, BorderLayout.EAST);



        name.getDocument().addDocumentListener(new DocumentChangeListener(QuestEdit.this::updateQuest));
        description.getDocument().addDocumentListener(new DocumentChangeListener(QuestEdit.this::updateQuest));
        synopsis.getDocument().addDocumentListener(new DocumentChangeListener(QuestEdit.this::updateQuest));
        storyQuest.getModel().addChangeListener(q -> QuestEdit.this.updateQuest());
        rewardDescription.getDocument().addDocumentListener(new DocumentChangeListener(QuestEdit.this::updateQuest));
        stages.addChangeListener(e -> QuestEdit.this.updateQuest());
        offerEditor.addChangeListener(e -> QuestEdit.this.updateQuest());
        prologueEditor.addChangeListener(e -> QuestEdit.this.updateQuest());
        epilogueEditor.addChangeListener(e -> QuestEdit.this.updateQuest());
        failureEditor.addChangeListener(e -> QuestEdit.this.updateQuest());
        declineEditor.addChangeListener(e -> QuestEdit.this.updateQuest());
        stages.addChangeListener(e -> QuestEdit.this.updateQuest());
        selectedTagModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                QuestEdit.this.updateQuest();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                QuestEdit.this.updateQuest();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                QuestEdit.this.updateQuest();
            }
        });

        refresh();
    }

    private void doUpdate(){
        QuestEdit.this.updateQuest();
    }

    private void addSelected(){
        if (existingTags.getSelectedIndex()>-1)
            selectedTagModel.addElement(existingTags.getModel().getElementAt(existingTags.getSelectedIndex()));
        doUpdate();
    }

    private void removeSelected(){
        if (selectedTags.getSelectedIndex()>-1)
            selectedTagModel.remove(selectedTags.getSelectedIndex());
        doUpdate();
    }

    private void filterExisting(DefaultListModel<String> filter){
        DefaultListModel<String> toReturn = new DefaultListModel<>();
        for (Enumeration<String> e = QuestController.getInstance().getSourceTags().elements(); e.hasMoreElements();){
            String toTest = e.nextElement();
            if (toTest != null & !filter.contains(toTest)){
                toReturn.addElement(toTest);
            }
        }
        existingTags.setModel(toReturn);
    }

    private void manualAdd(DefaultListModel<String> model){
        if (!manualEntry.getText().trim().isEmpty())
            model.addElement(manualEntry.getText().trim());
        manualEntry.setText("");
        doUpdate();
    }

    protected void updateQuest() {
        if(currentData==null||updating)
            return;

        currentData.name                   = name.getText();
        currentData.storyQuest             = storyQuest.isSelected();
        currentData.synopsis               = synopsis.getText();
        currentData.description            = description.getText();
        currentData.rewardDescription      = rewardDescription.getText();
        currentData.stages                 = stages.getStages();
        currentData.offerDialog            = offerEditor.getDialogData();
        currentData.prologue               = prologueEditor.getDialogData();
        currentData.epilogue               = epilogueEditor.getDialogData();
        currentData.failureDialog          = failureEditor.getDialogData();
        currentData.declinedDialog         = declineEditor.getDialogData();

        ArrayList<String> tags = new ArrayList<>();
        for (Enumeration<String> e = selectedTagModel.elements(); e.hasMoreElements();){
            tags.add(e.nextElement());
        }

        currentData.questSourceTags = tags.toArray(currentData.questSourceTags);
        QuestController.getInstance().refresh();
        filterExisting(selectedTagModel);
    }

    public void setCurrentQuest(AdventureQuestData data)
    {
        currentData=data;
        refresh();
    }

    private void refresh() {
        setEnabled(currentData!=null);
        if(currentData==null)
        {
            return;
        }
        setVisible(true);
        updating=true;
        id.setText(currentData.getID() + "");
        name.setText(currentData.name);
        description.setText(currentData.description);
        synopsis.setText(currentData.synopsis);
        storyQuest.getModel().setSelected(currentData.storyQuest);
        rewardDescription.setText(currentData.rewardDescription);
        stages.setStages(currentData);

        offerEditor.loadData(currentData.offerDialog);
        prologueEditor.loadData(currentData.prologue);
        epilogueEditor.loadData(currentData.epilogue);
        failureEditor.loadData(currentData.failureDialog);
        declineEditor.loadData(currentData.declinedDialog);

        selectedTagModel.clear();
        for(String val : currentData.questSourceTags) {
            if (val != null)
                selectedTagModel.addElement(val);
        }
        filterExisting(selectedTagModel);

        updating=false;
    }

    public JPanel getOfferTab(){
        JPanel offerTab = new JPanel();
        offerTab.setLayout(new BoxLayout(offerTab, BoxLayout.Y_AXIS));
        FormPanel center = new FormPanel();
        center.add(offerEditor);
        offerTab.add(center);
        return offerTab;
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

    public JPanel getFailureTab(){
        JPanel failureTab = new JPanel();
        failureTab.setLayout(new BoxLayout(failureTab, BoxLayout.Y_AXIS));
        FormPanel center = new FormPanel();
        center.add(failureEditor);
        failureTab.add(center);
        return failureTab;
    }

    public JPanel getDeclineTab(){
        JPanel declineTab = new JPanel();
        declineTab.setLayout(new BoxLayout(declineTab, BoxLayout.Y_AXIS));
        FormPanel center = new FormPanel();
        center.add(declineEditor);
        declineTab.add(center);
        return declineTab;
    }

    public JPanel getStagesTab(){
        JPanel stagesTab = new JPanel();
        stagesTab.setLayout(new BoxLayout(stagesTab, BoxLayout.Y_AXIS));
        FormPanel center = new FormPanel();
        center.add(stages);
        stagesTab.add(center);
        return stagesTab;
    }

    public JPanel getSourcesTab(){
        JPanel sourcesTab = new JPanel();
        sourcesTab.setLayout(new BoxLayout(sourcesTab, BoxLayout.Y_AXIS));
        FormPanel center = new FormPanel();
        center.add(tags);
        sourcesTab.add(center);
        return sourcesTab;
    }
}
