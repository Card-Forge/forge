package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardDamageMap;
import forge.game.card.CardUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.Localizer;

public class DamageDealEffect extends DamageBaseEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility spellAbility) {
        // when damageStackDescription is called, just build exactly what is happening
        final StringBuilder stringBuilder = new StringBuilder();
        final String damage = spellAbility.getParam("NumDmg");
        int dmg;
        try { // try-catch to fix Volcano Hellion Crash
            dmg = AbilityUtils.calculateAmount(spellAbility.getHostCard(), damage, spellAbility);
        } catch (NullPointerException e) {
            dmg = 0;
        }

        List<GameObject> targets = SpellAbilityEffect.getTargets(spellAbility);
        if (targets.isEmpty()) {
            return "";
        }

        final List<Card> definedSources = AbilityUtils.getDefinedCards(spellAbility.getHostCard(), spellAbility.getParam("DamageSource"), spellAbility);

        if (!definedSources.isEmpty() && definedSources.get(0) != spellAbility.getHostCard()) {
            stringBuilder.append(definedSources.get(0).toString()).append(" deals");
        } else {
            stringBuilder.append("Deals");
        }

        stringBuilder.append(" ").append(dmg).append(" damage ");

        // if use targeting we show all targets and corresponding damage
        if (spellAbility.usesTargeting()) {
            if (spellAbility.hasParam("DivideEvenly")) {
                stringBuilder.append("divided evenly (rounded down) to\n");
            } else if (spellAbility.isDividedAsYouChoose()) {
                stringBuilder.append("divided to\n");
            } else
                stringBuilder.append("to ");

            final List<Card> targetCards = SpellAbilityEffect.getTargetCards(spellAbility);
            final List<Player> players = SpellAbilityEffect.getTargetPlayers(spellAbility);

            int targetCount = targetCards.size() + players.size();

            // target cards
            for (int i = 0; i < targetCards.size(); i++) {
                Card targetCard = targetCards.get(i);
                stringBuilder.append(targetCard);
                Integer v = spellAbility.getDividedValue(targetCard);
                if (v != null) //fix null damage stack description
                    stringBuilder.append(" (").append(v).append(" damage)");

                if (i == targetCount - 2) {
                    stringBuilder.append(" and ");
                } else if (i + 1 < targetCount) {
                    stringBuilder.append(", ");
                }
            }

            // target players
            for (int i = 0; i < players.size(); i++) {
                Player targetPlayer = players.get(i);
                stringBuilder.append(targetPlayer);
                Integer v = spellAbility.getDividedValue(targetPlayer);
                if (v != null) //fix null damage stack description
                    stringBuilder.append(" (").append(v).append(" damage)");

                if (i == players.size() - 2) {
                    stringBuilder.append(" and ");
                } else if (i + 1 < players.size()) {
                    stringBuilder.append(", ");
                }
            }

        } else {
            if (spellAbility.hasParam("DivideEvenly")) {
                stringBuilder.append("divided evenly (rounded down) ");
            } else if (spellAbility.isDividedAsYouChoose()) {
                stringBuilder.append("divided as you choose ");
            }
            stringBuilder.append("to ").append(Lang.joinHomogenous(targets));
        }

        if (spellAbility.hasParam("Radiance")) {
            stringBuilder.append(" and each other ").append(spellAbility.getParam("ValidTgts"))
                    .append(" that shares a color with ");
            if (targets.size() > 1) {
                stringBuilder.append("them");
            } else {
                stringBuilder.append("it");
            }
        }

        stringBuilder.append(".");
        return stringBuilder.toString();
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();

        final List<Card> definedSources = AbilityUtils.getDefinedCards(hostCard, sa.getParam("DamageSource"), sa);
        if (definedSources == null || definedSources.isEmpty()) {
            return;
        }

        for (Card source : definedSources) {
            // Run replacement effects
            game.getReplacementHandler().run(ReplacementType.AssignDealDamage, AbilityKey.mapFromAffected(source));
        }

        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(hostCard, damage, sa);

        final boolean removeDamage = sa.hasParam("Remove");
        final boolean divideOnResolution = sa.hasParam("DividerOnResolution");

        List<GameObject> tgts = getTargets(sa);
        if (sa.hasParam("OptionalDecider")) {
            Player decider = Iterables.getFirst(AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("OptionalDecider"), sa), null);
            if (decider != null && !decider.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoyouWantDealTargetDamageToTarget", String.valueOf(dmg), tgts.toString()), null)) {
                return;
            }
        }

        // Right now for Fireball, maybe later for other stuff
        if (sa.hasParam("DivideEvenly")) {
            String evenly = sa.getParam("DivideEvenly");
            if (evenly.equals("RoundedDown")) {
                dmg = tgts.isEmpty() ? 0 : dmg / tgts.size();
            }
        }

        final CardCollection untargetedCards = CardUtil.getRadiance(sa);

        final boolean remember = sa.hasParam("RememberDamaged");

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
        if (sa.hasParam("DamageMap")) {
            sa.setDamageMap(damageMap);
            sa.setPreventMap(preventMap);
            sa.setCounterTable(counterTable);
            usedDamageMap = true;
        }

        for (Card source : definedSources) {
            final Card sourceLKI = hostCard.getGame().getChangeZoneLKIInfo(source);

            if (divideOnResolution) {
                // Dividing Damage up to multiple targets using combat damage box
                // Currently only used for Master of the Wild Hunt
                List<Player> players = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("DividerOnResolution"), sa);
                if (players.isEmpty()) {
                    return;
                }

                CardCollection assigneeCards = new CardCollection();
                // Do we have a way of doing this in a better fashion?
                for (GameObject obj : tgts) {
                    if (obj instanceof Card) {
                        assigneeCards.add((Card) obj);
                    }
                }

                Player assigningPlayer = players.get(0);
                Map<Card, Integer> map = assigningPlayer.getController().assignCombatDamage(sourceLKI, assigneeCards, dmg, null, true);
                for (Entry<Card, Integer> dt : map.entrySet()) {
                    damageMap.put(sourceLKI, dt.getKey(), dt.getValue());
                }

                if (!usedDamageMap) {
                    game.getAction().dealDamage(false, damageMap, preventMap, counterTable, sa);
                }
                replaceDying(sa);
                return;
            }

            if (sa.hasParam("RelativeTarget")) {
                tgts = AbilityUtils.getDefinedObjects(source, sa.getParam("Defined"), sa);
            }

            for (final GameObject o : tgts) {
                if (!removeDamage) {
                    dmg = (sa.usesTargeting() && sa.isDividedAsYouChoose()) ? sa.getDividedValue(o) : dmg;
                    if (dmg <= 0) {
                        continue;
                    }
                }
                if (o instanceof Card) {
                    final Card c = (Card) o;
                    final Card gc = game.getCardState(c, null);
                    if (gc == null || !c.equalsWithTimestamp(gc) || !gc.isInPlay()) {
                        // timestamp different or not in play
                        continue;
                    }
                    if (!sa.usesTargeting() || gc.canBeTargetedBy(sa)) {
                        internalDamageDeal(sa, sourceLKI, gc, dmg, damageMap);
                    }
                } else if (o instanceof Player) {
                    final Player p = (Player) o;
                    if (!sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                        damageMap.put(sourceLKI, p, dmg);
                    }
                }
            }
            for (final Card unTgtC : untargetedCards) {
                if (unTgtC.isInPlay()) {
                    internalDamageDeal(sa, sourceLKI, unTgtC, dmg, damageMap);
                }
            }

            if (remember) {
                source.addRemembered(damageMap.row(sourceLKI).keySet());
            }
        }
        if (!usedDamageMap) {
            game.getAction().dealDamage(false, damageMap, preventMap, counterTable, sa);
        }
        replaceDying(sa);
    }

    protected void internalDamageDeal(SpellAbility sa, Card sourceLKI, Card c, int dmg, CardDamageMap damageMap) {
        final Card hostCard = sa.getHostCard();
        final Player activationPlayer = sa.getActivatingPlayer();

        if (sa.hasParam("Remove")) {
            c.setDamage(0);
            c.setHasBeenDealtDeathtouchDamage(false);
            c.clearAssignedDamage();
        } else {
            if (sa.hasParam("ExcessDamage") && (!sa.hasParam("ExcessDamageCondition") ||
                    sourceLKI.isValid(sa.getParam("ExcessDamageCondition").split(","), activationPlayer, hostCard, sa))) {
                int lethal = c.getLethalDamage();
                if (sourceLKI.hasKeyword(Keyword.DEATHTOUCH)) {
                    lethal = Math.min(lethal, 1);
                }
                int dmgToTarget = Math.min(lethal, dmg);

                damageMap.put(sourceLKI, c, dmgToTarget);

                List<GameEntity> list = Lists.newArrayList();
                list.addAll(AbilityUtils.getDefinedCards(hostCard, sa.getParam("ExcessDamage"), sa));
                list.addAll(AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("ExcessDamage"), sa));

                if (!list.isEmpty()) {
                    damageMap.put(sourceLKI, list.get(0), dmg - dmgToTarget);
                }
            } else {
                damageMap.put(sourceLKI, c, dmg);
            }
        }
    }
}
