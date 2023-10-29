package forge.adventure.editor;

import forge.adventure.data.AdventureQuestData;
import forge.adventure.data.AdventureQuestStage;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class QuestStageEditor extends JComponent{
    DefaultListModel<AdventureQuestStage> model = new DefaultListModel<>();
    JList<AdventureQuestStage> list = new JList<>(model);
    JScrollPane scroll;
    JToolBar toolBar = new JToolBar("toolbar");
    QuestStageEdit edit=new QuestStageEdit();

    AdventureQuestData currentData;

    public class QuestStageRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof AdventureQuestStage))
                return label;
            AdventureQuestStage stageData=(AdventureQuestStage) value;
            label.setText(stageData.name);
            //label.setIcon(new ImageIcon(Config.instance().getFilePath(stageData.sourcePath))); //Type icon eventually?
            return label;
        }
    }
    public void addButton(String name, ActionListener action)
    {
        JButton newButton=new JButton(name);
        newButton.addActionListener(action);
        toolBar.add(newButton);

    }

    public QuestStageEditor()
    {
        list.setCellRenderer(new QuestStageRenderer());
        list.addListSelectionListener(e -> QuestStageEditor.this.updateEdit());
        addButton("Add Quest Stage", e -> QuestStageEditor.this.addStage());
        addButton("Remove Selected", e -> QuestStageEditor.this.remove());
        toolBar.setFloatable(false);
        setLayout(new BorderLayout());
        scroll = new JScrollPane(list);
        add(scroll, BorderLayout.WEST);
        JPanel editPanel = new JPanel();
        editPanel.setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        editPanel.add(edit,BorderLayout.CENTER);
        add(editPanel);
        edit.addChangeListener(e -> emitChanged());
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

    private void updateEdit() {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrentStage(model.get(selected),currentData);
    }

    void addStage()
    {
        AdventureQuestStage data=new AdventureQuestStage();
        data.name = "New Stage";
        model.add(model.size(),data);
        edit.setVisible(true);
        scroll.setVisible(true);
        int id = 0;
        for (int i = 0; i < model.size(); i++)
        {
            if (model.get(i).id >= id)
                id = model.get(i).id +1;
        }
        data.id = id;
        if (model.size() == 1){
            list.setSelectedIndex(0);
        }
        emitChanged();
    }
    void remove()
    {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        model.remove(selected);
        edit.setVisible(false);
        scroll.setVisible(list.getModel().getSize() > 0);
        emitChanged();
    }
    public void setStages(AdventureQuestData data) {

        currentData=data;
        model.clear();
        if(data==null||data.stages==null || data.stages.length == 0)
        {
            edit.setVisible(false);
            return;
        }
        for (int i=0;i<data.stages.length;i++) {
            model.add(i,data.stages[i]);
        }
        if (model.size() > 0) {
            list.setSelectedIndex(0);
        }
    }

    public AdventureQuestStage[] getStages() {

        AdventureQuestStage[] stages= new AdventureQuestStage[model.getSize()];
        for(int i=0;i<model.getSize();i++)
        {
            stages[i]=model.get(i);
        }
        return stages;
    }
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }
}
