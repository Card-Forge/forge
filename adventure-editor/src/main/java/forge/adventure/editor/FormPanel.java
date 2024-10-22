package forge.adventure.editor;

import javax.swing.*;
import java.awt.*;

public class FormPanel extends JPanel {
    int row=0;
    static final int MAXIMUM_LINES=300;
    public FormPanel()
    {
        setLayout(new GridBagLayout()) ;


        GridBagConstraints constraint=new GridBagConstraints();
        constraint.weightx = 1.0;
        constraint.weighty = 1.0;
        constraint.gridy=MAXIMUM_LINES;
        constraint.gridx=0;
        constraint.gridwidth=2;
        add(Box.createVerticalGlue(),constraint);
        row++;
    }
    public void add(JComponent name,JComponent element)
    {
        GridBagConstraints constraint=new GridBagConstraints();
        constraint.ipadx = 5;
        constraint.ipady = 5;
        constraint.weightx = 1.0;
        constraint.weighty = 0.0;
        constraint.gridy=row;
        constraint.gridx=0;
        constraint.anchor=GridBagConstraints.NORTHWEST;
        add(name,constraint);
        constraint.gridy=row;
        constraint.gridx=1;
        constraint.fill=GridBagConstraints.HORIZONTAL;
        constraint.anchor=GridBagConstraints.NORTHEAST;
        add(element,constraint);

        row++;
    }
    public void add(String name,JComponent element)
    {
        add(new JLabel(name),element);
    }
    public void add(JComponent element)
    {
        GridBagConstraints constraint=new GridBagConstraints();
        constraint.ipadx = 5;
        constraint.ipady = 5;
        constraint.weightx = 1.0;
        constraint.weighty = 0.0;
        constraint.gridy=row;
        constraint.gridx=0;
        constraint.gridwidth=2;
        constraint.fill=GridBagConstraints.HORIZONTAL;
        constraint.anchor=GridBagConstraints.NORTHEAST;
        add(element,constraint);

        row++;
    }

}
