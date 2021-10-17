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
        if (view.getBoxColorDecks().isSelected()) { allowedDeckTypes.add(DeckType.COLOR_DECK); }
        if (view.getBoxStandardColorDecks().isSelected()) { allowedDeckTypes.add(DeckType.STANDARD_COLOR_DECK); }
        if (view.getBoxStandardGenDecks().isSelected()) { allowedDeckTypes.add(DeckType.STANDARD_CARDGEN_DECK); }
        if (view.getBoxPioneerGenDecks().isSelected()) { allowedDeckTypes.add(DeckType.PIONEER_CARDGEN_DECK); }
        if (view.getBoxHistoricGenDecks().isSelected()) { allowedDeckTypes.add(DeckType.HISTORIC_CARDGEN_DECK); }
        if (view.getBoxModernGenDecks().isSelected()) { allowedDeckTypes.add(DeckType.MODERN_CARDGEN_DECK); }
        if (view.getBoxLegacyGenDecks().isSelected()) { allowedDeckTypes.add(DeckType.LEGACY_CARDGEN_DECK); }
        if (view.getBoxVintageGenDecks().isSelected()) { allowedDeckTypes.add(DeckType.VINTAGE_CARDGEN_DECK); }
        if (view.getBoxModernColorDecks().isSelected()) { allowedDeckTypes.add(DeckType.MODERN_COLOR_DECK); }
        if (view.getBoxThemeDecks().isSelected()) { allowedDeckTypes.add(DeckType.THEME_DECK); }
        if (view.getBoxUserDecks().isSelected()) { allowedDeckTypes.add(DeckType.CUSTOM_DECK); }
        if (view.getBoxQuestDecks().isSelected()) { allowedDeckTypes.add(DeckType.QUEST_OPPONENT_DECK); }
        if (view.getBoxPreconDecks().isSelected()) { allowedDeckTypes.add(DeckType.PRECONSTRUCTED_DECK); }

        final GauntletData gd = GauntletUtil.createQuickGauntlet(player.getDeck(), view.getSliOpponents().getValue(), allowedDeckTypes, null);

        final List<RegisteredPlayer> starter = new ArrayList<>();
        final RegisteredPlayer human = player.setPlayer(GamePlayerUtil.getGuiPlayer());
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
