package forge.game.ability.effects;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class MakeCardEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        for (final Player player : getTargetPlayers(sa)) {
            final Card source = sa.getHostCard();
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
                name = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedName"), sa).getFirst().getName();
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
                    game.getAction().moveTo(ZoneType.None, card, sa);
                    cards.add(card);
                    amount--;
                }

                for (final Card c : cards) {
                    game.getAction().moveTo(zone, c, sa);
                    if (sa.hasParam("RememberMade")) {
                        sa.getHostCard().addRemembered(c);
                    }
                    if (sa.hasParam("ImprintMade")) {
                        sa.getHostCard().addImprintedCard(c);
                    }
                }
                if (zone.equals(ZoneType.Library)) {
                    player.shuffle(sa);
                }
            }
        }
    }
}
