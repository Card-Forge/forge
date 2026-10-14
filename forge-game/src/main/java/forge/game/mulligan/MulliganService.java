package forge.game.mulligan;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import forge.MulliganDefs;
import forge.StaticData;
import forge.game.Game;
import forge.game.GameAction;
import forge.game.GameType;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.PlayerController.DanDanMulliganDecision;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class MulliganService {
    Player firstPlayer;
    Game game;
    List<AbstractMulligan> mulligans = Lists.newArrayList();
    private final boolean danDan;

    public MulliganService(Player player) {
        firstPlayer = player;
        game = firstPlayer.getGame();
        danDan = game.getRules().hasAppliedVariant(GameType.DanDan);
    }

    public void perform() {
        initializeMulligans();
        if (danDan) {
            runDanDanMulligans();
        } else {
            runPlayerMulligans();
        }
        if (!danDan || !game.isGameOver()) {
            runPostMulligans();
        }
    }

    private void initializeMulligans() {
        List<Player> whoCanMulligan = Lists.newArrayList(game.getPlayers());
        int offset = whoCanMulligan.indexOf(firstPlayer);

        for (int i = 0; i < offset; i++) {
            whoCanMulligan.add(whoCanMulligan.remove(0));
        }

        boolean firstMullFree = !danDan
                && (game.getPlayers().size() > 2 || game.getRules().hasAppliedVariant(GameType.Brawl));

        for (Player player : whoCanMulligan) {
            MulliganDefs.MulliganRule rule = danDan
                    ? MulliganDefs.MulliganRule.London : StaticData.instance().getMulliganRule();
            AbstractMulligan mulligan;
            switch (rule) {
                case Original:
                    mulligan = new OriginalMulligan(player, firstMullFree);
                    break;
                case Paris:
                    mulligan = new ParisMulligan(player, firstMullFree);
                    break;
                case Vancouver:
                    mulligan = new VancouverMulligan(player, firstMullFree);
                    break;
                case London:
                    mulligan = new LondonMulligan(player, firstMullFree);
                    break;
                case Houston:
                    mulligan = new HoustonMulligan(player, firstMullFree);
                    break;
                default:
                    mulligan = new VancouverMulligan(player, firstMullFree);
                    break;
            }

            mulligans.add(mulligan);
            mulligan.beforeFirstMulligan();
        }
    }

    private void runDanDanMulligans() {
        boolean allKept;
        do {
            List<AbstractMulligan> selected = new ArrayList<>();
            List<AbstractMulligan> freeSelected = new ArrayList<>();
            allKept = true;

            // Collect every player's choice before returning or shuffling any hand.
            for (AbstractMulligan mulligan : mulligans) {
                if (mulligan.hasKept()) {
                    continue;
                }
                Player player = mulligan.getPlayer();
                boolean canFree = mulligan.timesMulliganed == 0 && qualifiesForFreeMulligan(player);
                boolean canRegular = mulligan.tuckCardsDuringMulligan() + 1 <= player.getMaxHandSize();
                if (!canFree && !canRegular) {
                    mulligan.keep();
                    continue;
                }

                DanDanMulliganDecision decision = player.getController().chooseDanDanMulligan(firstPlayer,
                        mulligan.tuckCardsDuringMulligan(), canFree);
                if (game.isGameOver()) {
                    return;
                }
                if ((decision == DanDanMulliganDecision.FREE && !canFree)
                        || (decision == DanDanMulliganDecision.REGULAR && !canRegular)) {
                    decision = DanDanMulliganDecision.KEEP;
                }
                if (decision == DanDanMulliganDecision.KEEP) {
                    mulligan.keep();
                } else {
                    allKept = false;
                    selected.add(mulligan);
                    if (decision == DanDanMulliganDecision.FREE) {
                        freeSelected.add(mulligan);
                    } else {
                        mulligan.timesMulliganed++;
                    }
                }
            }

            if (selected.isEmpty()) {
                continue;
            }

            for (AbstractMulligan mulligan : freeSelected) {
                Player player = mulligan.getPlayer();
                game.getAction().reveal(new CardCollection(player.getCardsIn(ZoneType.Hand)), player,
                        true, Localizer.getInstance().getMessage("lblDanDanFreeMulliganRevealed", player.getName()));
                if (game.isGameOver()) {
                    return;
                }
            }

            // Return all selected hands first; each selected player's shuffle
            // instruction remains a separate engine call over the shared pile.
            for (AbstractMulligan mulligan : selected) {
                Player player = mulligan.getPlayer();
                for (Card card : new CardCollection(player.getCardsIn(ZoneType.Hand))) {
                    game.getAction().moveToLibrary(card, null);
                }
            }
            for (AbstractMulligan mulligan : selected) {
                mulligan.getPlayer().shuffle(null);
            }

            GameAction.DrawInstruction instruction = game.getAction().newDanDanDrawInstruction(firstPlayer);
            for (AbstractMulligan mulligan : selected) {
                Player player = mulligan.getPlayer();
                player.prepareDanDanDrawBatch(player.getStartingHandSize(), null, AbilityKey.newMap(),
                        player.getZone(ZoneType.Hand), instruction);
            }
            game.getAction().resolveDanDanDrawInstruction(instruction);

            // London bottoms are private and immediate. First-player batches are
            // appended first, so the second player's batch sits below them.
            for (AbstractMulligan mulligan : selected) {
                if (game.isGameOver()) {
                    return;
                }
                Player player = mulligan.getPlayer();
                ((LondonMulligan) mulligan).tuckCardsAfterMulligan();
                if (game.isGameOver()) {
                    return;
                }
                player.onMulliganned();
            }
        } while (!allKept && !game.isGameOver());
    }

    private boolean qualifiesForFreeMulligan(Player player) {
        int lands = 0;
        int handSize = 0;
        for (Card card : player.getCardsIn(ZoneType.Hand)) {
            handSize++;
            if (card.isLand()) {
                lands++;
            }
        }
        return lands < 2 || handSize - lands < 2;
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
