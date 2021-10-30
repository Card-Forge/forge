package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import forge.ai.*;
import forge.card.CardStateName;
import forge.card.CardTypeView;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.*;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PlayAi extends SpellAbilityAi {

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        final String logic = sa.getParamOrDefault("AILogic", "");

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

        CardCollection cards = getPlayableCards(sa, ai);
        if (cards.isEmpty()) {
            return false;
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

        if ("ReplaySpell".equals(logic)) {
            return ComputerUtil.targetPlayableSpellCard(ai, cards, sa, sa.hasParam("WithoutManaCost"), false);
        } else if (logic.startsWith("NeedsChosenCard")) {
            int minCMC = 0;
            if (sa.getPayCosts().getCostMana() != null) {
                minCMC = sa.getPayCosts().getTotalMana().getCMC();
            }
            cards = CardLists.filter(cards, CardPredicates.greaterCMC(minCMC));
            return chooseSingleCard(ai, sa, cards, sa.hasParam("Optional"), null, null) != null;
        } else if ("WithTotalCMC".equals(logic)) {
            // Try to play only when there are more than three playable cards.
            if (cards.size() < 3)
                return false;
            ManaCost mana = sa.getPayCosts().getTotalMana();
            if (mana.countX() > 0) {
                int amount = ComputerUtilCost.getMaxXValue(sa, ai);
                if (amount < ComputerUtilCard.getBestAI(cards).getCMC())
                    return false;
                int totalCMC = 0;
                for (Card c : cards) {
                    totalCMC += c.getCMC();
                }
                if (amount > totalCMC)
                    amount = totalCMC;
                sa.setXManaCostPaid(amount);
            }
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

            if ("ReplaySpell".equals(sa.getParam("AILogic"))) {
                return ComputerUtil.targetPlayableSpellCard(ai, getPlayableCards(sa, ai), sa, sa.hasParam("WithoutManaCost"), mandatory);
            } 

            return checkApiLogic(ai, sa);
        }

        return true;
    }

    @Override
    public boolean confirmAction(Player ai, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
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
                // TODO needs to be aligned for MDFC along with getAbilityToPlay so the knowledge
                // of which spell was the reason for the choice can be used there
                for (SpellAbility s : c.getBasicSpells(c.getState(CardStateName.Original))) {
                    Spell spell = (Spell) s;
                    s.setActivatingPlayer(ai);
                    // timing restrictions still apply
                    if (!s.getRestrictions().checkTimingRestrictions(c, s))
                        continue;
                    if (params != null && params.containsKey("CMCLimit")) {
                        Integer cmcLimit = (Integer) params.get("CMCLimit");
                        if (spell.getPayCosts().getTotalMana().getCMC() > cmcLimit)
                            continue;
                    }
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
                    if (AiPlayDecision.WillPlay == ((PlayerControllerAi)ai.getController()).getAi().canPlayFromEffectAI(spell, !(isOptional || sa.hasParam("Optional")), true)) {
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
    
    private static CardCollection getPlayableCards(SpellAbility sa, Player ai) {
        CardCollection cards = new CardCollection();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();

        if (tgt != null) {
            ZoneType zone = tgt.getZone().get(0);
            cards = CardLists.getValidCards(ai.getGame().getCardsIn(zone), tgt.getValidTgts(), ai, source, sa);
        } else if (!sa.hasParam("Valid")) {
            cards = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
        }

        if (cards != null & sa.hasParam("ValidSA")) {
            final String valid[] = sa.getParam("ValidSA").split(",");
            final Iterator<Card> itr = cards.iterator();
            while (itr.hasNext()) {
                final Card c = itr.next();
                if (!Iterables.any(AbilityUtils.getBasicSpellsFromPlayEffect(c, ai), SpellAbilityPredicates.isValid(valid, ai , c, sa))) {
                    itr.remove();
                }
            }
        }

        // Ensure that if a ValidZone is specified, there's at least something to choose from in that zone.
        if (sa.hasParam("ValidZone")) {
            cards = new CardCollection(AbilityUtils.filterListByType(ai.getGame().getCardsIn(ZoneType.valueOf(sa.getParam("ValidZone"))),
                    sa.getParam("Valid"), sa));
        }
        return cards;
    }

}
