package forge.interfaces;

public interface IDevModeCheats {

    void setCanPlayUnlimitedLands(boolean canPlayUnlimitedLands0);

    void setViewAllCards(boolean canViewAll);

    void generateMana();

    void dumpGameState();

    void setupGameState();

    void tutorForCard();

    void addCountersToPermanent();

    void tapPermanents();

    void untapPermanents();

    void setPlayerLife();

    void winGame();

    void addCardToHand();

    void addCardToBattlefield();

    void riggedPlanarRoll();

    void planeswalkTo();

    /**
     * Implementation of {@link IDevModeCheats} that disallows cheating by
     * performing no action whatsoever when any of its methods is called.
     */
    public static final IDevModeCheats NO_CHEAT = new IDevModeCheats() {
        @Override
        public void winGame() {
        }
        @Override
        public void untapPermanents() {
        }
        @Override
        public void tutorForCard() {
        }
        @Override
        public void tapPermanents() {
        }
        @Override
        public void setupGameState() {
        }
        @Override
        public void setViewAllCards(final boolean canViewAll) {
        }
        @Override
        public void setPlayerLife() {
        }
        @Override
        public void setCanPlayUnlimitedLands(final boolean canPlayUnlimitedLands0) {
        }
        @Override
        public void riggedPlanarRoll() {
        }
        @Override
        public void planeswalkTo() {
        }
        @Override
        public void generateMana() {
        }
        @Override
        public void dumpGameState() {
        }
        @Override
        public void addCountersToPermanent() {
        }
        @Override
        public void addCardToHand() {
        }
        @Override
        public void addCardToBattlefield() {
        }
    };

}