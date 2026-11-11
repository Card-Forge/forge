package forge.toolbox;

/**
 * Something that answers a right click (desktop) off the match board, before the right click falls back to toggling the
 * magnifier: the Arena deck editor's card lists, whose left click adds or removes a card, open their menu on it.
 */
public interface ContextClickable {
    /** A right click at this point (local coordinates). True if it did something, which stops the magnifier toggle. */
    boolean contextClick(float x, float y);
}
