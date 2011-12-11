package forge.view.home;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class ViewSealed extends JPanel {
    private FSkin skin;
    private HomeTopLevel parentView;

    /**
     * TODO: Write javadoc for Constructor.
     * @param v0 &emsp; HomeTopLevel parent view
     */
    public ViewSealed(HomeTopLevel v0) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0"));
        parentView = v0;
        skin = AllZone.getSkin();

        JLabel lblTitle = new JLabel("Select a deck for each player: ");
        lblTitle.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        lblTitle.setForeground(skin.getColor("text"));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(lblTitle, "w 100%!, gap 0 0 2% 2%, span 3 1, wrap");

        String[] human = {"one", "two", "three"};
        String[] ai = {"four", "five:", "siz"};

        JList humanDecks = new JList(human);
        JList aiDecks = new JList(ai);

        //
        this.add(new JScrollPane(humanDecks), "w 30%!, gapleft 15%, gapright 5%, h 30%!");
        this.add(new JScrollPane(aiDecks), "w 30%!, h 30%!, wrap");

        SubButton buildHuman = new SubButton("Build New Human Deck");
        this.add(buildHuman, "w 30%!, h 5%!, gapleft 15%, gapright 15%, gaptop 1%");

        SubButton buildAI = new SubButton("Build New AI Deck");
        this.add(buildAI, "w 30%!, h 5%!, gaptop 1%, wrap");

        // Start button
        JButton btnStart = new JButton();
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
        this.add(pnlButtonContainer, "w 100%!, gaptop 10%, span 3 1");

        pnlButtonContainer.setLayout(new BorderLayout());
        pnlButtonContainer.add(btnStart, SwingConstants.CENTER);
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getParentView() {
        return parentView;
    }
}
