package forge.game.ability.effects;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class MakeCardEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Player player = sa.getActivatingPlayer();
        final Game game = player.getGame();

        final String name = sa.hasParam("Name") ? sa.getParam("Name") : sa.getHostCard().getName();
        final ZoneType zone = ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Library"));
        int amount = sa.hasParam("Amount") ? Integer.parseInt(sa.getParam("Amount")) : 1;

        CardCollection cards = new CardCollection();

        while (amount > 0) {
            Card card = Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(name), player);
            if (!sa.hasParam("NotToken")) { card.setTokenCard(true); }
            cards.add(card);
            amount--;
        }

        for (final Card c : cards) {
            game.getAction().moveTo(zone, c, sa);
        }
        if (zone.equals(ZoneType.Library)) {
            player.shuffle(sa);
        }
    }
}
