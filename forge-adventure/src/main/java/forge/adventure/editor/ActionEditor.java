package forge.adventure.editor;

import forge.adventure.data.DialogData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class ActionEditor extends JComponent{
    DefaultListModel<DialogData.ActionData> model = new DefaultListModel<>();
    JList<DialogData.ActionData> list = new JList<>(model);
    JToolBar toolBar = new JToolBar("toolbar");
    ActionEdit edit=new ActionEdit();
    boolean updating;


    public class RewardDataRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof DialogData.ActionData))
                return label;
            DialogData.ActionData action=(DialogData.ActionData) value;
            StringBuilder builder=new StringBuilder();
//            if(action.type==null||action.type.isEmpty())
                builder.append("Action");
//            else
//                builder.append(action.type);
            label.setText(builder.toString());
            return label;
        }
    }
    public void addButton(String name, ActionListener action)
    {
        JButton newButton=new JButton(name);
        newButton.addActionListener(action);
        toolBar.add(newButton);

    }

    public ActionEditor()
    {

        list.setCellRenderer(new RewardDataRenderer());
        list.addListSelectionListener(e -> ActionEditor.this.updateEdit());
        addButton("add", e -> ActionEditor.this.addAction());
        addButton("remove", e -> ActionEditor.this.remove());
        addButton("copy", e -> ActionEditor.this.copy());
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(list, BorderLayout.LINE_START);
        add(toolBar, BorderLayout.PAGE_START);
        add(edit,BorderLayout.CENTER);


        edit.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                emitChanged();
            }
        });
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
    private void copy() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        DialogData.ActionData data=new DialogData.ActionData(model.get(selected));
        model.add(model.size(),data);
    }

    private void updateEdit() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrentAction(model.get(selected));
    }

    void addAction()
    {
        DialogData.ActionData data=new DialogData.ActionData();
        model.add(model.size(),data);
    }
    void remove()
    {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        model.remove(selected);
    }
    public void setAction(DialogData.ActionData[] actions) {

        model.clear();
        if(actions==null)
            return;
        for (int i=0;i<actions.length;i++) {
            if (actions[i].grantRewards.length > 0){
                continue; //handled in separate editor and joined in on save, will get duplicated if it appears here
            }
            model.add(i,actions[i]);
        }
    }

    public DialogData.ActionData[] getAction() {

        DialogData.ActionData[] action= new DialogData.ActionData[model.getSize()];
        for(int i=0;i<model.getSize();i++)
        {
            action[i]=model.get(i);
        }
        return action;
    }

    public void clear(){
        updating = true;
        model.clear();
        updating = false;
    }
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }
}
