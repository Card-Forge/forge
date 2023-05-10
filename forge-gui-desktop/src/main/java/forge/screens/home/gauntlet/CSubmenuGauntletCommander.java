package forge.screens.home.gauntlet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import forge.deck.DeckType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.gauntlet.GauntletData;
import forge.gamemodes.gauntlet.GauntletUtil;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.player.GamePlayerUtil;

/**
 * Controls the "commander gauntlet" submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */

public enum CSubmenuGauntletCommander implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final ActionListener actStartGame = new ActionListener() {
        @Override public void actionPerformed(final ActionEvent arg0) {
            startGame();
        }
    };

    private final VSubmenuGauntletCommander view = VSubmenuGauntletCommander.SINGLETON_INSTANCE;

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
        view.getLstDecks().initialize(DeckType.COMMANDER_DECK);
    }

    private void startGame() {
        final RegisteredPlayer player = view.getLstDecks().getPlayer();
        if (player == null) { // no deck selected
            return;
        }

        // Start game overlay
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        // Find appropriate filename for new save, create and set new save file.
        final List<DeckType> allowedDeckTypes = new ArrayList<>();
        if (view.getBoxRandomCommanderDecks().isSelected()) { allowedDeckTypes.add(DeckType.RANDOM_COMMANDER_DECK); }
        if (view.getBoxPreconCommanderDecks().isSelected()) { allowedDeckTypes.add(DeckType.PRECON_COMMANDER_DECK); }
        if (view.getBoxCommanderDecks().isSelected()) { allowedDeckTypes.add(DeckType.COMMANDER_DECK); }

        final GauntletData gd = GauntletUtil.createCommanderGauntlet(player.getDeck(), view.getSliOpponents().getValue(), allowedDeckTypes, null);

        final List<RegisteredPlayer> starter = new ArrayList<>();
        final RegisteredPlayer human = RegisteredPlayer.forCommander(player.getDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
        starter.add(human);
        starter.add(RegisteredPlayer.forCommander(gd.getDecks().get(gd.getCompleted())).setPlayer(GamePlayerUtil.createAiPlayer()));

        gd.startRound(starter, human);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.hideOverlay();
            }
        });
    }

}
