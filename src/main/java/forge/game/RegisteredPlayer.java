package forge.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.item.CardPrinted;
import forge.item.IPaperCard;


public class RegisteredPlayer {
    private final Deck originalDeck;
    private Deck currentDeck;

    private static final Iterable<CardPrinted> EmptyList = Collections.unmodifiableList(new ArrayList<CardPrinted>());
    
    private LobbyPlayer player = null;
    
    private int startingLife = 20;
    private int startingHand = 7;
    private Iterable<IPaperCard> cardsOnBattlefield = null;
    private final List<IPaperCard> cardsInCommand = new ArrayList<IPaperCard>();
    private Iterable<? extends IPaperCard> schemes = null;
    private Iterable<CardPrinted> planes = null;
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
    public final Iterable<? extends IPaperCard> getCardsOnBattlefield(Player p) {
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
    public Iterable<? extends IPaperCard> getCardsInCommand(Player p) {
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
    public Iterable<? extends IPaperCard> getSchemes(Player p) {
        return schemes == null ? EmptyList : schemes;
    }

    /**
     * @param schemes0 the schemes to set
     */
    public void setSchemes(Iterable<? extends IPaperCard> s) {
        this.schemes = s;
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
    public Iterable<CardPrinted> getPlanes(final Player p) {
        return planes == null ? EmptyList : planes;
    }

    /**
     * @param planes0 the planes to set
     */
    public void setPlanes(Iterable<CardPrinted> planes0) {
        this.planes = planes0;
    }

    public int getTeamNumber() {
        return teamNumber;
    }

    public void setTeamNumber(int teamNumber0) {
        this.teamNumber = teamNumber0;
    }

    
    public static RegisteredPlayer fromDeck(final Deck deck) {
        RegisteredPlayer start = new RegisteredPlayer(deck);
        if( deck != null && deck.has(DeckSection.Commander)) {
            start.setStartingLife(40);
            start.addCardsInCommand(deck.get(DeckSection.Commander).toFlatList());
        }
        return new RegisteredPlayer(deck);
    }

    public static RegisteredPlayer forVanguard(final Deck deck, final CardPrinted avatar) {
        RegisteredPlayer start = fromDeck(deck);
        start.setStartingLife(start.getStartingLife() + avatar.getRules().getLife());
        start.setStartingHand(start.getStartingHand() + avatar.getRules().getHand());
        start.addCardsInCommand(avatar);
        return start;
    }


    public static RegisteredPlayer forArchenemy(final Deck deck, final Iterable<CardPrinted> schemes) {
        RegisteredPlayer start = fromDeck(deck);
        start.setSchemes(schemes);
        return start;
    }
    
    public static RegisteredPlayer forPlanechase(final Deck deck, final Iterable<CardPrinted> planes) {
        RegisteredPlayer start = fromDeck(deck);
        start.setPlanes(planes);
        return start;
    }

    public LobbyPlayer getPlayer() {
        return player;
    }

    public RegisteredPlayer setPlayer(LobbyPlayer player0) {
        this.player = player0;
        return this;
    }


}
