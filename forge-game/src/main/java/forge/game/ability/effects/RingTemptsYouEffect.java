package forge.game.ability.effects;

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
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.Map;

public class RingTemptsYouEffect extends EffectEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("The Ring tempts " + sa.getActivatingPlayer());
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        Player p = sa.getActivatingPlayer();
        Game game = p.getGame();
        Card card = sa.getHostCard();
        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
        game.getTriggerHandler().runTrigger(TriggerType.RingTemptsYou, runParams, false);
        Card theRing = null;
        for (Card c : p.getCardsIn(ZoneType.Command)) {
            if (c.getName().equals("The Ring")) {
                theRing = c;
                break;
            }
        }

        int level = 0;
        if (theRing == null) {
            // Create the ring with first level
            String image = ImageKeys.getTokenKey("the_ring");
            theRing = createEffect(sa, p, "The Ring", image);
            theRing.setSetCode(card.getSetCode());
            theRing.setRarity(CardRarity.Common);
            theRing.setColor(MagicColor.COLORLESS);

            game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
            theRing = p.getGame().getAction().moveTo(p.getZone(ZoneType.Command), theRing, sa);
            game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        } else {
            level = Integer.parseInt(theRing.getSVar("RingLevel"));
        }

        level += 1;
        theRing.setSVar("RingLevel", String.valueOf(Math.min(level, 4)));

        switch(level) {
            case 1:
                String legendary = "Mode$ Continuous | EffectZone$ Command | Affected$ Card.IsRemembered | AddType$ Legendary | AddDesignation$ Ring-bearer | Description$ Your Ring-bearer is legendary.";
                String cantBeBlocked = "Mode$ CantBlockBy | EffectZone$ Command | ValidAttacker$ Card.IsRemembered | ValidBlocker$ Creature.powerGTX | Description$ Your Ring-bearer can't be blocked by creatures with greater power.";
                theRing.addStaticAbility(legendary);
                theRing.setSVar("X", "Remembered$CardPower");
                theRing.addStaticAbility(cantBeBlocked);
                break;
            case 2:
                final String attackTrig = "Mode$ Attacks | ValidCard$ Card.IsRemembered | TriggerDescription$ Whenever your ring-bearer attacks, draw a card, then discard a card. | TriggerZones$ Command";
                final String drawEffect = "DB$ Draw | Defined$ You | NumCards$ 1";
                final String discardEffect = "DB$ Discard | Defined$ You | NumCards$ 1 | Mode$ TgtChoose";

                final Trigger attackTrigger = TriggerHandler.parseTrigger(attackTrig, theRing, true);

                SpellAbility drawExecute = AbilityFactory.getAbility(drawEffect, card);
                AbilitySub discardExecute = (AbilitySub) AbilityFactory.getAbility(discardEffect, card);

                drawExecute.setSubAbility(discardExecute);
                attackTrigger.setOverridingAbility(drawExecute);
                theRing.addTrigger(attackTrigger);

                break;
            case 3:
                final String becomesBlockedTrig = "Mode$ AttackerBlockedByCreature | ValidCard$ Card.IsRemembered | ValidBlocker$ Creature | TriggerZones$ Command | Execute$ TrigEndCombat | TriggerDescription$ Whenever your Ring-bearer becomes blocked a creature, that creature's controller sacrifices it at the end of combat.";
                final String endOfCombatTrig = "DB$ DelayedTrigger | Mode$ Phase | Phase$ EndCombat | Execute$ TrigSacBlocker | RememberObjects$ TriggeredBlockerLKICopy | TriggerDescription$ At end of combat, the controller of the creature that blocked CARDNAME sacrifices that creature.";
                final String sacBlockerEffect = "DB$ Destroy | Defined$ DelayTriggerRememberedLKI | Sacrifice$ True";
                theRing.setSVar("TrigEndCombat", endOfCombatTrig);
                theRing.setSVar("TrigSacBlocker", sacBlockerEffect);
                final Trigger becomesBlockedTrigger = TriggerHandler.parseTrigger(becomesBlockedTrig, theRing, true);

                SpellAbility endCombatExecute = AbilityFactory.getAbility(endOfCombatTrig, theRing);
                AbilitySub sacExecute = (AbilitySub) AbilityFactory.getAbility(sacBlockerEffect, theRing);

                endCombatExecute.setSubAbility(sacExecute);
                becomesBlockedTrigger.setOverridingAbility(endCombatExecute);
                theRing.addTrigger(becomesBlockedTrigger);

                break;
            case 4:
                final String damageTrig = "Mode$ DamageDone | ValidSource$ Card.IsRemembered | ValidTarget$ Player | CombatDamage$ True | TriggerZones$ Command | TriggerDescription$ Whenever your Ring-bearer deals combat damage to a player, each opponent loses 3 life.";
                final String loseEffect = "DB$ LoseLife | Defined$ Opponent | LifeAmount$ 3";

                final Trigger damageTrigger = TriggerHandler.parseTrigger(damageTrig, theRing, true);
                SpellAbility loseExecute = AbilityFactory.getAbility(loseEffect, theRing);

                damageTrigger.setOverridingAbility(loseExecute);
                theRing.addTrigger(damageTrigger);

                break;
            default:
                System.out.println("Ring level should always be 1-4. Currently its " + level);
        }

        // Then choose a ring-bearer (You may keep the same one). Auto pick if <2 choices.
        // This should probably set a thing rather than just remembering on the Ring but its a start
        CardCollection creatures = p.getCreaturesInPlay();
        Card ringbearer = p.getController().chooseSingleEntityForEffect(creatures, sa, "Choose your Ring-bearer", false, null);
        theRing.clearRemembered();
        if (ringbearer != null) {
            theRing.addRemembered(ringbearer);
            // Run triggers
            final Map<AbilityKey, Object> runParams2 = AbilityKey.mapFromCard(ringbearer);
            game.getTriggerHandler().runTrigger(TriggerType.RingbearerChosen, runParams2, false);
        }
        // make sure it shows up in the command zone
        theRing.updateStateForView();
    }
}
