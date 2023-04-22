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
public class DialogOptionEditor extends JComponent{
    DefaultListModel<DialogData> model = new DefaultListModel<>();
    JList<DialogData> list = new JList<>(model);
    JToolBar toolBar = new JToolBar("toolbar");
    DialogOptionEdit edit=new DialogOptionEdit();



    public class DialogDataRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof DialogData))
                return label;
            DialogData dialog=(DialogData) value;
            StringBuilder builder=new StringBuilder();
            if(dialog.name==null||dialog.name.isEmpty())
                builder.append("[[Blank Option]]");
            else
                builder.append(dialog.name, 0, Math.min(dialog.name.length(), 25));
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

    public DialogOptionEditor()
    {

        list.setCellRenderer(new DialogDataRenderer());
        list.addListSelectionListener(e -> DialogOptionEditor.this.updateEdit());
        addButton("add", e -> DialogOptionEditor.this.addOption());
        addButton("remove", e -> DialogOptionEditor.this.remove());
        addButton("copy", e -> DialogOptionEditor.this.copy());
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(list, BorderLayout.LINE_START);
        add(toolBar, BorderLayout.PAGE_START);
        toolBar.setFloatable(false);
        add(edit,BorderLayout.CENTER);
        edit.setVisible(false);

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

    private void copy() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        DialogData data=new DialogData(model.get(selected));
        model.add(model.size(),data);
    }

    private void updateEdit() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            edit.setCurrentOption(new DialogData());
        else
            edit.setCurrentOption(model.get(selected));
    }

    void addOption()
    {
        DialogData data=new DialogData();
        model.add(model.size(),data);
        edit.setVisible(true);
        list.setSelectedIndex(model.size() - 1);
    }
    void remove()
    {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        model.remove(selected);
        edit.setVisible(model.size() > 0);
    }
    public void setOptions(DialogData[] options) {
        model.clear();
        if(options==null || options.length == 0)
            options = new DialogData[0];
        for (int i=0;i<options.length;i++) {
            model.add(i,options[i]);
        }
        if (model.size() > 0)
        {
            edit.setVisible(true);
            list.setSelectedIndex(0);
            updateEdit();
        }
        else{
            edit.setVisible(false);
        }
    }

    public DialogData[] getOptions() {
        DialogData[] options= new DialogData[model.getSize()];
        for(int i=0;i<model.getSize();i++)
        {
            options[i]=model.get(i);
        }
        return options;
    }
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

}
