package forge.gui.home.limited;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.slightlymagic.braids.util.UtilFunctions;

import org.apache.commons.lang3.StringUtils;

import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameNew;
import forge.game.GameType;
import forge.game.limited.SealedDeck;
import forge.gui.GuiUtils;
import forge.gui.home.ICSubmenu;
import forge.gui.home.utilities.CSubmenuDeckEditor;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuSealed implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    private Map<String, Deck> aiDecks;

    private final Command cmdDeckExit = new Command() {
        @Override
        public void execute() {
            update();
            GuiUtils.closeOverlay();
        }
    };

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
        final VSubmenuSealed view = VSubmenuSealed.SINGLETON_INSTANCE;

        view.populate();
        CSubmenuSealed.SINGLETON_INSTANCE.update();

        VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().setExitCommand(cmdDeckExit);
        VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().setSelectCommand(cmdDeckSelect);

        VSubmenuSealed.SINGLETON_INSTANCE.getBtnBuildDeck().addMouseListener(
                new MouseAdapter() { @Override
                    public void mousePressed(MouseEvent e) { setupSealed(); } });

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
            public void mouseClicked(MouseEvent e) {
                VSubmenuSealed.SINGLETON_INSTANCE.showDirections();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                VSubmenuSealed.SINGLETON_INSTANCE.getBtnDirections().setForeground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                VSubmenuSealed.SINGLETON_INSTANCE.getBtnDirections().setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
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
        List<Deck> humanDecks = new ArrayList<Deck>();
        aiDecks = new HashMap<String, Deck>();

        // Since AI decks are tied directly to the human choice,
        // they're just mapped in a parallel map and grabbed when the game starts.
        for (DeckGroup d : Singletons.getModel().getDecks().getSealed()) {
            aiDecks.put(d.getName(), d.getAiDecks().get(0));
            humanDecks.add(d.getHumanDeck());
        }

        VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().setDecks(humanDecks);
    }

    private void startGame() {
        Deck human = VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().getSelectedDeck();

        if (human == null) {
            JOptionPane.showMessageDialog(null,
                    "Please build and/or select a deck for yourself.",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        GuiUtils.startGameOverlay().showOverlay();

        Constant.Runtime.HUMAN_DECK[0] = human;
        Constant.Runtime.COMPUTER_DECK[0] = Singletons.getModel().getDecks().getSealed().get(human.getName()).getAiDecks().get(0);

        GameNew.newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);
    }

    /** */
    private void setupSealed() {
        ArrayList<String> sealedTypes = new ArrayList<String>();
        sealedTypes.add("Full Cardpool");
        sealedTypes.add("Block / Set");
        sealedTypes.add("Custom");

        final String prompt = "Choose Sealed Deck Format:";
        final Object o = GuiUtils.chooseOne(prompt, sealedTypes.toArray());

        SealedDeck sd = null;

        if (o.toString().equals(sealedTypes.get(0))) {
            sd = new SealedDeck("Full");
        }

        else if (o.toString().equals(sealedTypes.get(1))) {
            sd = new SealedDeck("Block");
        }

        else if (o.toString().equals(sealedTypes.get(2))) {
            sd = new SealedDeck("Custom");
        }
        else {
            throw new IllegalStateException("choice <<" + UtilFunctions.safeToString(o)
                    + ">> does not equal any of the sealedTypes.");
        }

        if (sd.getCardpool().isEmpty()) {
            return;
        }

        final String sDeckName = JOptionPane.showInputDialog(null,
                ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.SAVE_SEALED_MSG),
                ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.SAVE_SEALED_TTL),
                JOptionPane.QUESTION_MESSAGE);

        if (StringUtils.isBlank(sDeckName)) {
            return;
        }

        // May check for name uniqueness here

        final ItemPool<CardPrinted> sDeck = sd.getCardpool();

        Deck deck = new Deck(sDeckName);
        deck.getSideboard().addAll(sDeck);

        for (final String element : Constant.Color.BASIC_LANDS) {
            deck.getSideboard().add(element, sd.getLandSetCode()[0], 18);
        }

        DeckGroup sealed = new DeckGroup(sDeckName);
        sealed.setHumanDeck(deck);
        sealed.addAiDeck(sd.buildAIDeck(sDeck.toForgeCardList()));
        Singletons.getModel().getDecks().getSealed().add(sealed);

        CSubmenuDeckEditor.SINGLETON_INSTANCE.showDeckEditor(GameType.Sealed, sealed);
    }
}
