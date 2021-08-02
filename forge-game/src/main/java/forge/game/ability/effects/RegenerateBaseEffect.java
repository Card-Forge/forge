package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public abstract class RegenerateBaseEffect extends SpellAbilityEffect {

    public void createRegenerationEffect(SpellAbility sa, final Iterable<Card> list) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();

        // create Effect for Regeneration
        final Card eff = createEffect(
                sa, sa.getActivatingPlayer(), hostCard.getName() + "'s Regeneration", hostCard.getImageKey());
        
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
        AbilitySub saExile =  (AbilitySub)AbilityFactory.getAbility(exileEff, eff);

        saReg.setSubAbility(saExile);
        re.setOverridingAbility(saReg);
        eff.addReplacementEffect(re);
        
        // add extra Remembered
        if (sa.hasParam("RememberObjects")) {
            eff.addRemembered(AbilityUtils.getDefinedObjects(hostCard, sa.getParam("RememberObjects"), sa));
        }
        
        if (sa.hasParam("RegenerationTrigger")) {
            final String str = sa.getSVar(sa.getParam("RegenerationTrigger")); 
            
            SpellAbility trigSA = AbilityFactory.getAbility(str, eff);
            
            final String trigStr = "Mode$ Regenerated | ValidCause$ Effect.Self | TriggerZones$ Command "
                    + " | TriggerDescription$ " + trigSA.getDescription();
            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, eff, true);
            trigger.setOverridingAbility(trigSA);
            eff.addTrigger(trigger);
        }
        
        // Copy text changes
        if (sa.isIntrinsic()) {
            eff.copyChangedTextFrom(hostCard);
        }

        eff.updateStateForView();

        // add RegenEffect as Shield to the Affected Cards
        for (final Card c : list) {
            c.addShield(eff);
        }
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, eff, sa);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        
        final GameCommand untilEOT = new GameCommand() {
            private static final long serialVersionUID = 259368227093961103L;

            @Override
            public void run() {
                game.getAction().exile(eff, null);
            }
        };
        game.getEndOfTurn().addUntil(untilEOT);
    }
}
