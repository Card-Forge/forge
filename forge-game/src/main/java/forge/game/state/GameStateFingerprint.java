package forge.game.state;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REFORGE COMMANDER EXTENSION
 * Board/player-state fingerprint for infinite-loop detection (#44/#48).
 *
 * The fingerprint intentionally omits the absolute turn counter and the stack:
 * a real loop returns to the same board/player position within a turn, and
 * keeping those fields out maximizes the chance of catching the repeat. It is
 * name-level only (not full card state) — enough to recognize a recurring
 * position, and cheap to compute.
 */
public final class GameStateFingerprint {
    private GameStateFingerprint() {
    }

    public static String compute(final Game game) {
        final StringBuilder sb = new StringBuilder();
        sb.append("PT:").append(game.getPhaseHandler().getPlayerTurn().getName());
        sb.append("|PH:").append(game.getPhaseHandler().getPhase().name());

        for (final Player p : game.getPlayers()) {
            sb.append("|").append(p.getName());
            sb.append("L:").append(p.getLife());
            sb.append("Po:").append(p.getPoisonCounters());

            // canonicalize so entry order does not change the fingerprint
            final List<String> cd = new ArrayList<>();
            for (final Map.Entry<Card, Integer> e : p.getCommanderDamage()) {
                cd.add(e.getKey().getName() + "=" + e.getValue());
            }
            Collections.sort(cd);
            sb.append("CD:").append(cd);

            final List<String> mp = new ArrayList<>();
            for (final Mana m : p.getManaPool()) {
                mp.add(MagicColor.toShortString(m.getColor()));
            }
            Collections.sort(mp);
            sb.append("MP:").append(mp);
        }

        for (final ZoneType zt : ZoneType.values()) {
            final List<String> names = new ArrayList<>();
            if (zt == ZoneType.Battlefield) {
                for (final Player p : game.getPlayers()) {
                    final PlayerZone z = p.getZone(ZoneType.Battlefield);
                    final CardCollectionView cards = (z instanceof PlayerZoneBattlefield)
                            ? ((PlayerZoneBattlefield) z).getCardsUnexpanded() : z.getCards(false);
                    for (final Card c : cards) {
                        names.add(c.getName());
                    }
                }
            } else {
                for (final Card c : game.getCardsIn(zt)) {
                    names.add(c.getName());
                }
            }
            Collections.sort(names);
            sb.append("|Z:").append(zt.getName()).append(':').append(names);
        }
        return sb.toString();
    }
}
