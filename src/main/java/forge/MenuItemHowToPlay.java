package forge;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * MenuItem_HowToPlay class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class MenuItemHowToPlay extends JMenuItem {
    /** Constant <code>serialVersionUID=5552000208438248428L</code>. */
    private static final long serialVersionUID = 5552000208438248428L;

    /**
     * <p>
     * Constructor for MenuItem_HowToPlay.
     * </p>
     */
    public MenuItemHowToPlay() {
        super(ForgeProps.getLocalized(NewConstants.Lang.HowTo.TITLE));

        this.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent a) {
                String text = ForgeProps.getLocalized(NewConstants.Lang.HowTo.MESSAGE);

                JTextArea area = new JTextArea(text, 25, 40);
                area.setWrapStyleWord(true);
                area.setLineWrap(true);
                area.setEditable(false);

                area.setOpaque(false);

                JOptionPane.showMessageDialog(null, new JScrollPane(area), ForgeProps.getLocalized(NewConstants.Lang.HowTo.TITLE),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    } // constructor
} // MenuItem_HowToPlay

