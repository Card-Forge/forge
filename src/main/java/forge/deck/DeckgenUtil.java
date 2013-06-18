package     forge.deck;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JList;
import javax.swing.JOptionPane;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Singletons;
import forge.card.CardDb;
import forge.deck.generate.Generate2ColorDeck;
import forge.deck.generate.Generate3ColorDeck;
import forge.deck.generate.Generate5ColorDeck;
import forge.deck.generate.GenerateColoredDeckBase;
import forge.deck.generate.GenerateMonoColorDeck;
import forge.deck.generate.GenerateThemeDeck;
import forge.item.PaperCard;
import forge.item.ItemPoolView;
import forge.item.PreconDeck;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestEventDuel;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.MyRandom;
import forge.util.storage.IStorage;
import forge.util.storage.IStorageView;

/** 
 * Utility collection for various types of decks.
 * - Builders (builds or retrieves deck based on a selection)
 * - Randomizers (retrieves random deck of selected type)
 * - Color checker (see javadoc)
 * - Decklist display-er
 */
// TODO This class can be used for home menu constructed deck generation as well.
public class DeckgenUtil {
    /** */
    public enum DeckTypes {
        COLORS,
        THEMES,
        CUSTOM,
        QUESTEVENTS,
        PRECON
    }

    /**
     * @param selection {@link java.lang.String} array
     * @return {@link forge.deck.Deck}
     */
    public static Deck buildColorDeck(List<String> selection, boolean forAi) {
        
        final Deck deck;
        String deckName = null;  
        
        GenerateColoredDeckBase gen = null;
        
        if (selection.size() == 1) {
            gen = new GenerateMonoColorDeck(selection.get(0));
        } else if (selection.size() == 2) {
            gen = new Generate2ColorDeck(selection.get(0), selection.get(1));
        } else if (selection.size() == 3) {
            gen = new Generate3ColorDeck(selection.get(0), selection.get(1), selection.get(2));
        } else {
            gen = new Generate5ColorDeck();
            deckName = "5 colors";
        }
        
        ItemPoolView<PaperCard> cards = gen == null ? null : gen.getDeck(60, forAi);
        
        if(null == deckName)
            deckName = Lang.joinHomogenous(Arrays.asList(selection));
        
        // After generating card lists, build deck.
        deck = new Deck("Random deck : " + deckName);
        deck.getMain().addAll(cards);

        return deck;
    }

    /**
     * @param selection {@link java.lang.String}
     * @return {@link forge.deck.Deck}
     */
    public static Deck buildThemeDeck(final String selection) {
        final GenerateThemeDeck gen = new GenerateThemeDeck();
        final Deck deck = new Deck();
        deck.getMain().addAll(gen.getThemeDeck(selection, 60));

        return deck;
    }

    /**
     * Gets a user deck.
     *
     * @param selection {java.lang.String}
     * @return {@link forge.deck.Deck}
     */
    public static Deck getConstructedDeck(final String selection) {
        return Singletons.getModel().getDecks().getConstructed().get(selection);
    }
    
    public static Deck getPreconDeck(String selection) {
        return QuestController.getPrecons().get(selection).getDeck();
    }

    public static QuestEvent getQuestEvent(final String name) {
        QuestController qCtrl = Singletons.getModel().getQuest();
        for(QuestEventChallenge challenge : qCtrl.getChallenges()) {
            if( challenge.getTitle().equals(name) )
                return challenge;
        }

        QuestEventDuel duel = Iterables.find(qCtrl.getDuelsManager().getAllDuels(), new Predicate<QuestEventDuel>() {
            @Override public boolean apply(QuestEventDuel in) { return in.getName().equals(name); } 
        });
        return duel;
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomColorDeck(boolean forAi) {
        final int[] colorCount = new int[] {1, 2, 3, 5};
        final int count = colorCount[MyRandom.getRandom().nextInt(colorCount.length)];
        final List<String> selection = new ArrayList<String>();

        // A simulated selection of "random 1" will trigger the AI selection process.
        for (int i = 0; i < count; i++) { selection.add("Random"); }

        return DeckgenUtil.buildColorDeck(selection, forAi);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomThemeDeck() {
        final List<String> themeNames = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { themeNames.add(s); }
        final int rand = (int) (Math.floor(Math.random() * themeNames.size()));
        return DeckgenUtil.buildThemeDeck(themeNames.get(rand));
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomCustomDeck() {
        final IStorage<Deck> allDecks = Singletons.getModel().getDecks().getConstructed();
        final int rand = (int) (Math.floor(Math.random() * allDecks.size()));
        final String name = allDecks.getNames().toArray(new String[0])[rand];
        return allDecks.get(name);
    }

    public static Deck getRandomPreconDeck() {
        final IStorageView<PreconDeck> allDecks = QuestController.getPrecons();
        final int rand = (int) (Math.floor(Math.random() * allDecks.size()));
        final String name = allDecks.getNames().toArray(new String[0])[rand];
        return allDecks.get(name).getDeck();
    }
    
    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomQuestDeck() {
        final List<Deck> allQuestDecks = new ArrayList<Deck>();
        QuestController qCtrl = Singletons.getModel().getQuest();


        for (final QuestEvent e : qCtrl.getDuelsManager().getAllDuels()) {
            allQuestDecks.add(e.getEventDeck());
        }

        for (final QuestEvent e : qCtrl.getChallenges()) {
            allQuestDecks.add(e.getEventDeck());
        }

        final int rand = (int) (Math.floor(Math.random() * allQuestDecks.size()));
        return allQuestDecks.get(rand);
    }

    public static int[] randomSelectColors(int maxColors) {
        int nColors = MyRandom.getRandom().nextInt(3) + 1;
        int[] result = new int[nColors];
        for(int i = 0; i < nColors; i++) {
            int next = MyRandom.getRandom().nextInt(maxColors);

            boolean isUnique = true;
            for(int j = 0; j < i; j++) {
                if(result[j] == next) {
                    isUnique = false;
                    break;
                }
            }
            if(isUnique)
                result[i] = next;
            else
                i--; // try over with this number 
        }
        return result;
    }

    /** @param lst0 {@link javax.swing.JList} */
    public static void randomSelect(final JList<String> lst0) {
        final int size = lst0.getModel().getSize();

        if (size > 0) {
            final Random r = new Random();
            final int i = r.nextInt(size);

            lst0.setSelectedIndex(i);
            lst0.ensureIndexIsVisible(lst0.getSelectedIndex());
        }
    }

    /** Shows decklist dialog for a given deck.
     * @param lst0 {@link javax.swing.JList}
     */
    public static void showDecklist(final JList<String> lst0) {
        final String deckName = lst0.getSelectedValue().toString();
        final Deck deck;

        // Retrieve from custom or quest deck maps
        if (lst0.getName().equals(DeckTypes.CUSTOM.toString())) 
            deck = Singletons.getModel().getDecks().getConstructed().get(deckName);
        else if (lst0.getName().equals(DeckTypes.PRECON.toString()))
            deck = QuestController.getPrecons().get(deckName).getDeck();
         else 
            deck = getQuestEvent(deckName).getEventDeck();

        // Dump into map and display.
        final String nl = System.getProperty("line.separator");
        final StringBuilder deckList = new StringBuilder();
        final String dName = deck.getName();
        deckList.append(dName == null ? "" : dName + nl + nl);

        int nLines = 0;
        for(DeckSection s : DeckSection.values()){
            CardPool cp = deck.get(s);
            if ( cp == null || cp.isEmpty() )
                continue;
            
            deckList.append(s.toString()).append(": ");
            if ( s.isSingleCard() ) {
                deckList.append(cp.get(0).getName()).append(nl);
                nLines++;
            } else {
                deckList.append(nl);
                nLines++;
                for (final Entry<PaperCard, Integer> ev : cp) {
                    deckList.append(ev.getValue()).append(" ").append(ev.getKey()).append(nl);
                    nLines++;
                }
            }
            deckList.append(nl);
            nLines++;
        }


        final StringBuilder msg = new StringBuilder();
        if (nLines <= 32) {
            msg.append(deckList.toString());
        } else {
            msg.append("Decklist too long for dialog." + nl + nl);
        }

        msg.append("Copy Decklist to Clipboard?");

        // Output
        final int rcMsg = JOptionPane.showConfirmDialog(null, msg, "Decklist", JOptionPane.OK_CANCEL_OPTION);
        if (rcMsg == JOptionPane.OK_OPTION) {
            final StringSelection ss = new StringSelection(deckList.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
        }
    } // End showDecklist

    /** 
     * Checks lengths of selected values for color lists
     * to see if a deck generator exists. Alert and visual reminder if fail.
     * 
     * @param colors0 String[]
     * @return boolean
     */
    public static boolean colorCheck(final List<String> colors0) {
        boolean result = true;

        if (colors0.size() == 4) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, four color generated decks aren't supported yet."
                    + "\n\rPlease use 2, 3, or 5 colors for this deck.",
                    "Generate deck: 4 colors", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        else if (colors0.size() > 5) {
            JOptionPane.showMessageDialog(null,
                    "Generate deck: maximum five colors!",
                    "Generate deck: too many colors", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        return result;
    }

    public static CardPool generateSchemeDeck() {
        CardPool schemes = new CardPool();
        List<PaperCard> allSchemes = new ArrayList<PaperCard>();
        for (PaperCard c : CardDb.variants().getAllCards()) {
            if (c.getRules().getType().isScheme()) {
                allSchemes.add(c);
            }
        }

        int schemesToAdd = 20;
        int attemptsLeft = 100; // to avoid endless loop
        while (schemesToAdd > 0 && attemptsLeft > 0) {
            PaperCard cp = Aggregates.random(allSchemes);
            int appearances = schemes.count(cp) + 1;
            if (appearances < 2) {
                schemes.add(cp);
                schemesToAdd--;
            } else {
                attemptsLeft--;
            }
        }

        return schemes;
    }
    
    public static CardPool generatePlanarDeck() {
        CardPool res = new CardPool();
        List<PaperCard> allPlanars = new ArrayList<PaperCard>();
        for (PaperCard c : CardDb.variants().getAllCards()) {
            if (c.getRules().getType().isPlane() || c.getRules().getType().isPhenomenon()) {
                allPlanars.add(c);
            }
        }

        int phenoms = 0;
        int targetsize = MyRandom.getRandom().nextInt(allPlanars.size()-10)+10;
        while(true)
        {
            PaperCard rndPlane = Aggregates.random(allPlanars);
            allPlanars.remove(rndPlane);
            
            if(rndPlane.getRules().getType().isPhenomenon() && phenoms < 2)
            {
                res.add(rndPlane);
                phenoms++;
            }
            else if (rndPlane.getRules().getType().isPlane())
            {
                res.add(rndPlane);
            }
            
            if(allPlanars.isEmpty() || res.countAll() == targetsize)
            {
                break;
            }
        }

        return res;
    }
}
