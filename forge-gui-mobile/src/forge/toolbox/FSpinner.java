package forge.toolbox;

import forge.Forge;
import forge.toolbox.FEvent.FEventType;
import forge.util.Callback;


public class FSpinner extends FTextField {
    private int value, minValue, maxValue;

    public FSpinner(int minValue0, int maxValue0) {
        this(minValue0, maxValue0, minValue0);
    }

    public FSpinner(int minValue0, int maxValue0, int initialValue) {
        minValue = minValue0;
        maxValue = maxValue0;
        value = minValue - 1; //ensure value changes so text updated
        setValue(initialValue);
    }
    
    public int getValue() {
        return value;
    }
    public void setValue(int value0) {
        if (value0 < minValue) {
            value0 = minValue;
        }
        else if (value0 > maxValue) {
            value0 = maxValue;
        }
        if (value == value0) { return; }
        value = value0;
        setText(String.valueOf(value));
    }

    @Override
    public boolean tap(float x, float y, int count) {
        GuiChoose.getInteger(Forge.getLocalizer().getMessage("lblSelectANumber"), minValue, maxValue, new Callback<Integer>() {
            @Override
            public void run(Integer result) {
                if (result != null && result != value) {
                    int oldValue = value;
                    setValue(result);
                    if (getChangedHandler() != null) {
                        //handle change event if value changed from input
                        getChangedHandler().handleEvent(new FEvent(FSpinner.this, FEventType.CHANGE, oldValue));
                    }
                }
            }
        });
        return true;
    }

    @Override
    public boolean startEdit() {
        return false; //don't allow editing text
    }
}
