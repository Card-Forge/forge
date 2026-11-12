package forge.game.mulligan;

import java.util.List;

import com.google.common.collect.Lists;

import forge.StaticData;
import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;

public class MulliganService {
    Player firstPlayer;
    Game game;
    List<AbstractMulligan> mulligans = Lists.newArrayList();

    public MulliganService(Player player) {
        firstPlayer = player;
        game = firstPlayer.getGame();
    }

    public void perform() {
        initializeMulligans();
        runPlayerMulligans();
        runPostMulligans();
    }

    private void initializeMulligans() {
        List<Player> whoCanMulligan = Lists.newArrayList(game.getPlayers());
        int offset = whoCanMulligan.indexOf(firstPlayer);

        for (int i = 0; i < offset; i++) {
            whoCanMulligan.add(whoCanMulligan.remove(0));
        }

        boolean firstMullFree = isFirstMulliganFree(game);

        for (Player player : whoCanMulligan) {
            AbstractMulligan mulligan = ruleFor(player, firstMullFree);
            mulligans.add(mulligan);
            mulligan.beforeFirstMulligan();
        }
    }

    /** Whether a player's first mulligan is free in this game (multiplayer games and Brawl). */
    public static boolean isFirstMulliganFree(final Game game) {
        return game.getPlayers().size() > 2 || game.getRules().hasAppliedVariant(GameType.Brawl);
    }

    /** A new mulligan of the rule in force (the preference's) for this player: to run it, or to read its numbers. */
    public static AbstractMulligan ruleFor(final Player player, final boolean firstMullFree) {
        switch (StaticData.instance().getMulliganRule()) {
            case Original:
                return new OriginalMulligan(player, firstMullFree);
            case Paris:
                return new ParisMulligan(player, firstMullFree);
            case London:
                return new LondonMulligan(player, firstMullFree);
            case Houston:
                return new HoustonMulligan(player, firstMullFree);
            case Vancouver:
            default:
                return new VancouverMulligan(player, firstMullFree);
        }
    }

    private void runPlayerMulligans() {
        boolean allKept;
        do {
            allKept = true;
            for (AbstractMulligan mulligan : mulligans) {
                if (mulligan.hasKept()) {
                    continue;
                }

                Player p = mulligan.getPlayer();

                boolean keep = !mulligan.canMulligan() ||
                        p.getController().mulliganKeepHand(
                                firstPlayer,
                                mulligan.tuckCardsDuringMulligan()
                        );

                if (game.isGameOver()) {
                    // conceded during mulligan prompt
                    return;
                }

                if (keep) {
                    mulligan.keep();
                    continue;
                }

                allKept = false;
                mulligan.mulligan();
            }
        } while (!allKept);
    }

    private void runPostMulligans() {
        for (AbstractMulligan mulligan : mulligans) {
            mulligan.afterMulligan();
        }
    }
}