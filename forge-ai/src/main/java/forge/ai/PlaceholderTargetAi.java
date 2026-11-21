package forge.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPayLife;
import forge.game.cost.CostSacrifice;
import forge.game.player.Player;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The AI logic of {@code DB$ TargetOnly | ValidTgts$ Opponent} (see {@link forge.ai.ability.TargetOnlyAi}): the choice of "target opponent" for the
 * effects that follow (Speedbrood Stalker, Descendant of Masumaro, Iroh, Jon Irenicus, Coveted Falcon, Parnesse,
 * Tresserhorn's Lord). The scripts used a Pump for it, which has nothing to pump: the general code picked a random
 * opponent and gave away a permanent whenever the effect after it had a target. The script names what the choice is for
 * ({@code AILogic$}):
 * <ul>
 *   <li>{@code TargetEdict}: the opponent with the fewest creatures and planeswalkers to sacrifice, at least one if
 *       any has one (the edict hits the best of what is left)</li>
 *   <li>{@code TargetFewestHand}: the opponent with the fewest cards in hand (a counter removed from each card there)</li>
 *   <li>{@code TargetWeakest}: the opponent whose board hurts the AI least (it draws cards, or gets a creature)</li>
 *   <li>{@code TargetNone}: no target: the effect only helps the opponent (Parnesse's copy)</li>
 *   <li>{@code DonateJunk}: an opponent, and only when the AI has something worth giving away
 *       ({@link #junk}); also the logic of the gain-control effect that follows, which picks what to give</li>
 * </ul>
 */
public final class PlaceholderTargetAi {
    private PlaceholderTargetAi() {
    }

    /** The AILogic values this class decides. */
    public static boolean handles(final String logic) {
        return "TargetEdict".equals(logic) || "TargetFewestHand".equals(logic) || "TargetWeakest".equals(logic)
                || "TargetNone".equals(logic) || "DonateJunk".equals(logic);
    }

    /** The decision for the pump that targets an opponent, and the choice is put on the spell ability. */
    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final String logic = sa.getParamOrDefault("AILogic", "");
        if ("TargetNone".equals(logic)) {
            sa.resetTargets();
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactGame);
        }
        final List<Player> opps = new ArrayList<>(ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa)));
        if (opps.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        Comparator<Player> by;
        switch (logic) {
            case "TargetEdict":
                by = Comparator.comparingInt((Player p) -> {
                    final int n = sacrificable(p);
                    return n == 0 ? Integer.MAX_VALUE : n;
                });
                break;
            case "TargetFewestHand":
                by = Comparator.comparingInt((Player p) -> p.getCardsIn(ZoneType.Hand).size());
                break;
            case "DonateJunk":
                if (junk(ai, sa).isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactGame);
                }
                by = Comparator.comparingInt((Player p) -> power(p));
                break;
            default: // TargetWeakest
                by = Comparator.comparingInt((Player p) -> power(p));
                break;
        }
        final Player chosen = Collections.min(opps, by);
        sa.resetTargets();
        sa.getTargets().add(chosen);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static int sacrificable(final Player p) {
        return CardLists.filter(p.getCardsIn(ZoneType.Battlefield), c -> c.isCreature() || c.isPlaneswalker()).size();
    }

    private static int power(final Player p) {
        int sum = 0;
        for (final Card c : p.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature()) {
                sum += Math.max(0, c.getNetCombatDamage());
            }
        }
        return sum;
    }

    /**
     * The permanents of the AI that are worth giving to an opponent: a card that says so ({@code DonateMe}), a token that
     * is no creature (Food, Clue), or a small token creature when the AI can take what the creature does with it (a
     * gift that is goaded and carries counters must not be a threat to the AI's life). Only what the ability can target.
     */
    public static CardCollection junk(final Player ai, final SpellAbility sa) {
        final SpellAbility gift = giftAbility(sa);
        final CardCollection out = new CardCollection();
        if (gift == null) {
            return out;
        }
        gift.setActivatingPlayer(ai);
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!gift.canTarget(c) || c.isLand() && !c.hasSVar("DonateMe")) {
                continue;
            }
            if (c.hasSVar("DonateMe")) {
                out.add(c);
            } else if (c.isToken() && !c.isCreature() && !c.getType().hasSubtype("Treasure")) {
                out.add(c);
            } else if (c.isCreature() && c.isToken() && !c.isEnchantment() && ComputerUtilCard.evaluateCreature(c) <= 150
                    && ai.getLife() > 3 * (Math.max(0, c.getNetCombatDamage()) + 2)) {
                out.add(c);
            }
        }
        return out;
    }

    /** The gain-control ability of the chain: the one that is given the permanents. */
    private static SpellAbility giftAbility(final SpellAbility sa) {
        for (SpellAbility s = sa; s != null; s = s.getSubAbility()) {
            if (s.getApi() == forge.game.ability.ApiType.GainControl) {
                return s;
            }
        }
        return null;
    }

    /** The gain-control ability that gives the permanents away: aims it at what {@link #junk} says, or at nothing. */
    public static AiAbilityDecision donate(final Player ai, final SpellAbility sa) {
        final CardCollection junk = junk(ai, sa);
        sa.resetTargets();
        if (junk.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactGame);
        }
        int max = 1;
        if (sa.getTargetRestrictions() != null && sa.hasParam("TargetMax")) {
            try {
                max = Math.max(1, forge.game.ability.AbilityUtils.calculateAmount(sa.getHostCard(),
                        sa.getParam("TargetMax"), sa));
            } catch (final RuntimeException e) {
                max = 1;
            }
        }
        final CardCollection rest = new CardCollection(junk);
        while (!rest.isEmpty() && sa.getTargets().size() < max) {
            final Card worst = ComputerUtilCard.getWorstAI(rest);
            rest.remove(worst);
            sa.getTargets().add(worst);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /**
     * "Sacrifice it unless you sacrifice creatures and pay life ..." (Tresserhorn's Lord): pays when the creatures it
     * takes are worth less than the card, and the life is no danger.
     */
    public static boolean worthPaying(final Player payer, final SpellAbility sa, final Cost cost) {
        final Card host = sa.getHostCard();
        if (host == null || cost == null) {
            return false;
        }
        int sac = 0;
        int life = 0;
        for (final CostPart part : cost.getCostParts()) {
            try {
                if (part instanceof CostSacrifice) {
                    sac += Integer.parseInt(part.getAmount());
                } else if (part instanceof CostPayLife) {
                    life += Integer.parseInt(part.getAmount());
                }
            } catch (final NumberFormatException e) {
                return false;
            }
        }
        if (payer.getLife() - life < 8) {
            return false;
        }
        final List<Integer> values = new ArrayList<>();
        for (final Card c : payer.getCardsIn(ZoneType.Battlefield)) {
            if (c != host && c.isCreature()) {
                values.add(ComputerUtilCard.evaluateCreature(c));
            }
        }
        if (values.size() < sac) {
            return false;
        }
        Collections.sort(values);
        int lost = 0;
        for (int i = 0; i < sac; i++) {
            lost += values.get(i);
        }
        // every creature sacrificed draws a card (Tresserhorn's own trigger), which is worth about a creature's body in
        // the evaluator's points, and the opponent's three cards cost a little
        return lost - 80 * sac + 60 < ComputerUtilCard.evaluateCreature(host);
    }
}
