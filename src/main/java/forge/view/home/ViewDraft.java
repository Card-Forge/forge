package forge.view.home;

import java.awt.BorderLayout;
import java.awt.Font;

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
public class ViewDraft extends JPanel {
    private FSkin skin;
    private HomeTopLevel parentView;

    /**
     * TODO: Write javadoc for Constructor.
     * @param v0 &emsp; HomeTopLevel parent view
     */
    public ViewDraft(HomeTopLevel v0) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0"));
        parentView = v0;
        skin = AllZone.getSkin();

        JLabel lblHuman = new JLabel("Select a deck for human: ");
        lblHuman.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        lblHuman.setHorizontalAlignment(SwingConstants.CENTER);
        lblHuman.setForeground(skin.getColor("text"));
        this.add(lblHuman, "w 30%!, gap 15% 15% 2% 2%");

        JLabel lblAI = new JLabel("AI Deck Count: ");
        lblAI.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        lblAI.setHorizontalAlignment(SwingConstants.CENTER);
        lblAI.setForeground(skin.getColor("text"));
        this.add(lblAI, "w 30%!, gap 0 0 2% 2%, wrap");

        String[] human = {"one", "two", "three"};
        String[] ai = {"1", "2", "3", "4", "5", "6", "7"};

        JList humanDecks = new JList(human);
        JList aiDecks = new JList(ai);
        this.add(new JScrollPane(humanDecks), "w 30%!, gapleft 15%, gapright 5%, h 30%!");
        this.add(new JScrollPane(aiDecks), "w 30%!, h 30%!, wrap");

        SubButton buildHuman = new SubButton("Build New Human Deck");
        this.add(buildHuman, "w 30%!, h 5%!, gap 15% 15% 1% 1%, wrap");

        //

        // Start button
        StartButton btnStart = new StartButton(parentView);

        JPanel pnlButtonContainer = new JPanel();
        pnlButtonContainer.setOpaque(false);
        this.add(pnlButtonContainer, "w 100%!, gaptop 10%, dock south");

        pnlButtonContainer.setLayout(new BorderLayout());
        pnlButtonContainer.add(btnStart, SwingConstants.CENTER);
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getParentView() {
        return parentView;
    }
}
