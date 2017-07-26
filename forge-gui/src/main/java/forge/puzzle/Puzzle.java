package forge.puzzle;

import forge.ai.GameState;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.model.FModel;

import java.util.List;
import java.util.Map;

public class Puzzle extends GameState implements InventoryItem, Comparable {
    String name;
    String goal;
    String url;
    String difficulty;
    String description;
    int turns;

    public Puzzle(Map<String, List<String>> puzzleLines) {
        loadMetaData(puzzleLines.get("metadata"));
        loadGameState(puzzleLines.get("state"));
        // Generate goal enforcement
    }

    private void loadMetaData(List<String> metadataLines) {
        for(String line : metadataLines) {
            String[] split = line.split(":");
            if ("Name".equalsIgnoreCase(split[0])) {
                this.name = split[1];
            } else if ("Goal".equalsIgnoreCase(split[0])) {
                this.goal = split[1];
            } else if ("Url".equalsIgnoreCase(split[0])) {
                this.url = split[1];
            } else if ("Turns".equalsIgnoreCase(split[0])) {
                this.turns = Integer.parseInt(split[1]);
            } else if ("Difficulty".equalsIgnoreCase(split[0])) {
                this.difficulty = split[1];
            } else if ("Description".equalsIgnoreCase(split[0])) {
                this.description = split[1];
            }
        }
    }

    private String getGoalDescription() {
        StringBuilder desc = new StringBuilder();

        String name = this.name == null ? "Unnamed Puzzle" : this.name;
        String goal = this.goal == null ? "(Unspecified)" : this.goal;
        String diff = this.difficulty == null ? "(Unspecified)" : this.difficulty;

        desc.append(name);
        desc.append("\nDifficulty: ");
        desc.append(diff);

        desc.append("\n\nGoal: ");
        desc.append(goal);
        desc.append("\nTurns Limit: ");
        desc.append(this.turns);

        if (this.description != null) {
            desc.append("\n\n");
            desc.append(this.description);
        }

        return desc.toString();
    }
    
    private void loadGameState(List<String> stateLines) {
        this.parse(stateLines);
    }

    public IPaperCard getPaperCard(final String cardName) {
        return FModel.getMagicDb().getCommonCards().getCard(cardName);
    }

    public void setupMaxPlayerHandSize(Game game, int maxHandSize) {
        for(Player p : game.getPlayers()) {
            p.setMaxHandSize(maxHandSize);
        }
    }

    public void addGoalEnforcement(Game game) {
        Player human = null;
        for(Player p : game.getPlayers()) {
            if (p.getController().isGuiPlayer()) {
                human = p;
            }
        }

        Card goalCard = new Card(-1, game);

        goalCard.setOwner(human);
        goalCard.setImageKey("t:puzzle");
        goalCard.setName("Puzzle Goal");
        goalCard.addType("Effect");
        goalCard.setOracleText(getGoalDescription());

        final String loseTrig = "Mode$ Phase | Phase$ Cleanup | TriggerZones$ Command | Static$ True | " +
                "TurnCount$ " + turns + " | TriggerDescription$ At the beginning of your cleanup step, you lose the game.";
        final String loseEff = "DB$ LosesGame | Defined$ You";

        final Trigger loseTrigger = TriggerHandler.parseTrigger(loseTrig, goalCard, true);

        loseTrigger.setOverridingAbility(AbilityFactory.getAbility(loseEff, goalCard));
        goalCard.addTrigger(loseTrigger);

        human.getZone(ZoneType.Command).add(goalCard);
    }

    @Override
    protected void applyGameOnThread(final Game game) {
        setupMaxPlayerHandSize(game, 7);
        super.applyGameOnThread(game);
        addGoalEnforcement(game);
    }

    @Override
    public String getItemType() {
        return "Puzzle";
    }

    @Override
    public String getImageKey(boolean altState) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    public String toString() { return name; }

    public int compareTo(Object pzl) throws ClassCastException {
        if (!(pzl instanceof Puzzle)) {
            throw new ClassCastException("Tried to compare a Puzzle object to a non-Puzzle object.");
        }
        
        return getName().compareTo(((Puzzle)pzl).getName());
    }
}
