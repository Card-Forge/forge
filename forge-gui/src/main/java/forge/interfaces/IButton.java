package forge.interfaces;

import forge.UiCommand;
import forge.assets.FSkinProp;

public interface IButton extends ITextComponent {
    boolean isSelected();
    void setSelected(boolean b0);
    boolean requestFocusInWindow();
    void setCommand(UiCommand command0);
    void setImage(FSkinProp color);
    void setTextColor(int r, int g, int b);
}
