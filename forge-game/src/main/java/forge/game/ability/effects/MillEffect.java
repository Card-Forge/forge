package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;

import java.util.Map;

public class MillEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;
        final boolean facedown = sa.hasParam("ExileFaceDown");
        final boolean reveal = !sa.hasParam("NoReveal");
        final boolean showRevealDialog = sa.hasParam("ShowMilledCards");

        if (sa.hasParam("ForgetOtherRemembered")) {
            source.clearRemembered();
        }

        ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        if (destination == null) {
            destination = ZoneType.Graveyard;
        }

        final CardZoneTable table = new CardZoneTable();
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());
        moveParams.put(AbilityKey.InternalTriggerTable, table);

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }

            String toZoneStr = destination.equals(ZoneType.Graveyard) ? "" : " (" +
                    Localizer.getInstance().getMessage("lblMilledToZone", destination.getTranslatedName()) + ")";
            if (sa.hasParam("Optional")) {
                String d = destination.equals(ZoneType.Graveyard) ? "" : " (" + destination.getTranslatedName() + ")";
                final String prompt = TextUtil.concatWithSpace(Localizer.getInstance().
                        getMessage("lblDoYouWantToMill", Lang.nounWithNumeral(numCards, "card"), d));
                // CR 701.13b
                if (numCards > p.getZone(ZoneType.Library).size() || !p.getController().confirmAction(sa, null, prompt, null)) {
                    continue;
                }
            }
            final CardCollectionView milled = p.mill(numCards, destination, sa, moveParams);
            // Reveal the milled cards, so players don't have to manually inspect the
            // graveyard to figure out which ones were milled.
            if (!facedown && reveal) { // do not reveal when exiling face down
                if (showRevealDialog) {
                    final String message = Localizer.getInstance().getMessage("lblMilledCards");
                    final boolean addSuffix = !toZoneStr.equals("");
                    game.getAction().reveal(milled, destination, p, false, message, addSuffix);
                }
                p.getGame().getGameLog().add(GameLogEntryType.ZONE_CHANGE, p + " milled " +
                        Lang.joinHomogenous(milled) + toZoneStr + ".");
            }
            if (destination.equals(ZoneType.Exile)) {
                for (final Card c : milled) {
                    handleExiledWith(c, sa);
                    if (facedown) {
                        c.turnFaceDown(true);
                    }
                }
            }
            if (sa.hasParam("RememberMilled")) {
                source.addRemembered(milled);
            }
            if (sa.hasParam("Imprint")) {
                source.addImprintedCards(milled);
            }
        }

        // run trigger if something got milled
        table.triggerChangesZoneAll(game, sa);
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;
        final boolean optional = sa.hasParam("Optional");
        final boolean eachP = sa.hasParam("Defined") && sa.getParam("Defined").equals("Player");
        String each = "Each player";
        final PlayerCollection tgtPs = getTargetPlayers(sa);

        if (sa.hasParam("IfDesc")) {
            final String ifD = sa.getParam("IfDesc");
            if (ifD.equals("True")) {
                String ifDesc = sa.getDescription();
                if (ifDesc.contains(",")) {
                    sb.append(ifDesc, 0, ifDesc.indexOf(",") + 1);
                } else {
                    sb.append("[MillEffect IfDesc parsing error]");
                }
            } else {
                sb.append(ifD);
            }
            sb.append(" ");
            each = each.toLowerCase();
        }

        sb.append(eachP ? each : Lang.joinHomogenous(tgtPs));
        sb.append(" ");

        final ZoneType dest = ZoneType.smartValueOf(sa.getParam("Destination"));
        sb.append(optional ? "may " : "");
        if ((dest == null) || dest.equals(ZoneType.Graveyard)) {
            sb.append("mill");
        } else if (dest.equals(ZoneType.Exile)) {
            sb.append("exile");
        } else if (dest.equals(ZoneType.Ante)) {
            sb.append("ante");
        }
        sb.append((optional || tgtPs.size() > 1) && !eachP ? " " : "s ");

        sb.append(Lang.nounWithNumeralExceptOne(numCards, "card")).append(".");

        return sb.toString();
    }
}
