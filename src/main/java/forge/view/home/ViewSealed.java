package forge.view.home;

import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.control.home.ControlSealed;
import forge.game.GameType;
import forge.view.toolbox.DeckLister;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;

/** 
 * Assembles swing components for "Sealed" mode menu.
 *
 */
@SuppressWarnings("serial")
public class ViewSealed extends JPanel {
    private FSkin skin;
    private HomeTopLevel parentView;
    private ControlSealed control;
    private DeckLister lstHumanDecks;
    private JButton btnBuild, btnStart;

    /**
     * Assembles swing components for "Sealed" mode menu.
     * 
     * @param v0 {@link forge.view.home.HomeTopLevel} parent view
     */
    public ViewSealed(HomeTopLevel v0) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        parentView = v0;
        skin = AllZone.getSkin();
        control = new ControlSealed(this);

        // Title
        JLabel lblTitle = new JLabel("Select a deck for yourself, or build a new one: ");
        lblTitle.setFont(skin.getFont1().deriveFont(Font.BOLD, 15));
        lblTitle.setForeground(skin.getColor("text"));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);

        // Deck lister
        lstHumanDecks = new DeckLister(GameType.Sealed, control.getExitCommand());

        // Build button
        btnBuild = new SubButton("Build A New Deck");

        // Start button
        btnStart = new StartButton(parentView);

        // Add components
        this.add(lblTitle, "w 100%!, gap 0 0 2% 2%");
        this.add(new FScrollPane(lstHumanDecks), "w 90%!, h 35%!, gap 5% 0 2% 2%");
        this.add(btnBuild, "w 50%!, h 5%!, gap 25% 0 0 0, wrap");
        this.add(btnStart, "ax center, gaptop 5%");

        control.updateDeckLists();
        control.addListeners();
    }

    /** @return {@link forge.view.home.HomeTopLevel} */
    public HomeTopLevel getParentView() {
        return parentView;
    }

    /** @return {@link javax.swing.JList} */
    public DeckLister getLstHumanDecks() {
        return lstHumanDecks;
    }

    /** @return {@link forge.control.home.ControlSealed} */
    public ControlSealed getController() {
        return control;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnBuild() {
        return btnBuild;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return btnStart;
    }
}
