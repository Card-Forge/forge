package forge.game.ability.effects;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.Lang;

import java.util.List;
import java.util.Map;

public class OpenAttractionEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa) : 1;

        if(tgtPlayers.isEmpty())
            return "";

        sb.append(Lang.joinHomogenous(tgtPlayers));

        if (tgtPlayers.size() > 1) {
            sb.append(" each");
        }
        sb.append(Lang.joinVerb(tgtPlayers, " open")).append(" ");
        sb.append(amount == 1 ? "an Attraction." : (Lang.getNumeral(amount) + " Attractions."));
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa) : 1;

        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        final CardZoneTable triggerList = AbilityKey.addCardZoneTableParams(moveParams, sa);

        for (Player p : tgtPlayers) {
            if (!p.isInGame())
                continue;
            final PlayerZone attractionDeck = p.getZone(ZoneType.AttractionDeck);
            for (int i = 0; i < amount; i++) {
                if(attractionDeck.isEmpty())
                    continue;
                Card attraction = attractionDeck.get(0);
                attraction = p.getGame().getAction().moveToPlay(attraction, sa, moveParams);
                if (sa.hasParam("Remember")) {
                    source.addRemembered(attraction);
                }
            }
        }
        triggerList.triggerChangesZoneAll(sa.getHostCard().getGame(), sa);
    }
}
