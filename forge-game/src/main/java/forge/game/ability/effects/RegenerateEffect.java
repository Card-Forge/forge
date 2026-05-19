package forge.game.ability.effects;

import java.util.Collection;
import java.util.List;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class RegenerateEffect extends SpellAbilityEffect {

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

        if (!tgtCards.isEmpty()) {
            sb.append("Regenerate ");
            sb.append(Lang.joinHomogenous(tgtCards));
            sb.append(".");
        }

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        CardCollection result = new CardCollection();

        for (Card c : getDefinedCardsOrTargeted(sa)) {
            if (!c.isInPlay()) {
                continue;
            }

            // check if the object is still in game or if it was moved
            Card gameCard = game.getCardState(c, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !c.equalsWithGameTimestamp(gameCard)) {
                continue;
            }
            result.add(gameCard);
        }
        // create Effect for Regeneration
        createRegenerationEffect(sa, result);
    }

    private void createRegenerationEffect(SpellAbility sa, final Collection<Card> list) {
        if (list.isEmpty()) {
            return;
        }
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();

        // create Effect for Regeneration
        final Card eff = createEffect(
                sa, sa.getActivatingPlayer(), hostCard + "'s Regeneration", hostCard.getImageKey());

        eff.addRemembered(list);
        addForgetOnMovedTrigger(eff, "Battlefield");

        // build ReplacementEffect
        String repeffstr = "Event$ Destroy | ActiveZones$ Command | ValidCard$ Card.IsRemembered | Regeneration$ True"
                + " | Description$ Regeneration (if creature would be destroyed, regenerate it instead)";

        String effect = "DB$ Regeneration | Defined$ ReplacedCard";
        String exileEff = "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile"
                + " | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ EQ0";
        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);

        SpellAbility saReg = AbilityFactory.getAbility(effect, eff);
        AbilitySub saExile = (AbilitySub)AbilityFactory.getAbility(exileEff, eff);

        if (sa.hasAdditionalAbility("RegenerationAbility")) {
            AbilitySub trigSA = (AbilitySub)sa.getAdditionalAbility("RegenerationAbility").copy(eff, sa.getActivatingPlayer(), false);
            saExile.setSubAbility(trigSA);
        }

        saReg.setSubAbility(saExile);
        re.setOverridingAbility(saReg);
        eff.addReplacementEffect(re);

        // add extra Remembered
        if (sa.hasParam("RememberObjects")) {
            eff.addRemembered(AbilityUtils.getDefinedObjects(hostCard, sa.getParam("RememberObjects"), sa));
        }

        // Copy text changes
        if (sa.isIntrinsic()) {
            eff.copyChangedTextFrom(hostCard);
        }

        // add RegenEffect as Shield to the Affected Cards
        for (final Card c : list) {
            c.incShieldCount();
        }
        game.getAction().moveToCommand(eff, sa);

        game.getEndOfTurn().addUntil(() -> game.getAction().exileEffect(eff));
    }

}
