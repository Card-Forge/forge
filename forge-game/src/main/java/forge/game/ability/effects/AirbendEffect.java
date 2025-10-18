package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Iterables;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Lang;

public class AirbendEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder("Airbend ");

        Iterable<Card> tgts;
        if (sa.usesTargeting()) {
            tgts = getCardsfromTargets(sa);
        } else { // otherwise add self to list and go from there
            tgts = sa.knownDetermineDefined(sa.getParam("Defined"));
        }

        sb.append(sa.getParamOrDefault("DefinedDesc", Lang.joinHomogenous(tgts)));
        sb.append(".");
        if (Iterables.size(tgts) > 1) {
            sb.append(" (Exile them. While each one is exiled, its owner may cast it for {2} rather than its mana cost.)");
        } else {
            sb.append(" (Exile it. While it’s exiled, its owner may cast it for {2} rather than its mana cost.)");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final Player pl = sa.getActivatingPlayer();

        final CardZoneTable triggerList = CardZoneTable.getSimultaneousInstance(sa);

        for (Card c : getTargetCards(sa)) {
            final Card gameCard = game.getCardState(c, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !c.equalsWithGameTimestamp(gameCard) || gameCard.isPhasedOut()) {
                continue;
            }

            if (!gameCard.canExiledBy(sa, true)) {
                continue;
            }
            handleExiledWith(gameCard, sa);

            Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
            AbilityKey.addCardZoneTableParams(moveParams, triggerList);

            Card movedCard = game.getAction().exile(gameCard, sa, moveParams);

            if (movedCard == null || !movedCard.isInZone(ZoneType.Exile)) {
                continue;
            }

            // Effect to cast for 2 from exile
            Card eff = createEffect(sa, movedCard.getOwner(), "Airbend" + movedCard, hostCard.getImageKey());
            eff.addRemembered(movedCard);

            StringBuilder sbPlay = new StringBuilder();
            sbPlay.append("Mode$ Continuous | MayPlay$ True | MayPlayAltManaCost$ 2 | EffectZone$ Command | Affected$ Card.IsRemembered+nonLand");
            sbPlay.append(" | AffectedZone$ Exile | Description$ You may cast the card.");
            eff.addStaticAbility(sbPlay.toString());

            addForgetOnMovedTrigger(eff, "Exile");
            addForgetOnCastTrigger(eff, "Card.IsRemembered");

            game.getAction().moveToCommand(eff, sa);
        }
        triggerList.triggerChangesZoneAll(game, sa);
        handleExiledWith(triggerList.allCards(), sa);

        pl.triggerElementalBend(TriggerType.Airbend);
    }

}
