package forge;

/**
 * <p>
 * CardListFilter interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public interface CardListFilter {
    /**
     * <p>
     * addCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    boolean addCard(Card c);

    /**
     * a CardListFilter to get all cards that are tapped.
     */
    CardListFilter tapped = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isTapped();
        }
    };

    /**
     * a CardListFilter to get all cards that are untapped.
     */
    CardListFilter untapped = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isUntapped();
        }
    };

    /**
     * a CardListFilter to get all creatures.
     */
    CardListFilter creatures = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isCreature();
        }
    };

    /**
     * a CardListFilter to get all enchantments.
     */
    CardListFilter enchantments = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEnchantment();
        }
    };

    /**
     * a CardListFilter to get all equipment.
     */
    CardListFilter equipment = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEquipment();
        }
    };

    /**
     * a CardListFilter to get all unenchanted cards in a list.
     */
    CardListFilter unenchanted = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all enchanted cards in a list.
     */
    CardListFilter enchanted = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all nontoken cards.
     */
    CardListFilter nonToken = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isToken();
        }
    };

    /**
     * a CardListFilter to get all token cards.
     */
    CardListFilter token = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isToken();
        }
    };

    /**
     * a CardListFilter to get all nonbasic lands.
     */
    CardListFilter nonBasicLand = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all basicLands.
     */
    CardListFilter basicLands = new CardListFilter() {
        public boolean addCard(final Card c) {
            // the isBasicLand() check here may be sufficient...
            return c.isLand() && c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all artifacts.
     */
    CardListFilter artifacts = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all nonartifacts.
     */
    CardListFilter nonartifacts = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all lands.
     */
    CardListFilter lands = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isLand();
        }
    };

    /**
     * a CardListFilter to get all nonlands.
     */
    CardListFilter nonlands = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isLand();
        }
    };

    /**
     * a CardListFilter to get all cards that are black.
     */
    CardListFilter black = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isBlack();
        }
    };

    /**
     * a CardListFilter to get all cards that are blue.
     */
    CardListFilter blue = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isBlue();
        }
    };

    /**
     * a CardListFilter to get all cards that are green.
     */
    CardListFilter green = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isGreen();
        }
    };

    /**
     * a CardListFilter to get all cards that are red.
     */
    CardListFilter red = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isRed();
        }
    };

    /**
     * a CardListFilter to get all cards that are white.
     */
    CardListFilter white = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isWhite();
        }
    };

}
