package forge.adventure.editor;

import forge.adventure.data.DialogData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class ActionEdit extends FormPanel {
    DialogData.ActionData currentData;

    JTextField issueQuest = new JTextField();
    JTextField characterFlagName = new JTextField();
    JTextField mapFlagName = new JTextField();
    JTextField questFlagName = new JTextField();
    JTextField advanceCharacterFlag = new JTextField();
    JTextField advanceMapFlag = new JTextField();
    JTextField advanceQuestFlag = new JTextField();
    JTextField battleWithActorID = new JTextField();
    JTextField activateObjectID = new JTextField();
    JTextField deleteMapObject = new JTextField();
    JTextField setColorIdentity = new JTextField();
    JSpinner addReputation = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
    JSpinner addLife = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
    JTextField POIReference = new JTextField();
    JTextField removeItem = new JTextField();
    JSpinner characterFlagValue = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    JSpinner mapFlagValue = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    JSpinner questFlagValue = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));

    private boolean updating=false;

    public ActionEdit()
    {

        //todo: add info pane to explain primary usage

        add("Issue Quest:",issueQuest);
        add("Set Map Flag Name:",mapFlagName);
        add("Map Flag value to set:",mapFlagValue);
        add("Set Quest Flag name:",questFlagName);
        add("Quest Flag value to set:",questFlagValue);
        add("Set Character Flag name:",characterFlagName);
        add("Character Flag value to set:",characterFlagValue);

        add("Advance Map Flag name:",advanceMapFlag);
        add("Advance Quest Flag name:",advanceQuestFlag);
        add("Advance Character Flag name:",advanceCharacterFlag);
        add("Battle with actor ID:",battleWithActorID);
        add("Delete map object:",deleteMapObject);
        add("Set color identity:",setColorIdentity);
        add("Add Reputation:",addReputation);
        add("Add Life:",addLife);
        add("POI Reference:",POIReference);
        add("Remove Item:",removeItem);

        issueQuest.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));
        mapFlagName.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));
        mapFlagValue.getModel().addChangeListener(e -> ActionEdit.this.updateAction());
        questFlagName.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));
        questFlagValue.getModel().addChangeListener(e -> ActionEdit.this.updateAction());
        advanceMapFlag.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));
        advanceQuestFlag.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));
        battleWithActorID.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));
        activateObjectID.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));
        deleteMapObject.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));
        setColorIdentity.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));
        addLife.getModel().addChangeListener(e -> ActionEdit.this.updateAction());
        addReputation.getModel().addChangeListener(e -> ActionEdit.this.updateAction());
        POIReference.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));
        removeItem.getDocument().addDocumentListener(new DocumentChangeListener(ActionEdit.this::updateAction));

    }

    private void updateAction() {
        if(updating)
            return;
        if (currentData == null)
            currentData = new DialogData.ActionData();

        DialogData.ActionData.QuestFlag characterFlag = new DialogData.ActionData.QuestFlag();
        characterFlag.key = characterFlagName.getText();
        characterFlag.val = (int)characterFlagValue.getModel().getValue();
        currentData.setCharacterFlag= characterFlag;

        DialogData.ActionData.QuestFlag mapFlag = new DialogData.ActionData.QuestFlag();
        mapFlag.key = mapFlagName.getText();
        mapFlag.val = (int)mapFlagValue.getModel().getValue();
        currentData.setMapFlag= mapFlag;

        DialogData.ActionData.QuestFlag questFlag = new DialogData.ActionData.QuestFlag();
        questFlag.key = questFlagName.getText();
        questFlag.val = (int)questFlagValue.getModel().getValue();
        currentData.setQuestFlag= questFlag;

        currentData.issueQuest = issueQuest.getText();
        currentData.advanceMapFlag= advanceMapFlag.getText();
        currentData.advanceQuestFlag= advanceQuestFlag.getText();
        currentData.advanceCharacterFlag= advanceCharacterFlag.getText();
        currentData.battleWithActorID= Integer.parseInt(battleWithActorID.getText());
        currentData.activateMapObject= Integer.parseInt((activateObjectID.getText()));
        currentData.deleteMapObject= Integer.parseInt(deleteMapObject.getText());
        currentData.setColorIdentity= setColorIdentity.getText();
        currentData.addLife= (int) addLife.getModel().getValue();
        currentData.addMapReputation= (int)addReputation.getModel().getValue();
        currentData.POIReference= POIReference.getText();
        currentData.removeItem= removeItem.getText();

        //These need a dedicated effect editor
//        JTextField setEffect = new JTextField();
//        JTextField giveBlessing = new JTextField();
//        currentData.giveBlessing= giveBlessing.getText();
//        currentData.setEffect= setEffect.getText();

        //These are valid pre-existing fields, but should be handled through the dedicated rewards editor
//        currentData.addGold =;
//        currentData.addItem=;
//        currentData.grantRewards =;

        ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
        if (listeners != null && listeners.length > 0) {
            ChangeEvent evt = new ChangeEvent(this);
            for (ChangeListener listener : listeners) {
                listener.stateChanged(evt);
            }
        }

    }
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }
    public void setCurrentAction(DialogData.ActionData data)
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
        updating=true;

        mapFlagName.setText(currentData.setMapFlag==null?"":currentData.setMapFlag.key);
        mapFlagValue.getModel().setValue(currentData.setMapFlag==null?0:currentData.setMapFlag.val);

        questFlagName.setText(currentData.setQuestFlag==null?"":currentData.setQuestFlag.key);
        questFlagValue.getModel().setValue(currentData.setQuestFlag==null?0:currentData.setQuestFlag.val);

        issueQuest.setText(currentData.issueQuest);
        advanceMapFlag.setText(currentData.advanceMapFlag);
        advanceQuestFlag.setText(currentData.advanceQuestFlag);
        advanceCharacterFlag.setText(currentData.advanceCharacterFlag);

        battleWithActorID.setText("" + currentData.battleWithActorID);
        activateObjectID.setText("" + currentData.battleWithActorID);
        deleteMapObject.setText("" + currentData.deleteMapObject);
        setColorIdentity.setText(currentData.setColorIdentity);
        addLife.getModel().setValue(currentData.addLife);
        addReputation.getModel().setValue(currentData.addMapReputation);

        POIReference.setText(currentData.POIReference);
        removeItem.setText(currentData.removeItem);

        updating=false;
    }
}
