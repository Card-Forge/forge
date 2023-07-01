package forge.view;

import java.awt.Image;

import javax.swing.JMenuBar;

public interface ITitleBarOwner {
    boolean isMinimized();

    void setMinimized(boolean b);

    boolean isMaximized();

    void setMaximized(boolean b);

    boolean isFullScreen();

    void setFullScreen(boolean b);

    boolean getLockTitleBar();

    void setLockTitleBar(boolean b);

    int getWidth();

    void setJMenuBar(JMenuBar menuBar);

    String getTitle();

    Image getIconImage();
}
