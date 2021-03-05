package forge.screens.home.puzzle;

import forge.GuiBase;
import forge.UiCommand;
import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.puzzle.Puzzle;
import forge.gamemodes.puzzle.PuzzleIO;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.localinstance.assets.FSkinProp;
import forge.localinstance.properties.ForgeConstants;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.player.GamePlayerUtil;
import forge.util.Localizer;
import forge.util.gui.SOptionPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum CSubmenuPuzzleSolve implements ICDoc, IMenuProvider {
    SINGLETON_INSTANCE;

    private VSubmenuPuzzleSolve view = VSubmenuPuzzleSolve.SINGLETON_INSTANCE;

    @Override
    public void register() {

    }

    @Override
    public void initialize() {
        view.getList().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        updateData();
        view.getBtnStart().addActionListener(
                new ActionListener() { @Override
                public void actionPerformed(final ActionEvent e) { startPuzzleSolve(); } });
    }

    private final UiCommand cmdStart = new UiCommand() {
		private static final long serialVersionUID = -367368436333443417L;

		@Override public void run() {
            startPuzzleSolve();
        }
    };

    private void updateData() {
        final ArrayList<Puzzle> puzzles = PuzzleIO.loadPuzzles(ForgeConstants.PUZZLE_DIR);
        Collections.sort(puzzles);

        for(Puzzle p : puzzles) {
            view.getModel().addElement(p);
        }
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

    private boolean startPuzzleSolve() {
        final Puzzle selected = (Puzzle)view.getList().getSelectedValue();
        if (selected == null) {
            SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblPleaseFirstSelectAPuzzleFromList"), Localizer.getInstance().getMessage("lblNoSelectedPuzzle"), FSkinProp.ICO_ERROR);
            return false;
        }

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
                SOptionPane.showMessageDialog(selected.getGoalDescription(), selected.getName(), SOptionPane.INFORMATION_ICON);
                selected.applyToGame(hostedMatch.getGame());
            }
        });

        hostedMatch.setEndGameHook((new Runnable() {
            @Override
            public void run() {
                selected.savePuzzleSolve(hostedMatch.getGame().getOutcome().isWinner(GamePlayerUtil.getGuiPlayer()));
            }
        }));

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

        return true;
    }
}
