package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardDamageMap;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

import java.util.List;
import java.util.Map;

public class FightEffect extends DamageBaseEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        List<Card> fighters = getFighters(sa);

        if (fighters.size() > 1) {
            sb.append(fighters.get(0) + " fights " + fighters.get(1));
        }
        else if (fighters.size() == 1) {
            sb.append(fighters.get(0) + " fights unknown");
        }
        return sb.toString();
    }


    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        List<Card> fighters = getFighters(sa);
        final Game game = sa.getActivatingPlayer().getGame();

        if (fighters.size() < 2 || !fighters.get(0).isInPlay()
                || !fighters.get(1).isInPlay()) {
            return;
        }
        
        if (sa.hasParam("RememberObjects")) {
            final String remembered = sa.getParam("RememberObjects");
            for (final Object o : AbilityUtils.getDefinedObjects(host, remembered, sa)) {
                host.addRemembered(o);
            }
        }
        
        boolean fightToughness = sa.hasParam("FightWithToughness");
        CardDamageMap damageMap = new CardDamageMap();
        CardDamageMap preventMap = new CardDamageMap();

        // Damage is dealt simultaneously, so we calculate the damage from source to target before it is applied
        final int dmg1 = fightToughness ? fighters.get(0).getNetToughness() : fighters.get(0).getNetPower();
        final int dmg2 = fightToughness ? fighters.get(1).getNetToughness() : fighters.get(1).getNetPower();

        dealDamage(fighters.get(0), fighters.get(1), dmg1, damageMap, preventMap);
        dealDamage(fighters.get(1), fighters.get(0), dmg2, damageMap, preventMap);

        preventMap.triggerPreventDamage(false);
        damageMap.triggerDamageDoneOnce(false);
        
        replaceDying(sa);

        for (Card c : fighters) {
            final Map<String, Object> runParams = Maps.newHashMap();
            runParams.put("Fighter", c);
            game.getTriggerHandler().runTrigger(TriggerType.Fight, runParams, false);
        }
    }

    private static List<Card> getFighters(SpellAbility sa) {
        final List<Card> fighterList = Lists.newArrayList();

        Card fighter1 = null;
        Card fighter2 = null;

        List<Card> tgts = null;
        if (sa.usesTargeting()) {
            tgts = Lists.newArrayList(sa.getTargets().getTargetCards());
            if (tgts.size() > 0) {
                fighter1 = tgts.get(0);
            }
        }
        if (sa.hasParam("Defined")) {
            List<Card> defined = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            // Allow both fighters to come from defined list if first fighter not already found
            if (sa.hasParam("ExtraDefined")) {
                defined.addAll(AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("ExtraDefined"), sa));
            }

            if (!defined.isEmpty()) {
                if (defined.size() > 1 && fighter1 == null) {
                    fighter1 = defined.get(0);
                    fighter2 = defined.get(1);
                }
                else {
                    fighter2 = defined.get(0);
                }
            }
        } else if (tgts.size() > 1) {
            fighter2 = tgts.get(1);
        }

        if (fighter1 != null) {
            fighterList.add(fighter1);
        }
        if (fighter2 != null) {
            fighterList.add(fighter2);
        }

        return fighterList;
    }
    
    private void dealDamage(Card source, Card target, int damage, CardDamageMap damageMap, CardDamageMap preventMap) {
        target.addDamage(damage, source, damageMap, preventMap);
    }

}
