package forge.interfaces;

public interface IDevModeCheats {

    void setCanPlayUnlimitedLands(boolean canPlayUnlimitedLands0);

    void setViewAllCards(boolean canViewAll);

    void generateMana();

    void rollbackPhase();

    void dumpGameState();

    void setupGameState();

    void tutorForCard();

    void addCountersToPermanent();

    void removeCountersFromPermanent();

    void tapPermanents();

    void untapPermanents();

    void setPlayerLife();

    void winGame();

    void addCardToHand();

    void addCardToBattlefield();

    void addTokenToBattlefield();

    void addCardToLibrary();

    void addCardToGraveyard();

    void addCardToExile();

    void castASpell();

    void repeatLastAddition();

    /*
     * Exiles cards from specified player's hand. Will prompt user for player and cards.
     */
    void exileCardsFromHand();

    /*
     * Exiles cards from play. Will prompt user for player and cards.
     */
    void exileCardsFromBattlefield();

    void removeCardsFromGame();


    void riggedPlanarRoll();

    void planeswalkTo();

    void askAI(boolean useSimulation);

    /**
     * Implementation of {@link IDevModeCheats} that disallows cheating by
     * performing no action whatsoever when any of its methods is called.
     */
    IDevModeCheats NO_CHEAT = new IDevModeCheats() {
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
        public void rollbackPhase() {
        }

        @Override
        public void dumpGameState() {
        }
        @Override
        public void addCountersToPermanent() {
        }
        @Override
        public void removeCountersFromPermanent() {
        }
        @Override
        public void addCardToHand() {
        }
        @Override
        public void exileCardsFromHand() {
        }
        @Override
        public void exileCardsFromBattlefield() {
        }
        @Override
        public void addCardToBattlefield() {
        }
        @Override
        public void addTokenToBattlefield() {
        }
        @Override
        public void addCardToLibrary() {
        }
        @Override
        public void addCardToGraveyard() {
        }
        @Override
        public void addCardToExile() {
        }
        @Override
        public void castASpell() {
        }
        @Override
        public void repeatLastAddition() {
        }
        @Override
        public void removeCardsFromGame() {
        }
        @Override
        public void askAI(boolean useSimulation) {
        }
    };

}