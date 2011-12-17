package forge.view.home;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Command;
import forge.control.home.ControlDraft;
import forge.deck.Deck;
import forge.game.GameType;
import forge.view.toolbox.DeckLister;
import forge.view.toolbox.FSkin;

/** 
 * Assembles swing components for "Draft" mode menu.
 *
 */
@SuppressWarnings("serial")
public class ViewDraft extends JPanel {
    private FSkin skin;
    private ControlDraft control;
    private HomeTopLevel parentView;
    private JList lstAI;
    private DeckLister lstHumanDecks;
    private JTextPane tpnDirections;

    // Just for fun...maybe a waste of space :) Doublestrike 141211
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
    /**
     * Assembles swing components for "Draft" mode menu.
     * @param v0 (HomeTopLevel, parent view)
     */
    public ViewDraft(HomeTopLevel v0) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0"));
        parentView = v0;
        skin = AllZone.getSkin();

        JLabel lblHuman = new JLabel("Select your deck: ");
        lblHuman.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        lblHuman.setHorizontalAlignment(SwingConstants.CENTER);
        lblHuman.setForeground(skin.getColor("text"));
        this.add(lblHuman, "w 60%!, gap 5% 5% 2% 2%");

        JLabel lblAI = new JLabel("Who will you play?");
        lblAI.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
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
                lstHumanDecks.setSelectedIndex(0);
            }
        };

        // Human deck list area, ai names
        Collection<Deck[]> temp = AllZone.getDeckManager().getDraftDecks().values();
        List<Deck> human = new ArrayList<Deck>();
        for (Deck[] d : temp) { human.add(d[0]); }
        lstHumanDecks = new DeckLister(GameType.Draft, exit);
        lstHumanDecks.setDecks(human.toArray(new Deck[0]));
        lstAI = new JList(ai);
        this.add(new JScrollPane(lstHumanDecks), "w 60%!, h 30%!, gap 5% 5% 2% 2%");
        this.add(new JScrollPane(lstAI), "w 25%!, h 37%!, gap 0 0 2% 0, span 1 2, wrap");

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
        tpnDirections.setFont(skin.getFont1().deriveFont(Font.PLAIN, 15));
        tpnDirections.setAlignmentX(SwingConstants.CENTER);
        tpnDirections.setFocusable(false);
        tpnDirections.setEditable(false);
        tpnDirections.setBorder(null);
        tpnDirections.setText("Click here for draft mode instructions.");

        StyledDocument doc = tpnDirections.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        add(tpnDirections, "w 80%, gap 10% 0 5% 5%, span 2 1, wrap");

        // Start button
        StartButton btnStart = new StartButton(parentView);
        btnStart.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { control.start(); }
        });

        JPanel pnlButtonContainer = new JPanel();
        pnlButtonContainer.setOpaque(false);
        this.add(pnlButtonContainer, "w 100%!, span 2 1");

        pnlButtonContainer.setLayout(new BorderLayout());
        pnlButtonContainer.add(btnStart, SwingConstants.CENTER);

        control = new ControlDraft(this);
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
    public JTextPane getTpnDirections() {
        return tpnDirections;
    }
}
