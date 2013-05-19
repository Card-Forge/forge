package forge.gui.home.variant;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.SwingUtilities;
import com.google.common.collect.Iterables;

import forge.Command;
import forge.FThreads;
import forge.Singletons;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.game.PlayerStartConditions;
import forge.game.player.LobbyPlayer;
import forge.gui.GuiDialog;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FDeckChooser;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/** 
 * Controls the constructed submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuVanguard implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    private final VSubmenuVanguard view = VSubmenuVanguard.SINGLETON_INSTANCE;


    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getBtnStart().requestFocusInWindow(); }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        for (FDeckChooser fdc : view.getDeckChoosers()) {
            fdc.initialize();
        }

        // Checkbox event handling
        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                startGame();
            }
        });

        // Checkbox event handling
        view.getCbSingletons().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(FPref.DECKGEN_SINGLETONS,
                        String.valueOf(view.getCbSingletons().isSelected()));
                prefs.save();
            }
        });

        view.getCbArtifacts().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(
                        FPref.DECKGEN_ARTIFACTS, String.valueOf(view.getCbArtifacts().isSelected()));
                prefs.save();
            }
        });

        view.getCbRemoveSmall().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(
                        FPref.DECKGEN_NOSMALL, String.valueOf(view.getCbRemoveSmall().isSelected()));
                prefs.save();
            }
        });

        // Pre-select checkboxes
        view.getCbSingletons().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        view.getCbArtifacts().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        view.getCbRemoveSmall().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_NOSMALL));
    }


    /** @param lists0 &emsp; {@link java.util.List}<{@link javax.swing.JList}> */
    private void startGame() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });


        Random rnd = new Random();
        String nl = System.getProperty("line.separator");
        boolean usedDefaults = false;
        StringBuilder defaultAvatarInfo = new StringBuilder("The following decks will use a default avatar:" + nl);

        List<Deck> playerDecks = new ArrayList<Deck>();
        for (int i = 0; i < view.getNumPlayers(); i++) {
            PlayerStartConditions d = view.getDeckChoosers().get(i).getDeck();

            if (d == null) {
                //ERROR!
                GuiDialog.message("No deck selected for player " + (i + 1));
                return;
            }
            playerDecks.add(d.getOriginalDeck());
        }

        List<CardPrinted> playerAvatars = new ArrayList<CardPrinted>();
        for (int i = 0; i < view.getNumPlayers(); i++) {
            CardPrinted avatar = null;
            Object obj = view.getAvatarLists().get(i).getSelectedValue();

            boolean useDefault = VSubmenuVanguard.SINGLETON_INSTANCE.getCbDefaultAvatars().isSelected();
            useDefault &= playerDecks.get(i).get(DeckSection.Avatar) != null;

            if (useDefault) {
                avatar = playerDecks.get(i).get(DeckSection.Avatar).get(0);
                defaultAvatarInfo.append("Player " + (i + 1) + ": ");
                defaultAvatarInfo.append(avatar.getName() + nl);
                usedDefaults = true;
            } else {

                if (obj instanceof String) {
                    //Random is the only string in the list so grab a random avatar.
                    if (i == 0)  {
                        //HUMAN
                        avatar = Iterables.get(view.getAllAvatars(), rnd.nextInt(Iterables.size(view.getNonRandomHumanAvatars())));
                    } else {
                        //AI
                        avatar = Iterables.get(view.getAllAiAvatars(), rnd.nextInt(Iterables.size(view.getNonRandomAiAvatars())));
                    }
                } else {
                    avatar = (CardPrinted) obj;
                }
            }
            if (avatar == null) {
                //ERROR!
                GuiDialog.message("No avatar selected for player " + (i + 1));
                return;
            }
            playerAvatars.add(avatar);
        }

        if (usedDefaults) {
            GuiDialog.message(defaultAvatarInfo.toString());
        }

        Lobby lobby = Singletons.getControl().getLobby();
        MatchStartHelper helper = new MatchStartHelper();
        for (int i = 0; i < view.getNumPlayers(); i++) {
            LobbyPlayer player = i == 0 ? lobby.getGuiPlayer() : lobby.getAiPlayer();

            helper.addVanguardPlayer(player, playerDecks.get(i), playerAvatars.get(i));
        }
        final MatchController mc = new MatchController(GameType.Vanguard, helper.getPlayerMap());
        FThreads.invokeInEdtLater(new Runnable(){
            @Override
            public void run() {
                mc.startRound();
                SOverlayUtils.hideOverlay();
            }
        });
    }


    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
