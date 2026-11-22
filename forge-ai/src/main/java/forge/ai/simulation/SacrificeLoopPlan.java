package forge.ai.simulation;

import forge.ai.RunawayLoop;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Loops that go round a sacrifice outlet with creatures that come back.
 * <ul>
 * <li><b>Persist.</b> Melira, Sylvok Outcast stops the -1/-1 counter of persist, so a Murderous Redcap sacrificed to
 * Viscera Seer is back at once with the same power and its enter trigger hits for two again. Melira also gives the persist
 * creature the SacMe mark, which makes the Seer pay with the Redcap.</li>
 * <li><b>Undying.</b> Yawgmoth, Thran Physician puts a -1/-1 counter on a creature as it sacrifices another: two undying
 * creatures keep each other going. One is sacrificed and comes back with a +1/+1 counter; the next activation sacrifices
 * the other and puts its -1/-1 counter on the one that came back, which cancels the +1/+1 and makes it sacrificeable again.
 * Every round is a creature that dies (and one that enters), a card drawn and a life paid. Run only with something that
 * pays for it: a Blood Artist, a Zulaport Cutthroat or a kin that drains one for each death, which gives back the
 * life, or a Geralf's Messenger that takes two from the opponent each time it comes back.</li>
 * </ul>
 * Each step is the next activation; the guards are the activation limit and the opponent's life (the loop stops when
 * the caller has no opponent left alive).
 */
public final class SacrificeLoopPlan {
    private static final int LIMIT = LoopRunner.MAX_ACTIVATIONS;

    private SacrificeLoopPlan() {
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final SpellAbility melira = meliraAndRedcap(ai, candidates, approve);
        if (melira != null) {
            return melira;
        }
        final SpellAbility yawgmoth = yawgmothAndUndying(ai, candidates, approve);
        if (yawgmoth != null) {
            return yawgmoth;
        }
        final SpellAbility works = ironworks(ai, candidates, approve);
        return works != null ? works : deathmantle(ai, candidates, approve);
    }

    private static SpellAbility meliraAndRedcap(final Player ai, final List<SpellAbility> candidates,
                                                final Predicate<SpellAbility> approve) {
        final Card melira = LoopRunner.inPlay(ai, "Melira, Sylvok Outcast");
        final Card redcap = LoopRunner.inPlay(ai, "Murderous Redcap");
        final Card seer = LoopRunner.inPlay(ai, "Viscera Seer");
        if (melira == null || redcap == null || seer == null || !redcap.hasKeyword(Keyword.PERSIST)
                || redcap.getCounters(CounterEnumType.M1M1) > 0) {
            return null;
        }
        final SpellAbility scry = LoopRunner.activated(candidates, seer, ApiType.Scry);
        if (scry == null) {
            return null;
        }
        scry.setActivatingPlayer(ai);
        scry.resetTargets();
        if (RunawayLoop.exceeded(scry, LIMIT) || !approve.test(scry)) {
            return null;
        }
        VetoLog.note(ai, "sacrifice-loop | Seer sacrifices the Redcap");
        return scry;
    }

    /** What the opponent loses, and the AI gets back, for each creature that dies or enters: the payoffs in play. */
    static int drainPerDeath(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            switch (c.getName()) {
                case "Blood Artist":
                case "Zulaport Cutthroat":
                case "Falkenrath Noble":
                case "Cruel Celebrant":
                case "Syr Konrad, the Grim":
                    n++;
                    break;
                default:
                    break;
            }
        }
        return n;
    }

    /** The undying creatures that are the AI's, other than the outlet. */
    private static List<Card> undying(final Player ai, final Card outlet) {
        final List<Card> out = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c != outlet && c.isCreature() && c.hasKeyword(Keyword.UNDYING)) {
                out.add(c);
            }
        }
        return out;
    }

    private static SpellAbility yawgmothAndUndying(final Player ai, final List<SpellAbility> candidates,
                                                   final Predicate<SpellAbility> approve) {
        final Card yawgmoth = LoopRunner.inPlay(ai, "Yawgmoth, Thran Physician");
        if (yawgmoth == null) {
            return null;
        }
        final List<Card> fodder = undying(ai, yawgmoth);
        Card spare = null;   // no counter: it is the one that is sacrificed next
        Card counted = null; // has the +1/+1 counter: it takes the -1/-1 counter
        for (final Card c : fodder) {
            if (c.getCounters(CounterEnumType.P1P1) > 0) {
                counted = c;
            } else {
                spare = c;
            }
        }
        final int drain = drainPerDeath(ai);
        final boolean messenger = fodder.stream().anyMatch(c -> c.getName().equals("Geralf's Messenger"));
        if (fodder.size() < 2 || spare == null || (drain < 1 && !messenger) || ai.getLife() < 2) {
            return null;
        }
        if (drain < 1) {
            int oppLife = 0;
            for (final Player opp : ai.getOpponents()) {
                oppLife = Math.max(oppLife, opp.getLife());
            }
            if (ai.getLife() <= (oppLife + 1) / 2 + 1) {
                return null; // a life for every two that the opponent loses: the AI would be dead first
            }
        }
        final SpellAbility put = LoopRunner.activated(candidates, yawgmoth, ApiType.PutCounter);
        if (put == null) {
            return null;
        }
        put.setActivatingPlayer(ai);
        put.resetTargets();
        if (RunawayLoop.exceeded(put, LIMIT)) {
            return null;
        }
        if (counted != null) {
            if (!put.canTarget(counted)) {
                return null;
            }
            put.getTargets().add(counted);
        }
        if (!approve.test(put)) {
            return null;
        }
        // the payment of the sacrifice picks the creature marked for it: the one that has no counter, nothing else
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c == spare) {
                c.setSVar("SacMe", "6");
            } else if (c.hasSVar("SacMe") && c.getSVar("SacMe").equals("6")) {
                c.removeSVar("SacMe");
            }
        }
        VetoLog.note(ai, "sacrifice-loop | Yawgmoth sacrifices " + spare.getName());
        return put;
    }

    // ------------------------------------------------------------------------------------------------ Ironworks

    private static final String KCI = "Krark-Clan Ironworks";
    private static final String TRAWLER = "Scrap Trawler";
    private static final String RETRIEVER = "Myr Retriever";
    private static final String INSPECTOR = "Foundry Inspector";

    private static Card in(final Player ai, final ZoneType zone, final String name) {
        for (final Card c : ai.getCardsIn(zone)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    /**
     * The colourless mana that a round of the Ironworks loop leaves: two for each of the Myr Retriever and the Scrap Trawler
     * that is sacrificed, less what they cost to cast again (two and three), with every Foundry Inspector taking one off each.
     * One Inspector makes it one more than it takes, none makes it one less.
     */
    public static int ironworksNet(final int inspectors) {
        return 4 - Math.max(0, 3 - inspectors) - Math.max(0, 2 - inspectors);
    }

    /**
     * Krark-Clan Ironworks with Scrap Trawler and Myr Retriever (and a Foundry Inspector): the Retriever dies and returns the
     * Trawler from the graveyard, the Trawler is cast and dies and returns the Retriever (its mana value is less), the
     * Retriever is cast: each sacrifice is {C}{C} and the two casts cost less than they give. One step at a time, by where
     * the two are: a Trawler or a Retriever in the hand is cast; the Retriever on the battlefield with the Trawler in the
     * graveyard (or on the battlefield) is sacrificed; the Trawler on the battlefield with the Retriever in the graveyard is
     * sacrificed. The sacrifice is marked for the payment so that the Ironworks never takes the Inspector. Run only with
     * something to spend the mana on, until the pool holds what the hand costs.
     */
    private static SpellAbility ironworks(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(forge.game.phase.PhaseType.MAIN1) || ph.is(forge.game.phase.PhaseType.MAIN2))) {
            return null;
        }
        final Card kci = in(ai, ZoneType.Battlefield, KCI);
        if (kci == null) {
            return null;
        }
        int inspectors = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            inspectors += c.getName().equals(INSPECTOR) ? 1 : 0;
        }
        final Card retrieverIn = in(ai, ZoneType.Battlefield, RETRIEVER);
        final Card trawlerIn = in(ai, ZoneType.Battlefield, TRAWLER);
        final boolean retrieverKnown = retrieverIn != null || in(ai, ZoneType.Hand, RETRIEVER) != null
                || in(ai, ZoneType.Graveyard, RETRIEVER) != null;
        final boolean trawlerKnown = trawlerIn != null || in(ai, ZoneType.Hand, TRAWLER) != null
                || in(ai, ZoneType.Graveyard, TRAWLER) != null;
        if (ironworksNet(inspectors) <= 0 || !retrieverKnown || !trawlerKnown) {
            return null;
        }
        final int goal = ManaLoopPlan.goal(ai);
        if (goal <= 0 || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        // the casts first: what is in the hand is what the last sacrifice returned
        for (final String name : new String[] {TRAWLER, RETRIEVER}) {
            if (in(ai, ZoneType.Hand, name) != null) {
                final SpellAbility cast = LoopRunner.castFromHand(ai, candidates, name);
                if (cast != null && forge.ai.ComputerUtilMana.canPayManaCost(cast, ai, 0, false)) {
                    cast.setActivatingPlayer(ai);
                    if (approve.test(cast)) {
                        VetoLog.note(ai, "ironworks-loop | " + name + " cast, pool " + ai.getManaPool().totalMana() + " of " + goal);
                        return cast;
                    }
                }
                return null;
            }
        }
        final boolean retrieverGone = retrieverIn == null && in(ai, ZoneType.Graveyard, RETRIEVER) != null;
        final boolean trawlerGone = trawlerIn == null && in(ai, ZoneType.Graveyard, TRAWLER) != null;
        final Card victim;
        if (retrieverIn != null && (trawlerGone || trawlerIn != null)) {
            victim = retrieverIn;
        } else if (trawlerIn != null && retrieverGone) {
            victim = trawlerIn;
        } else {
            return null;
        }
        SpellAbility sac = null;
        for (final SpellAbility sa : candidates) {
            if (sa != null && sa.getHostCard() == kci && sa.isManaAbility()) {
                sac = sa;
                break;
            }
        }
        if (sac == null || RunawayLoop.exceeded(sac, LIMIT)) {
            return null;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c == victim) {
                c.setSVar("SacMe", "6");
            } else if (c.hasSVar("SacMe") && c.getSVar("SacMe").equals("6")) {
                c.removeSVar("SacMe");
            }
        }
        sac.setActivatingPlayer(ai);
        if (!approve.test(sac)) {
            return null;
        }
        VetoLog.note(ai, "ironworks-loop | the Ironworks takes " + victim.getName() + ", pool " + ai.getManaPool().totalMana()
                + " of " + goal);
        return sac;
    }

    // ------------------------------------------------------------------------------------------------ Deathmantle

    /** The tokens that come with a creature each time it comes to the battlefield or dies, for the creatures that make them. */
    private static int tokensWith(final Card c) {
        switch (c.getName()) {
            case "Wurmcoil Engine":
                return 2; // when it dies: a deathtouch Wurm and a lifelink Wurm
            case "Myr Battlesphere":
                return 4; // when it enters: four Myr
            case "Grave Titan":
                return 2; // when it enters: two Zombies
            default:
                return 0;
        }
    }

    /** The mana of a round of the Deathmantle loop: the creature and its tokens sacrificed, the {4} of the Deathmantle paid. */
    public static int deathmantleNet(final int tokens) {
        return 2 + 2 * tokens - 4;
    }

    /**
     * Nim Deathmantle and Ashnod's Altar with a creature that brings tokens (Wurmcoil Engine, Myr Battlesphere, Grave Titan):
     * the Altar takes the creature ({C}{C}), the Deathmantle pays {4} to return it with itself attached, the creature brings
     * its tokens again, and the Altar takes those for {C}{C} each. A round makes the tokens' mana plus two, less four.
     * One step at a time: the tokens go to the Altar first; then the creature, when the mana of the pool and the untapped
     * lands can pay the {4} once the Altar has given its two. The sacrifice is marked for the payment, as the Ironworks loop does.
     */
    private static SpellAbility deathmantle(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(forge.game.phase.PhaseType.MAIN1) || ph.is(forge.game.phase.PhaseType.MAIN2))) {
            return null;
        }
        final Card mantle = in(ai, ZoneType.Battlefield, "Nim Deathmantle");
        final Card altar = in(ai, ZoneType.Battlefield, "Ashnod's Altar");
        if (mantle == null || altar == null) {
            return null;
        }
        Card fat = null;
        final List<Card> tokens = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature() && c.isToken()) {
                tokens.add(c);
            } else if (c.isCreature() && tokensWith(c) > 0) {
                fat = c;
            }
        }
        if (fat == null || deathmantleNet(tokensWith(fat)) <= 0) {
            return null;
        }
        final int goal = ManaLoopPlan.goal(ai);
        if (goal <= 0 || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        int lands = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            lands += c.isLand() && !c.isTapped() && !c.getManaAbilities().isEmpty() ? 1 : 0;
        }
        final Card victim;
        if (!tokens.isEmpty()) {
            victim = tokens.get(0);
        } else if (ai.getManaPool().totalMana() + lands >= 2) {
            victim = fat; // the {4} is paid from the pool, the two of the Altar, and a land or two
        } else {
            return null;
        }
        SpellAbility sac = null;
        for (final SpellAbility sa : candidates) {
            if (sa != null && sa.getHostCard() == altar && sa.isManaAbility()) {
                sac = sa;
                break;
            }
        }
        if (sac == null || RunawayLoop.exceeded(sac, LIMIT)) {
            return null;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c == victim) {
                c.setSVar("SacMe", "6");
            } else if (c.hasSVar("SacMe") && c.getSVar("SacMe").equals("6")) {
                c.removeSVar("SacMe");
            }
        }
        sac.setActivatingPlayer(ai);
        if (!approve.test(sac)) {
            return null;
        }
        VetoLog.note(ai, "deathmantle-loop | the Altar takes " + victim.getName() + ", pool " + ai.getManaPool().totalMana()
                + " of " + goal);
        return sac;
    }
}
