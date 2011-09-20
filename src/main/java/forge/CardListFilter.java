package forge;

/**
 * <p>CardListFilter interface.</p>
 *
 * @author Forge
 * @version $Id$
 */
public interface CardListFilter {
    /**
     * <p>addCard.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a boolean.
     */
    boolean addCard(Card c);
    
    /**
     * a CardListFilter to get all cards that are tapped.
     */
    public static final CardListFilter tapped = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isTapped();
        }
    };

    /**
     * a CardListFilter to get all cards that are untapped.
     */
    public static final CardListFilter untapped = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isUntapped();
        }
    };

    /**
     * a CardListFilter to get all creatures.
     */
    public static final CardListFilter creatures = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isCreature();
        }
    };

    /**
     * a CardListFilter to get all enchantments.
     */
    public static final CardListFilter enchantments = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEnchantment();
        }
    };

    /**
     * a CardListFilter to get all equipment.
     */
    public static final CardListFilter equipment = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEquipment();
        }
    };

    /**
     * a CardListFilter to get all unenchanted cards in a list.
     */
    public static final CardListFilter unenchanted = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all enchanted cards in a list.
     */
    public static final CardListFilter enchanted = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all nontoken cards.
     */
    public static final CardListFilter nonToken = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isToken();
        }
    };

    /**
     * a CardListFilter to get all token cards.
     */
    public static final CardListFilter token = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isToken();
        }
    };

    /**
     * a CardListFilter to get all nonbasic lands.
     */
    public static final CardListFilter nonBasicLand = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all basicLands.
     */
    public static final CardListFilter basicLands = new CardListFilter() {
        public boolean addCard(final Card c) {
            //the isBasicLand() check here may be sufficient...
            return c.isLand() && c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all artifacts.
     */
    public static final CardListFilter artifacts = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all nonartifacts.
     */
    public static final CardListFilter nonartifacts = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all lands.
     */
    public static final CardListFilter lands = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isLand();
        }
    };

    /**
     * a CardListFilter to get all nonlands.
     */
    public static final CardListFilter nonlands = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isLand();
        }
    };   
    

    /**
     * a CardListFilter to get all cards that are black.
     */
    public static final CardListFilter black = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isBlack();
        }
    };

    /**
     * a CardListFilter to get all cards that are blue.
     */
    public static final CardListFilter blue = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isBlue();
        }
    };

    /**
     * a CardListFilter to get all cards that are green.
     */
    public static final CardListFilter green = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isGreen();
        }
    };

    /**
     * a CardListFilter to get all cards that are red.
     */
    public static final CardListFilter red = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isRed();
        }
    };

    /**
     * a CardListFilter to get all cards that are white.
     */
    public static final CardListFilter white = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isWhite();
        }
    };

}
