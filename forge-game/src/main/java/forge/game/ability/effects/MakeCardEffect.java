package forge.game.ability.effects;

import java.util.Map;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class MakeCardEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());
        final Card source = sa.getHostCard();
        final PlayerCollection players = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

        for (final Player player : players) {
            final Game game = player.getGame();

            String name = sa.getParamOrDefault("Name", "");
            if (name.equals("ChosenName")) {
                if (sa.getHostCard().hasChosenName()) {
                    name = sa.getHostCard().getChosenName();
                } else {
                    continue;
                }
            }
            if (sa.hasParam("DefinedName")) {
                final CardCollection def = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedName"), sa);
                if (def.size() > 0) {
                    name = def.getFirst().getName();
                }
            }
            final ZoneType zone = ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Library"));
            int amount = sa.hasParam("Amount") ?
                    AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa) : 1;

            CardCollection cards = new CardCollection();

            if (!name.equals("")) {
                while (amount > 0) {
                    Card card = Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(name), player);
                    if (sa.hasParam("IsToken")) {
                        card.setToken(true);
                    }
                    if (!sa.hasParam("NotToken")) {
                        card.setTokenCard(true);
                    }
                    game.getAction().moveTo(ZoneType.None, card, sa, moveParams);
                    cards.add(card);
                    amount--;
                }

                final CardZoneTable triggerList = new CardZoneTable();
                for (final Card c : cards) {
                    Card made = game.getAction().moveTo(zone, c, sa, moveParams);
                    triggerList.put(ZoneType.None, made.getZone().getZoneType(), made);
                    if (sa.hasParam("RememberMade")) {
                        sa.getHostCard().addRemembered(made);
                    }
                    if (sa.hasParam("ImprintMade")) {
                        sa.getHostCard().addImprintedCard(made);
                    }
                }
                triggerList.triggerChangesZoneAll(game, sa);
                if (zone.equals(ZoneType.Library)) {
                    player.shuffle(sa);
                }
            }
        }
    }
}
