package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class QuestEditor extends JComponent {
    JList<AdventureQuestData> list = new JList<>(QuestController.getInstance().getAllQuests());
    JToolBar toolBar = new JToolBar("toolbar");
    QuestEdit edit=new QuestEdit();

    public class QuestDataRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof AdventureQuestData))
                return label;
            AdventureQuestData quest=(AdventureQuestData) value;
            // Get the renderer component from parent class

            label.setText(quest.name);
            return label;
        }
    }
    public void addButton(String name,ActionListener action)
    {
        JButton newButton=new JButton(name);
        newButton.addActionListener(action);
        toolBar.add(newButton);

    }
    public QuestEditor()
    {

        list.setCellRenderer(new QuestDataRenderer());
        list.addListSelectionListener(e -> QuestEditor.this.updateEdit());
        addButton("Add Quest", e -> QuestEditor.this.addStage());
        addButton("Remove", e -> QuestEditor.this.remove());
        addButton("Copy", e -> QuestEditor.this.copy());
        addButton("Load", e -> QuestController.getInstance().load());
        addButton("Save", e -> QuestEditor.this.save());
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(new JScrollPane(list), BorderLayout.LINE_START);
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.PAGE_START);
        add(edit,BorderLayout.CENTER);
        edit.setVisible(false);
    }
    private void copy() {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        AdventureQuestData data=new AdventureQuestData(QuestController.getInstance().getAllQuests().get(selected));
        data.isTemplate = true;
        QuestController.getInstance().getAllQuests().add(QuestController.getInstance().getAllQuests().size(),data);
    }
    private void updateEdit() {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrentQuest(QuestController.getInstance().getAllQuests().get(selected));
    }

    void save()
    {
        Array<AdventureQuestData> allQuests=new Array<>();
        for(int i=0;i<QuestController.getInstance().getAllQuests().getSize();i++) {
            allQuests.add(QuestController.getInstance().getAllQuests().get(i));
        }
        Json json = new Json(JsonWriter.OutputType.json);
        FileHandle handle = Config.instance().getFile(Paths.QUESTS);
        handle.writeString(json.prettyPrint(json.toJson(allQuests,Array.class, AdventureQuestData.class)),false);
        QuestController.getInstance().save();
    }
    void addStage()
    {
        AdventureQuestData data=new AdventureQuestData();
        data.name="New Quest "+QuestController.getInstance().getAllQuests().getSize();
        data.isTemplate = true;
        QuestController.getInstance().getAllQuests().add(QuestController.getInstance().getAllQuests().size(),data);
    }
    void remove()
    {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        QuestController.getInstance().getAllQuests().remove(selected);
        edit.setVisible(false);
    }
}
