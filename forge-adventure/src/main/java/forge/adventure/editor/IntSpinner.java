package forge.adventure.editor;

import javax.swing.*;


public class IntSpinner extends JSpinner {

    public IntSpinner()
    {
        this( 0, 100, 1);
    }
    public IntSpinner(int min,int max,int stepSize)
    {
        super(new SpinnerNumberModel(new Integer(0), new Integer(min), new Integer (max), new Integer(stepSize)));
    }
    public int intValue()
    {
        return ((Integer)getValue()).intValue();
    }
}