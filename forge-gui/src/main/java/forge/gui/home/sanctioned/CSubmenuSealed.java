package forge.gui.home.sanctioned;

import forge.UiCommand;
import forge.Singletons;
import forge.card.MagicColor;
import forge.deck.*;
import forge.game.GameType;
import forge.gui.GuiChoose;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.DeckProxy;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.gui.deckeditor.controllers.CEditorLimited;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.itemmanager.ItemManagerConfig;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.limited.DraftRankCache;
import forge.limited.LimitedPoolType;
import forge.limited.SealedCardPoolGenerator;
import forge.limited.SealedDeckBuilder;
import forge.properties.ForgePreferences.FPref;
import forge.util.MyRandom;
import forge.util.storage.IStorage;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    private final UiCommand cmdDeckSelect = new UiCommand() {
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
        final VSubmenuSealed view = VSubmenuSealed.SINGLETON_INSTANCE;

        view.getLstDecks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildDeck().setCommand(new UiCommand() {
            @Override
            public void run() {
                setupSealed();
            }
        });

        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                startGame(GameType.Sealed);
            }
        });

        view.getBtnDirections().setCommand(new UiCommand() {
            @Override
            public void run() {
                view.showDirections();
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
        view.getLstDecks().setPool(DeckProxy.getAllSealedDecks(Singletons.getModel().getDecks().getSealed()));
        view.getLstDecks().setup(ItemManagerConfig.SEALED_DECKS);

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
        final DeckProxy human = VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().getSelectedItem();

        if (human == null) {
            FOptionPane.showErrorDialog("Please build and/or select a deck for yourself.", "No Deck");
            return;
        }

        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            String errorMessage = gameType.getDecksFormat().getDeckConformanceProblem(human.getDeck());
            if (null != errorMessage) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage + " Please edit or choose a different deck.", "Invalid Deck");
                return;
            }
        }

        int matches = Singletons.getModel().getDecks().getSealed().get(human.getName()).getAiDecks().size();
        Singletons.getModel().getGauntletMini().launch(matches, human.getDeck(), gameType);
    }

    @SuppressWarnings("unchecked")
    private <T extends DeckBase> void setupSealed() {
        final String prompt = "Choose Sealed Deck Format";
        final LimitedPoolType poolType = GuiChoose.oneOrNone(prompt, LimitedPoolType.values());
        if (poolType == null) { return; }

        SealedCardPoolGenerator sd = new SealedCardPoolGenerator(poolType);
        if (sd.isEmpty()) { return; }

        final CardPool humanPool = sd.getCardPool(true);
        if (humanPool == null) { return; }

        // System.out.println(humanPool);

        // This seems to be limited by the MAX_DRAFT_PLAYERS constant
        // in DeckGroupSerializer.java. You could create more AI decks
        // but only the first seven would load. --BBU
        Integer rounds = GuiChoose.getInteger("How many opponents are you willing to face?", 1, 7);
        if (rounds == null) { return; }

        final String sDeckName = FOptionPane.showInputDialog(
                "Save this card pool as:",
                "Save Card Pool",
                FOptionPane.QUESTION_ICON);

        if (StringUtils.isBlank(sDeckName)) {
            return;
        }

        final Deck deck = new Deck(sDeckName);
        deck.getOrCreate(DeckSection.Sideboard).addAll(humanPool);

        final int landsCount = 10;

        final boolean isZendikarSet = sd.getLandSetCode().equals("ZEN"); // we want to generate one kind of Zendikar lands at a time only
        final boolean zendikarSetMode = MyRandom.getRandom().nextBoolean();

        for (final String element : MagicColor.Constant.BASIC_LANDS) {
            int numArt = Singletons.getMagicDb().getCommonCards().getArtCount(element, sd.getLandSetCode());
            int minArtIndex = isZendikarSet ? (zendikarSetMode ? 1 : 5) : 1;
            int maxArtIndex = isZendikarSet ? minArtIndex + 3 : numArt;
            
            if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_ART_IN_POOLS)) {

                for (int i = minArtIndex; i <= maxArtIndex; i++) {
                    deck.get(DeckSection.Sideboard).add(element, sd.getLandSetCode(), i, numArt > 1 ? landsCount : 30);
                }
            } else {
                deck.get(DeckSection.Sideboard).add(element, sd.getLandSetCode(), 30);
            }
        }

        final IStorage<DeckGroup> sealedDecks = Singletons.getModel().getDecks().getSealed();

        if (sealedDecks.contains(sDeckName)) {
            if (!FOptionPane.showConfirmDialog(
                    "'" + sDeckName + "' already exists. Do you want to replace it?",
                    "Sealed Deck Game Exists")) {
                return;
            }
            sealedDecks.delete(sDeckName);
        }

        final DeckGroup sealed = new DeckGroup(sDeckName);
        sealed.setHumanDeck(deck);
        for (int i = 0; i < rounds; i++) {
            // Generate other decks for next N opponents
            final CardPool aiPool = sd.getCardPool(false);
            if (aiPool == null) { return; }

            sealed.addAiDeck(new SealedDeckBuilder(aiPool.toFlatList()).buildDeck());
        }

        // Rank the AI decks
        sealed.rankAiDecks(new DeckComparer());

        Singletons.getModel().getDecks().getSealed().add(sealed);

        final ACEditorBase<? extends InventoryItem, T> editor = (ACEditorBase<? extends InventoryItem, T>) new CEditorLimited(
                Singletons.getModel().getDecks().getSealed(), FScreen.DECK_EDITOR_SEALED);

        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_SEALED);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editor);
        editor.getDeckController().setModel((T) sealed);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    static class DeckComparer implements java.util.Comparator<Deck> {
        public double getDraftValue(Deck d) {
            double value = 0;
            double divider = 0;

            if (d.getMain().isEmpty()) {
                return 0;
            }

            double best = 1.0;

            for (Entry<PaperCard, Integer> kv : d.getMain()) {
                PaperCard evalCard = kv.getKey();
                int count = kv.getValue();
                if (DraftRankCache.getRanking(evalCard.getName(), evalCard.getEdition()) != null) {
                    double add = DraftRankCache.getRanking(evalCard.getName(), evalCard.getEdition());
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
