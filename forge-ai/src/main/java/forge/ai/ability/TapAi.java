package forge.ai.ability;

import forge.ai.*;
import forge.card.ColorSet;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPayLife;
import forge.ai.simulation.ActivationGuard;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

public class TapAi extends TapAiBase {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        if (ManaDenial.isLandTapper(sa)) {
            // Rishadan Port, Rishadan Dockhand: which land and when is ManaDenial's call; the generic logic below
            // only looks at creatures and permanents with tap abilities, so it never taps a land
            final Card land = ManaDenial.choose(ai, sa);
            if (land == null) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.resetTargets();
            sa.getTargets().add(land);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        final PhaseHandler phase = ai.getGame().getPhaseHandler();

        final AiAbilityDecision cantrip = cantripAtTheirEnd(ai, sa, phase);
        if (cantrip != null) {
            return cantrip;
        }

        if (phase.getPlayerTurn().isOpponentOf(ai) && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            // Tap things down if it's Human's turn
        } else if (phase.isPlayerTurn(ai)) {
            if (isSorcerySpeed(sa, ai) && phase.getPhase().isBefore(PhaseType.COMBAT_BEGIN)) {
                // Cast it if it's a sorcery.
            } else if (phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                // Aggro Brains are willing to use TapEffects aggressively instead of defensively
                if (!AiProfileUtil.getBoolProperty(ai, AiProps.PLAY_AGGRO)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else {
                // Don't tap down after blockers
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if (!playReusable(ai, sa)) {
            // Generally don't want to tap things with an Instant during Players turn outside of combat
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        final Card source = sa.getHostCard();

        final String aiLogic = sa.getParamOrDefault("AILogic", "");
        if ("GoblinPolkaBand".equals(aiLogic)) {
            return SpecialCardAi.GoblinPolkaBand.consider(ai, sa);
        } else if ("Arena".equals(aiLogic)) {
            return SpecialCardAi.Arena.consider(ai, sa);
        }

        if (sa.usesTargeting()) {
            // X controls the minimum targets
            if ("X".equals(sa.getTargetRestrictions().getMinTargets()) && sa.getSVar("X").equals("Count$xPaid")) {
                ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());
            }

            sa.resetTargets();
            if (tapPrefTargeting(ai, source, sa, false)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        } else {
            CardCollection untap;
            if (sa.hasParam("CardChoices")) {
                untap = CardLists.getValidCards(source.getGame().getCardsIn(ZoneType.Battlefield), sa.getParam("CardChoices"), ai, source, sa);
            } else {
                untap = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
            }

            int value = 0;
            for (final Card c : untap) {
                if (c.isUntapped()) {
                    value += ComputerUtilCard.evaluateCreature(c);
                }
            }

            if (value > 0) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }

    /**
     * A tap with a card drawn (Ice of Fire // Ice) is a cantrip that costs two mana: at the end of the opponent's turn,
     * with the mana otherwise unused, it is cast on their best untapped land (or other permanent): they untap it at once,
     * but the card is drawn, which is the point. Not at sorcery speed; the rest of this class judges the other times.
     */
    private static AiAbilityDecision cantripAtTheirEnd(final Player ai, final SpellAbility sa, final PhaseHandler phase) {
        if (!sa.isSpell() || sa.getSubAbility() == null || sa.getSubAbility().getApi() != forge.game.ability.ApiType.Draw
                || !sa.usesTargeting() || isSorcerySpeed(sa, ai)
                || !phase.is(PhaseType.END_OF_TURN) || !phase.getPlayerTurn().isOpponentOf(ai) || !ai.canDraw()) {
            return null;
        }
        Card best = null;
        for (final Card c : ai.getOpponents().getCardsIn(ZoneType.Battlefield)) {
            if (c.isUntapped() && sa.canTarget(c) && (best == null || c.isLand() && !best.isLand()
                    || c.isLand() == best.isLand() && ComputerUtilCard.evaluatePermanentList(new CardCollection(c)) > ComputerUtilCard.evaluatePermanentList(new CardCollection(best)))) {
                best = c;
            }
        }
        if (best == null) {
            return null;
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /** Above this life even a one-drop on curve is worth two life for the land. */
    private static final int LOW_LIFE_FOR_ONE_DROP = 14;

    /**
     * Whether the AI can afford to pay life for a land to enter untapped: it must stay at or above 10 (the burn that the deck
     * across the table may hold; a life total of 10 in Adventure mode is already low) and at or above what the opponent's
     * creatures hit for on their next turn plus five. That estimate is the one of the Manabarbs reserve: their tapped
     * creatures untap, the creatures that cannot attack and the ones that the AI's untapped blockers stop are not counted.
     */
    static boolean lifeCanBeSpared(final Player payer, final int amount) {
        return payer.getLife() - amount >= Math.max(10, ActivationGuard.nextTurnThreat(payer) + 5);
    }

    @Override
    public boolean willPayUnlessCost(Player payer, SpellAbility sa, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        // Check for shocklands and similar ETB replacement effects
        if (sa.hasParam("ETB")) {
            final Card source = sa.getHostCard();
            for (final CostPart part : cost.getCostParts()) {
                if (part instanceof CostPayLife) {
                    final CostPayLife lifeCost = (CostPayLife) part;
                    Integer amount = lifeCost.convertAmount();
                    if (payer.getLife() > (amount + 1) && payer.canPayLife(amount, true, sa) && lifeCanBeSpared(payer, amount)) {
                        final int landsize = payer.getLandsInPlay().size() + 1;
                        // Check if the AI has enough lands to play the card
                        final CardCollection playable = CardLists.filter(
                                payer.getCardsIn(ZoneType.Hand), CardPredicates.hasCMC(landsize));
                        if (playable.isEmpty()) {
                            return false;
                        }
                        // the same sources answer this for every card in hand, so ask once
                        final byte availableColors = ColorSet.fromNames(
                                ComputerUtilCost.getAvailableManaColors(payer, source)).getColor();
                        final boolean creatureInPlay = payer.getGame().getCardsIn(ZoneType.Battlefield).anyMatch(CardPredicates.CREATURES);
                        for (Card c : playable) {
                            if (c.isAura() && !creatureInPlay) {
                                continue; // an Aura with nothing to enchant is not a reason to pay 2 life
                            }
                            if (c.isInstant() || c.hasKeyword(Keyword.FLASH)) {
                                continue; // burn and counterspells are held for the opponent's turn: the land can come in tapped
                            }
                            if (c.getCMC() < 2 && payer.getLife() < LOW_LIFE_FOR_ONE_DROP) {
                                continue; // a one-drop is not worth the life while it is not plentiful
                            }
                            // Check if the AI can pay for the card with the mana it has and intends to play it
                            // canPlaySa can't be asked here: the land is not on the battlefield yet, so the
                            // card always looks unaffordable. A nonland card that needs exactly this land's mana will do
                            if (!c.isLand() && c.getManaCost().canBePaidWithAvailable(availableColors)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }
        } else if (sa.hasParam("UnlessSwitched")) {
            // effect is each opponent may sacrifice to tap creature
            Card source = sa.getHostCard();
            if (alreadyPaid) {
                return false;
            }
            // if it can't attack the payer, do nothing?
            // TODO check if it can attack team mates?
            if (!CombatUtil.canAttack(source, payer)) {
                return false;
            }

            // predict combat damage
            int dmg = ComputerUtilCombat.damageIfUnblocked(source, payer, null, false);
            if (payer.getLife() < dmg * 1.5) {
                return true;
            }
        }
        return super.willPayUnlessCost(payer, sa, cost, alreadyPaid, payers);
    }
}
