package forge.gauntlet;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import forge.deck.Deck;


/**
 * Handles layout saving and loading.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class GauntletData {
    private int completed;
    private File activeFile;
    private String timestamp;
    private List<String> eventRecords = new ArrayList<String>();
    private List<String> eventNames = new ArrayList<String>();
    private Deck userDeck;
    private List<Deck> decks;


    /** Constructor. */
    public GauntletData() {
    }

    //========== Mutator / accessor methods

    /** @param file0 {@link java.io.File} */
    public void setActiveFile(final File file0) {
        this.activeFile = file0;
    }

    /** @return {@link java.io.File} */
    public File getActiveFile() {
        return this.activeFile;
    }

    /** */
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

    /** @return {@link java.lang.String} */
    public String getTimestamp() {
        return this.timestamp;
    }

    /** @param i0 int */
    public void setCompleted(final int i0) {
        this.completed = i0;
    }

    /** @return int */
    public int getCompleted() {
        return this.completed;
    }

    /** @param d0 {@link forge.deck.Deck} */
    public void setUserDeck(final Deck d0) {
        this.userDeck = d0;
    }

    /** @return d0 {@link forge.deck.Deck} */
    public Deck getUserDeck() {
        return this.userDeck;
    }

    /** @return List<String> */
    public List<String> getDeckNames() {
        final List<String> names = new ArrayList<String>();
        for (final Deck d : decks) { names.add(d.getName()); }
        return names;
    }

    /** @param records0 List<String> */
    public void setEventRecords(final List<String> records0) {
        this.eventRecords = records0;
    }

    /** @return List<String> */
    public List<String> getEventRecords() {
        return this.eventRecords;
    }

    /** @param names0 List<String> */
    public void setEventNames(final List<String> names0) {
        this.eventNames = names0;
    }

    /** @return List<String> */
    public List<String> getEventNames() {
        return this.eventNames;
    }

    /** @param decks0 List<Deck> */
    public void setDecks(final List<Deck> decks0) {
        this.decks = decks0;
    }

    /** @return List<Deck> */
    public List<Deck> getDecks() {
        return this.decks;
    }
}
