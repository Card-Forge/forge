package forge.gui.home.sanctioned;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.game.limited.LimitedPoolType;
import forge.game.limited.ReadDraftRankings;
import forge.game.limited.SealedDeckBuilder;
import forge.game.limited.SealedCardPoolGenerator;
import forge.gui.GuiChoose;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.gui.deckeditor.controllers.CEditorLimited;
import forge.gui.framework.ICDoc;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.util.storage.IStorage;

/** 
 * Controls the sealed submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuSealed implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private Map<String, Deck> aiDecks;

    private final Command cmdDeckSelect = new Command() {
        @Override
        public void run() {
            VSubmenuSealed.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
        }
    };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().setSelectCommand(cmdDeckSelect);

        VSubmenuSealed.SINGLETON_INSTANCE.getBtnBuildDeck().addMouseListener(
                new MouseAdapter() { @Override
                    public void mousePressed(final MouseEvent e) { setupSealed(); } });

        VSubmenuSealed.SINGLETON_INSTANCE.getBtnStart().addMouseListener(
            new MouseAdapter() {
                @Override
                public void mouseReleased(final MouseEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            startGame(GameType.Sealed);
                        }
                    });
                }
            });

        VSubmenuSealed.SINGLETON_INSTANCE.getBtnDirections().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                VSubmenuSealed.SINGLETON_INSTANCE.showDirections();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final List<Deck> humanDecks = new ArrayList<Deck>();
        aiDecks = new HashMap<String, Deck>();

        // Since AI decks are tied directly to the human choice,
        // they're just mapped in a parallel map and grabbed when the game starts.
        for (final DeckGroup d : Singletons.getModel().getDecks().getSealed()) {
            aiDecks.put(d.getName(), d.getAiDecks().get(0));
            humanDecks.add(d.getHumanDeck());
        }

        final VSubmenuSealed view = VSubmenuSealed.SINGLETON_INSTANCE;
        view.getLstDecks().setDecks(humanDecks);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JButton btnStart = view.getBtnStart();
                if (btnStart.isEnabled()) {
                    view.getBtnStart().requestFocusInWindow();
                } else {
                    view.getBtnBuildDeck().requestFocusInWindow();
                }
            }
        });
    }

    private void startGame(final GameType gameType) {
        final Deck human = VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().getSelectedDeck();

        if (human == null) {
            JOptionPane.showMessageDialog(null,
                    "Please build and/or select a deck for yourself.",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        } 
        
        String errorMessage = gameType.getDecksFormat().getDeckConformanceProblem(human);
        if (null != errorMessage) {
            JOptionPane.showMessageDialog(null, "Your deck " + errorMessage +  " Please edit or choose a different deck.", "Invalid deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int matches = Singletons.getModel().getDecks().getSealed().get(human.getName()).getAiDecks().size();
        Singletons.getModel().getGauntletMini().launch(matches, human, gameType);
    }

    @SuppressWarnings("unchecked")
    private <T extends DeckBase> void setupSealed() {


        final String prompt = "Choose Sealed Deck Format:";
        final LimitedPoolType o = GuiChoose.oneOrNone(prompt, LimitedPoolType.values());
        if ( o == null ) return;
        
        SealedCardPoolGenerator sd = new SealedCardPoolGenerator(o);
        if (sd.isEmpty()) return;

        final ItemPool<CardPrinted> humanPool = sd.getCardpool(true);
        
        // System.out.println(humanPool);

        // This seems to be limited by the MAX_DRAFT_PLAYERS constant
        // in DeckGroupSerializer.java. You could create more AI decks
        // but only the first seven would load. --BBU
        final Integer[] integers = {1,2,3,4,5,6,7};
        Integer rounds = GuiChoose.oneOrNone("How many opponents are you willing to face?", integers);
        if ( null == rounds ) return;


        final String sDeckName = JOptionPane.showInputDialog(null,
                "Save this card pool as:",
                "Save Card Pool",
                JOptionPane.QUESTION_MESSAGE);

        if (StringUtils.isBlank(sDeckName)) {
            return;
        }


        final Deck deck = new Deck(sDeckName);
        deck.getOrCreate(DeckSection.Sideboard).addAll(humanPool);

        for (final String element : Constant.Color.BASIC_LANDS) {
            deck.get(DeckSection.Sideboard).add(element, sd.getLandSetCode(), 18);
        }

        final IStorage<DeckGroup> sealedDecks = Singletons.getModel().getDecks().getSealed();

        if (!(sealedDecks.isUnique(sDeckName))) {
            final int deleteDeck = JOptionPane.showConfirmDialog(null, "\"" + sDeckName
                    + "\" already exists! Do you want to replace it?",
                    "Sealed Deck Game Exists", JOptionPane.YES_NO_OPTION);

            if (deleteDeck == JOptionPane.NO_OPTION) {
                return;
            }
            sealedDecks.delete(sDeckName);
        }

        final DeckGroup sealed = new DeckGroup(sDeckName);
        sealed.setHumanDeck(deck);
        for (int i = 0; i < rounds; i++) {
            // Generate other decks for next N opponents
            sealed.addAiDeck(new SealedDeckBuilder(sd.getCardpool(false).toFlatList()).buildDeck());
        }

        // Rank the AI decks
        sealed.rankAiDecks(new DeckComparer());

        Singletons.getModel().getDecks().getSealed().add(sealed);

        final ACEditorBase<? extends InventoryItem, T> editor = (ACEditorBase<? extends InventoryItem, T>) new CEditorLimited(
                Singletons.getModel().getDecks().getSealed());

        FControl.SINGLETON_INSTANCE.changeState(FControl.Screens.DECK_EDITOR_LIMITED);
        CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(editor);
        editor.getDeckController().setModel((T) sealed);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
    
    static class DeckComparer implements java.util.Comparator<Deck> {
        ReadDraftRankings ranker = new ReadDraftRankings();

        public double getDraftValue(Deck d) {
            double value = 0;
            double divider = 0;



            if (d.getMain().isEmpty()) {
                return 0;
            }

            double best = 1.0;

            for (Entry<CardPrinted, Integer> kv : d.getMain()) {
                CardPrinted evalCard = kv.getKey();
                int count = kv.getValue();
                if (ranker.getRanking(evalCard.getName(), evalCard.getEdition()) != null) {
                    double add = ranker.getRanking(evalCard.getName(), evalCard.getEdition());
                    // System.out.println(evalCard.getName() + " is worth " + add);
                    value += add * count;
                    divider += count;
                    if (best > add) {
                        best = add;
                    }
                }
            }

            if (divider == 0 || value == 0) {
                return 0;
            }

            value /= divider;

            return (20.0 / (best + (2 * value)));
        }

        
        @Override
        public int compare(Deck o1, Deck o2) {
            double delta = getDraftValue(o1) - getDraftValue(o2);
            if ( delta > 0 ) return 1;
            if ( delta < 0 ) return -1;
            return 0;
        }
        
    }
}
