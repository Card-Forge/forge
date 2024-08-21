package forge.adventure.editor;

import forge.adventure.data.EffectData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class EffectEditor extends JComponent  {
    EffectData currentData;


    JTextField name =new JTextField();
    JSpinner changeStartCards = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    JSpinner lifeModifier = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    JSpinner moveSpeed = new JSpinner(new SpinnerNumberModel(0f, 0, 1, 0.1f));
    TextListEdit startBattleWithCard =new TextListEdit();
    JCheckBox colorView =new JCheckBox();
    EffectEditor opponent = null; 
    private boolean updating=false;

    public EffectEditor(boolean isOpponentEffect)
    {
        if(!isOpponentEffect)
            opponent=new EffectEditor(true);
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        FormPanel parameters=new FormPanel();
        parameters.setBorder(BorderFactory.createTitledBorder("Effect"));

        parameters.add("Name:",  name);
        parameters.add("Start with extra cards:",    changeStartCards);
        parameters.add("Change life:",             lifeModifier);
        parameters.add("Movement speed:",          moveSpeed);
        parameters.add("Start battle with cards:",    startBattleWithCard);
        parameters.add("color view:",              colorView);
        add(parameters);
        if(!isOpponentEffect)
        {    add(new JLabel("Opponent:")); add(opponent);}

 
        changeStartCards.addChangeListener(e -> EffectEditor.this.updateEffect());
        lifeModifier.addChangeListener(e -> EffectEditor.this.updateEffect());
        moveSpeed.addChangeListener(e -> EffectEditor.this.updateEffect());
        colorView.addChangeListener(e -> EffectEditor.this.updateEffect());
        name.getDocument().addDocumentListener(new DocumentChangeListener(EffectEditor.this::updateEffect));
        startBattleWithCard.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(EffectEditor.this::updateEffect));
        if(opponent!=null)

            opponent.addChangeListener(e -> EffectEditor.this.updateEffect());

    }

    private void updateEffect() {
        if(currentData==null||updating)
            return;

 

        currentData.name=name.getText();
        currentData.changeStartCards = (Integer) changeStartCards.getValue();
        currentData.lifeModifier = (Integer) lifeModifier.getValue();
        currentData.moveSpeed = (Float) moveSpeed.getValue();
        currentData.startBattleWithCard = startBattleWithCard.getList();
        currentData.colorView = colorView.isSelected();
        currentData.opponent = opponent.currentData;

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
    public void setCurrentEffect(EffectData data)
    {
        if(data==null)
            return;
        currentData=data;
        refresh();
    }
    public EffectData getCurrentEffect()
    {
        return currentData;
    }

    private void refresh() {
        setEnabled(currentData!=null);
        if(currentData==null)
        {
            return;
        }
        updating=true;
        name.setText(currentData.name);



        lifeModifier.setValue(currentData.lifeModifier);
        changeStartCards.setValue(currentData.changeStartCards);
        startBattleWithCard.setText(currentData.startBattleWithCard);
        colorView.setSelected(currentData.colorView);
        moveSpeed.setValue(currentData.moveSpeed);
        if(opponent!=null)
            opponent.setCurrentEffect(currentData.opponent);


        updating=false;
    }
}
