package forge.ai.ability;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicates;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.card.CardType.Supertype;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class PermanentAi extends SpellAbilityAi {

    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {

        final Card card = sa.getHostCard();

        if (card.hasKeyword("MayFlashSac") && !ai.couldCastSorcery(sa)) {
            // AiPlayDecision.AnotherTime
            return false;
        }

        // Wait for Main2 if possible
        return !ph.is(PhaseType.MAIN1) || !ph.isPlayerTurn(ai) || ComputerUtil.castPermanentInMain1(ai, sa) || sa.hasParam("WithoutManaCost");
    }

    /**
     * The rest of the logic not covered by the canPlayAI template is defined
     * here
     */
    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {

        final Card card = sa.getHostCard();
        final Game game = ai.getGame();

        // check on legendary
        if (card.getType().isLegendary()
                && !game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)) {
            if (ai.isCardInPlay(card.getName())) {
                if (!card.hasSVar("AILegendaryException")) {
                    // AiPlayDecision.WouldDestroyLegend
                    return false;
                } else {
                    String specialRule = card.getSVar("AILegendaryException");
                    if ("TwoCopiesAllowed".equals(specialRule)) {
                        // One extra copy allowed on the battlefield, e.g. Brothers Yamazaki
                        if (CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals(card.getName())).size() > 1) {
                            return false;
                        }
                    } else if ("AlwaysAllowed".equals(specialRule)) {
                        // Nothing to do here, check for Legendary is disabled
                    } else {
                        // Unknown hint, assume two copies not allowed
                        return false;
                    }
                }
            }
        }

        /* -- not used anymore after Ixalan (Planeswalkers are now legendary, not unique by subtype) --
        if (card.isPlaneswalker()) {
            CardCollection list = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                    CardPredicates.Presets.PLANESWALKERS);
            for (String type : card.getType().getSubtypes()) { // determine
                                                               // planewalker
                                                               // subtype
                final CardCollection cl = CardLists.getType(list, type);
                if (!cl.isEmpty()) {
                    // AiPlayDecision.WouldDestroyOtherPlaneswalker
                    return false;
                }
                break;
            }
        }*/

        if (card.getType().hasSupertype(Supertype.World)) {
            CardCollection list = CardLists.getType(ai.getCardsIn(ZoneType.Battlefield), "World");
            if (!list.isEmpty()) {
                // AiPlayDecision.WouldDestroyWorldEnchantment
                return false;
            }
        }

        ManaCost mana = sa.getPayCosts().getTotalMana();
        if (mana.countX() > 0) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai);
            final Card source = sa.getHostCard();
            if (source.hasConverge()) {
                int nColors = ComputerUtilMana.getConvergeCount(sa, ai);
                for (int i = 1; i <= xPay; i++) {
                    sa.setXManaCostPaid(i);
                    int newColors = ComputerUtilMana.getConvergeCount(sa, ai);
                    if (newColors > nColors) {
                        nColors = newColors;
                    } else {
                        sa.setXManaCostPaid(i - 1);
                        break;
                    }
                }
            } else {
                // AiPlayDecision.CantAffordX
                if (xPay <= 0) {
                    return false;
                }
                sa.setXManaCostPaid(xPay);
            }
        } else if (mana.isZero()) {
            // if mana is zero, but card mana cost does have X, then something
            // is wrong
            ManaCost cardCost = card.getManaCost();
            if (cardCost != null && cardCost.countX() > 0) {
                // AiPlayDecision.CantPlayAi
                return false;
            }
        }

        if ("SacToReduceCost".equals(sa.getParam("AILogic"))) {
            // reset X to better calculate
            sa.setXManaCostPaid(0);
            ManaCostBeingPaid paidCost = ComputerUtilMana.calculateManaCost(sa, true, 0);

            int generic = paidCost.getGenericManaAmount();
            // Set PayX here to maximum value.
            int xPay = ComputerUtilCost.getMaxXValue(sa, ai);
            // currently cards with SacToReduceCost reduce by 2 generic
            xPay = Math.min(xPay, generic / 2);
            sa.setXManaCostPaid(xPay);
        }

        if (sa.hasParam("Announce") && sa.getParam("Announce").startsWith("Multikicker")) {
            // String announce = sa.getParam("Announce");
            ManaCost mkCost = sa.getMultiKickerManaCost();
            ManaCost mCost = sa.getPayCosts().getTotalMana();
            boolean isZeroCost = mCost.isZero();
            for (int i = 0; i < 10; i++) {
                mCost = ManaCost.combine(mCost, mkCost);
                ManaCostBeingPaid mcbp = new ManaCostBeingPaid(mCost);
                if (!ComputerUtilMana.canPayManaCost(mcbp, sa, ai)) {
                    card.setKickerMagnitude(i);
                    sa.setSVar("Multikicker", String.valueOf(i));
                    break;
                }
                card.setKickerMagnitude(i + 1);
            }
            if (isZeroCost && card.getKickerMagnitude() == 0) {
                // Bail if the card cost was {0} and no multikicker was paid (e.g. Everflowing Chalice).
                // TODO: update this if there's ever a card where it makes sense to play it for {0} with no multikicker
                return false;
            }
        }

        // don't play cards without being able to pay the upkeep for
        for (KeywordInterface inst : card.getKeywords()) {
            String ability = inst.getOriginal();
            if (ability.startsWith("UpkeepCost")) {
                final String[] k = ability.split(":");
                final String costs = k[1];

                final SpellAbility emptyAbility = new SpellAbility.EmptySa(card, ai);
                emptyAbility.setPayCosts(new Cost(costs, true));
                emptyAbility.setTargetRestrictions(sa.getTargetRestrictions());

                emptyAbility.setActivatingPlayer(ai);
                if (!ComputerUtilCost.canPayCost(emptyAbility, ai)) {
                    // AiPlayDecision.AnotherTime
                    return false;
                }
            }
        }

        // check for specific AI preferences
        if (card.hasSVar("AICastPreference")) {
            String pref = card.getSVar("AICastPreference");
            String[] groups = StringUtils.split(pref, "|");
            boolean dontCast = false;
            for (String group : groups) {
                String[] elems = StringUtils.split(group.trim(), '$');
                String param = elems[0].trim();
                String value = elems[1].trim();

                if (param.equals("MustHaveInHand")) {
                    // Only cast if another card is present in hand (e.g. Illusions of Grandeur followed by Donate)
                    boolean hasCard = CardLists.filter(ai.getCardsIn(ZoneType.Hand), CardPredicates.nameEquals(value)).size() > 0;
                    if (!hasCard) {
                        dontCast = true;
                    }
                } else if (param.startsWith("MaxControlled")) {
                    // Only cast unless there are X or more cards like this on the battlefield under AI control already,
                    CardCollectionView valid = param.contains("Globally") ? ai.getGame().getCardsIn(ZoneType.Battlefield)
                            : ai.getCardsIn(ZoneType.Battlefield);
                    CardCollection ctrld = CardLists.filter(valid, CardPredicates.nameEquals(card.getName()));

                    int numControlled = 0;
                    if (param.endsWith("WithoutOppAuras")) {
                        // Check that the permanent does not have any auras attached to it by the opponent (this assumes that if
                        // the opponent cast an aura on the opposing permanent, it's not with good intentions, and thus it might
                        // be better to have a pristine copy of the card - might not always be a correct assumption, but sounds
                        // like a reasonable default for some cards).
                        for (Card c : ctrld) {
                            if (c.getEnchantedBy().isEmpty()) {
                                numControlled++;
                            } else {
                                for (Card att : c.getEnchantedBy()) {
                                    if (!att.getController().isOpponentOf(ai)) {
                                        numControlled++;
                                    }
                                }
                            }
                        }
                    } else {
                        numControlled = ctrld.size();
                    }

                    if (numControlled >= Integer.parseInt(value)) {
                        dontCast = true;
                    }
                } else if (param.equals("NumManaSources")) {
                    // Only cast if there are X or more mana sources controlled by the AI
                    CardCollection m = ComputerUtilMana.getAvailableManaSources(ai, true);
                    if (m.size() < Integer.parseInt(value)) {
                        dontCast = true;
                    }
                } else if (param.equals("NumManaSourcesNextTurn")) {
                    // Only cast if there are X or more mana sources controlled by the AI *or*
                    // if there are X-1 mana sources in play but the AI has an extra land in hand
                    CardCollection m = ComputerUtilMana.getAvailableManaSources(ai, true);
                    int extraMana = CardLists.filter(ai.getCardsIn(ZoneType.Hand), CardPredicates.Presets.LANDS).size() > 0 ? 1 : 0;
                    if (card.getName().equals("Illusions of Grandeur")) {
                        // TODO: this is currently hardcoded for specific Illusions-Donate cost reduction spells, need to make this generic.
                       extraMana += Math.min(3, CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Predicates.or(CardPredicates.nameEquals("Sapphire Medallion"), CardPredicates.nameEquals("Helm of Awakening"))).size()) * 2; // each cost-reduction spell accounts for {1} in both Illusions and Donate
                    }
                    if (m.size() + extraMana < Integer.parseInt(value)) {
                        dontCast = true;
                    }
                } else if (param.equals("NeverCastIfLifeBelow")) {
                    // Do not cast this spell if AI life is below a certain threshold
                    if (ai.getLife() < Integer.parseInt(value)) {
                        dontCast = true;
                    }
                } else if (param.equals("AlwaysCastIfLifeBelow")) {
                    if (ai.getLife() < Integer.parseInt(value)) {
                        dontCast = false;
                        break; // disregard other preferences, always cast as a last resort
                    }
                } else if (param.equals("OnlyFromZone")) {
                    if (!card.getZone().getZoneType().toString().equals(value)) {
                        dontCast = true;
                        break;  // limit casting to a specific zone only
                    }
                }
            }

            return !dontCast;
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final Cost cost = sa.getPayCosts();

        if (!sa.metConditions()) {
            return false;
        }

        if (sa.hasParam("AILogic") && !checkAiLogic(ai, sa, sa.getParam("AILogic"))) {
            return false;
        }
        if (cost != null && !willPayCosts(ai, sa, cost, source)) {
            return false;
        }
        if (!checkPhaseRestrictions(ai, sa, ai.getGame().getPhaseHandler())) {
            return false;
        }
        return mandatory || checkApiLogic(ai, sa);
    }

}
