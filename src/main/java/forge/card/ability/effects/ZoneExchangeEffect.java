package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;


public class ZoneExchangeEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        Card object1;
        if (sa.hasParam("Object")) {
            object1 = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Object"), sa).get(0);
        } else {
            object1 = sa.getSourceCard();
        }
        return "Exchange a " + sa.getParam("Type") + " in " + sa.getParam("Zone2") + " with " + object1;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        final String type = sa.getParam("Type");
        final ZoneType zone1 = sa.hasParam("Zone1") ? ZoneType.smartValueOf(sa.getParam("Zone1")) : ZoneType.Battlefield;
        final ZoneType zone2 = sa.hasParam("Zone2") ? ZoneType.smartValueOf(sa.getParam("Zone2")) : ZoneType.Hand;
        Card object1 = null;
        if (sa.hasParam("Object")) {
            object1 = AbilityUtils.getDefinedCards(source, sa.getParam("Object"), sa).get(0);
        } else {
            object1 = source;
        }
        List<Card> list = new ArrayList<Card>(p.getCardsIn(zone2));
        if (type != null) {
            list = CardLists.getValidCards(list, type, p, source);
        }
        // if object is not in the original zone, or there is no card in zone 2
        // then return
        if (object1 == null || !object1.isInZone(zone1) || list.isEmpty() || !object1.isType(type)) {
            return;
        }
        Card object2 = p.getController().chooseSingleCardForEffect(list, sa, "Choose a card");
        if (object2 == null || !object2.isInZone(zone2) || !object2.isType(type)) {
            return;
        }
        // if the aura can't enchant, nothing happened.
        Card c = null;
        if (type.equals("Aura") && object1.getEnchantingCard() != null) {
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
