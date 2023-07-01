package forge.gamemodes.tournament;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import forge.deck.Deck;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;

public class TournamentData {
    @XStreamOmitField
    private String name; // set based on the the filename on load

    private transient HostedMatch hostedMatch = null;

    private int completed;
    private String timestamp;
    private List<String> eventRecords = new ArrayList<>();
    private List<String> eventNames = new ArrayList<>();
    private Deck userDeck;
    private List<Deck> decks;

    public TournamentData() {
    }

    public void setName(String name0) {
        name = name0;
    }

    public void rename(final String newName) {
        File newpath = new File(ForgeConstants.GAUNTLET_DIR.userPrefLoc, newName + ".dat");
        File oldpath = new File(ForgeConstants.GAUNTLET_DIR.userPrefLoc, name + ".dat");
        oldpath.renameTo(newpath);

        name = newName;
        TournamentIO.saveTournament(this);
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        if (name.startsWith(TournamentIO.PREFIX_LOCKED)) { //trim locked prefix if needed
            return name.substring(TournamentIO.PREFIX_LOCKED.length());
        }
        return name;
    }

    public void stamp() {
        final DateFormat dateFormat = new SimpleDateFormat("MM-dd-yy, H:m");
        timestamp = dateFormat.format(new Date());
    }

    /** Resets a Tournament data to an unplayed state, then stamps and saves. */
    public void reset() {
        completed = 0;
        stamp();
        eventRecords.clear();

        for (int i = 0; i < decks.size(); i++) {
            eventRecords.add("");
        }

        TournamentIO.saveTournament(this);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setCompleted(final int i0) {
        completed = i0;
    }

    public int getCompleted() {
        return completed;
    }

    public void setUserDeck(final Deck d0) {
        userDeck = d0;
    }

    public Deck getUserDeck() {
        return userDeck;
    }

    public List<String> getDeckNames() {
        final List<String> names = new ArrayList<>();
        for (final Deck d : decks) { names.add(d.getName()); }
        return names;
    }

    public void setEventRecords(final List<String> records0) {
        eventRecords = records0;
    }

    public List<String> getEventRecords() {
        return eventRecords;
    }

    public void setEventNames(final List<String> names0) {
        eventNames = names0;
    }

    public List<String> getEventNames() {
        return eventNames;
    }

    public void setDecks(final List<Deck> decks0) {
        decks = decks0;
    }

    public List<Deck> getDecks() {
        return decks;
    }

    public void startRound(final List<RegisteredPlayer> players, final RegisteredPlayer human) {
        hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(GameType.Tournament, null, players, human, GuiBase.getInterface().getNewGuiGame());
    }

    public void nextRound(final List<RegisteredPlayer> players, final RegisteredPlayer human) {
        if (hostedMatch == null) {
            throw new IllegalStateException("Cannot advance round when no match has been hosted.");
        }

        hostedMatch.endCurrentGame();
        startRound(players, human);
    }

    @Override
    public String toString() {
        String str = getDisplayName();
        if (decks != null) {
            str += " (" + decks.size() + " opponents)";
        }
        return str;
    }
}