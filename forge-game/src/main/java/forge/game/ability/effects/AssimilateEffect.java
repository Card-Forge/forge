package forge.game.ability.effects;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

/**
 * CR 701.x assimilate: the card ends up under your control as a Borg artifact creature with a
 * +1/+1 counter, but how it gets there depends on where it started - a card in a graveyard has to
 * be moved, a permanent only changes hands.
 */
public class AssimilateEffect extends SpellAbilityEffect {

    private static final String ANIMATE = "Mode$ Continuous | Affected$ Card.IsRemembered"
            + " | AddType$ Artifact & Creature & Borg | RemoveCreatureTypes$ True";

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Assimilate " + Lang.joinHomogenous(getCardsfromTargets(sa)) + ".";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        final Player controller = sa.getActivatingPlayer();
        final CounterType p1p1 = CounterEnumType.P1P1;

        final CardZoneTable triggerList = new CardZoneTable();
        final GameEntityCounterTable alreadyInPlay = new GameEntityCounterTable();

        for (final Card tgt : getCardsfromTargets(sa)) {
            final Card gameCard = game.getCardState(tgt, null);
            if (gameCard == null || !tgt.equalsWithGameTimestamp(gameCard) || gameCard.isPhasedOut()) {
                continue;
            }

            // has to happen before any move, or the card enters under the wrong controller
            if (!controller.equals(gameCard.getController())) {
                gameCard.runChangeControllerCommands();
                gameCard.setController(controller, game.getNextTimestamp());
            }

            if (gameCard.isInPlay()) {
                gameCard.addCounter(p1p1, 1, controller, alreadyInPlay);
                animateInPlace(gameCard, sa, game);
            } else {
                // riding the zone change is what makes the types apply as the card enters, so a
                // "whenever an artifact enters" trigger sees it as one
                sa.putParam("StaticEffect", "AssimilateAnimate");
                sa.setSVar("AssimilateAnimate", ANIMATE);

                final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                AbilityKey.addCardZoneTableParams(moveParams, triggerList);
                moveParams.put(AbilityKey.SimultaneousETB, entering(sa));
                final GameEntityCounterTable counters = new GameEntityCounterTable();
                counters.put(controller, gameCard, p1p1, 1);
                moveParams.put(AbilityKey.CounterTable, counters);
                game.getAction().moveToPlay(gameCard, controller, sa, moveParams);
            }
        }

        alreadyInPlay.replaceCounterEffect(game, sa);
        triggerList.triggerChangesZoneAll(game, sa);
    }

    /** Everything being assimilated out of a hidden zone enters together. */
    private static CardCollection entering(final SpellAbility sa) {
        final CardCollection cards = new CardCollection();
        for (final Card c : getCardsfromTargets(sa)) {
            if (!c.isInPlay()) {
                cards.add(c);
            }
        }
        return cards;
    }

    /** Nothing is entering, so there is no zone change for the static effect to ride on. */
    private void animateInPlace(final Card c, final SpellAbility sa, final Game game) {
        final CardType add = new CardType(true);
        add.addAll(Arrays.asList("Artifact", "Creature", "Borg"));
        sa.putParam("RemoveCreatureTypes", "True");
        AnimateEffectBase.doAnimate(c, sa, null, null, add, new CardType(true), null,
                Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(),
                Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(),
                game.getNextTimestamp(), "Permanent");
    }
}
