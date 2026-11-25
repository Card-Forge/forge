package forge.ai.ability;

import forge.ai.*;
import java.util.ArrayList;
import java.util.List;
import forge.card.ColorSet;
import forge.card.mana.ManaCostShard;
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
    private static int lifeReserve(final Player payer) {
        return Math.max(10, ActivationGuard.nextTurnThreat(payer) + 5);
    }

    static boolean lifeCanBeSpared(final Player payer, final int amount) {
        return payer.getLife() - amount >= lifeReserve(payer);
    }

    /** A spell that only aims at the AI's own permanents (Ephemerate, Cloudshift) is no play while it has none. */
    private static boolean needsAnOwnTargetThatIsNotThere(final Player payer, final Card c) {
        boolean any = false;
        for (final SpellAbility spell : c.getSpellAbilities()) {
            if (!spell.isSpell()) {
                continue;
            }
            final String valid = spell.getParam("ValidTgts");
            if (valid == null || !valid.contains("YouCtrl")) {
                return false;
            }
            any = true;
            if (!CardLists.getValidCards(payer.getCardsIn(ZoneType.Battlefield), valid, payer, c, spell).isEmpty()) {
                return false;
            }
        }
        return any;
    }

    /**
     * Life can replace a Phyrexian pip only when the real payment path permits it for every cast mode of the card.
     * Being conservative here avoids paying life for a land based on a spell that cannot use the life alternative.
     */
    private static boolean canPayPhyrexianWithLife(final Player payer, final Card card) {
        boolean foundSpell = false;
        for (final SpellAbility spell : card.getSpellAbilities()) {
            if (!spell.isSpell()) {
                continue;
            }
            foundSpell = true;
            if (!ComputerUtilMana.canPayLifeForManaShard(payer, spell, 2)) {
                return false;
            }
        }
        return foundSpell;
    }

    /** The most mana of the cards that the AI can spend with {@code mana} from lands, counting the mana tokens they make. */
    private static int spendable(final Player payer, final List<Card> cards, final int mana, final List<Byte> sourceColors,
                                 final int phyrexianLife) {
        final int n = Math.min(cards.size(), 10);
        int best = 0;
        final List<Byte> requirements = new ArrayList<>();
        final List<Byte> phyrexianRequirements = new ArrayList<>();
        for (int mask = 1; mask < (1 << n); mask++) {
            int cost = 0;
            int bonus = 0;
            requirements.clear();
            phyrexianRequirements.clear();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    final Card c = cards.get(i);
                    cost += c.getCMC();
                    for (final ManaCostShard shard : c.getManaCost()) {
                        if (shard.isPhyrexian()) {
                            final byte colors = shard.getColorMask();
                            if (colors != 0) {
                                if (canPayPhyrexianWithLife(payer, c)) {
                                    phyrexianRequirements.add(colors);
                                } else {
                                    requirements.add(colors); // rules or card policy require mana for this pip
                                }
                            }
                        } else if (!shard.isOr2Generic() && !shard.isColorless()) {
                            final byte colors = shard.getColorMask();
                            if (colors != 0) {
                                requirements.add(colors);
                            }
                        }
                    }
                    if (c.getCMC() <= mana) {
                        bonus += forge.ai.simulation.RampPlan.tokenMana(c);
                    }
                }
            }
            final int phyrexianPaidWithMana = ManaSourceAllocation.maxPayableOptionalColoredPips(
                    requirements, phyrexianRequirements, sourceColors);
            if (phyrexianPaidWithMana < 0) {
                continue;
            }
            final int maxLifePaidPips = Math.min(phyrexianRequirements.size(), Math.max(0, phyrexianLife) / 2);
            boolean payable = false;
            for (int lifePaidPips = 0; lifePaidPips <= maxLifePaidPips; lifePaidPips++) {
                if (phyrexianPaidWithMana >= phyrexianRequirements.size() - lifePaidPips
                        && cost - lifePaidPips <= mana + bonus) {
                    payable = true;
                    break;
                }
            }
            if (payable && cost > best) {
                best = cost;
            }
        }
        return best;
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
                        final int lifeFloor = lifeReserve(payer);
                        final int lifeAvailableNow = Math.max(0, payer.getLife() - lifeFloor);
                        final int lifeAvailableWith = Math.max(0, payer.getLife() - amount - lifeFloor);
                        // the mana of the lands that are untapped now, and what the cards in hand can spend with and without this one
                        int untapped = 0;
                        final List<Byte> sourcesNow = new ArrayList<>();
                        for (final Card land : payer.getLandsInPlay()) {
                            if (land.isUntapped()) {
                                untapped++;
                                sourcesNow.add(forge.ai.simulation.RampPlan.landColors(land));
                            }
                        }
                        // A dual land is one source: it can pay one coloured shard, not one shard of each colour.
                        final List<Byte> sourcesWith = new ArrayList<>(sourcesNow);
                        sourcesWith.add(forge.ai.simulation.RampPlan.landColors(source));
                        // the same sources answer this for every card in hand, so ask once
                        final byte availableColors = ColorSet.fromNames(
                                ComputerUtilCost.getAvailableManaColors(payer, source)).getColor();
                        final boolean creatureInPlay = payer.getGame().getCardsIn(ZoneType.Battlefield).anyMatch(CardPredicates.CREATURES);
                        final List<Card> candidates = new ArrayList<>();
                        for (Card c : payer.getCardsIn(ZoneType.Hand)) {
                            if (c.isLand() || c.isInstant() || c.hasKeyword(Keyword.FLASH)) {
                                continue; // burn and counterspells are held for the opponent's turn: the land can come in tapped
                            }
                            if (c.isAura() && !creatureInPlay) {
                                continue; // an Aura with nothing to enchant is not a reason to pay 2 life
                            }
                            if (c.getCMC() < 2 && payer.getLife() < LOW_LIFE_FOR_ONE_DROP) {
                                continue; // a one-drop is not worth the life while it is not plentiful
                            }
                            if (needsAnOwnTargetThatIsNotThere(payer, c)) {
                                continue; // Ephemerate with no creature to blink
                            }
                            // canPlaySa can't be asked here: the land is not on the battlefield yet, so the card always
                            // looks unaffordable. The colours of the sources with this land are what can be checked
                            if (c.getManaCost().canBePaidWithAvailable(availableColors)) {
                                candidates.add(c);
                            }
                        }
                        // a land that lets the hand spend more this turn is worth the life: a card that needs exactly
                        // this land, or two cards that fit together once a Malevolent Rumble's Spawn is counted
                        if (spendable(payer, candidates, untapped + 1, sourcesWith, lifeAvailableWith)
                                > spendable(payer, candidates, untapped, sourcesNow, lifeAvailableNow)) {
                            return true;
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
