package forge.gauntlet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import forge.deck.Deck;


/**
 * Handles layout saving and loading.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class GauntletData {
    @XStreamOmitField
    private String name; // set based on the the filename on load
    
    private int completed;
    private String timestamp;
    private List<String> eventRecords = new ArrayList<String>();
    private List<String> eventNames = new ArrayList<String>();
    private Deck userDeck;
    private List<Deck> decks;


    /** Constructor. */
    public GauntletData() {
    }

    //========== Mutator / accessor methods

    public void setName(String name0) {
        name = name0;
    }

    public String getName() {
        return name;
    }

    public void stamp() {
        final DateFormat dateFormat = new SimpleDateFormat("dd-mm-yy, H:m");
        this.timestamp = dateFormat.format(new Date()).toString();
    }

    /** Resets a gauntlet data to an unplayed state, then stamps and saves. */
    public void reset() {
        this.completed = 0;
        this.stamp();
        this.eventRecords.clear();
        this.userDeck = null;

        for (int i = 0; i < decks.size(); i++) {
            this.eventRecords.add("");
        }

        GauntletIO.saveGauntlet(this);
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public void setCompleted(final int i0) {
        this.completed = i0;
    }

    public int getCompleted() {
        return this.completed;
    }

    public void setUserDeck(final Deck d0) {
        this.userDeck = d0;
    }

    public Deck getUserDeck() {
        return this.userDeck;
    }

    public List<String> getDeckNames() {
        final List<String> names = new ArrayList<String>();
        for (final Deck d : decks) { names.add(d.getName()); }
        return names;
    }

    public void setEventRecords(final List<String> records0) {
        this.eventRecords = records0;
    }

    public List<String> getEventRecords() {
        return this.eventRecords;
    }

    public void setEventNames(final List<String> names0) {
        this.eventNames = names0;
    }

    public List<String> getEventNames() {
        return this.eventNames;
    }

    public void setDecks(final List<Deck> decks0) {
        this.decks = decks0;
    }

    public List<Deck> getDecks() {
        return this.decks;
    }
}
