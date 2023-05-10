package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.collect.FCollection;

public class DamagePreventEffect extends DamagePreventEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<GameEntity> tgts = getTargetEntities(sa);

        sb.append("Prevent the next ");
        sb.append(sa.getParam("Amount"));
        sb.append(" damage that would be dealt ");
        if (sa.isDividedAsYouChoose()) {
            sb.append("between ");
        } else {
            sb.append("to ");
        }
        for (int i = 0; i < tgts.size(); i++) {
            if (i != 0) {
                sb.append(" ");
            }

            final Object o = tgts.get(i);
            if (o instanceof Card) {
                final Card tgtC = (Card) o;
                if (tgtC.isFaceDown()) {
                    sb.append("Morph");
                } else {
                    sb.append(tgtC);
                }
            } else if (o instanceof Player) {
                sb.append(o.toString());
            }
        }

        if (sa.hasParam("Radiance") && (sa.usesTargeting())) {
            sb.append(" and each other ").append(sa.getParam("ValidTgts"))
                    .append(" that shares a color with ");
            if (tgts.size() > 1) {
                sb.append("them");
            } else {
                sb.append("it");
            }
        }
        sb.append(" this turn.");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card host = sa.getHostCard();
        int numDam = AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa);

        List<GameEntity> tgts = Lists.newArrayList();
        if (sa.hasParam("CardChoices") || sa.hasParam("PlayerChoices")) { // choosing outside Defined/Targeted
            // only for Whimsy, for more robust version see DamageDealEffect
            FCollection<GameEntity> choices = new FCollection<>();
            if (sa.hasParam("CardChoices")) {
                choices.addAll(CardLists.getValidCards(host.getGame().getCardsIn(ZoneType.Battlefield),
                        sa.getParam("CardChoices"), sa.getActivatingPlayer(), host, sa));
            }
            if (sa.hasParam("PlayerChoices")) {
                choices.addAll(AbilityUtils.getDefinedPlayers(host, sa.getParam("PlayerChoices"), sa));
            }
            if (sa.hasParam("Random")) { // currently everything using Choices is random
                GameEntity random = Aggregates.random(choices);
                tgts.add(random);
                host.addRemembered(random); // remember random choices for log
            }
        } else {
            tgts = getTargetEntities(sa);
        }

        final CardCollection untargetedCards = CardUtil.getRadiance(sa);

        for (final GameEntity o : tgts) {
            numDam = sa.usesTargeting() && sa.isDividedAsYouChoose() ? sa.getDividedValue(o) : numDam;
            if (o instanceof Card) {
                final Card c = (Card) o;
                if (c.isInPlay()) {
                    addPreventNextDamage(sa, o, numDam);
                }
            } else if (o instanceof Player) {
                addPreventNextDamage(sa, o, numDam);
            }
        }

        for (final Card c : untargetedCards) {
            if (c.isInPlay()) {
                addPreventNextDamage(sa, c, numDam);
            }
        }
    }
}
