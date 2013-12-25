package forge.game.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.item.IPaperCard;


public class RegisteredPlayer {
    private final Deck originalDeck;
    private Deck currentDeck;

    private static final Iterable<PaperCard> EmptyList = Collections.unmodifiableList(new ArrayList<PaperCard>());
    
    private LobbyPlayer player = null;
    
    private int startingLife = 20;
    private int startingHand = 7;
    private Iterable<IPaperCard> cardsOnBattlefield = null;
    private final List<IPaperCard> cardsInCommand = new ArrayList<IPaperCard>();
    private Iterable<? extends IPaperCard> schemes = null;
    private Iterable<PaperCard> planes = null;
    private PaperCard commander = null;
    private int teamNumber = -1; // members of teams with negative id will play FFA.
    
    public RegisteredPlayer(Deck deck0) {
        originalDeck = deck0;
        currentDeck = originalDeck;
    }

    public final Deck getOriginalDeck() {
        return originalDeck;
    }
    
    public final Deck getCurrentDeck() {
        return currentDeck;
    }
    
    public void setCurrentDeck(Deck currentDeck0) {
        this.currentDeck = currentDeck0; 
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
        for(IPaperCard pc : function)
            this.cardsInCommand.add(pc);
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
     * TODO: Write javadoc for this method.
     */
    public void restoreOriginalDeck() {
        currentDeck = originalDeck;
    }

    /**
     * @return the planes
     */
    public Iterable<PaperCard> getPlanes() {
        return planes == null ? EmptyList : planes;
    }

    public int getTeamNumber() {
        return teamNumber;
    }

    public void setTeamNumber(int teamNumber0) {
        this.teamNumber = teamNumber0;
    }

    
    public static RegisteredPlayer fromDeck(final Deck deck) {
        return new RegisteredPlayer(deck);
    }

    public static RegisteredPlayer forVanguard(final Deck deck, final PaperCard avatar) {
        RegisteredPlayer start = fromDeck(deck);
        start.setStartingLife(start.getStartingLife() + avatar.getRules().getLife());
        start.setStartingHand(start.getStartingHand() + avatar.getRules().getHand());
        start.addCardsInCommand(avatar);
        return start;
    }


    public static RegisteredPlayer forArchenemy(final Deck deck, final Iterable<PaperCard> schemes) {
        RegisteredPlayer start = fromDeck(deck);
        start.schemes = schemes;
        return start;
    }
    
    public static RegisteredPlayer forPlanechase(final Deck deck, final Iterable<PaperCard> planes) {
        RegisteredPlayer start = fromDeck(deck);
        start.planes = planes;
        return start;
    }
    
    public static RegisteredPlayer forCommander(final Deck deck) {
        RegisteredPlayer start = fromDeck(deck);
        start.commander = deck.get(DeckSection.Commander).get(0);
        start.setStartingLife(40);
        return start;
    }

    public LobbyPlayer getPlayer() {
        return player;
    }

    public RegisteredPlayer setPlayer(LobbyPlayer player0) {
        this.player = player0;
        return this;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public IPaperCard getCommander() {
        return commander;
    }    
}
