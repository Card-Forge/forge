package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardDamageMap;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;

public class FightEffect extends DamageBaseEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        List<Card> fighters = getFighters(sa);

        if (fighters.size() > 1) {
            sb.append(fighters.get(0)).append(" fights ").append(fighters.get(1)).append(".");
        }
        else if (fighters.size() == 1) {
            sb.append(fighters.get(0)).append(" fights.");
        }
        if (sa.hasParam("ReplaceDyingDefined")) {
            CardCollection cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("ReplaceDyingDefined"), sa);
            if (!cards.isEmpty()) {
                sb.append(" If ").append(Lang.joinHomogenous(cards, null, "or"));
                sb.append(" would die this turn, exile it instead.");
            }
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
        final Game game = host.getGame();

        // check is done in getFighters
        if (fighters.size() < 2) {
            return;
        }

        Player controller = host.getController();
        boolean isOptional = sa.hasParam("Optional");

        if (isOptional && !controller.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblWouldYouLikeFight", CardTranslation.getTranslatedName(fighters.get(0).getName()), CardTranslation.getTranslatedName(fighters.get(1).getName())), null)) {
            return;
        }

        dealDamage(sa, fighters.get(0), fighters.get(1));

        for (Card c : fighters) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Fighter, c);
            game.getTriggerHandler().runTrigger(TriggerType.Fight, runParams, false);
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Fighters, fighters);
        game.getTriggerHandler().runTrigger(TriggerType.FightOnce, runParams, false);
    }

    private static List<Card> getFighters(SpellAbility sa) {
        final List<Card> fighterList = Lists.newArrayList();

        Card fighter1 = null;
        Card fighter2 = null;
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        CardCollectionView tgts = null;
        if (sa.usesTargeting()) {
            tgts = sa.getTargets().getTargetCards();
            if (!tgts.isEmpty()) {
                fighter1 = tgts.get(0);
            }
        }
        if (sa.hasParam("Defined")) {
            List<Card> defined = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            // Allow both fighters to come from defined list if first fighter not already found
            if (sa.hasParam("ExtraDefined")) {
                defined.addAll(AbilityUtils.getDefinedCards(host, sa.getParam("ExtraDefined"), sa));
            }

            List<Card> newDefined = Lists.newArrayList();
            for (final Card d : defined) {
                final Card g = game.getCardState(d, null);
                // 701.12b If a creature instructed to fight is no longer on the battlefield or is no longer a creature,
                // no damage is dealt. If a creature is an illegal target
                // for a resolving spell or ability that instructs it to fight, no damage is dealt.
                if (g == null || !g.equalsWithGameTimestamp(d) || !d.isInPlay() || d.isPhasedOut() || !d.isCreature()) {
                    // Test to see if the card we're trying to add is in the expected state
                    continue;
                }
                newDefined.add(g);
            }
            // replace with new List using CardState
            defined = newDefined;

            if (!defined.isEmpty()) {
                if (defined.size() > 1 && fighter1 == null) {
                    fighter1 = defined.get(0);
                    fighter2 = defined.get(1);
                } else { // template often Defined$ ParentTarget vs. Targeted - this yields good StackDesc order
                    fighter2 = fighter1;
                    fighter1 = defined.get(0);
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

    private void dealDamage(final SpellAbility sa, Card fighterA, Card fighterB) {
        boolean usedDamageMap = true;
        CardDamageMap damageMap = sa.getDamageMap();
        CardDamageMap preventMap = sa.getPreventMap();
        GameEntityCounterTable counterTable = sa.getCounterTable();

        if (damageMap == null) {
            // make a new damage map
            damageMap = new CardDamageMap();
            preventMap = new CardDamageMap();
            counterTable = new GameEntityCounterTable();
            usedDamageMap = false;
        }

        // Run replacement effects
        fighterA.getGame().getReplacementHandler().run(ReplacementType.AssignDealDamage, AbilityKey.mapFromAffected(fighterA));
        fighterB.getGame().getReplacementHandler().run(ReplacementType.AssignDealDamage, AbilityKey.mapFromAffected(fighterB));

        // 701.12c If a creature fights itself, it deals damage to itself equal to twice its power.

        final int dmg1 = fighterA.getNetPower();
        if (fighterA.equals(fighterB)) {
            damageMap.put(fighterA, fighterA, dmg1 * 2);
        } else {
            final int dmg2 = fighterB.getNetPower();

            damageMap.put(fighterA, fighterB, dmg1);
            damageMap.put(fighterB, fighterA, dmg2);
            fighterB.setFoughtThisTurn(true);
        }
        fighterA.setFoughtThisTurn(true);

        if (!usedDamageMap) {
            sa.getHostCard().getGame().getAction().dealDamage(false, damageMap, preventMap, counterTable, sa);
        }

        replaceDying(sa);
    }
}
