package forge.screens.home.puzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.puzzle.Puzzle;
import forge.gamemodes.puzzle.PuzzleIO;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.player.GamePlayerUtil;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.NewGameMenu;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextArea;
import forge.toolbox.GuiChoose;
import forge.util.Callback;
import forge.util.Utils;

public class PuzzleScreen extends LaunchScreen {

    private static final float PADDING = Utils.scale(10);

    private final FTextArea lblDesc = add(new FTextArea(false,
            Forge.getLocalizer().getMessage("lblPuzzleText1") + "\n\n" +
            Forge.getLocalizer().getMessage("lblPuzzleText2") + "\n\n" +
            Forge.getLocalizer().getMessage("lblPuzzleText3")));

    public PuzzleScreen() {
        super(null, NewGameMenu.getMenu());

        lblDesc.setFont(FSkinFont.get(12));
        lblDesc.setTextColor(FLabel.getInlineLabelColor());
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float w = width - 2 * PADDING;
        float h = height - y - PADDING;
        lblDesc.setBounds(x, y, w, h);
    }

    @Override
    protected void startMatch() {
        final ArrayList<Puzzle> puzzles = PuzzleIO.loadPuzzles(ForgeConstants.PUZZLE_DIR);
        Collections.sort(puzzles);

        GuiChoose.oneOrNone(Forge.getLocalizer().getMessage("lblChooseAPuzzle"), puzzles, new Callback<Puzzle>() {
            @Override
            public void run(final Puzzle chosen) {
                if (chosen != null) {
                    LoadingOverlay.show(Forge.getLocalizer().getMessage("lblLoadingThePuzzle"), true, () -> {
                        // Load selected puzzle
                        final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
                        hostedMatch.setStartGameHook(() -> chosen.applyToGame(hostedMatch.getGame()));

                        hostedMatch.setEndGameHook((() -> chosen.savePuzzleSolve(hostedMatch.getGame().getOutcome().isWinner(GamePlayerUtil.getGuiPlayer()))));

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
                        FOptionPane.showMessageDialog(chosen.getGoalDescription(), chosen.getName());
                    });
                }
            }
        });

    }
}
