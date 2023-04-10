package forge.adventure.editor;

import forge.adventure.data.AdventureQuestData;

import javax.swing.*;

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

        refresh();
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
}
