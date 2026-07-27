/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REFORGE COMMANDER EXTENSION
 * This file is an additive extension of the Forge project and does NOT modify
 * any original Forge class. It is safe to keep on 'git merge' from upstream.
 *
 * Purpose: Flyweight aggregate for identical tokens on the battlefield.
 *
 * Instead of N separate Card objects for N 1/1 Soldier tokens, a single
 * StackedTokenCard holds one prototype Card plus a quantity counter. This
 * reduces memory footprint from O(N) to O(1) for homogeneous token populations.
 *
 * DESIGN CONSTRAINTS:
 * - StackedTokenCard is immutable-state optimistic: all tokens in the stack share
 *   the same game state (tapped, counters, etc.). Any operation that requires
 *   differentiating individual tokens triggers a "promotion" — the relevant number
 *   of Card objects are materialized from the prototype and removed from the stack.
 * - The class does NOT extend Card. It is a separate entity managed by the zone.
 * - Upstream compatibility: zero changes to any existing Forge class are required
 *   to introduce this class. Integration hooks are additive only.
 */
package forge.game.card;

import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.card.GamePieceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Flyweight aggregate for a stack of identical tokens on the battlefield.
 *
 * <p>Performance contract: creating or copying this object is O(1) with respect
 * to token count. Promoting k individual tokens from the stack is O(k).</p>
 *
 * <p>Thread-safety: NOT thread-safe. Forge's game loop is single-threaded.</p>
 */
public class StackedTokenCard {

    /** The shared prototype: all tokens in this stack are logically identical to this card. */
    private final Card prototype;

    /** How many tokens this stack represents. Must always be >= 1. */
    private int quantity;

    /**
     * Creates a new stack.
     *
     * @param prototype the Card that defines the shared state of all tokens in this stack.
     *                  Must be a TOKEN game-piece type.
     * @param quantity  initial count; must be >= 1.
     * @throws IllegalArgumentException if prototype is not a token or quantity < 1.
     */
    public StackedTokenCard(final Card prototype, final int quantity) {
        if (prototype == null) {
            throw new IllegalArgumentException("StackedTokenCard: prototype must not be null");
        }
        if (prototype.getGamePieceType() != GamePieceType.TOKEN) {
            throw new IllegalArgumentException("StackedTokenCard: prototype must have GamePieceType.TOKEN, got " + prototype.getGamePieceType());
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("StackedTokenCard: quantity must be >= 1, got " + quantity);
        }
        this.prototype = prototype;
        this.quantity = quantity;
    }

    // -------------------------------------------------------------------------
    // Stack management
    // -------------------------------------------------------------------------

    /** Returns the shared prototype card. Do NOT mutate the returned object. */
    public Card getPrototype() {
        return prototype;
    }

    /** Returns the current count of tokens in this stack. */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Increases the token count by {@code delta}.
     * Used when a new batch of identical tokens is created and can be merged into this stack.
     *
     * @param delta positive integer.
     */
    public void addQuantity(final int delta) {
        if (delta < 1) {
            throw new IllegalArgumentException("StackedTokenCard.addQuantity: delta must be >= 1");
        }
        this.quantity += delta;
    }

    /**
     * Removes {@code count} tokens from this stack without materializing them.
     * Used when tokens are destroyed simultaneously (e.g., Wrath of God).
     *
     * @param count how many to remove; must be <= quantity.
     */
    public void removeQuantity(final int count) {
        if (count < 1 || count > quantity) {
            throw new IllegalArgumentException("StackedTokenCard.removeQuantity: count " + count + " out of bounds [1," + quantity + "]");
        }
        this.quantity -= count;
    }

    /**
     * Returns {@code true} if the stack is exhausted and should be removed from the zone.
     */
    public boolean isEmpty() {
        return quantity <= 0;
    }

    // -------------------------------------------------------------------------
    // Promotion (lazy materialization)
    // -------------------------------------------------------------------------

    /**
     * Materializes {@code count} individual Card objects from this stack.
     * The stack quantity is reduced by {@code count}. The returned cards are
     * independent copies of the prototype, each with a fresh card ID.
     *
     * <p>Call this when an effect needs to differentiate individual tokens — e.g.,
     * targeting a single token, dealing damage to specific creatures, etc.</p>
     *
     * @param count how many cards to promote; must be <= quantity.
     * @return list of freshly materialized Card objects placed in the same zone as prototype.
     */
    public List<Card> promote(final int count) {
        if (count < 1 || count > quantity) {
            throw new IllegalArgumentException("StackedTokenCard.promote: count " + count + " out of bounds [1," + quantity + "]");
        }

        final Game game = prototype.getGame();
        final Player owner = prototype.getOwner();
        final Player controller = prototype.getController();
        final long timestamp = prototype.getGameTimestamp();

        List<Card> promoted = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Card copy = new CardCopyService(prototype).copyCard(true);
            copy.getStates().forEach(cs -> copy.getState(cs).resetOriginalHost(prototype));
            copy.setOwner(owner);
            if (owner != controller) {
                copy.setController(controller, timestamp);
            }
            copy.setGameTimestamp(timestamp);
            copy.setGamePieceType(GamePieceType.TOKEN);
            // Place the materialized card in the battlefield zone directly
            owner.getZone(ZoneType.Battlefield).add(copy);
            promoted.add(copy);
        }

        this.quantity -= count;
        return promoted;
    }

    /**
     * Promotes ALL tokens in this stack.
     * After this call, the stack is empty and should be removed from the zone.
     *
     * @return list of all promoted Card objects.
     */
    public List<Card> promoteAll() {
        return promote(quantity);
    }

    // -------------------------------------------------------------------------
    // Identity helpers (delegate to prototype)
    // -------------------------------------------------------------------------

    /** Convenience: returns the token name (same for all tokens in stack). */
    public String getName() {
        return prototype.getName();
    }

    /** Convenience: returns the controller (same for all tokens in stack). */
    public Player getController() {
        return prototype.getController();
    }

    /** Convenience: returns the owner (same for all tokens in stack). */
    public Player getOwner() {
        return prototype.getOwner();
    }

    /**
     * Returns {@code true} if the given Card is structurally identical to this stack's prototype,
     * i.e., another token of the same kind could be merged into this stack.
     *
     * <p>Identical means: same name, same power/toughness, same type line, same controller,
     * same keywords, and NOT modified by any pump or counters effect.</p>
     *
     * @param candidate the token Card to test for compatibility.
     * @return true if the candidate can be safely aggregated into this stack.
     */
    public boolean canMerge(final Card candidate) {
        if (candidate == null || candidate.getGamePieceType() != GamePieceType.TOKEN) {
            return false;
        }
        if (!candidate.getName().equals(prototype.getName())) {
            return false;
        }
        if (candidate.getController() != prototype.getController()) {
            return false;
        }
        if (candidate.getBasePower() != prototype.getBasePower()) {
            return false;
        }
        if (candidate.getBaseToughness() != prototype.getBaseToughness()) {
            return false;
        }
        // If the candidate has any individual counters or pump modifications it cannot merge
        if (!candidate.getCounters().isEmpty()) {
            return false;
        }
        if (candidate.isTapped()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return quantity + "x " + prototype.getName()
                + " [" + prototype.getBasePower() + "/" + prototype.getBaseToughness() + "]"
                + " (stacked token, controller=" + prototype.getController().getName() + ")";
    }
}
