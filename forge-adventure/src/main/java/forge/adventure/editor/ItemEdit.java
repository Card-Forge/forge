package forge.adventure.editor;

import forge.adventure.data.ItemData;

import javax.swing.*;
import java.awt.*;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class ItemEdit extends JComponent {
    ItemData currentData;


    JTextField nameField=new JTextField();
    JTextField equipmentSlot=new JTextField();
    JTextField iconName=new JTextField();
    EffectEditor effect=new EffectEditor(false);
    JTextField description=new JTextField();
    JCheckBox questItem=new JCheckBox();
    JSpinner cost= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));

    private boolean updating=false;

    public ItemEdit()
    {

        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        JPanel parameters=new JPanel();
        parameters.setBorder(BorderFactory.createTitledBorder("Parameter"));
        parameters.setLayout(new GridLayout(6,2)) ;

        parameters.add(new JLabel("Name:")); parameters.add(nameField);
        parameters.add(new JLabel("equipmentSlot:")); parameters.add(equipmentSlot);
        parameters.add(new JLabel("description:")); parameters.add(description);
        parameters.add(new JLabel("iconName")); parameters.add(iconName);
        parameters.add(new JLabel("questItem")); parameters.add(questItem);
        parameters.add(new JLabel("cost")); parameters.add(cost);

        add(parameters);
        add(effect);

        nameField.getDocument().addDocumentListener(new DocumentChangeListener(() -> ItemEdit.this.updateItem()));
        equipmentSlot.getDocument().addDocumentListener(new DocumentChangeListener(() -> ItemEdit.this.updateItem()));
        description.getDocument().addDocumentListener(new DocumentChangeListener(() -> ItemEdit.this.updateItem()));
        iconName.getDocument().addDocumentListener(new DocumentChangeListener(() -> ItemEdit.this.updateItem()));
        cost.addChangeListener(e -> ItemEdit.this.updateItem());
        questItem.addChangeListener(e -> ItemEdit.this.updateItem());
        effect.addChangeListener(e -> ItemEdit.this.updateItem());
        refresh();
    }

    private void updateItem() {
        if(currentData==null||updating)
            return;
        currentData.name=nameField.getText();
        currentData.equipmentSlot=  equipmentSlot.getText();
        currentData.effect= effect.getCurrentEffect();
        currentData.description=description.getText();
        currentData.iconName=iconName.getText();
        currentData.questItem=questItem.isSelected();
        currentData.cost=((Integer)  cost.getValue()).intValue();
    }

    public void setCurrentItem(ItemData data)
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
        nameField.setText(currentData.name);
        effect.setCurrentEffect(currentData.effect);
        equipmentSlot.setText(currentData.equipmentSlot);
        description.setText(currentData.description);
        iconName.setText(currentData.iconName);
        questItem.setSelected(currentData.questItem);
        cost.setValue(currentData.cost);

        updating=false;
    }
}