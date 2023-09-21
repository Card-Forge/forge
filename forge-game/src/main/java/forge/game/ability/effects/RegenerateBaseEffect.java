package forge.game.ability.effects;

import java.util.Collection;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public abstract class RegenerateBaseEffect extends SpellAbilityEffect {

    public void createRegenerationEffect(SpellAbility sa, final Collection<Card> list) {
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
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, eff, sa, AbilityKey.newMap());
        eff.updateStateForView();
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        final GameCommand untilEOT = new GameCommand() {
            private static final long serialVersionUID = 259368227093961103L;

            @Override
            public void run() {
                game.getAction().exile(eff, null, null);
            }
        };
        game.getEndOfTurn().addUntil(untilEOT);
    }
}
