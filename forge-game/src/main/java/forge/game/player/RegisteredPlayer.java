package forge.game.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import forge.LobbyPlayer;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.item.IPaperCard;
import forge.item.PaperCard;

public class RegisteredPlayer {
    private final Deck originalDeck; // never return or modify this instance (it's a reference to game resources)
    private Deck currentDeck;

    private static final Iterable<PaperCard> EmptyList = Collections.unmodifiableList(new ArrayList<>());
    
    private LobbyPlayer player = null;
    
    private int startingLife = 20;
    private int startingHand = 7;
    private Iterable<IPaperCard> cardsOnBattlefield = null;
    private Iterable<? extends IPaperCard> schemes = null;
    private Iterable<PaperCard> planes = null;
    private Iterable<PaperCard> conspiracies = null;
    private List<PaperCard> commanders = Lists.newArrayList();
    private List<PaperCard> vanguardAvatars = null;
    private PaperCard planeswalker = null;
    private int teamNumber = -1; // members of teams with negative id will play FFA.
    private boolean randomFoil = false;
    
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

    public int getStartingHand() {
        return startingHand;
    }
    public void setStartingHand(int startingHand0) {
        this.startingHand = startingHand0;
    }

    public Iterable<? extends IPaperCard> getSchemes() {
        return schemes == null ? EmptyList : schemes;
    }

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
        if (currentDeck.has(DeckSection.Conspiracy)) {
            conspiracies = currentDeck.get(DeckSection.Conspiracy).toFlatList();
        }
    }

    public int getTeamNumber() {
        return teamNumber;
    }

    public void setTeamNumber(int teamNumber0) {
        this.teamNumber = teamNumber0;
    }

    public static RegisteredPlayer forCommander(final Deck deck) {
        RegisteredPlayer start = new RegisteredPlayer(deck);
        start.commanders = deck.getCommanders();
        start.setStartingLife(40);
        return start;
    }

    public static RegisteredPlayer forVariants(final int playerCount,
    		final Set<GameType> appliedVariants, final Deck deck,	              //General vars
    		final Iterable<PaperCard> schemes, final boolean playerIsArchenemy,   //Archenemy specific vars
    		final Iterable<PaperCard> planes, final CardPool vanguardAvatar) {   //Planechase and Vanguard
        
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
            start.commanders = deck.getCommanders();
            start.setStartingLife(start.getStartingLife() + 20); // 903.7: ...each player sets his or her life total to 40
		                                                         // Modified for layering of variants to life +20
    	}
        if (appliedVariants.contains(GameType.Oathbreaker)) {
            start.commanders = deck.getCommanders();
        }
    	if (appliedVariants.contains(GameType.TinyLeaders)) {
            start.commanders = deck.getCommanders();
            start.setStartingLife(start.getStartingLife() + 5);
        }
        if (appliedVariants.contains(GameType.Brawl)) {
            start.commanders = deck.getCommanders();
            start.setStartingLife(start.getStartingLife() + 10);
        }
    	if (appliedVariants.contains(GameType.Planechase)) {
            start.planes = planes;
    	}
        if (appliedVariants.contains(GameType.Vanguard) || appliedVariants.contains(GameType.MomirBasic)
                || appliedVariants.contains(GameType.MoJhoSto)) { //fix the crash, if somehow the avatar is null, get it directly from the deck
            start.setVanguardAvatars(vanguardAvatar == null ? deck.get(DeckSection.Avatar).toFlatList() : vanguardAvatar.toFlatList());
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

    public List<PaperCard> getCommanders() {
        return commanders;
    }
    public void assignCommander() {
    	commanders = currentDeck.getCommanders();
    }

    public List<PaperCard> getVanguardAvatars() {
        return vanguardAvatars;
    }
    public void assignVanguardAvatar() {
        CardPool section = currentDeck.get(DeckSection.Avatar);
        setVanguardAvatars(section == null ? null : section.toFlatList());
    }
    private void setVanguardAvatars(List<PaperCard> vanguardAvatars0) {
        vanguardAvatars = vanguardAvatars0;
        if (vanguardAvatars == null) { return; }
        for (PaperCard avatar: vanguardAvatars) {
            setStartingLife(getStartingLife() + avatar.getRules().getLife());
            setStartingHand(getStartingHand() + avatar.getRules().getHand());
        }

    }

    public PaperCard getPlaneswalker() {
        return planeswalker;
    }
    public void setPlaneswalker(PaperCard planeswalker0) {
        planeswalker = planeswalker0;
        if (planeswalker != null) {
            currentDeck.getMain().remove(planeswalker); //ensure planeswalker removed from main deck
        }
    }

    public void restoreDeck() {
        currentDeck = (Deck) originalDeck.copyTo(originalDeck.getName());
    }

    public boolean useRandomFoil() {
        return randomFoil;
    }
    public void setRandomFoil(boolean useRandomFoil) {
        randomFoil = useRandomFoil;
    }
}
