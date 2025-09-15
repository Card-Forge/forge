package forge.game.ability.effects;

import java.util.Arrays;
import java.util.EnumSet;

import forge.card.RemoveType;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCopyService;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.util.Lang;

public class EarthbendEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder("Earthbend ");
        final Card card = sa.getHostCard();
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);

        sb.append(amount).append(". (Target land you control becomes a 0/0 creature with haste that's still a land. Put  ");
        sb.append(Lang.nounWithNumeral(amount, "+1/+1 counter"));
        sb.append(" on it. When it dies or is exiled, return it to the battlefield tapped.)");

        return sb.toString();
    }

    @Override
    public void buildSpellAbility(final SpellAbility sa) {
        TargetRestrictions abTgt = new TargetRestrictions("Select target land you control", "Land.YouCtrl".split(","), "1", "1");
        sa.setTargetRestrictions(abTgt);
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final Player pl = sa.getActivatingPlayer();
        int num = AbilityUtils.calculateAmount(source, sa.getParamOrDefault("Num", "1"), sa);

        long ts = game.getNextTimestamp();

        String desc = "When it dies or is exiled, return it to the battlefield tapped.";
        String sbTrigA = "Mode$ ChangesZone | ValidCard$ Card.IsTriggerRemembered | Origin$ Battlefield | Destination$ Graveyard | TriggerDescription$ " + desc;
        String sbTrigB = "Mode$ Exiled | Origin$ Battlefield | ValidCard$ Card.IsTriggerRemembered | TriggerZones$ Battlefield | TriggerDescription$ " + desc;

        // Earthbend should only target one land
        for (Card c : getTargetCards(sa)) {
            c.addNewPT(0, 0, ts, 0);
            c.addChangedCardTypes(Arrays.asList("Creature"), null, false, EnumSet.noneOf(RemoveType.class), ts, 0, true, false);
            c.addChangedCardKeywords(Arrays.asList("Haste"), null, false, ts, null);

            GameEntityCounterTable table = new GameEntityCounterTable();
            c.addCounter(CounterEnumType.P1P1, num, pl, table);
            table.replaceCounterEffect(game, sa, true);

            buildTrigger(sa, c, sbTrigA, "Graveyard");
            buildTrigger(sa, c, sbTrigB, "Exile");
        }
        pl.triggerElementalBend(TriggerType.Earthbend);
    }

    protected void buildTrigger(SpellAbility sa, Card c, String sbTrig, String zone) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        String trigSA = "DB$ ChangeZone | Defined$ DelayTriggerRemembered | Origin$ " + zone + " | Destination$ Battlefield | Tapped$ True";

        final Trigger trig = TriggerHandler.parseTrigger(sbTrig, CardCopyService.getLKICopy(source), sa.isIntrinsic());
        final SpellAbility newSa = AbilityFactory.getAbility(trigSA, sa.getHostCard());
        newSa.setIntrinsic(sa.isIntrinsic());
        trig.addRemembered(c);
        trig.setOverridingAbility(newSa);
        trig.setSpawningAbility(sa.copy(sa.getHostCard(), true));
        trig.setKeyword(trig.getSpawningAbility().getKeyword());

        game.getTriggerHandler().registerDelayedTrigger(trig);
    }
}
