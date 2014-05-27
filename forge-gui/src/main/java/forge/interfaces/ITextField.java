package forge.interfaces;

public interface ITextField {
    boolean isEnabled();
    void setEnabled(boolean b0);
    boolean isVisible();
    void setVisible(boolean b0);
    String getText();
    void setText(String text0);
    boolean requestFocusInWindow();
}
