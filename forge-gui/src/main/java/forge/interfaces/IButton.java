package forge.interfaces;

import forge.UiCommand;
import forge.assets.FSkinProp;

public interface IButton {
    void setEnabled(boolean b0);
    void setVisible(boolean b0);
    void setText(String text0);
    boolean isSelected();
    void setSelected(boolean b0);
    boolean requestFocusInWindow();
    void setCommand(UiCommand command0);
    void setTextColor(FSkinProp color);
    void setTextColor(int r, int g, int b);
}
