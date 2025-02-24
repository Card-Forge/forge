package forge.game.ability.effects;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.Map;

public class UntapEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();

        sb.append("Untap ");

        if (sa.hasParam("UntapUpTo")) {
            sb.append("up to ").append(sa.getParam("Amount")).append(" ");
            sb.append(sa.getParam("UntapType")).append("s");
        } else {
            sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final boolean etb = sa.hasParam("ETB");
        final Game game = sa.getHostCard().getGame();

        if (sa.hasParam("UntapUpTo")) {
            untapChoose(sa, false);
        } else if (sa.hasParam("UntapExactly")) {
            untapChoose(sa, true);
        } else {
            final CardCollection affectedCards = getTargetCards(sa);
            affectedCards.addAll(CardUtil.getRadiance(sa));

            CardCollection untapped = new CardCollection();
            for (final Card tgtC : affectedCards) {
                if (tgtC.isPhasedOut()) {
                    continue;
                }
                if (tgtC.isInPlay()) {
                    // check if the object is still in game or if it was moved
                    Card gameCard = game.getCardState(tgtC, null);
                    // gameCard is LKI in that case, the card is not in game anymore
                    // or the timestamp did change
                    // this should check Self too
                    if (gameCard == null || !tgtC.equalsWithGameTimestamp(gameCard)) {
                        continue;
                    }
                    if (gameCard.untap(true)) untapped.add(gameCard);
                }
                if (etb) {
                    // do not fire triggers
                    tgtC.setTapped(false);
                }
            }
            if (!untapped.isEmpty() && !etb) {
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                final Map<Player, CardCollection> map = Maps.newHashMap();
                map.put(activator, untapped);
                runParams.put(AbilityKey.Map, map);
                activator.getGame().getTriggerHandler().runTrigger(TriggerType.UntapAll, runParams, false);
            }
        }
    }

    /**
     * <p>
     * Choose cards to untap.
     * </p>
     *
     * @param sa
     *            a {@link SpellAbility}.
     * @param mandatory
     *            whether the untapping is mandatory.
     */
    private static void untapChoose(final SpellAbility sa, final boolean mandatory) {
        final Map<Player, CardCollection> map = Maps.newHashMap();
        final int num = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa);
        final String valid = sa.getParam("UntapType");

        for (final Player p : AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa)) {
            if (!p.isInGame()) {
                continue;
            }

            CardCollectionView list = CardLists.getValidCards(p.getGame().getCardsIn(ZoneType.Battlefield),
                    valid, sa.getActivatingPlayer(), sa.getHostCard(), sa);
            // the few mandatory are handled differently
            if (!mandatory) {
                list = CardLists.filter(list, CardPredicates.TAPPED);
            }

            CardCollection untapped = new CardCollection();
            final CardCollectionView selected = p.getController().chooseCardsForEffect(list, sa, Localizer.getInstance().getMessage("lblSelectCardToUntap"), mandatory ? num : 0, num, !mandatory, null);
            if (selected != null) {
                for (final Card c : selected) {
                    if (c.untap(true)) untapped.add(c);
                }
            }
            if (!untapped.isEmpty()) {
                map.put(p, untapped);
            }
        }
        if (!map.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Map, map);
            sa.getActivatingPlayer().getGame().getTriggerHandler()
                    .runTrigger(TriggerType.UntapAll, runParams, false);
        }
    }

}
