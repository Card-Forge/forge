package forge.gui.home.sanctioned;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.game.limited.SealedDeck;
import forge.game.limited.SealedDeckFormat;
import forge.gui.GuiChoose;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.gui.deckeditor.controllers.CEditorLimited;
import forge.gui.framework.ICDoc;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.IStorage;
import forge.util.TextUtil;

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
        public void execute() {
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
                                startGame();
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

        VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().setDecks(humanDecks);
    }

    private void startGame() {
        final Deck human = VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().getSelectedDeck();

        if (human == null) {
            JOptionPane.showMessageDialog(null,
                    "Please build and/or select a deck for yourself.",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (!human.meetsGameTypeRequirements(GameType.Sealed)) {
            JOptionPane.showMessageDialog(null,
                    "The selected deck doesn't have enough cards to play (minimum 40)."
                    + "\r\nUse the deck editor to choose the cards you want before starting.",
                    "Invalid deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int matches = Singletons.getModel().getDecks().getSealed().get(human.getName()).getAiDecks().size();

        AllZone.getGauntlet().launch(matches, human, GameType.Sealed);
    }

    /** */
    @SuppressWarnings("unchecked")
    private <T extends DeckBase> void setupSealed() {

        final List<String> sealedTypes = new ArrayList<String>();
        sealedTypes.add("Full Cardpool");
        sealedTypes.add("Block / Set");
        sealedTypes.add("Fantasy Block");
        sealedTypes.add("Custom");

        final String prompt = "Choose Sealed Deck Format:";
        final Object o = GuiChoose.one(prompt, sealedTypes);

        SealedDeckFormat sd = null;

        if (o.toString().equals(sealedTypes.get(0))) {
            sd = new SealedDeckFormat("Full");
        }

        else if (o.toString().equals(sealedTypes.get(1))) {
            sd = new SealedDeckFormat("Block");
        }

        else if (o.toString().equals(sealedTypes.get(2))) {
            sd = new SealedDeckFormat("FBlock");
        }

        else if (o.toString().equals(sealedTypes.get(3))) {
            sd = new SealedDeckFormat("Custom");
        }
        else {
            throw new IllegalStateException("choice <<" + TextUtil.safeToString(o)
                    + ">> does not equal any of the sealedTypes.");
        }

        if (sd.getCardpool(false).isEmpty()) {
            return;
        }

        // This seems to be limited by the MAX_DRAFT_PLAYERS constant
        // in DeckGroupSerializer.java. You could create more AI decks
        // but only the first seven would load. --BBU
        final Integer[] integers = new Integer[7];

        for (int i = 0; i <= 6; i++) {
            integers[i] = Integer.valueOf(i + 1);
        }

        Integer rounds = GuiChoose.one("How many matches?", integers);

        // System.out.println("You selected " + rounds + " rounds.");

        final String sDeckName = JOptionPane.showInputDialog(null,
                ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.SAVE_SEALED_MSG),
                ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.SAVE_SEALED_TTL),
                JOptionPane.QUESTION_MESSAGE);

        if (StringUtils.isBlank(sDeckName)) {
            return;
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

        final ItemPool<CardPrinted> sDeck = sd.getCardpool(true);
        ItemPool<CardPrinted> aiDecks = sd.getCardpool(false);

        final Deck deck = new Deck(sDeckName);
        deck.getSideboard().addAll(sDeck);

        for (final String element : Constant.Color.BASIC_LANDS) {
            deck.getSideboard().add(element, sd.getLandSetCode()[0], 18);
        }

        final DeckGroup sealed = new DeckGroup(sDeckName);
        sealed.setHumanDeck(deck);
        for (int i = 0; i < rounds; i++) {
            if (i > 0) {
                // Re-randomize for AI decks beyond the first...
                aiDecks = sd.getCardpool(false);
            }
            sealed.addAiDeck(new SealedDeck(aiDecks.toFlatList()).buildDeck());
        }

        // Rank the AI decks
        sealed.rankAiDecks();

        Singletons.getModel().getDecks().getSealed().add(sealed);

        final ACEditorBase<?, T> editor = (ACEditorBase<?, T>) new CEditorLimited(
                Singletons.getModel().getDecks().getSealed());

        CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(editor);
        FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_LIMITED);
        editor.getDeckController().setModel((T) sealed);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
