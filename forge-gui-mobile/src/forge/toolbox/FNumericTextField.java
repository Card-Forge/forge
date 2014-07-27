package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

public class FNumericTextField extends FTextField {
    private int value;

    public FNumericTextField() {
        setAlignment(HAlignment.RIGHT);
    }

    public int getValue() {
        return value;
    }
    public void setValue(int value0) {
        if (value0 < 0) {
            value0 = 0;
        }
        if (value == value0) { return; }
        value = value0;
        setText(String.valueOf(value));
    }
}
