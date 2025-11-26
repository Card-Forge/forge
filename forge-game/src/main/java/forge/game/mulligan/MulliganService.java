package forge.game.mulligan;

import java.util.List;

import com.google.common.collect.Lists;

import forge.MulliganDefs;
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

        for (AbstractMulligan mulligan : mulligans) {
            if (mulligan.hasKept()) {
                mulligan.afterMulligan();
            }
        }
    }

    private void initializeMulligans() {
        List<Player> whoCanMulligan = Lists.newArrayList(game.getPlayers());
        int offset = whoCanMulligan.indexOf(firstPlayer);

        for (int i = 0; i < offset; i++) {
            whoCanMulligan.add(whoCanMulligan.remove(0));
        }

        boolean firstMullFree = game.getPlayers().size() > 2 || game.getRules().hasAppliedVariant(GameType.Brawl);

        for (Player player : whoCanMulligan) {
            final int baseHandSize = player.getStartingHandSize();

            MulliganDefs.MulliganRule rule = StaticData.instance().getMulliganRule();
            switch (rule) {
                case Original:
                    mulligans.add(new OriginalMulligan(player, firstMullFree));
                    break;
                case Paris:
                    mulligans.add(new ParisMulligan(player, firstMullFree));
                    break;
                case Vancouver:
                    mulligans.add(new VancouverMulligan(player, firstMullFree));
                    break;
                case London:
                    mulligans.add(new LondonMulligan(player, firstMullFree));
                    break;
                case Houston:
                    mulligans.add(new HoustonMulligan(player, firstMullFree));
                    break;
                default:
                    mulligans.add(new VancouverMulligan(player, firstMullFree));
                    break;
            }
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
                                mulligan.tuckCardsAfterKeepHand()
                        );

                if (game.isGameOver()) {
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

        for (AbstractMulligan mulligan : mulligans) {
            mulligan.afterMulligan();
        }
    }
}