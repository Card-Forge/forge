package forge.adventure.editor;

import forge.adventure.data.DialogData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class DialogEdit extends FormPanel {
    private boolean updating=false;
    DialogData currentData;

    public JTextField name=new JTextField(80);
    public JTextField text=new JTextField(80);
    public JTextField locname=new JTextField(80);
    public JTextField loctext=new JTextField(80);

    JPanel namePanel;
    JPanel locNamePanel;

    public JButton addNode = new JButton("Add node");
    public JButton removeNode = new JButton("Remove node");

    public DialogEdit()
    {
        FormPanel center=new FormPanel() {  };

        name.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!updating)
                    emitChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!updating)
                    emitChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!updating)
                    emitChanged();
            }
        });

        text.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
               if (!updating)
                    emitChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!updating)
                    emitChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!updating)
                    emitChanged();
            }
        });


        JPanel editData = new JPanel();
        editData.setLayout(new BoxLayout(editData, BoxLayout.Y_AXIS));

        namePanel = new JPanel();
        namePanel.setLayout(new FlowLayout());
        namePanel.add(new JLabel("Name:"));
        namePanel.add(name);
        editData.add(namePanel);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new FlowLayout());
        textPanel.add(new JLabel("Text:"));
        textPanel.add(text);
        editData.add(textPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(addNode);
        buttonPanel.add(removeNode);
        editData.add(buttonPanel);

        editData.add(new JLabel("localization tokens for translation"));

        locNamePanel = new JPanel();
        locNamePanel.setLayout(new FlowLayout());
        locNamePanel.add(new JLabel("Name Token:"));
        locNamePanel.add(locname);


        JPanel locTextPanel = new JPanel();
        locTextPanel.setLayout(new FlowLayout());
        locTextPanel.add(new JLabel("Text Token:"));
        locTextPanel.add(loctext);

        editData.add(locNamePanel);
        editData.add(locTextPanel);
        center.add(editData);

        add(center);

        refresh();
    }

    public void refresh(){
        refresh(false);
    }

    public void refresh(boolean onRootNode) {
        setEnabled(currentData!=null);
        if(currentData==null)
        {
            return;
        }
        updating=true;
        name.setText(currentData.name);
        locname.setText(currentData.locname);
        text.setText(currentData.text);

        loctext.setText(currentData.loctext);

        namePanel.setVisible(!onRootNode);
        locNamePanel.setVisible(!onRootNode);





        updating=false;
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
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
}
