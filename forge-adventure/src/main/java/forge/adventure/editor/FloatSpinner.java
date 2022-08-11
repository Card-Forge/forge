package forge.adventure.editor;

import javax.swing.*;

public class FloatSpinner extends JSpinner{

    public FloatSpinner()
    {
        this( 0.f, 1f, 0.1f);
    }
    public FloatSpinner(float min,float max,float stepSize)
    {
        super(new SpinnerNumberModel(new Float(0.0f), new Float(min), new Float (max), new Float(stepSize)));
    }
    public float floatValue()
    {
        return ((Float)getValue()).floatValue();
    }
}
