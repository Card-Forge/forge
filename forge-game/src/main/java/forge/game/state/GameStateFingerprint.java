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

            final StringBuilder cd = new StringBuilder();
            for (final Map.Entry<Card, Integer> e : p.getCommanderDamage()) {
                cd.append(e.getKey().getName()).append('=').append(e.getValue()).append(',');
            }
            sb.append("CD:").append(cd);

            final StringBuilder mp = new StringBuilder();
            for (final Mana m : p.getManaPool()) {
                mp.append(MagicColor.toShortString(m.getColor())).append(',');
            }
            sb.append("MP:").append(mp);
        }

        for (final ZoneType zt : ZoneType.values()) {
            final StringBuilder names = new StringBuilder();
            if (zt == ZoneType.Battlefield) {
                for (final Player p : game.getPlayers()) {
                    final PlayerZone z = p.getZone(ZoneType.Battlefield);
                    final CardCollectionView cards = (z instanceof PlayerZoneBattlefield)
                            ? ((PlayerZoneBattlefield) z).getCardsUnexpanded() : z.getCards(false);
                    for (final Card c : cards) {
                        names.append(c.getName()).append(',');
                    }
                }
            } else {
                for (final Card c : game.getCardsIn(zt)) {
                    names.append(c.getName()).append(',');
                }
            }
            sb.append("|Z:").append(zt.getName()).append(':').append(names);
        }
        return sb.toString();
    }
}
