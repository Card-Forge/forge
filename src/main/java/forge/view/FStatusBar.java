package forge.view;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import forge.gui.toolbox.FDigitalClock;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.Colors;
import forge.gui.toolbox.FSkin.JComponentSkin;
import forge.gui.toolbox.FSkin.SkinColor;

@SuppressWarnings("serial")
public class FStatusBar extends JPanel {
    private static final SkinColor foreColor = FSkin.getColor(Colors.CLR_TEXT).alphaColor(150);
    private static final SkinColor clrTheme = FSkin.getColor(Colors.CLR_THEME);
    private static final SkinColor backColor = clrTheme.stepColor(0).darker();
    private static final SkinColor borderColor = clrTheme.stepColor(-80);

    private final FFrame frame;
    private final JComponentSkin<FStatusBar> skin = FSkin.get(this);
    private final SpringLayout layout = new SpringLayout();
    private final JLabel lblStatus = new JLabel();
    private final FDigitalClock clock = new FDigitalClock();

    public FStatusBar(FFrame f, boolean visible0) {
        this.frame = f;
        setPreferredSize(new Dimension(f.getWidth(), 19));
        setLayout(this.layout);
        setStatusText(""); //set default status based on frame title
        skin.setBackground(backColor);
        skin.setMatteBorder(1, 0, 0, 0, borderColor);
        FSkin.get(lblStatus).setForeground(foreColor);
        FSkin.get(clock).setForeground(foreColor);

        add(lblStatus);
        layout.putConstraint(SpringLayout.WEST, lblStatus, 5, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, lblStatus, 0, SpringLayout.NORTH, this);
        
        add(clock);
        layout.putConstraint(SpringLayout.EAST, clock, -4, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.NORTH, clock, 0, SpringLayout.NORTH, lblStatus);
        
        this.setVisible(visible0);
    }

    public void setStatusText(String text) {
        text = text.trim();
        if (text.isEmpty()) {
            text = this.frame.getTitle(); //show Forge frame title if no other status to show
        }
        this.lblStatus.setText(text);
    }
}
