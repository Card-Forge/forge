package forge.game.ability.effects;

import forge.GameCommand;
import forge.ImageKeys;
import forge.card.CardRarity;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.Map;

public class RingTemptsYouEffect extends EffectEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "The Ring tempts " + sa.getActivatingPlayer() + ".";
    }

    @Override
    public void resolve(SpellAbility sa) {
        Player p = sa.getActivatingPlayer();
        Game game = p.getGame();
        Card card = sa.getHostCard();

        int level = p.getNumRingTemptedYou();
        if (p.getTheRing() == null) {
            // Create the ring with first level
            String image = ImageKeys.getTokenKey("the_ring");
            Card theRing = createEffect(sa, p, "The Ring", image);
            theRing.setSetCode(card.getSetCode());
            theRing.setRarity(CardRarity.Common);
            theRing.setColor(MagicColor.COLORLESS);

            game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
            p.setTheRing(p.getGame().getAction().moveTo(p.getZone(ZoneType.Command), theRing, sa));
            game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        }

        level += 1;

        switch(level) {
            case 1:
                String legendary = "Mode$ Continuous | EffectZone$ Command | Affected$ Card.YouCtrl+IsRingbearer | AddType$ Legendary | Description$ Your Ring-bearer is legendary.";
                String cantBeBlocked = "Mode$ CantBlockBy | EffectZone$ Command | ValidAttacker$ Card.YouCtrl+IsRingbearer | ValidBlockerRelative$ Creature.powerGTX | Description$ Your Ring-bearer can't be blocked by creatures with greater power.";
                p.getTheRing().addStaticAbility(legendary);
                StaticAbility st = p.getTheRing().addStaticAbility(cantBeBlocked);
                st.setSVar("X", "Count$CardPower");
                break;
            case 2:
                final String attackTrig = "Mode$ Attacks | ValidCard$ Card.YouCtrl+IsRingbearer | TriggerDescription$ Whenever your ring-bearer attacks, draw a card, then discard a card. | TriggerZones$ Command";
                final String drawEffect = "DB$ Draw | Defined$ You | NumCards$ 1";
                final String discardEffect = "DB$ Discard | Defined$ You | NumCards$ 1 | Mode$ TgtChoose";

                final Trigger attackTrigger = TriggerHandler.parseTrigger(attackTrig, p.getTheRing(), true);

                SpellAbility drawExecute = AbilityFactory.getAbility(drawEffect, p.getTheRing());
                AbilitySub discardExecute = (AbilitySub) AbilityFactory.getAbility(discardEffect, p.getTheRing());

                drawExecute.setSubAbility(discardExecute);
                attackTrigger.setOverridingAbility(drawExecute);
                p.getTheRing().addTrigger(attackTrigger);

                break;
            case 3:
                final String becomesBlockedTrig = "Mode$ AttackerBlockedByCreature | ValidCard$ Card.YouCtrl+IsRingbearer| ValidBlocker$ Creature | TriggerZones$ Command | TriggerDescription$ Whenever your Ring-bearer becomes blocked a creature, that creature's controller sacrifices it at the end of combat.";
                final String endOfCombatTrig = "DB$ DelayedTrigger | Mode$ Phase | Phase$ EndCombat | RememberObjects$ TriggeredBlockerLKICopy | TriggerDescription$ At end of combat, the controller of the creature that blocked CARDNAME sacrifices that creature.";
                final String sacBlockerEffect = "DB$ Destroy | Defined$ DelayTriggerRememberedLKI | Sacrifice$ True";

                final Trigger becomesBlockedTrigger = TriggerHandler.parseTrigger(becomesBlockedTrig, p.getTheRing(), true);

                SpellAbility endCombatExecute = AbilityFactory.getAbility(endOfCombatTrig, p.getTheRing());
                AbilitySub sacExecute = (AbilitySub) AbilityFactory.getAbility(sacBlockerEffect, p.getTheRing());

                endCombatExecute.setAdditionalAbility("Execute", sacExecute);
                becomesBlockedTrigger.setOverridingAbility(endCombatExecute);
                p.getTheRing().addTrigger(becomesBlockedTrigger);

                break;
            case 4:
                final String damageTrig = "Mode$ DamageDone | ValidSource$ Card.YouCtrl+IsRingbearer | ValidTarget$ Player | CombatDamage$ True | TriggerZones$ Command | TriggerDescription$ Whenever your Ring-bearer deals combat damage to a player, each opponent loses 3 life.";
                final String loseEffect = "DB$ LoseLife | Defined$ Opponent | LifeAmount$ 3";

                final Trigger damageTrigger = TriggerHandler.parseTrigger(damageTrig, p.getTheRing(), true);
                SpellAbility loseExecute = AbilityFactory.getAbility(loseEffect, p.getTheRing());

                damageTrigger.setOverridingAbility(loseExecute);
                p.getTheRing().addTrigger(damageTrigger);

                break;
            default:
                break;
        }

        // Then choose a ring-bearer (You may keep the same one). Auto pick if <2 choices.
        CardCollection creatures = p.getCreaturesInPlay();
        Card ringBearer = p.getController().chooseSingleEntityForEffect(creatures, sa, "Choose your Ring-bearer", false, null);
        p.setRingBearer(ringBearer);

        // 701.52a That creature becomes your Ring-bearer until another player gains control of it.
        if (ringBearer != null) {
            GameCommand loseCommand = new GameCommand() {

                private static final long serialVersionUID = 1L;

                @Override
                public void run() {
                    if (ringBearer.isRingBearer()) {
                        p.clearRingBearer();
                    }
                }
            };
            ringBearer.addChangeControllerCommand(loseCommand);
            ringBearer.addLeavesPlayCommand(loseCommand);
        }

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
        runParams.put(AbilityKey.Card, ringBearer);
        game.getTriggerHandler().runTrigger(TriggerType.RingTemptsYou, runParams, false);
        // make sure it shows up in the command zone
        p.getTheRing().updateStateForView();
        //increment ring tempted you for property
        p.incrementRingTemptedYou();
    }
}
