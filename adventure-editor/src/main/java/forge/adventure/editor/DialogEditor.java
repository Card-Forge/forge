package forge.adventure.editor;

import com.google.common.collect.ObjectArrays;
import forge.adventure.data.DialogData;
import forge.adventure.data.RewardData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;


/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class DialogEditor extends JComponent{

    private boolean updating = false;
    private java.util.List<DialogData> allNodes = new ArrayList<>();

    public JTextArea text =new JTextArea("(Initial dialog text)", 3, 80);
    public RewardsEditor rewardsEditor = new RewardsEditor();
    public ActionEditor actionEditor = new ActionEditor();
    public DialogOptionEditor optionEditor = new DialogOptionEditor();
    public DialogTree navTree = new DialogTree();

    public DialogEdit edit = new DialogEdit();

    private DialogData root = new DialogData();
    private DialogData current = new DialogData();

    public DialogEditor(){
        buildUI();

        navTree.addSelectionListener(q -> loadNewNodeSelection());
        edit.addChangeListener(q-> acceptEdits());
        edit.addNode.addActionListener(q -> addNode());
        edit.removeNode.addActionListener(q -> removeNode());
    }

    public void loadData(DialogData rootDialogData){
        updating = true;
        if (rootDialogData == null)
        {
            rootDialogData = new DialogData();
        }
        root = rootDialogData;
        navTree.loadDialog(rootDialogData);
        text.setText(rootDialogData.text);

        updating = false;
    }

    public DialogData getDialogData(){

        return root;
    }

    JTabbedPane tabs = new JTabbedPane();
    JToolBar conditionsToolbar = new JToolBar("conditionsToolbar");
    JToolBar actionsToolbar = new JToolBar("actionsToolbar");
    JToolBar optionsToolbar = new JToolBar("optionsToolbar");
    JToolBar tokensToolbar = new JToolBar("tokensToolbar");

    JPanel conditionsPanel = new JPanel();
    JPanel actionsPanel = new JPanel();
    JPanel optionsPanel = new JPanel();
    JPanel rewardsPanel = new JPanel();
    JPanel tokensPanel = new JPanel();

    class QuestTextDocumentListener implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            root.text = text.getText();
            emitChanged();
        }
        public void removeUpdate(DocumentEvent e) {
            root.text = text.getText();
            emitChanged();
        }
        public void insertUpdate(DocumentEvent e) {
            root.text = text.getText();
            emitChanged();
        }
    }

    public void buildUI(){
        buildTabs();
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        JPanel textArea = new JPanel();
        textArea.setLayout(new FlowLayout());
        textArea.add(new JLabel("Dialog Start"));
        textArea.add(text);
        text.getDocument().addDocumentListener(new QuestTextDocumentListener());

        JSplitPane splitPane = new JSplitPane();
        splitPane.setLeftComponent(navTree);
        splitPane.setRightComponent(tabs);
        splitPane.setResizeWeight(0.2);
        splitPane.setDividerLocation(.2);

        add(textArea, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    public void loadNewNodeSelection(){
        updating = true;
        current = navTree.getSelectedData();
        edit.currentData = navTree.getSelectedData();
        edit.refresh(root.equals(current));
        rewardsEditor.clear();
        actionEditor.clear();
        if (navTree.getSelectedData() != null) {
            for (DialogData.ActionData action : navTree.getSelectedData().action) {
                if (action.grantRewards != null && action.grantRewards.length > 0)
                    rewardsEditor.setRewards(action.grantRewards);
            }
            actionEditor.setAction(navTree.getSelectedData().action);
        }
        updating = false;
    }

    public void acceptEdits(){
        if (current == null)
            return;
        current.name = edit.name.getText();
        current.text = edit.text.getText();
        current.locname = edit.locname.getText();
        current.loctext = edit.loctext.getText();
        root.text = text.getText();

        DialogData.ActionData[] action = actionEditor.getAction();

        ArrayList<DialogData.ActionData> actionsList = new ArrayList<>(Arrays.stream(action).collect(Collectors.toList()));

        RewardData[] rewards= rewardsEditor.getRewards();
        if (rewards.length > 0)
        {
            DialogData.ActionData rewardAction = new DialogData.ActionData();
            rewardAction.grantRewards = rewards;
            actionsList.add(rewardAction);
        }

        current.action = actionsList.toArray(current.action);

        navTree.replaceCurrent();
        emitChanged();
    }

    public void addNode(){
        DialogData newNode = new DialogData();
        newNode.name = "NewResponse";
        DialogData parent = navTree.getSelectedData()!=null?navTree.getSelectedData():root;
        parent.options = ObjectArrays.concat(parent.options, newNode);
        navTree.loadDialog(root);
        navTree.setSelectedData(newNode);
        emitChanged();
    }
    public void removeNode(){
        if (navTree.getSelectedData() == null)
            return;
        navTree.removeSelectedData();
        emitChanged();
    }

    public void buildTabs(){
        buildToolBars();

        actionsPanel.add(actionsToolbar);
        actionsPanel.add(actionEditor);
        optionsPanel.add(edit);
        rewardsPanel.add(rewardsEditor);
        rewardsEditor.addChangeListener(e -> DialogEditor.this.acceptEdits());
        actionEditor.addChangeListener(e -> DialogEditor.this.acceptEdits());
        tokensPanel.add(tokensToolbar);
        tokensPanel.add(new JLabel("Insert token editor here"));

        tabs.addTab("Options", optionsPanel);
        tabs.addTab("Conditions", conditionsPanel);
        tabs.addTab("Actions", actionsPanel);
        tabs.addTab("Rewards", rewardsPanel);
        tabs.addTab("Tokens",tokensPanel);
    }

    public void buildToolBars(){
        conditionsToolbar.setFloatable(false);
        actionsToolbar.setFloatable(false);
        optionsToolbar.setFloatable(false);

        JButton addOption = new JButton("Add Option");
        addOption.addActionListener(e -> DialogEditor.this.addOption());
        optionsToolbar.add(addOption);

        JButton copyOption = new JButton("Copy Selected");
        copyOption.addActionListener(e -> DialogEditor.this.copyOption());
        optionsToolbar.add(copyOption);

        JButton removeOption = new JButton("Remove Selected");
        removeOption.addActionListener(e -> DialogEditor.this.removeOption());
        optionsToolbar.add(removeOption);

    }
        public void addOption(){
        optionEditor.addOption();
            emitChanged();
    }

    public void copyOption(){
        emitChanged();
    }

    public void removeOption(){
        int selected=optionEditor.list.getSelectedIndex();
        if(selected<0)
            return;
        optionEditor.model.remove(selected);
        emitChanged();
    }

    protected void emitChanged() {
        if (updating)
            return;

        ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
        if (listeners != null && listeners.length > 0) {
            ChangeEvent evt = new ChangeEvent(this);
            for (ChangeListener listener : listeners) {
                listener.stateChanged(evt);
            }
        }
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

}
