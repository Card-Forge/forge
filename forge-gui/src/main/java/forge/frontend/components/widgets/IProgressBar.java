package forge.frontend.components.widgets;

public interface IProgressBar {
    void setDescription(String s0);
    void setValue(int progress);
    void reset();
    void setShowETA(boolean b0);
    void setShowCount(boolean b0);
    void setPercentMode(boolean percentMode0);
    int getMaximum();
    void setMaximum(int maximum0);
}
