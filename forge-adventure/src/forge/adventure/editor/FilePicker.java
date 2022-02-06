package forge.adventure.editor;

import forge.adventure.util.Config;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class FilePicker extends Box {
    JTextField edit=new JTextField();
    JButton findButton=new JButton(UIManager.getIcon("FileView.directoryIcon"));
    private final String[] fileEndings;

    public FilePicker(String[] fileEndings) {
        super(BoxLayout.X_AXIS);
        this.fileEndings = fileEndings;

        findButton.addActionListener(e->find());

        add(edit);
        add(findButton);

    }
    JTextField getEdit()
    {
        return edit;
    }

    private void find() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(Config.instance().getFilePath(edit.getText())));
        fc.setFileFilter( new FileNameExtensionFilter("Pick File",fileEndings));
        fc.setMultiSelectionEnabled(false);
        if (fc.showOpenDialog(this) ==
                JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();

            try {
                if (selected != null&&selected.getCanonicalPath().startsWith(new File(Config.instance().getFilePath("")).getCanonicalPath())) {
                    edit.setText(selected.getCanonicalPath().substring(new File(Config.instance().getFilePath("")).getCanonicalPath().length()+1).replace('\\','/'));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
