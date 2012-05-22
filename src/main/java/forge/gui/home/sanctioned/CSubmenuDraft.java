package forge.gui.home.sanctioned;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameNew;
import forge.game.GameType;
import forge.game.limited.BoosterDraft;
import forge.game.limited.CardPoolLimitation;
import forge.gui.GuiUtils;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorDraftingProcess;
import forge.gui.framework.ICDoc;
import forge.gui.home.ICSubmenu;
import forge.gui.toolbox.FSkin;

/** 
 * Controls the draft submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuDraft implements ICSubmenu, ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final String[] opponentNames = new String[] {
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

    private final Command cmdDeckSelect = new Command() {
        @Override
        public void execute() {
            VSubmenuDraft.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
        }
    };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;

        view.populate();
        CSubmenuDraft.SINGLETON_INSTANCE.update();

        view.getLstHumanDecks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildDeck().addMouseListener(
                new MouseAdapter() { @Override
                    public void mousePressed(MouseEvent e) { setupDraft(); } });

        view.getBtnStart().addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(final MouseEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                startGame();
                            }
                        });
                    }
                });

        view.getBtnDirections().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                view.showDirections();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                view.getBtnDirections().setForeground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                view.getBtnDirections().setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        VSubmenuDraft.SINGLETON_INSTANCE.getLstAIDecks().setListData(generateNames());
        VSubmenuDraft.SINGLETON_INSTANCE.getLstAIDecks().setSelectedIndex(
                (int) Math.floor(Math.random() * VSubmenuDraft.SINGLETON_INSTANCE.getLstAIDecks().getModel().getSize()));

        List<Deck> human = new ArrayList<Deck>();
        for (DeckGroup d : Singletons.getModel().getDecks().getDraft()) {
            human.add(d.getHumanDeck());
        }

        VSubmenuDraft.SINGLETON_INSTANCE.getLstHumanDecks().setDecks(human);

        if (human.size() > 1) {
            VSubmenuDraft.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
        }
    }

    private void startGame() {
        final Deck human = VSubmenuDraft.SINGLETON_INSTANCE.getLstHumanDecks().getSelectedDeck();
        final int aiIndex = VSubmenuDraft.SINGLETON_INSTANCE.getLstAIDecks().getSelectedIndex();

        if (human == null) {
            JOptionPane.showMessageDialog(null,
                    "No deck selected for human!\r\n(You may need to build a new deck.)",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if (human.getMain().countAll() < 40) {
            JOptionPane.showMessageDialog(null,
                    "The selected deck doesn't have enough cards to play (minimum 40)."
                    + "\r\nUse the deck editor to choose the cards you want before starting.",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        final SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
            @Override
            public Object doInBackground() {
                DeckGroup opponentDecks = Singletons.getModel().getDecks().getDraft().get(human.getName());

                Constant.Runtime.HUMAN_DECK[0] = human;
                Constant.Runtime.COMPUTER_DECK[0] = opponentDecks.getAiDecks().get(aiIndex); //zero is human deck, so it must be +1

                if (Constant.Runtime.COMPUTER_DECK[0] == null) {
                    throw new IllegalStateException("Draft: Computer deck is null!");
                }

                Constant.Runtime.setGameType(GameType.Draft);
                GameNew.newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);
                return null;
            }

            @Override
            public void done() {
                SOverlayUtils.hideOverlay();
            }
        };
        worker.execute();
    }

    /** */
    private void setupDraft() {
        final CEditorDraftingProcess draft = new CEditorDraftingProcess();

        // Determine what kind of booster draft to run
        final ArrayList<String> draftTypes = new ArrayList<String>();
        draftTypes.add("Full Cardpool");
        draftTypes.add("Block / Set");
        draftTypes.add("Custom");

        final String prompt = "Choose Draft Format:";
        final Object o = GuiUtils.chooseOne(prompt, draftTypes.toArray());

        if (o.toString().equals(draftTypes.get(0))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Full));
        }

        else if (o.toString().equals(draftTypes.get(1))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Block));
        }

        else if (o.toString().equals(draftTypes.get(2))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Custom));
        }

        CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(draft);
        FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_LIMITED);
    }

    private String[] generateNames() {
        // Generate random selection of names for display
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

        return ai;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
