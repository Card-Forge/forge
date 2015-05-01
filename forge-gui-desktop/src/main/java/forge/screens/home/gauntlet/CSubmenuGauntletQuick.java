package forge.screens.home.gauntlet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import forge.deck.DeckType;
import forge.game.player.RegisteredPlayer;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletUtil;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.player.GamePlayerUtil;

/**
 * Controls the "quick gauntlet" submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */

public enum CSubmenuGauntletQuick implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final ActionListener actStartGame = new ActionListener() {
        @Override public void actionPerformed(final ActionEvent arg0) {
            startGame();
        }
    };

    private final VSubmenuGauntletQuick view = VSubmenuGauntletQuick.SINGLETON_INSTANCE;

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
    }

    private void startGame() {
        // Start game overlay
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        // Find appropriate filename for new save, create and set new save file.
        final List<DeckType> allowedDeckTypes = new ArrayList<DeckType>();
        if (view.getBoxColorDecks().isSelected()) { allowedDeckTypes.add(DeckType.COLOR_DECK); }
        if (view.getBoxThemeDecks().isSelected()) { allowedDeckTypes.add(DeckType.THEME_DECK); }
        if (view.getBoxUserDecks().isSelected()) { allowedDeckTypes.add(DeckType.CUSTOM_DECK); }
        if (view.getBoxQuestDecks().isSelected()) { allowedDeckTypes.add(DeckType.QUEST_OPPONENT_DECK); }
        if (view.getBoxPreconDecks().isSelected()) { allowedDeckTypes.add(DeckType.PRECONSTRUCTED_DECK); }

        final GauntletData gd = GauntletUtil.createQuickGauntlet(view.getLstDecks().getPlayer().getDeck(), view.getSliOpponents().getValue(), allowedDeckTypes);

        final List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
        final RegisteredPlayer human = new RegisteredPlayer(gd.getUserDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
        starter.add(human);
        starter.add(new RegisteredPlayer(gd.getDecks().get(gd.getCompleted())).setPlayer(GamePlayerUtil.createAiPlayer()));

        gd.startRound(starter, human);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.hideOverlay();
            }
        });
    }

}
