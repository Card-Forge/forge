package forge.gamemodes.rogue;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import forge.deck.Deck;
import forge.gamemodes.match.HostedMatch;
import forge.item.PaperCard;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Main container for a Rogue Commander run state.
 * Tracks deck evolution, life persistence, path progress, and match history.
 */
public class RogueRun {

    @XStreamOmitField
    private String name;  // Set based on filename on load

    // Run Configuration
    private RogueDeck selectedRogueDeck;    // Selected Rogue Deck identifier
    private String timestamp;                   // Creation timestamp

    // Run State
    private Deck currentDeck;                   // Player's evolving deck (starts as Start Deck copy)
    private int currentLife;                    // Persistent life total (starts at 20)
    private int startingLife;                   // Initial life (default: 20)
    private int currentGold;                    // Currency (for future Bazaar support)
    private int currentEchoes;                  // Meta-currency (for future Codex support)
    private RoguePath path;                      // The generated path
    private int currentNodeIndex;               // Current position on path
    private RogueRunState runState;             // Current state of the run

    // Match History
    private List<String> matchResults;     // W/L record per match
    private int completedMatches;          // Number of completed matches
    private int matchesWon;                // Win counter
    private int matchesLost;               // Loss counter

    // Transient (runtime only, not serialized)
    @XStreamOmitField
    private transient HostedMatch hostedMatch = null;

    // Constructors
    public RogueRun() {
        this.startingLife = 20;
        this.currentLife = 20;
        this.currentGold = 2;
        this.currentEchoes = 2;
        this.currentNodeIndex = 0;
        this.runState = RogueRunState.STARTED;
        this.matchResults = new ArrayList<>();
        this.completedMatches = 0;
        this.matchesWon = 0;
        this.matchesLost = 0;
        stamp();
    }

    public RogueRun(RogueDeck selectedRogueDeck, RoguePath path) {
        this();
        this.selectedRogueDeck = selectedRogueDeck;
        // Create deep copy of start deck
        this.currentDeck = new Deck(selectedRogueDeck.getStartDeck());
        this.path = path;
    }

    // Timestamp management
    public void stamp() {
        final DateFormat dateFormat = new SimpleDateFormat("MM-dd-yy, H:m");
        timestamp = dateFormat.format(new Date());
    }

    // Path navigation
    public void nextNode() {
        if (currentNodeIndex < path.getNodeCount() - 1) {
            currentNodeIndex++;
        }
    }

    public RoguePathNode getCurrentNode() {
        return path.getNode(currentNodeIndex);
    }

    public boolean isRunComplete() {
        return currentNodeIndex >= path.getNodeCount() - 1 &&
               (getCurrentNode() == null || getCurrentNode().isCompleted());
    }

    public boolean isRunFailed() {
        return runState == RogueRunState.LOST || currentLife <= 0;
    }

    public void setRunFailed(boolean failed) {
        if (failed) {
            this.runState = RogueRunState.LOST;
        }
    }

    public boolean isRunWon() {
        return runState == RogueRunState.WON;
    }

    public void setRunWon(boolean won) {
        if (won) {
            this.runState = RogueRunState.WON;
        }
    }

    public RogueRunState getRunState() {
        return runState;
    }

    public void setRunState(RogueRunState state) {
        this.runState = state;
    }

    /**
     * Called after deserialization to ensure state is initialized.
     * Handles backward compatibility with old save files.
     */
    private Object readResolve() {
        // Initialize state if it's null (old save files)
        if (runState == null) {
            if (currentLife <= 0) {
                runState = RogueRunState.LOST;
            } else if (isRunComplete()) {
                runState = RogueRunState.WON;
            } else {
                runState = RogueRunState.STARTED;
            }
        }
        return this;
    }

    // Match result tracking
    public void recordMatchResult(boolean won) {
        completedMatches++;
        if (won) {
            matchesWon++;
            matchResults.add("W");
            // Mark current node as completed
            if (getCurrentNode() != null) {
                getCurrentNode().setCompleted(true);
            }
        } else {
            matchesLost++;
            matchResults.add("L");
        }
    }

    // Deck management
    public void addCardsToRun(List<PaperCard> cards) {
        if (currentDeck != null && cards != null) {
            for (PaperCard card : cards) {
                currentDeck.getMain().add(card);
            }
        }
    }

    public void removeCardsFromRun(List<PaperCard> cards) {
        if (currentDeck != null && cards != null) {
            for (PaperCard card : cards) {
                currentDeck.getMain().remove(card);
            }
        }
    }

    // Life management
    public void healLife(int amount) {
        currentLife = Math.min(currentLife + amount, startingLife);
    }

    public void setLifeAfterMatch(int finalLife) {
        this.currentLife = finalLife;
    }

    // Match hosting (transient)
    public void setHostedMatch(HostedMatch match) {
        this.hostedMatch = match;
    }

    public HostedMatch getHostedMatch() {
        return hostedMatch;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        if (name != null && name.startsWith(RogueIO.PREFIX_LOCKED)) {
            return name.substring(RogueIO.PREFIX_LOCKED.length());
        }
        return name;
    }

    public RogueDeck getSelectedRogueDeck() {
        return selectedRogueDeck;
    }

    public void setSelectedRogueDeck(RogueDeck selectedRogueDeck) {
        this.selectedRogueDeck = selectedRogueDeck;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Deck getCurrentDeck() {
        return currentDeck;
    }

    public void setCurrentDeck(Deck currentDeck) {
        this.currentDeck = currentDeck;
    }

    public int getCurrentLife() {
        return currentLife;
    }

    public void setCurrentLife(int currentLife) {
        this.currentLife = currentLife;
    }

    public int getStartingLife() {
        return startingLife;
    }

    public void setStartingLife(int startingLife) {
        this.startingLife = startingLife;
        this.currentLife = startingLife;
    }

    public int getCurrentGold() {
        return currentGold;
    }

    public void setCurrentGold(int currentGold) {
        this.currentGold = currentGold;
    }

    public int getCurrentEchoes() {
        return currentEchoes;
    }

    public void setCurrentEchoes(int currentEchoes) {
        this.currentEchoes = currentEchoes;
    }

    public RoguePath getPath() {
        return path;
    }

    public void setPath(RoguePath path) {
        this.path = path;
    }

    public int getCurrentNodeIndex() {
        return currentNodeIndex;
    }

    public void setCurrentNodeIndex(int currentNodeIndex) {
        this.currentNodeIndex = currentNodeIndex;
    }

    public List<String> getMatchResults() {
        return matchResults;
    }

    public int getCompletedMatches() {
        return completedMatches;
    }

    public int getMatchesWon() {
        return matchesWon;
    }

    public int getMatchesLost() {
        return matchesLost;
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + matchesWon + "-" + matchesLost + ", " + currentLife + " life)";
    }
}
