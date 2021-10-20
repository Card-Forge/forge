package forge.adventure.editor;

import forge.adventure.util.Config;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class TextListEdit extends Box {
    JTextField edit=new JTextField();
    JButton findButton=new JButton(UIManager.getIcon("add"));
    JComboBox elements;
    public TextListEdit(String[] possibleElements) {
        super(BoxLayout.X_AXIS);

        findButton.addActionListener(e->find());

        add(edit);
        //add(findButton);
        elements= new JComboBox(possibleElements);
        add(elements);

    }
    public TextListEdit()
    {
        this(new String[0]);

    }
    JTextField getEdit()
    {
        return edit;
    }

    private void find() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(Config.instance().getFilePath("")));
        fc.setMultiSelectionEnabled(false);
        if (fc.showOpenDialog(this) ==
                JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();

            try {
                if (selected != null&&selected.getCanonicalPath().startsWith(new File(Config.instance().getFilePath("")).getCanonicalPath())) {
                    edit.setText(selected.getCanonicalPath().substring(new File(Config.instance().getFilePath("")).getCanonicalPath().length()+1));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setText(String[] itemName) {
        if(itemName==null)
            edit.setText("");
        else
            edit.setText(String.join(";",itemName));
    }

    public void setText(int[] intValues) {
        if(intValues==null)
        {
            edit.setText("");
            return;
        }
        StringBuilder values= new StringBuilder();
        for(int i=0;i<intValues.length;i++)
        {
            values.append(intValues[i]);
            if(intValues.length>i+2)
                values.append(";");
        }
        edit.setText(values.toString());
    }

    public String[] getList() {
        return edit.getText().isEmpty()?null:edit.getText().split(";");
    }

    public int[] getListAsInt() {
        if(edit.getText().isEmpty())
            return null;
        String[] stringList=getList();
        int[] retList=new int[stringList.length];
        for(int i=0;i<retList.length;i++)
        {
            String intName=stringList[i];
            try
            {
                retList[i] = Integer.valueOf(intName);
            }
            catch (NumberFormatException e)
            {
                retList[i] =0;
            }
        }
        return retList;
    }
}
