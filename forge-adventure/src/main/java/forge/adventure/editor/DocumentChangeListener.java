package forge.adventure.editor;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DocumentChangeListener implements DocumentListener {
    private Runnable run;

    public DocumentChangeListener(Runnable run)
    {

        this.run = run;
    }
    @Override
    public void insertUpdate(DocumentEvent e) {

        changedUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        run.run();
    }

}
