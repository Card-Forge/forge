package forge.frontend.components.widgets;

public interface IComponent {
    boolean isEnabled();
    void setEnabled(boolean b0);
    boolean isVisible();
    void setVisible(boolean b0);
    String getToolTipText();
    void setToolTipText(String s0);
}
