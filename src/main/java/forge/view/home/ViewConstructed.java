package forge.view.home;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.control.home.ControlConstructed;
import forge.view.toolbox.FSkin;

/** 
 * Populates Swing components of Constructed mode in home screen.
 * 
 */
@SuppressWarnings("serial")
public class ViewConstructed extends JPanel {
    private String constraints;
    private FSkin skin;
    private HomeTopLevel parentView;

    private Timer timer1 = null;
    private int counter;
    private JList<String> lstColorsHuman, lstColorsAI, lstThemesHuman,
        lstThemesAI, lstDecksHuman, lstDecksAI;
    private SubButton btnHumanDeckList, btnAIDeckList;
    private ControlConstructed control;

    /**
     * Populates Swing components of "constructed" mode in home screen.
     * 
     * @param v0 &emsp; HomeTopLevel parent view
     */
    public ViewConstructed(HomeTopLevel v0) {
        // Basic init stuff
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0"));
        parentView = v0;
        skin = AllZone.getSkin();
        control = new ControlConstructed(this);

        // Assemble JLists with arrays from above.
        lstColorsHuman = new JList<String>();
        lstColorsHuman.setListData(control.oa2sa(control.getColorNames()));

        lstColorsAI = new JList<String>();
        lstColorsAI.setListData(control.oa2sa(control.getColorNames()));

        lstDecksHuman = new JList<String>();
        lstDecksHuman.setListData(control.oa2sa(control.getDeckNames()));

        lstDecksAI = new JList<String>();
        lstDecksAI.setListData(control.oa2sa(control.getDeckNames()));

        lstThemesHuman = new JList<String>();
        lstThemesHuman.setListData(control.oa2sa(control.getThemeNames()));

        lstThemesAI = new JList<String>();
        lstThemesAI.setListData(control.oa2sa(control.getThemeNames()));

        // Human deck options area
        JLabel lblHuman = new JLabel("Choose a deck for the human player:");
        lblHuman.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        lblHuman.setForeground(skin.getColor("text"));
        this.add(lblHuman, "w 90%!, h 5%!, gap 5% 5% 2% 0, wrap, span 5 1");

        // Human deck list button
        btnHumanDeckList = new SubButton();
        btnHumanDeckList.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                ViewConstructed.this.control.viewDeckList("Human");
            }
        });

        btnHumanDeckList.setFont(skin.getFont1().deriveFont(Font.PLAIN, 13));
        btnHumanDeckList.setText("View selected deck");
        btnHumanDeckList.setVerticalTextPosition(SwingConstants.CENTER);

        // Add components
        constraints = "w 28%!, h 30%!, span 1 2";
        this.add(new JScrollPane(lstColorsHuman), constraints + ", gapleft 3%");
        this.add(new OrPanel(), "w 5%!, h 30%!, span 1 2");
        this.add(new JScrollPane(lstThemesHuman), constraints);
        this.add(new OrPanel(), "w 5%!, h 30%!, span 1 2");
        this.add(new JScrollPane(lstDecksHuman), "w 28%!, h 25%!, gapbottom 1%, wrap");
        this.add(btnHumanDeckList, "w 28%!, h 4%!, wrap");

        // AI deck options area
        JLabel lblAI = new JLabel("Choose a deck for the AI player:");
        lblAI.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        lblAI.setForeground(skin.getColor("text"));
        this.add(lblAI, "w 90%!, h 5%!, gap 5% 5% 2% 0, wrap, span 5 1");

        // AI deck list button
        btnAIDeckList = new SubButton();
        btnAIDeckList.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                ViewConstructed.this.control.viewDeckList("Computer");
            }
        });
        btnAIDeckList.setFont(skin.getFont1().deriveFont(Font.PLAIN, 13));
        btnAIDeckList.setText("View selected deck");

        // Add components
        this.add(new JScrollPane(lstColorsAI), constraints + ", gapleft 3%");
        this.add(new OrPanel(), "w 5%!, h 30%!, span 1 2");
        this.add(new JScrollPane(lstThemesAI), constraints);
        this.add(new OrPanel(), "w 5%!, h 30%!, span 1 2");
        this.add(new JScrollPane(lstDecksAI), "w 28%!, h 25%!, gapbottom 1%, wrap");
        this.add(btnAIDeckList, "w 28%!, h 4%!, wrap");

        // List box properties
        this.lstColorsHuman.setName("lstColorsHuman");
        this.lstThemesHuman.setName("lstThemesHuman");
        this.lstDecksHuman.setName("lstDecksHuman");

        this.lstColorsAI.setName("lstColorsAI");
        this.lstThemesAI.setName("lstThemesAI");
        this.lstDecksAI.setName("lstDecksAI");

        this.lstThemesHuman.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        this.lstDecksHuman.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);

        this.lstThemesAI.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        this.lstDecksAI.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);

        // Start button
        JButton btnStart = new JButton();
        btnStart.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent arg0) { control.launch(); }
        });
        btnStart.setRolloverEnabled(true);
        btnStart.setPressedIcon(parentView.getStartButtonDown());
        btnStart.setRolloverIcon(parentView.getStartButtonOver());
        btnStart.setIcon(parentView.getStartButtonUp());
        btnStart.setOpaque(false);
        btnStart.setContentAreaFilled(false);
        btnStart.setBorder(null);
        btnStart.setBorderPainted(false);
        btnStart.setBounds(10, 476, 205, 84);

        JPanel pnlButtonContainer = new JPanel();
        pnlButtonContainer.setOpaque(false);
        this.add(pnlButtonContainer, "w 100%!, gaptop 2%, span 5 1");

        pnlButtonContainer.setLayout(new BorderLayout());
        pnlButtonContainer.add(btnStart, SwingConstants.CENTER);

        // When all components have been added, add listeners.
        control.addListeners();
    }

    // For some reason, MigLayout has sizing problems with a JLabel next to a JList.
    // So, the "or" label must be nested in a panel.
    private class OrPanel extends JPanel {
        public OrPanel() {
            super();
            setOpaque(false);
            setLayout(new BorderLayout());

            JLabel lblOr = new JLabel("OR");
            lblOr.setHorizontalAlignment(SwingConstants.CENTER);
            lblOr.setForeground(skin.getColor("text"));
            add(lblOr, BorderLayout.CENTER);
        }
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getParentView() {
        return parentView;
    }

    /** @return ControlConstructed */
    public ControlConstructed getController() {
        return control;
    }

    /**
     * Flashes a visual reminder in a list component.
     *
     * @param lst0 &emsp; JList
     */
    public void remind(JList<String> lst0) {
        if (timer1 != null) { return; }

        final JList<String> target = lst0;
        final int[] steps = {210, 215, 220, 220, 220, 215, 210};
        final Color oldBG = lst0.getBackground();
        counter = 0;

        ActionListener fader = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                counter++;
                if (counter != (steps.length - 1)) {
                    setBackground(new Color(255, 0, 0, steps[counter]));
                }
                else {
                    target.setBackground(oldBG);
                    timer1.stop();
                    timer1 = null;
                }
            }
        };

        timer1 = new Timer(100, fader);
        timer1.start();
    }

    //========= RETRIEVAL FUNCTIONS
    /** @return JList */
    public JList<String> getLstColorsHuman() {
        return lstColorsHuman;
    }

    /** @return JList */
    public JList<String> getLstThemesHuman() {
        return lstThemesHuman;
    }

    /** @return JList */
    public JList<String> getLstDecksHuman() {
        return lstDecksHuman;
    }

    /** @return JList */
    public JList<String> getLstColorsAI() {
        return lstColorsAI;
    }

    /** @return JList */
    public JList<String> getLstThemesAI() {
        return lstThemesAI;
    }

    /** @return JList */
    public JList<String> getLstDecksAI() {
        return lstDecksAI;
    }

    /** @return JButton */
    public JButton getBtnHumanDeckList() {
        return btnHumanDeckList;
    }

    /** @return JButton */
    public JButton getBtnAIDeckList() {
        return btnAIDeckList;
    }

}
