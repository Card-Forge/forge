package forge.ai.simulation;

import java.util.List;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCost;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.cost.CostSacrifice;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The infinite turn engine of the vintage artifact decks (Lurrus Vault Key, Raker Shops, Blue Tinker): Time Vault
 * (tap: take an extra turn, does not untap) with something that untaps it (Manifold Key for {1}, Tezzeret, Cruel
 * Captain for free). Every turn the vault is tapped for the extra turn and untapped again: each turn that is
 * taken therefore pays for the next. The rollouts cannot see an extra turn's worth, so this is played without them.
 * <p>
 * Goblin Welder and Goblin Engineer bring the pieces from the graveyard: the Engineer puts a piece there when it
 * enters (see {@link #pick}), the Welder swaps a spare artifact for it, the Engineer sacrifices one for it.
 * Everything is read from the abilities of the cards, not from names.
 */
public final class VaultLoop {
    /** The vault's extra turns stop when this few cards are left in the library: the loop must not deck the AI. */
    static final int LIBRARY_FLOOR = 3;

    private VaultLoop() {
    }

    /** An artifact that is tapped for an extra turn (Time Vault). */
    public static boolean isVault(final Card c) {
        if (c == null || !c.isArtifact()) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.AddTurn && sa.isAbility() && sa.getPayCosts() != null && sa.getPayCosts().hasTapCost()) {
                return true;
            }
        }
        return false;
    }

    /** Something that untaps an artifact: Manifold Key, Voltaic Key, Tezzeret, Cruel Captain. */
    public static boolean isUntapper(final Card c) {
        return untapAbility(c) != null;
    }

    private static SpellAbility untapAbility(final Card c) {
        if (c == null) {
            return null;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Untap && sa.isAbility() && sa.usesTargeting()
                    && sa.getParamOrDefault("ValidTgts", "").contains("Artifact")) {
                return sa;
            }
        }
        return null;
    }

    /** The first card that untaps an artifact, of any type: Tezzeret, Cruel Captain is a planeswalker, not an artifact. */
    private static Card untapper(final CardCollectionView cards) {
        for (final Card c : cards) {
            if (isUntapper(c)) {
                return c;
            }
        }
        return null;
    }

    private static boolean isPiece(final Card c) {
        return isVault(c) || c.isArtifact() && isUntapper(c);
    }

    private static boolean playable(final Player ai, final SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return sa.canPlay() && ComputerUtilCost.canPayCost(sa, ai, false);
    }

    private static Card first(final CardCollectionView cards, final boolean vault) {
        for (final Card c : cards) {
            if (vault ? isVault(c) : c.isArtifact() && isUntapper(c)) {
                return c;
            }
        }
        return null;
    }

    /**
     * The card to put into the graveyard when searching for an artifact (Goblin Engineer) or to return from it: the
     * vault while none is in play, else the untapper. Null when the deck has no vault or nothing is missing.
     */
    public static Card pick(final Player ai, final CardCollectionView options) {
        if (ai == null || options == null) {
            return null;
        }
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final CardCollectionView yard = ai.getCardsIn(ZoneType.Graveyard);
        final Card vaultOption = first(options, true);
        final Card keyOption = first(options, false);
        if (vaultOption != null && first(board, true) == null && first(yard, true) == null) {
            return vaultOption;
        }
        if (keyOption != null && first(board, false) == null && first(yard, false) == null
                && (first(board, true) != null || first(yard, true) != null || vaultOption == null)) {
            return keyOption;
        }
        return null;
    }

    /** Whether this deck has the loop: a vault and an untapper somewhere in the deck, in play or in a zone it can come from. */
    public static boolean inDeck(final Player ai) {
        boolean vault = false;
        boolean key = false;
        for (final ZoneType z : new ZoneType[] { ZoneType.Hand, ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Library }) {
            final CardCollectionView cards = ai.getCardsIn(z);
            vault |= first(cards, true) != null;
            key |= first(cards, false) != null;
        }
        return vault && key;
    }

    /**
     * The ability to play next, or null. In order: tap the vault in the second main phase for an extra turn (with the
     * library not nearly empty), untap a tapped vault with the untapper, bring a piece back from the graveyard.
     */
    public static SpellAbility sureCast(final Player ai, final List<SpellAbility> candidates) {
        if (ai == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !game.getStack().isEmpty() || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final Card vault = first(board, true);
        if (vault != null) {
            if (!vault.isTapped()) {
                if (ph.is(PhaseType.MAIN2) && ai.getCardsIn(ZoneType.Library).size() > LIBRARY_FLOOR) {
                    for (final SpellAbility sa : candidates) {
                        if (sa != null && vault.equals(sa.getHostCard()) && sa.getApi() == ApiType.AddTurn && playable(ai, sa)) {
                            return sa;
                        }
                    }
                }
            }
            for (final SpellAbility sa : candidates) {
                if (!vault.isTapped() || sa == null || sa.getApi() != ApiType.Untap || !sa.usesTargeting() || sa.getHostCard() == null
                        || !ai.equals(sa.getHostCard().getController()) || !isUntapper(sa.getHostCard())
                        || sa.getHostCard().equals(vault)) {
                    continue;
                }
                sa.setActivatingPlayer(ai);
                sa.resetTargets();
                if (sa.canTarget(vault) && sa.getTargets().add(vault) && playable(ai, sa)) {
                    return sa;
                }
                sa.resetTargets();
            }
        }
        final SpellAbility ring = untapRing(ai, candidates);
        if (ring != null) {
            return ring;
        }
        final SpellAbility forge = forgeLoop(ai, candidates);
        if (forge != null) {
            return forge;
        }
        final SpellAbility cauldron = cauldronStep(ai, candidates);
        if (cauldron != null) {
            return cauldron;
        }
        final SpellAbility paradox = ParadoxOutcome.sureCast(ai, candidates);
        if (paradox != null) {
            return paradox;
        }
        final SpellAbility tinker = tinkerStep(ai, candidates);
        if (tinker != null) {
            return tinker;
        }
        final SpellAbility oswald = oswaldStep(ai, candidates);
        return oswald != null ? oswald : bringBack(ai, candidates);
    }

    /**
     * The One Ring with something to untap it (Tezzeret, Cruel Captain, Manifold Key): every untap is one more burden
     * counter and one more card than the last time, paid for with life at each upkeep. Only while the library and the
     * life total can carry it, and never in place of the vault, which is worth more.
     */
    private static SpellAbility untapRing(final Player ai, final List<SpellAbility> candidates) {
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final Card ring = named(board, "The One Ring");
        final Card vault = first(board, true);
        if (ring == null || !ring.isTapped() || vault != null && vault.isTapped()
                || ai.getCardsIn(ZoneType.Library).size() <= LIBRARY_FLOOR + ring.getCounters(forge.game.card.CounterEnumType.BURDEN)
                || ai.getLife() <= 2 * (ring.getCounters(forge.game.card.CounterEnumType.BURDEN) + 3)) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            if (sa == null || sa.getApi() != ApiType.Untap || !sa.usesTargeting() || sa.getHostCard() == null
                    || !ai.equals(sa.getHostCard().getController()) || !isUntapper(sa.getHostCard())) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            sa.resetTargets();
            if (sa.canTarget(ring) && sa.getTargets().add(ring) && playable(ai, sa)) {
                return sa;
            }
            sa.resetTargets();
        }
        return null;
    }

    private static Card named(final CardCollectionView cards, final String name) {
        for (final Card c : cards) {
            if (name.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }

    /**
     * Raker Shops' kill: with Mystic Forge (cast artifacts and colorless spells from the top of the library) and Glaring
     * Fleshraker (whenever a colorless creature enters, a spawn token and one damage to each opponent... and again for
     * Top's own cast), Sensei's Divining Top draws and goes back on top of the library, and is cast from there for {1}
     * (the Eldrazi Spawn pay for it): each round is one damage, until the library is short or the opponent is dead.
     */
    private static SpellAbility forgeLoop(final Player ai, final List<SpellAbility> candidates) {
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        if (named(board, "Mystic Forge") == null || named(board, "Glaring Fleshraker") == null
                || ai.getCardsIn(ZoneType.Library).size() <= LIBRARY_FLOOR) {
            return null;
        }
        // each round draws one card and deals one damage: only worth starting when the library outlasts the opponent's life
        final Player opp = ai.getWeakestOpponent();
        if (opp == null || ai.getCardsIn(ZoneType.Library).size() - LIBRARY_FLOOR < opp.getLife()) {
            return null;
        }
        final Card onTop = ai.getCardsIn(ZoneType.Library).get(0);
        if ("Sensei's Divining Top".equals(onTop.getName())) {
            for (final SpellAbility sa : candidates) {
                if (sa != null && onTop.equals(sa.getHostCard()) && sa.isSpell() && playable(ai, sa)) {
                    return sa; // cast from the top of the library with Mystic Forge
                }
            }
            return null;
        }
        final Card top = named(board, "Sensei's Divining Top");
        if (top == null || top.isTapped()) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            if (sa != null && top.equals(sa.getHostCard()) && sa.getApi() == ApiType.Draw && playable(ai, sa)) {
                return sa; // draw a card and put Top back on the library
            }
        }
        return null;
    }

    /**
     * Agatha's Soul Cauldron: a Goblin Welder or Goblin Engineer in the graveyard is exiled with it (when the AI has a
     * creature for the +1/+1 counter), so that every creature with a counter has its ability and removal of the real
     * goblin no longer ends the combo.
     */
    private static SpellAbility cauldronStep(final Player ai, final List<SpellAbility> candidates) {
        final Card cauldron = named(ai.getCardsIn(ZoneType.Battlefield), "Agatha's Soul Cauldron");
        if (cauldron == null || cauldron.isTapped()) {
            return null;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Exile)) {
            if (cauldron.equals(c.getExiledWith())) {
                return null; // one is enough
            }
        }
        boolean creature = false;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            creature |= c.isCreature();
        }
        Card goblin = named(ai.getCardsIn(ZoneType.Graveyard), "Goblin Welder");
        if (goblin == null) {
            goblin = named(ai.getCardsIn(ZoneType.Graveyard), "Goblin Engineer");
        }
        if (!creature || goblin == null) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            if (sa != null && cauldron.equals(sa.getHostCard()) && sa.getApi() == ApiType.ChangeZone && sa.isAbility()) {
                sa.setActivatingPlayer(ai);
                sa.resetTargets();
                if (sa.getTargets().add(goblin) && playable(ai, sa)) {
                    return sa;
                }
                sa.resetTargets();
            }
        }
        return null;
    }

    /**
     * What a spell that searches the library for an artifact and puts it onto the battlefield (Tinker) should take: the
     * vault when a key is in play or in hand, the key when the vault is in play. Null when it has no opinion.
     */
    public static Card tinkerPick(final Player ai, final CardCollectionView options) {
        if (ai == null || options == null) {
            return null;
        }
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final Card vaultOption = first(options, true);
        final Card keyOption = first(options, false);
        final boolean vaultInPlay = first(board, true) != null;
        if (vaultOption != null && !vaultInPlay
                && (untapper(board) != null || untapper(ai.getCardsIn(ZoneType.Hand)) != null)) {
            return vaultOption; // the key (Manifold Key, or Tezzeret in play or in hand) is there: the vault, never The One Ring
        }
        if (keyOption != null && vaultInPlay && untapper(board) == null) {
            return keyOption;
        }
        if (vaultOption == null && !vaultInPlay && inDeck(ai)) {
            return stepToVault(options); // Oswald Fiddlebender: a stepping stone toward the vault, not a piece
        }
        return null;
    }

    /** An artifact that is neither the vault nor what untaps it and costs 1 or more: the next sacrifice of Oswald Fiddlebender. */
    private static Card stepToVault(final CardCollectionView options) {
        Card best = null;
        for (final Card c : options) {
            if (c.isArtifact() && !isPiece(c) && !isUntapper(c) && !isProtected(c) && c.getCMC() >= 1
                    && (best == null || c.getCMC() < best.getCMC())) {
                best = c;
            }
        }
        return best;
    }

    /** The mana that wins games by itself is not thrown away as the fodder of a sacrifice. */
    private static boolean isProtected(final Card c) {
        final String n = c.getName();
        return "Black Lotus".equals(n) || "Mana Crypt".equals(n);
    }

    /**
     * Tinker: cast it when it completes the loop (the vault with the key in play or in hand, the key with the vault in
     * play) and the artifact it would sacrifice is neither piece of the loop.
     */
    private static SpellAbility tinkerStep(final Player ai, final List<SpellAbility> candidates) {
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final CardCollectionView library = ai.getCardsIn(ZoneType.Library);
        final boolean needVault = first(board, true) == null && first(library, true) != null
                && (untapper(board) != null || untapper(ai.getCardsIn(ZoneType.Hand)) != null);
        final boolean needKey = first(board, true) != null && untapper(board) == null && first(library, false) != null;
        if (!needVault && !needKey) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            if (sa == null || !sa.isSpell() || sa.getApi() != ApiType.ChangeZone || sa.getHostCard() == null
                    || !ai.equals(sa.getHostCard().getController()) || !sa.getHostCard().isInZone(ZoneType.Hand)
                    || !"Library".equals(sa.getParamOrDefault("Origin", ""))
                    || !"Battlefield".equals(sa.getParamOrDefault("Destination", ""))
                    || !sa.getParamOrDefault("ChangeType", "").contains("Artifact")
                    || sa.getPayCosts() == null || !sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            final CostSacrifice cost = sa.getPayCosts().getCostPartByType(CostSacrifice.class);
            if (cost == null || cost.payCostFromSource()) {
                continue;
            }
            final CardCollectionView chosen = ComputerUtil.chooseSacrificeType(ai, cost.getType(), sa, sa.getTargetCard(),
                    false, Math.max(1, cost.getAbilityAmount(sa)), CardCollection.EMPTY);
            if (chosen == null || chosen.isEmpty()) {
                continue;
            }
            boolean safe = true;
            for (final Card c : chosen) {
                safe &= !isPiece(c);
            }
            if (safe && playable(ai, sa)) {
                return sa;
            }
        }
        return null;
    }


    /** Oswald Fiddlebender: tap and sacrifice an artifact to fetch one that costs exactly one more. */
    private static boolean isOswald(final SpellAbility sa, final Player ai) {
        final Card host = sa == null ? null : sa.getHostCard();
        return host != null && host.isCreature() && host.isInZone(ZoneType.Battlefield) && ai.equals(host.getController())
                && sa.isAbility() && sa.getApi() == ApiType.ChangeZone
                && "Library".equals(sa.getParamOrDefault("Origin", "")) && "Battlefield".equals(sa.getParamOrDefault("Destination", ""))
                && sa.getParamOrDefault("ChangeType", "").contains("cmcEQX")
                && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)
                && sa.getPayCosts().hasTapCost();
    }

    /** An artifact of the AI that may be sacrificed for the loop: not the vault, not what untaps, not the fast mana. */
    private static boolean isFodder(final Card c) {
        return c.isArtifact() && !isPiece(c) && !isUntapper(c) && !isProtected(c);
    }

    /**
     * Oswald Fiddlebender (Magical Tinkering): the sacrifice decides what comes: an artifact that costs one less than the
     * vault brings the vault, one that costs one less than the key brings the key. Without such an artifact the
     * cheapest one is traded up toward it (a Moxen for a one drop, the one drop for the vault). The sacrifice is
     * chosen through the AIPreferenceOverride of the card, which the cost payment reads (never a piece of the loop).
     */
    private static SpellAbility oswaldStep(final Player ai, final List<SpellAbility> candidates) {
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final CardCollectionView library = ai.getCardsIn(ZoneType.Library);
        Card goal = null;
        if (first(board, true) == null) {
            goal = first(library, true);
        } else if (untapper(board) == null) {
            goal = first(library, false);
        }
        for (final SpellAbility sa : candidates) {
            if (!isOswald(sa, ai)) {
                continue;
            }
            final Card host = sa.getHostCard();
            host.removeSVar("AIPreferenceOverride"); // a choice of an earlier turn must not stay
            if (goal == null || host.isTapped()) {
                continue;
            }
            // the sacrifices on offer, by mana value
            int sacCmc = -1;
            final int want = goal.getCMC() - 1;
            Card lowest = null;
            for (final Card c : board) {
                if (!isFodder(c)) {
                    continue;
                }
                if (c.getCMC() == want) {
                    sacCmc = want;
                    break;
                }
                if (c.getCMC() < want && (lowest == null || c.getCMC() < lowest.getCMC())) {
                    lowest = c;
                }
            }
            if (sacCmc < 0 && lowest != null) {
                // a stepping stone: the library must have an artifact that costs one more than the cheapest fodder
                for (final Card c : library) {
                    if (c.isArtifact() && !isPiece(c) && !isUntapper(c) && !isProtected(c) && c.getCMC() == lowest.getCMC() + 1) {
                        sacCmc = lowest.getCMC();
                        break;
                    }
                }
            }
            if (sacCmc < 0) {
                continue;
            }
            final StringBuilder valid = new StringBuilder("Artifact.cmcEQ").append(sacCmc);
            for (final Card c : board) {
                if (c.isArtifact() && !isFodder(c)) {
                    valid.append("+!named").append(c.getName());
                }
            }
            host.setSVar("AIPreferenceOverride", valid.toString());
            if (playable(ai, sa)) {
                return sa;
            }
            host.removeSVar("AIPreferenceOverride");
        }
        return null;
    }

    /** Goblin Welder or Goblin Engineer: a piece of the loop from the graveyard onto the battlefield. */
    private static SpellAbility bringBack(final Player ai, final List<SpellAbility> candidates) {
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final CardCollectionView yard = ai.getCardsIn(ZoneType.Graveyard);
        Card piece = first(board, true) == null ? first(yard, true) : null; // the vault first, then what untaps it
        if (piece == null && first(board, false) == null) {
            piece = first(yard, false);
        }
        if (piece == null) {
            return null;
        }
        Card fodder = null;
        boolean pieceInPlay = false;
        for (final Card c : board) {
            if (!c.isArtifact()) {
                continue;
            }
            if (isPiece(c)) {
                pieceInPlay = true;
            } else if (fodder == null || c.getCMC() < fodder.getCMC() || c.getCMC() == fodder.getCMC() && c.isToken()) {
                fodder = c;
            }
        }
        if (fodder == null) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !host.isCreature() || !ai.equals(host.getController()) || !sa.isAbility()
                    || !host.isInZone(ZoneType.Battlefield)) {
                continue;
            }
            if (sa.getApi() == ApiType.TargetOnly && sa.getSubAbility() != null && sa.getSubAbility().getApi() == ApiType.TargetOnly
                    && sa.usesTargeting()) {
                // Goblin Welder: the spare artifact is swapped for the piece
                sa.setActivatingPlayer(ai);
                sa.resetTargets();
                final SpellAbility yardSa = sa.getSubAbility();
                yardSa.resetTargets();
                // the targets are checked again when the ability resolves: both must still be there
                if (sa.getTargets().add(fodder) && yardSa.getTargets().add(piece) && playable(ai, sa)) {
                    return sa;
                }
                sa.resetTargets();
                yardSa.resetTargets();
            } else if (sa.getApi() == ApiType.ChangeZone && "Graveyard".equals(sa.getParamOrDefault("Origin", ""))
                    && "Battlefield".equals(sa.getParamOrDefault("Destination", "")) && sa.usesTargeting()
                    && !pieceInPlay && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(forge.game.cost.CostSacrifice.class)) {
                // Goblin Engineer: only while no piece is in play that the sacrifice could take
                sa.setActivatingPlayer(ai);
                sa.resetTargets();
                if (piece.getCMC() <= 3 && sa.canTarget(piece) && sa.getTargets().add(piece) && playable(ai, sa)) {
                    return sa;
                }
                sa.resetTargets();
            }
        }
        return null;
    }
}
