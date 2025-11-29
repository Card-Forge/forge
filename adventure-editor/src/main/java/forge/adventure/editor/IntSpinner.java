package forge.adventure.editor;

import javax.swing.*;


public class IntSpinner extends JSpinner {

    public IntSpinner()
    {
        this( 0, 100, 1);
    }
    public IntSpinner(int min,int max,int stepSize)
    {
        super(new SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(min), Integer.valueOf(max), Integer.valueOf(stepSize)));
    }
    public int intValue()
    {
        return (Integer) getValue();
    }
}