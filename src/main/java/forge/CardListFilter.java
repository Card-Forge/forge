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
    CardListFilter TAPPED = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isTapped();
        }
    };

    /**
     * a CardListFilter to get all cards that are untapped.
     */
    CardListFilter UNTAPPED = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isUntapped();
        }
    };

    /**
     * a CardListFilter to get all creatures.
     */
    CardListFilter CREATURES = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isCreature();
        }
    };

    /**
     * a CardListFilter to get all enchantments.
     */
    CardListFilter ENCHANTMENTS = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEnchantment();
        }
    };

    /**
     * a CardListFilter to get all equipment.
     */
    CardListFilter EQUIPMENT = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEquipment();
        }
    };

    /**
     * a CardListFilter to get all unenchanted cards in a list.
     */
    CardListFilter UNENCHANTED = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all enchanted cards in a list.
     */
    CardListFilter ENCHANTED = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all nontoken cards.
     */
    CardListFilter NON_TOKEN = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isToken();
        }
    };

    /**
     * a CardListFilter to get all token cards.
     */
    CardListFilter TOKEN = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isToken();
        }
    };

    /**
     * a CardListFilter to get all nonbasic lands.
     */
    CardListFilter NON_BASIC_LAND = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all basicLands.
     */
    CardListFilter BASIC_LANDS = new CardListFilter() {
        public boolean addCard(final Card c) {
            // the isBasicLand() check here may be sufficient...
            return c.isLand() && c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all artifacts.
     */
    CardListFilter ARTIFACTS = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all nonartifacts.
     */
    CardListFilter NON_ARTIFACTS = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all lands.
     */
    CardListFilter LANDS = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isLand();
        }
    };

    /**
     * a CardListFilter to get all nonlands.
     */
    CardListFilter NON_LANDS = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isLand();
        }
    };

    /**
     * a CardListFilter to get all cards that are black.
     */
    CardListFilter BLACK = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isBlack();
        }
    };

    /**
     * a CardListFilter to get all cards that are blue.
     */
    CardListFilter BLUE = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isBlue();
        }
    };

    /**
     * a CardListFilter to get all cards that are green.
     */
    CardListFilter GREEN = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isGreen();
        }
    };

    /**
     * a CardListFilter to get all cards that are red.
     */
    CardListFilter RED = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isRed();
        }
    };

    /**
     * a CardListFilter to get all cards that are white.
     */
    CardListFilter WHITE = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isWhite();
        }
    };

}
