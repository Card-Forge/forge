package forge.interfaces;

import forge.game.card.CardView;

/**
 * Interface that receives requests on whether a {@link Card} can be shown.
 */
public interface IMayViewCards {

    /**
     * @param c
     *            a {@link CardView}
     * @return whether {@code c} can be shown.
     */
    boolean mayView(CardView c);

    /**
     * @param c
     *            a {@link CardView}
     * @return whether the flip side of {@code c} can be shown.
     */
    boolean mayFlip(CardView c);

    /** {@link IMayViewCards} that lets you view all cards unconditionally. */
    public static final IMayViewCards ALL = new IMayViewCards() {
        @Override
        public boolean mayView(final CardView c) {
            return true;
        }

        @Override
        public boolean mayFlip(final CardView c) {
            return c.hasAlternateState();
        }
    };
}
