package forge.adventure.editor;

import forge.adventure.data.DialogData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class DialogOptionEdit extends FormPanel {
    DialogData currentData;

    JLabel nameLabel = new JLabel("Name (Player dialog / action)");
    JLabel textLabel = new JLabel("Text (Game response to Name - Leave blank to end dialog)");
    JTextArea text =new JTextArea(3,80);
    JTextArea name =new JTextArea(3,80);
    JButton add = new JButton();
    JButton load = new JButton();

    private boolean updating=false;

    public DialogOptionEdit()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel upper = new JPanel();
        upper.setLayout(new BorderLayout());

        upper.add(nameLabel, BorderLayout.NORTH);
        upper.add(name, BorderLayout.CENTER);
        add(upper);

        JPanel middle = new JPanel();
        middle.setLayout(new BorderLayout());
        middle.add(textLabel, BorderLayout.NORTH);
        middle.add(text, BorderLayout.CENTER);

        add(middle);

        name.getDocument().addDocumentListener(new DocumentChangeListener(() -> DialogOptionEdit.this.updateDialog()));
        text.getDocument().addDocumentListener(new DocumentChangeListener(() -> DialogOptionEdit.this.updateDialog()));


    }

    private void updateDialog() {
        if(currentData==null||updating)
            return;

        currentData.text = text.getText().trim();
        currentData.name = name.getText().trim();

        //currentData.condition = conditionEditor.getConditions();

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
    public void setCurrentOption(DialogData data)
    {
        currentData=data;
        refresh();
    }

    private void refresh() {
        setEnabled(currentData!=null);
        updating=true;

        text.setText(currentData.text!=null?currentData.text:"");
        name.setText(currentData.name!=null?currentData.name:"");

        updating=false;
    }
}
