package forge.ai.ability;

import java.util.List;

import com.google.common.base.Predicate;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class PlayAi extends SpellAbilityAi {

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        final String logic = sa.hasParam("AILogic") ? sa.getParam("AILogic") : "";
        
        final Game game = ai.getGame();
        final Card source = sa.getHostCard();
        // don't use this as a response (ReplaySpell logic is an exception, might be called from a subability
        // while the trigger is on stack)
        if (!game.getStack().isEmpty() && !"ReplaySpell".equals(logic)) {
            return false;
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false; // prevent infinite loop
        }

        CardCollection cards = null;
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            ZoneType zone = tgt.getZone().get(0);
            cards = CardLists.getValidCards(game.getCardsIn(zone), tgt.getValidTgts(), ai, source, sa);
            if (cards.isEmpty()) {
                return false;
            }
        } else if (!sa.hasParam("Valid")) {
            cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            if (cards.isEmpty()) {
                return false;
            }
        }
        
        if ("ReplaySpell".equals(logic)) {
            return ComputerUtil.targetPlayableSpellCard(ai, cards, sa, sa.hasParam("WithoutManaCost"));                
        }
        
        return true;
    }
    
    /**
     * <p>
     * doTriggerAINoCost
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     *
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        if (sa.usesTargeting()) {
            if (!sa.hasParam("AILogic")) {
                return false;
            }
            
            return checkApiLogic(ai, sa);
        }

        return true;
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        // as called from PlayEffect:173
        return true;
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(final Player ai, final SpellAbility sa, Iterable<Card> options,
            final boolean isOptional,
            Player targetedPlayer) {
        List<Card> tgtCards = CardLists.filter(options, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                for (SpellAbility s : c.getBasicSpells()) {
                    Spell spell = (Spell) s;
                    s.setActivatingPlayer(ai);
                    // timing restrictions still apply
                    if (!s.getRestrictions().checkTimingRestrictions(c, s))
                        continue;
                    if (sa.hasParam("WithoutManaCost")) {
                        spell = (Spell) spell.copyWithNoManaCost();
                    } else if (sa.hasParam("PlayCost")) {
                        Cost abCost;
                        if ("ManaCost".equals(sa.getParam("PlayCost"))) {
                            abCost = new Cost(c.getManaCost(), false);
                        } else {
                            abCost = new Cost(sa.getParam("PlayCost"), false);
                        }

                        spell = (Spell) spell.copyWithDefinedCost(abCost);
                    }
                    if( AiPlayDecision.WillPlay == ((PlayerControllerAi)ai.getController()).getAi().canPlayFromEffectAI(spell, !isOptional, true)) {
                        return true;
                    }
                }
                return false;
            }
        });
        return ComputerUtilCard.getBestAI(tgtCards);
    }
}
