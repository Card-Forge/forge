package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.ApiType;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ChooseSourceAi extends SpellAiLogic {
 
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(final Player ai, SpellAbility sa) {
        // TODO: AI Support! Currently this is copied from AF ChooseCard.
        //       When implementing AI, I believe AI also needs to be made aware of the damage sources chosen
        //       to be prevented (e.g. so the AI doesn't attack with a creature that will not deal any damage
        //       to the player because a CoP was pre-activated on it - unless, of course, there's another
        //       possible reason to attack with that creature).
        final Card host = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (sa.canTarget(ai.getOpponent())) {
                tgt.addTarget(ai.getOpponent());
            } else {
                return false;
            }
        }
        if (sa.hasParam("AILogic")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            List<Card> choices = Singletons.getModel().getGame().getCardsIn(choiceZone);
            if (sa.hasParam("Choices")) {
                choices = CardLists.getValidCards(choices, sa.getParam("Choices"), host.getController(), host);
            }
            if (sa.hasParam("TargetControls")) {
                choices = CardLists.filterControlledBy(choices, ai.getOpponent());
            }
            if (sa.getParam("AILogic").equals("NeedsPrevention")) {
                if (!Singletons.getModel().getGame().getStack().isEmpty()) {
                    final SpellAbility topStack = Singletons.getModel().getGame().getStack().peekAbility();
                    if (!topStack.getActivatingPlayer().isHostileTo(ai)) {
                        return false;
                    }
                    final ApiType threatApi = topStack.getApi();
                    if (threatApi != ApiType.DealDamage && threatApi != ApiType.DamageAll) {
                        return false;
                    }
                    
                    final Card source = topStack.getSourceCard();
                    ArrayList<Object> objects = new ArrayList<Object>();
                    final Target threatTgt = topStack.getTarget();

                    if (threatTgt == null) {
                        if (topStack.hasParam("Defined")) {
                            objects = AbilityFactory.getDefinedObjects(source, topStack.getParam("Defined"), topStack);
                        } else if (topStack.hasParam("ValidPlayers")) {
                            objects.addAll(AbilityFactory.getDefinedPlayers(source, topStack.getParam("ValidPlayers"), topStack));
                        }
                    } else {
                        objects.addAll(threatTgt.getTargetPlayers());
                    }
                    if (objects.contains(ai)) {
                        return true;
                    }
                    return false;
                }
                if (!Singletons.getModel().getGame().getPhaseHandler().getPhase()
                        .equals(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    return false;
                }
                choices = CardLists.filter(choices, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (!c.isAttacking(ai) || !Singletons.getModel().getGame().getCombat().isUnblocked(c)) {
                            return false;
                        }
                        return CombatUtil.damageIfUnblocked(c, ai, Singletons.getModel().getGame().getCombat()) > 0;
                    }
                });
                if (choices.isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }
    
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return canPlayAI(ai, sa);
    }
}
