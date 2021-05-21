package forge.ai.ability;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.ai.AiProps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.card.CardStateName;
import forge.card.CardTypeView;
import forge.game.Game;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
import forge.game.spellability.SpellPermanent;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

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

        if (sa.hasParam("ValidSA")) {
            final String valid[] = {sa.getParam("ValidSA")};
            final Iterator<Card> itr = cards.iterator();
            while (itr.hasNext()) {
                final Card c = itr.next();
                final List<SpellAbility> validSA = Lists.newArrayList(Iterables.filter(AbilityUtils.getBasicSpellsFromPlayEffect(c, ai), SpellAbilityPredicates.isValid(valid, ai , c, sa)));
                if (validSA.size() == 0) {
                    itr.remove();
                }
            }
            if (cards.isEmpty()) {
                return false;
            }
        }

        if (game.getRules().hasAppliedVariant(GameType.MoJhoSto) && source.getName().equals("Jhoira of the Ghitu Avatar")) {
            // Additional logic for MoJhoSto:
            // Do not activate Jhoira too early, usually there are few good targets
            AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
            int numLandsForJhoira = aic.getIntProperty(AiProps.MOJHOSTO_NUM_LANDS_TO_ACTIVATE_JHOIRA);
            int chanceToActivateInst = 100 - aic.getIntProperty(AiProps.MOJHOSTO_CHANCE_TO_USE_JHOIRA_COPY_INSTANT);
            if (ai.getLandsInPlay().size() < numLandsForJhoira) {
                return false;
            }
            // Don't spam activate the Instant copying ability all the time to give the AI a chance to use other abilities
            // Can probably be improved, but as random as MoJhoSto already is, probably not a huge deal for now
            if ("Instant".equals(sa.getParam("AnySupportedCard")) && MyRandom.percentTrue(chanceToActivateInst)) {
                return false;
            }
        }

        // Ensure that if a ValidZone is specified, there's at least something to choose from in that zone.
        CardCollectionView validOpts = new CardCollection();
        if (sa.hasParam("ValidZone")) {
            validOpts = AbilityUtils.filterListByType(game.getCardsIn(ZoneType.valueOf(sa.getParam("ValidZone"))),
                    sa.getParam("Valid"), sa);
            if (validOpts.isEmpty()) {
                return false;
            }
        }

        if ("ReplaySpell".equals(logic)) {
            return ComputerUtil.targetPlayableSpellCard(ai, cards, sa, sa.hasParam("WithoutManaCost"));                
        } else if (logic.startsWith("NeedsChosenCard")) {
            int minCMC = 0;
            if (sa.getPayCosts().getCostMana() != null) {
                minCMC = sa.getPayCosts().getTotalMana().getCMC();
            }
            validOpts = CardLists.filter(validOpts, CardPredicates.greaterCMC(minCMC));
            return chooseSingleCard(ai, sa, validOpts, sa.hasParam("Optional"), null, null) != null;
        }

        if (source != null && source.hasKeyword(Keyword.HIDEAWAY) && source.hasRemembered()) {
            // AI is not very good at playing non-permanent spells this way, at least yet
            // (might be possible to enable it for Sorceries in Main1/Main2 if target is available,
            // but definitely not for most Instants)
            Card rem = (Card) source.getFirstRemembered();
            CardTypeView t = rem.getState(CardStateName.Original).getType();

            return t.isPermanent() && !t.isLand();
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
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(final Player ai, final SpellAbility sa, Iterable<Card> options,
            final boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        List<Card> tgtCards = CardLists.filter(options, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                for (SpellAbility s : c.getBasicSpells(c.getState(CardStateName.Original))) {
                    Spell spell = (Spell) s;
                    s.setActivatingPlayer(ai);
                    // timing restrictions still apply
                    if (!s.getRestrictions().checkTimingRestrictions(c, s))
                        continue;
                    if (sa.hasParam("WithoutManaCost")) {
                        // Try to avoid casting instants and sorceries with X in their cost, since X will be assumed to be 0.
                        if (!(spell instanceof SpellPermanent)) {
                            if (spell.getPayCosts().getTotalMana().countX() > 0) {
                                continue;
                            }
                        }

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
                        // Before accepting, see if the spell has a valid number of targets (it should at this point).
                        // Proceeding past this point if the spell is not correctly targeted will result
                        // in "Failed to add to stack" error and the card disappearing from the game completely.
                        return spell.isTargetNumberValid();
                    }
                }
                return false;
            }
        });
        return ComputerUtilCard.getBestAI(tgtCards);
    }
}
