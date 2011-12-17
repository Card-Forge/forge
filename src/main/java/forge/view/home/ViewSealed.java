package forge.view.home;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Command;
import forge.control.home.ControlSealed;
import forge.deck.Deck;
import forge.game.GameType;
import forge.view.toolbox.DeckLister;
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
    private JList lstAIDecks;
    private DeckLister lstHumanDecks;

    /**
     * Assembles swing components for "Sealed" mode menu.
     * 
     * @param v0 &emsp; HomeTopLevel parent view
     */
    public ViewSealed(HomeTopLevel v0) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));
        parentView = v0;
        skin = AllZone.getSkin();

        JLabel lblTitle = new JLabel("Select a deck for each player: ");
        lblTitle.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        lblTitle.setForeground(skin.getColor("text"));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(lblTitle, "w 100%!, gap 0 0 2% 2%, span 2 1");

        lstAIDecks = new JList();

        final Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            @Override
            public void execute() {
                control.updateDeckLists();
            }
        };

        // Human deck list area, ai names
        Collection<Deck[]> temp = AllZone.getDeckManager().getDraftDecks().values();
        List<Deck> human = new ArrayList<Deck>();
        for (Deck[] d : temp) { human.add(d[0]); }
        lstHumanDecks = new DeckLister(GameType.Sealed, exit);
        //
        this.add(new JScrollPane(lstHumanDecks), "w 60%!, h 30%!, gap 5% 5% 2% 2%");
        this.add(new JScrollPane(lstAIDecks), "w 25%!, h 37%!, gap 0 0 2% 0, span 1 2, wrap");

        SubButton buildHuman = new SubButton("Build New Human Deck");
        buildHuman.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { control.setupSealed(); }
        });
        this.add(buildHuman, "w 60%!, h 5%!, gap 5% 5% 0 0, wrap");

        // Start button
        JButton btnStart = new JButton();
        btnStart.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { control.start(); }
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
        this.add(pnlButtonContainer, "w 100%!, gaptop 10%, span 3 1");

        pnlButtonContainer.setLayout(new BorderLayout());
        pnlButtonContainer.add(btnStart, SwingConstants.CENTER);

        control = new ControlSealed(this);
        control.updateDeckLists();
        lstAIDecks.setSelectedIndex(0);
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getParentView() {
        return parentView;
    }

    /** @return JList */
    public DeckLister getLstHumanDecks() {
        return lstHumanDecks;
    }

    /** @return JList */
    public JList getLstAIDecks() {
        return lstAIDecks;
    }

    /** @return ControlSealed */
    public ControlSealed getController() {
        return control;
    }
}
