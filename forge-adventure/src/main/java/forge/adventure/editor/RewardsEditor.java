package forge.adventure.editor;

import forge.adventure.data.RewardData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class RewardsEditor extends JComponent{
    DefaultListModel<RewardData> model = new DefaultListModel<>();
    JList<RewardData> list = new JList<>(model);
    JToolBar toolBar = new JToolBar("toolbar");
    RewardEdit edit=new RewardEdit();



    public class RewardDataRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof RewardData))
                return label;
            RewardData reward=(RewardData) value;
            StringBuilder builder=new StringBuilder();
            if(reward.type==null||reward.type.isEmpty())
                builder.append("Reward");
            else
                builder.append(reward.type);
            builder.append(" ");
            builder.append(reward.count);
            if(reward.addMaxCount>0)
            {
                builder.append("-");
                builder.append(reward.count+reward.addMaxCount);
            }
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

    public RewardsEditor()
    {

        list.setCellRenderer(new RewardsEditor.RewardDataRenderer());
        list.addListSelectionListener(e -> updateEdit());
        addButton("add",e->addReward());
        addButton("remove",e->remove());
        addButton("copy",e->copy());
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
        RewardData data=new RewardData(model.get(selected));
        model.add(model.size(),data);
    }

    private void updateEdit() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrentReward(model.get(selected));
    }

    void addReward()
    {
        RewardData data=new RewardData();
        model.add(model.size(),data);
    }
    void remove()
    {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        model.remove(selected);
    }
    public void setRewards(RewardData[] rewards) {

        model.clear();
        for (int i=0;i<rewards.length;i++) {
            model.add(i,rewards[i]);
        }
    }

    public RewardData[] getRewards() {

        RewardData[] rewards= new RewardData[model.getSize()];
        for(int i=0;i<model.getSize();i++)
        {
            rewards[i]=model.get(i);
        }
        return rewards;
    }
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }
}
