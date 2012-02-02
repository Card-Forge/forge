package forge.view.toolbox;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import forge.Singletons;

/** 
 * Standard button used for for submenus on the home screen.
 *
 */
@SuppressWarnings("serial")
public class SubButton extends JButton {
    private FSkin skin;
    private final Color clrHover, clrInactive;

    /** */
    public SubButton() {
        this("");
    }

    /**
     * 
     * Standard button used for for submenus on the home screen.
     *
     * @param txt0 &emsp; String
     */
    public SubButton(String txt0) {
        super(txt0);
        this.skin = Singletons.getView().getSkin();
        this.clrHover = skin.getColor(FSkin.Colors.CLR_HOVER);
        this.clrInactive = skin.getColor(FSkin.Colors.CLR_INACTIVE);

        setBorder(new LineBorder(skin.getColor(FSkin.Colors.CLR_BORDERS), 1));
        setBackground(clrInactive);
        setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
        setVerticalTextPosition(SwingConstants.CENTER);
        setFocusPainted(false);

        this.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (isEnabled() && !isSelected()) { setBackground(clrHover); }
            }

            public void mouseExited(MouseEvent e) {
                if (isEnabled() && !isSelected()) { setBackground(clrInactive); }
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int px =  (int) (SubButton.this.getHeight() / 2);
                px = (px < 10 ? 10 : px);
                px = (px > 15 ? 15 : px);
                SubButton.this.setFont(Singletons.getView().getSkin().getFont(px));
            }
        });
    }

    @Override
    public void setEnabled(boolean b0) {
        super.setEnabled(b0);

        if (b0) { setBackground(skin.getColor(FSkin.Colors.CLR_INACTIVE)); }
        else { setBackground(new Color(220, 220, 220)); }
    }

    @Override
    public void setSelected(boolean b0) {
        super.setSelected(b0);
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.clearRect(0, 0, getWidth(), getHeight());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
