package forge.view.home;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.control.home.ControlDraft;
import forge.deck.Deck;
import forge.game.GameType;
import forge.view.toolbox.DeckLister;
import forge.view.toolbox.FButton;
import forge.view.toolbox.FOverlay;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;

/** 
 * Assembles swing components for "Draft" mode menu.
 *
 */
@SuppressWarnings("serial")
public class ViewDraft extends JPanel {
    private final FSkin skin;
    private final ControlDraft control;
    private final HomeTopLevel parentView;
    private final JList lstAI;
    private final DeckLister lstHumanDecks;
    private final JTextPane tpnDirections;
    private final JLabel lblDirections;

    private String[] opponentNames = new String[] {
            "Abigail", "Ada", "Adeline", "Adriana", "Agatha", "Agnes", "Aileen", "Alba", "Alcyon",
            "Alethea", "Alice", "Alicia", "Alison", "Amanda", "Amelia", "Amy", "Andrea", "Angelina",
            "Anita", "Ann", "Annabel", "Anne", "Audrey", "Barbara", "Belinda", "Bernice", "Bertha",
            "Bonnie", "Brenda", "Bridget", "Bunny", "Carmen", "Carol", "Catherine", "Cheryl",
            "Christine", "Cinderalla", "Claire", "Clarice", "Claudia", "Constance", "Cora",
            "Corinne", "Cnythia", "Daisy", "Daphne", "Dawn", "Deborah", "Diana", "Dolly", "Dora",
            "Doreen", "Doris", "Dorothy", "Eileen", "Elaine", "Elizabeth", "Emily", "Emma", "Ethel",
            "Evelyn", "Fiona", "Florence", "Frances", "Geraldine", "Gertrude", "Gladys", "Gloria",
            "Grace", "Greta", "Harriet", "Hazel", "Helen", "Hilda", "Ida", "Ingrid", "Irene",
            "Isabel", "Jacinta", "Jackie", "Jane", "Janet", "Janice", "Jennifer", "Jessie", "Joan",
            "Jocelyn", "Josephine", "Joyce", "Judith", "Julia", "Juliana", "Karina", "Kathleen",
            "Laura", "Lilian", "Lily", "Linda", "Lisa", "Lilita", "Lora", "Lorna", "Lucy", "Lydia",
            "Mabel", "Madeline", "Maggie", "Maria", "Mariam", "Marilyn", "Mary", "Matilda", "Mavis",
            "Melanie", "Melinda", "Melody", "Michelle", "Mildred", "Molly", "Mona", "Monica",
            "Nancy", "Nora", "Norma", "Olga", "Pamela", "Patricia", "Paula", "Pauline", "Pearl",
            "Peggy", "Penny", "Phoebe", "Phyllis", "Polly", "Priscilla", "Rachel", "Rebecca",
            "Rita", "Rosa", "Rosalind", "Rose", "Rosemary", "Rowena", "Ruby", "Sally", "Samantha",
            "Sarah", "Selina", "Sharon", "Sheila", "Shirley", "Sonya", "Stella", "Sue", "Susan",
            "Sylvia", "Tina", "Tracy", "Ursula", "Valentine", "Valerie", "Vanessa", "Veronica",
            "Victoria", "Violet", "Vivian", "Wendy", "Winnie", "Yvonne", "Aaron", "Abraham", "Adam",
            "Adrain", "Alain", "Alan", "Alban", "Albert", "Alec", "Alexander", "Alfonso", "Alfred",
            "Allan", "Allen", "Alonso", "Aloysius", "Alphonso", "Alvin", "Andrew", "Andy", "Amadeus",
            "Amselm", "Anthony", "Arnold", "Augusta", "Austin", "Barnaby", "Benedict", "Benjamin",
            "Bertie", "Bertram", "Bill", "Bob", "Boris", "Brady", "Brian", "Bruce", "Burt", "Byron",
            "Calvin", "Carl", "Carter", "Casey", "Cecil", "Charles", "Christian", "Christopher",
            "Clarence", "Clement", "Colin", "Conan", "Dalton", "Damian", "Daniel", "David", "Denis",
            "Derek", "Desmond", "Dick", "Dominic", "Donald", "Douglas", "Duncan", "Edmund",
            "Edward", "Ellen", "Elton", "Elvis", "Eric", "Eugene", "Felix", "Francis", "Frank",
            "Frederick", "Gary", "Geoffrey", "George", "Gerald", "Gerry", "Gordon", "Hamish",
            "Hardy", "Harold", "Harry", "Henry", "Herbert", "Ignatius", "Jack", "James", "Jeffrey",
            "Jim", "Joe", "John", "Joseph", "Karl", "Keith", "Kenneth", "Kevin", "Larry", "Lawrence",
            "Leonard", "Lionel", "Louis", "Lucas", "Malcolm", "Mark", "Martin", "Mathew", "Maurice",
            "Max", "Melvin", "Michael", "Milton", "Morgan", "Morris", "Murphy", "Neville",
            "Nicholas", "Noel", "Norman", "Oliver", "Oscar", "Patrick", "Paul", "Perkin", "Peter",
            "Philip", "Ralph", "Randy", "Raymond", "Richard", "Ricky", "Robert", "Robin", "Rodney",
            "Roger", "Roland", "Ronald", "Roy", "Sam", "Sebastian", "Simon", "Stanley", "Stephen",
            "Stuart", "Terence", "Thomas", "Tim", "Tom", "Tony", "Victor", "Vincent", "Wallace",
            "Walter", "Wilfred", "William", "Winston"
    };

    private final String instructions = "BOOSTER DRAFT MODE INSTRUCTIONS"
            + "\r\n\r\n"
            + "In a booster draft, several players (usually eight) are seated "
            + "around a table and each player is given three booster packs."
            + "\r\n\r\n"
            + "Each player opens a pack, selects a card from it and passes the remaining "
            + "cards to his or her left. Each player then selects one of the 14 remaining "
            + "cards from the pack that was just passed to him or her, and passes the "
            + "remaining cards to the left again. This continues until all of the cards "
            + "are depleted. The process is repeated with the second and third packs, "
            + "except that the cards are passed to the right in the second pack."
            + "\r\n\r\n"
            + "Players then build decks out of any of the cards that they selected "
            + "during the drafting and add as many basic lands as they want."
            + "\r\n\r\n"
            + "(Credit: Wikipedia <http://en.wikipedia.org/wiki/Magic:_The_Gathering_formats#Booster_Draft>)";

    /**
     * Assembles swing components for "Draft" mode menu.
     * @param v0 (HomeTopLevel, parent view)
     */
    public ViewDraft(HomeTopLevel v0) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0"));
        parentView = v0;
        skin = Singletons.getView().getSkin();

        JLabel lblHuman = new JLabel("Select your deck: ");
        lblHuman.setFont(skin.getBoldFont(16));
        lblHuman.setHorizontalAlignment(SwingConstants.CENTER);
        lblHuman.setForeground(skin.getColor("text"));
        this.add(lblHuman, "w 60%!, gap 5% 5% 2% 2%");

        JLabel lblAI = new JLabel("Who will you play?");
        lblAI.setFont(skin.getBoldFont(16));
        lblAI.setHorizontalAlignment(SwingConstants.CENTER);
        lblAI.setForeground(skin.getColor("text"));
        this.add(lblAI, "w 25%!, gap 0 0 2% 2%, wrap");

        Random generator = new Random();
        int i = opponentNames.length;
        String[] ai = {
                opponentNames[generator.nextInt(i)],
                opponentNames[generator.nextInt(i)],
                opponentNames[generator.nextInt(i)],
                opponentNames[generator.nextInt(i)],
                opponentNames[generator.nextInt(i)],
                opponentNames[generator.nextInt(i)],
                opponentNames[generator.nextInt(i)]
        };

        // Define exit command for deck editor (TODO put this in controller)
        final Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            @Override
            public void execute() {
                control.updateHumanDecks();
            }
        };

        // Human deck list area, ai names
        Collection<Deck[]> temp = AllZone.getDeckManager().getDraftDecks().values();
        List<Deck> human = new ArrayList<Deck>();
        for (Deck[] d : temp) { human.add(d[0]); }
        lstHumanDecks = new DeckLister(GameType.Draft, exit);
        lstHumanDecks.setDecks(human.toArray(new Deck[0]));
        lstAI = new JList(ai);
        this.add(new FScrollPane(lstHumanDecks), "w 60%!, h 30%!, gap 5% 5% 2% 2%");
        this.add(new FScrollPane(lstAI), "w 25%!, h 37%!, gap 0 0 2% 0, span 1 2, wrap");

        lstAI.setSelectedIndex(0);

        SubButton buildHuman = new SubButton("Build A New Deck");
        buildHuman.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { control.setupDraft(); }
        });
        this.add(buildHuman, "w 60%!, h 5%!, gap 5% 5% 0 0, wrap");

        // Directions
        tpnDirections = new JTextPane();
        tpnDirections.setOpaque(false);
        tpnDirections.setForeground(skin.getColor("text"));
        tpnDirections.setFont(skin.getFont(15));
        tpnDirections.setAlignmentX(SwingConstants.CENTER);
        tpnDirections.setFocusable(false);
        tpnDirections.setEditable(false);
        tpnDirections.setBorder(null);
        tpnDirections.setText(instructions);

        StyledDocument doc = tpnDirections.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        // Start button
        StartButton btnStart = new StartButton(parentView);
        btnStart.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { control.start(); }
        });

        final JPanel pnlButtonContainer = new JPanel();
        pnlButtonContainer.setOpaque(false);
        this.add(pnlButtonContainer, "w 100%!, span 2 1, wrap");

        pnlButtonContainer.setLayout(new BorderLayout());
        pnlButtonContainer.add(btnStart, SwingConstants.CENTER);

        lblDirections = new JLabel("Click For Directions");
        lblDirections.setFont(skin.getFont(16));
        lblDirections.setHorizontalAlignment(SwingConstants.CENTER);
        lblDirections.setForeground(skin.getColor("text"));
        this.add(lblDirections, "alignx center, span 2 1, gaptop 5%");

        control = new ControlDraft(this);
    }

    /** */
    public void showDirections() {
        final FOverlay overlay = AllZone.getOverlay();
        final FButton btnClose = new FButton();
        final FPanel pnlContainer = new FPanel();

        btnClose.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                overlay.hideOverlay();
            }
        });
        btnClose.setText("Close");

        pnlContainer.setBorder(new LineBorder(skin.getColor("border"), 1));
        pnlContainer.setBGTexture(new ImageIcon(skin.getImage("bg.texture")));
        pnlContainer.setLayout(new MigLayout("insets 0, wrap"));
        pnlContainer.add(tpnDirections, "w 90%, gap 5% 0 20px 0, wrap");
        pnlContainer.add(btnClose, "w 300px!, h 40px!, gap 0 0 20px 20px, alignx center");

        overlay.removeAll();
        overlay.setLayout(new MigLayout("insets 0"));
        overlay.add(pnlContainer, "w 50%, gap 25% 0 5% 5%, wrap");
        overlay.showOverlay();
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
       return lstAI;
    }

    /** @return ControlDraft */
    public ControlDraft getController() {
        return control;
    }

    /** @return JTextArea */
    public JLabel getLblDirections() {
        return lblDirections;
    }
}
