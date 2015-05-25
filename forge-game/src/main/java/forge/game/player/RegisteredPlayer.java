package forge.game.player;

import forge.LobbyPlayer;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.item.IPaperCard;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class RegisteredPlayer {
    private final Deck originalDeck; // never return or modify this instance (it's a reference to game resources)
    private Deck currentDeck;

    private static final Iterable<PaperCard> EmptyList = Collections.unmodifiableList(new ArrayList<PaperCard>());
    
    private LobbyPlayer player = null;
    
    private int startingLife = 20;
    private int startingHand = 7;
    private Iterable<IPaperCard> cardsOnBattlefield = null;
    private final List<IPaperCard> cardsInCommand = new ArrayList<IPaperCard>();
    private Iterable<? extends IPaperCard> schemes = null;
    private Iterable<PaperCard> planes = null;
    private Iterable<PaperCard> conspiracies = null;
    private PaperCard commander = null;
    private int teamNumber = -1; // members of teams with negative id will play FFA.
    
    public RegisteredPlayer(Deck deck0) {
        originalDeck = deck0;
        restoreDeck();
    }

    public final Deck getDeck() {
        return currentDeck;
    }
    
    public final int getStartingLife() {
        return startingLife;
    }
    public final Iterable<? extends IPaperCard> getCardsOnBattlefield() {
        return cardsOnBattlefield == null ? EmptyList : cardsOnBattlefield;
    }

    public final void setStartingLife(int startingLife) {
        this.startingLife = startingLife;
    }

    public final void setCardsOnBattlefield(Iterable<IPaperCard> cardsOnTable) {
        this.cardsOnBattlefield = cardsOnTable;
    }

    /**
     * @return the startingHand
     */
    public int getStartingHand() {
        return startingHand;
    }

    /**
     * @param startingHand0 the startingHand to set
     */
    public void setStartingHand(int startingHand0) {
        this.startingHand = startingHand0;
    }

    /**
     * @return the cardsInCommand
     */
    public Iterable<? extends IPaperCard> getCardsInCommand() {
        return cardsInCommand == null ? EmptyList : cardsInCommand;
    }

    /**
     * @param function the cardsInCommand to set
     */
    public void addCardsInCommand(Iterable<? extends IPaperCard> function) {
        for (IPaperCard pc : function) {
            this.cardsInCommand.add(pc);
        }
    }

    public void addCardsInCommand(IPaperCard pc) {
        this.cardsInCommand.add(pc);
    }
    
    /**
     * @return the schemes
     */
    public Iterable<? extends IPaperCard> getSchemes() {
        return schemes == null ? EmptyList : schemes;
    }

    /**
     * @return the planes
     */
    public Iterable<PaperCard> getPlanes() {
        return planes == null ? EmptyList : planes;
    }
    public void setPlanes(Iterable<PaperCard> planes0) {
        planes = planes0;
    }

    public Iterable<PaperCard> getConspiracies() {
        return conspiracies == null ? EmptyList : conspiracies;
    }
    public void assignConspiracies() {
        if (currentDeck.has(DeckSection.Conspiracy))
            conspiracies = currentDeck.get(DeckSection.Conspiracy).toFlatList();
    }

    public int getTeamNumber() {
        return teamNumber;
    }

    public void setTeamNumber(int teamNumber0) {
        this.teamNumber = teamNumber0;
    }

    public static RegisteredPlayer forCommander(final Deck deck) {
        RegisteredPlayer start = new RegisteredPlayer(deck);
        start.commander = deck.get(DeckSection.Commander).get(0);
        start.setStartingLife(40);
        return start;
    }

    public static RegisteredPlayer forVariants(final int playerCount,
    		final Set<GameType> appliedVariants, final Deck deck,	              //General vars
    		final Iterable<PaperCard> schemes, final boolean playerIsArchenemy,   //Archenemy specific vars
    		final Iterable<PaperCard> planes, final PaperCard vanguardAvatar) {   //Planechase and Vanguard
        
    	RegisteredPlayer start = new RegisteredPlayer(deck);
    	if (appliedVariants.contains(GameType.Archenemy) && playerIsArchenemy) {
    		start.setStartingLife(40); // 904.5: The Archenemy has 40 life.
    		start.schemes = schemes;
    	}
    	if (appliedVariants.contains(GameType.ArchenemyRumble)) {
    		start.setStartingLife(40);
    		start.schemes = schemes;
    	}
    	if (appliedVariants.contains(GameType.Commander)) {
            start.commander = deck.has(DeckSection.Commander) ? deck.get(DeckSection.Commander).get(0) : null;
            start.setStartingLife(start.getStartingLife() + 20); // 903.7: ...each player sets his or her life total to 40
		                                                         // Modified for layering of variants to life +20
    	}
        if (appliedVariants.contains(GameType.TinyLeaders)) {
            start.commander = deck.has(DeckSection.Commander) ? deck.get(DeckSection.Commander).get(0) : null;
            start.setStartingLife(start.getStartingLife() + 5);
        }
    	if (appliedVariants.contains(GameType.Planechase)) {
            start.planes = planes;
    	}
    	if (vanguardAvatar != null) {
            start.setStartingLife(start.getStartingLife() + vanguardAvatar.getRules().getLife());
            start.setStartingHand(start.getStartingHand() + vanguardAvatar.getRules().getHand());
            start.addCardsInCommand(vanguardAvatar);
    	}
    	return start;
    }

    public LobbyPlayer getPlayer() {
        return player;
    }

    public RegisteredPlayer setPlayer(LobbyPlayer player0) {
        this.player = player0;
        return this;
    }

    public IPaperCard getCommander() {
        return commander;
    }
    public void assignCommander() {
        CardPool section = currentDeck.get(DeckSection.Commander);
        commander = section == null ? null : section.get(0);
    }

    public void restoreDeck() {
        currentDeck = (Deck) originalDeck.copyTo(originalDeck.getName());
    }

    private boolean randomFoil = false;
    public void setRandomFoil(boolean useRandomFoil) {
        this.randomFoil = useRandomFoil;
    }

    public boolean useRandomFoil() {
        return randomFoil;
    }
}
