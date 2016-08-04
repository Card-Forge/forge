package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;


public class ZoneExchangeEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        Card object1;
        if (sa.hasParam("Object")) {
            object1 = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Object"), sa).get(0);
        } else {
            object1 = sa.getHostCard();
        }
        return "Exchange a " + sa.getParam("Type") + " in " + sa.getParam("Zone2") + " with " + object1;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        final String type = sa.getParam("Type");
        final String valid = sa.getParam("ValidExchange");
        final ZoneType zone1 = sa.hasParam("Zone1") ? ZoneType.smartValueOf(sa.getParam("Zone1")) : ZoneType.Battlefield;
        final ZoneType zone2 = sa.hasParam("Zone2") ? ZoneType.smartValueOf(sa.getParam("Zone2")) : ZoneType.Hand;
        Card object1 = null;
        if (sa.hasParam("Object")) {
            object1 = AbilityUtils.getDefinedCards(source, sa.getParam("Object"), sa).get(0);
        } else {
            object1 = source;
        }

        if (object1 == null || !object1.isInZone(zone1) || !object1.getOwner().equals(p)) {
            // No original object, can't exchange.
            return;
        }

        CardCollection list = new CardCollection(p.getCardsIn(zone2));

        String filter;

        if (type != null) {
            // If Type was declared, both objects need to match the type
            if (!object1.getType().hasStringType(type)) {
                return;
            }
            filter = type;
        } else if (valid != null) {
            filter = valid;
        } else {
            filter = "Card";
        }

        list = CardLists.getValidCards(list, filter, p, source);
        if (list.isEmpty())  {
            // Nothing to exchange the object?
            return;
        }

        Card object2 = p.getController().chooseSingleEntityForEffect(list, sa, "Choose a card", !sa.hasParam("Mandatory"));
        if (object2 == null || !object2.isInZone(zone2) || (type != null && !object2.getType().hasStringType(type))) {
            return;
        }
        // if the aura can't enchant, nothing happened.
        Card c = null;
        if (type != null && type.equals("Aura") && object1.getEnchantingCard() != null) {
            c = object1.getEnchantingCard();
            if (!c.canBeEnchantedBy(object2)) {
                return;
            }
        }
        // Enchant first
        if (c != null) {
            object1.unEnchantEntity(c);
            object2.enchantEntity(c);
        }
        // Exchange Zone
        game.getAction().moveTo(zone2, object1);
        game.getAction().moveTo(zone1, object2);
    }
}
