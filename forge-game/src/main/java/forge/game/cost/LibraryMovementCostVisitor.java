package forge.game.cost;

import forge.game.zone.ZoneType;

/**
 * Answers whether paying a cost moves any card to or from a library.
 *
 * CR 605.1a: an activated ability is not a mana ability if its cost or effect moves any card to
 * or from a library. Costs that merely look at a library, such as revealing its top card, move
 * nothing and so keep the inherited false, as do costs that touch only the hand, the graveyard,
 * the battlefield or exile.
 *
 * Like the matching predicate on SpellAbilityEffect, this reads the cost as scripted rather than
 * the game state, because CR 605.1a says to disregard replacement effects other than
 * self-replacement effects when evaluating the criteria.
 */
public class LibraryMovementCostVisitor extends ICostVisitor.Base<Boolean> {

    private static final LibraryMovementCostVisitor INSTANCE = new LibraryMovementCostVisitor();

    private LibraryMovementCostVisitor() {
    }

    /**
     * True when paying the given cost moves at least one card to or from a library.
     */
    public static boolean movesCardToOrFromLibrary(final Cost cost) {
        if (cost == null) {
            return false;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (Boolean.TRUE.equals(part.accept(INSTANCE))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visit(final CostMill cost) {
        // top of library to graveyard
        return true;
    }

    @Override
    public Boolean visit(final CostDraw cost) {
        // top of library to hand
        return true;
    }

    @Override
    public Boolean visit(final CostPutCardToLib cost) {
        // some other zone to library
        return true;
    }

    @Override
    public Boolean visit(final CostExile cost) {
        return cost.getFrom().contains(ZoneType.Library);
    }
}
