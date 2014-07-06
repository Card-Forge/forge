package forge.interfaces;

import forge.UiCommand;
import forge.assets.FSkinProp;

public interface IButton {
    boolean isEnabled();
    void setEnabled(boolean b0);
    boolean isVisible();
    void setVisible(boolean b0);
    String getText();
    void setText(String text0);
    boolean isSelected();
    void setSelected(boolean b0);
    boolean requestFocusInWindow();
    void setCommand(UiCommand command0);
    void setTextColor(FSkinProp color);
    void setTextColor(int r, int g, int b);
}
