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
    }

    private void initializeMulligans() {
        List<Player> whoCanMulligan = Lists.newArrayList(game.getPlayers());
        int offset = whoCanMulligan.indexOf(firstPlayer);

        // Have to cycle-shift the list to get the first player on index 0
        for (int i = 0; i < offset; i++) {
            whoCanMulligan.add(whoCanMulligan.remove(0));
        }

        boolean firstMullFree = game.getPlayers().size() > 2 || game.getRules().hasAppliedVariant(GameType.Brawl);

        for (Player player : whoCanMulligan) {
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
                    mulligans.add(new HoustonMulligan(player));
                default:
                    // Default to Vancouver mulligan for now. Should ideally never get here.
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

                // 🚨 1. PRIMARY CHECK: If the mulligan state is 'kept', skip this player.
                // This is how you prevent a second interaction for Houston.
                if (mulligan.hasKept()) {
                    continue;
                }

                Player p = mulligan.getPlayer();
                boolean keep;

                if (mulligan instanceof HoustonMulligan) {
                    if (mulligan.canMulligan()) {
                        // This is the first and only mandatory mulligan for Houston.
                        mulligan.mulligan(); // Executes draw/tuck and MUST set internal 'hasKept' to true.

                        // 🚨 Force the keep() method call externally for safety/consistency.
                        mulligan.keep();

                        keep = true; // Set 'keep' to true for the following logic.
                    } else {
                        // If canMulligan() is false, and hasKept() was false (unlikely state, but handle it)
                        // we must force a keep to proceed and rely on the initial hasKept() check next loop.
                        mulligan.keep();
                        keep = true;
                    }

                } else {
                    // 2. STANDARD MULLIGAN RULES: This is the only place the dialog should run.
                    keep = !mulligan.canMulligan() || p.getController().mulliganKeepHand(firstPlayer, mulligan.tuckCardsAfterKeepHand());
                }

                if (game.isGameOver()) {
                    return;
                }

                if (keep) {
                    // If this was a standard mulligan player, we call keep() now.
                    // If this was Houston, we already called keep() inside the block (or it's idempotent).
                    if (!mulligan.hasKept()) { // Safer check to avoid redundant calls
                        mulligan.keep();
                    }
                    continue;
                }

                // If 'keep' is false (only for non-Houston rules), allKept is set to false.
                allKept = false;
                mulligan.mulligan();
            }
        } while (!allKept);
    }
}