package forge.screens.home.gauntlet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import forge.GuiBase;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.match.HostedMatch;
import forge.model.FModel;
import forge.player.GamePlayerUtil;

/**
 * Controls the "gauntlet contests" submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */

public enum CSubmenuGauntletContests implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final VSubmenuGauntletContests view = VSubmenuGauntletContests.SINGLETON_INSTANCE;

    private final ActionListener actStartGame = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent arg0) {
            startGame();
        }
    };

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getBtnStart().requestFocusInWindow(); }
        });
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
        view.getBtnStart().addActionListener(actStartGame);

        view.getLstDecks().initialize();
        updateData();

        view.getGauntletLister().setSelectedIndex(0);
    }

    private void updateData() {
        final File[] files = GauntletIO.getGauntletFilesLocked();
        final List<GauntletData> data = new ArrayList<GauntletData>();
        for (final File f : files) {
            final GauntletData gd = GauntletIO.loadGauntlet(f);
            if (gd != null) {
                data.add(gd);
            }
        }

        view.getGauntletLister().setGauntlets(data);
        view.getGauntletLister().setSelectedIndex(0);
    }

    /** */
    private void startGame() {
        final GauntletData gd = view.getGauntletLister().getSelectedGauntlet();
        final Deck userDeck;

        if (gd.getUserDeck() != null) {
            userDeck = gd.getUserDeck();
        } else {
            userDeck = view.getLstDecks().getPlayer().getDeck();
            gd.setUserDeck(userDeck);
        }

        gd.stamp();
        FModel.setGauntletData(gd);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        final Deck aiDeck = gd.getDecks().get(gd.getCompleted());

        final List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
        final RegisteredPlayer human = new RegisteredPlayer(gd.getUserDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
        starter.add(human);
        starter.add(new RegisteredPlayer(aiDeck).setPlayer(GamePlayerUtil.createAiPlayer()));

        final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(GameType.Gauntlet, null, starter, human, GuiBase.getInterface().getNewGuiGame());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.hideOverlay();
            }
        });
    }

}
