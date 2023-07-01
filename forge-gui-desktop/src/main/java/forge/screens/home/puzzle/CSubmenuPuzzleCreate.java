package forge.screens.home.puzzle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import com.google.common.collect.Maps;

import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.puzzle.Puzzle;
import forge.gui.GuiBase;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.gui.util.SGuiChoose;
import forge.gui.util.SOptionPane;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.player.GamePlayerUtil;
import forge.util.Localizer;

public enum CSubmenuPuzzleCreate implements ICDoc, IMenuProvider {
    SINGLETON_INSTANCE;

    private VSubmenuPuzzleCreate view = VSubmenuPuzzleCreate.SINGLETON_INSTANCE;

    @Override
    public void register() {

    }

    @Override
    public void initialize() {
        view.getBtnStart().addActionListener(
                new ActionListener() { @Override
                public void actionPerformed(final ActionEvent e) { startPuzzleCreate(); } });
    }

    @Override
    public void update() {
        MenuUtil.setMenuProvider(this);
    }

    @Override
    public List<JMenu> getMenus() {
        final List<JMenu> menus = new ArrayList<>();
        menus.add(PuzzleGameMenu.getMenu());
        return menus;
    }

    private Map<String, List<String>> generateEmptyPuzzle(String firstPlayer) {
        Map<String, List<String>> emptyPuzzle = Maps.newHashMap();

        emptyPuzzle.put("metadata", Arrays.asList(
                "Name:New Puzzle",
                "URL:http://www.cardforge.org",
                "Goal:Win",
                "Turns:999",
                "Difficulty:Easy",
                "Description:This is a completely empty puzzle placeholder."
                )
        );

        emptyPuzzle.put("state", Arrays.asList(
                "ActivePlayer=" + firstPlayer,
                "ActivePhase=upkeep",
                "HumanLife=20",
                "AILife=20"
                )
        );

        return emptyPuzzle;
    }

    private void startPuzzleCreate() {
        String firstPlayer = SGuiChoose.one(Localizer.getInstance().getMessage("lblWhoShouldBeFirstTakeTurn"),
                Arrays.asList("Human", "AI"));

        final Puzzle emptyPuzzle = new Puzzle(generateEmptyPuzzle(firstPlayer));

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.setStartGameHook(new Runnable() {
            @Override
            public final void run() {
                SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblWelcomePuzzleModeMessage"),
                        Localizer.getInstance().getMessage("lblCreateNewPuzzle"), SOptionPane.WARNING_ICON);
                emptyPuzzle.applyToGame(hostedMatch.getGame());
            }
        });

        final List<RegisteredPlayer> players = new ArrayList<>();
        final RegisteredPlayer human = new RegisteredPlayer(new Deck()).setPlayer(GamePlayerUtil.getGuiPlayer());
        human.setStartingHand(0);
        players.add(human);

        final RegisteredPlayer ai = new RegisteredPlayer(new Deck()).setPlayer(GamePlayerUtil.createAiPlayer());
        ai.setStartingHand(0);
        players.add(ai);

        GameRules rules = new GameRules(GameType.Puzzle);
        rules.setGamesPerMatch(1);
        hostedMatch.startMatch(rules, null, players, human, GuiBase.getInterface().getNewGuiGame());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.hideOverlay();
            }
        });
    }
}
