package forge.puzzle;

import com.google.common.collect.Sets;
import forge.ai.GameState;
import forge.game.Game;
import forge.game.GameType;
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

public class Puzzle extends GameState implements InventoryItem, Comparable<Puzzle> {
    String name;
    String goal;
    String url;
    String difficulty;
    String description;
    String targets;
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
                this.name = split[1].trim();
            } else if ("Goal".equalsIgnoreCase(split[0])) {
                this.goal = split[1].trim();
            } else if ("Url".equalsIgnoreCase(split[0])) {
                this.url = split[1].trim();
            } else if ("Turns".equalsIgnoreCase(split[0])) {
                this.turns = Integer.parseInt(split[1]);
            } else if ("Difficulty".equalsIgnoreCase(split[0])) {
                this.difficulty = split[1].trim();
            } else if ("Description".equalsIgnoreCase(split[0])) {
                this.description = split[1].trim();
            } else if ("Targets".equalsIgnoreCase(split[0])) {
                this.targets = split[1].trim();
            }
        }
    }

    public String getGoalDescription() {
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
            desc.append(this.description.replace("\\n", "\n"));
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

        // Default goal: win the game; lose on turn X
        String trig = "Mode$ Phase | Phase$ Cleanup | TriggerZones$ Command | Static$ True | " +
                "TurnCount$ " + turns + " | TriggerDescription$ At the beginning of your cleanup step, you lose the game.";
        String eff = "DB$ LosesGame | Defined$ You";

        switch(goal.toLowerCase()) {
            case "win":
                // Handled as default
                break;
            case "survive":
                trig = "Mode$ Phase | Phase$ Upkeep | TriggerZones$ Command | Static$ True | " +
                       "TurnCount$ " + (turns + 1) + " | TriggerDescription$ At the beginning of your upkeep step, you win the game.";
                eff = "DB$ WinsGame | Defined$ You";
                break;
            case "destroy specified permanents":
            case "destroy specified creatures":
            case "remove specified permanents from the battlefield":
            case "kill specified creatures":
                if (targets == null) {
                    targets = "Creature.OppCtrl"; // by default, kill all opponent's creatures
                }
                String trigKill = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Any | ValidCards$ " + targets + " | " +
                        "Static$ True | TriggerDescription$ When the last permanent specified in the goal leaves the battlefield, you win the game.";
                String effKill = "DB$ WinsGame | Defined$ You | ConditionCheckSVar$ PermCount | ConditionSVarCompare$ EQ0";
                final Trigger triggerKill = TriggerHandler.parseTrigger(trigKill, goalCard, true);
                triggerKill.setOverridingAbility(AbilityFactory.getAbility(effKill, goalCard));
                goalCard.addTrigger(triggerKill);

                String countVar = "Count$Valid " + targets;
                goalCard.setSVar("PermCount", countVar);
                break;
            default:
                break;
        }

        final Trigger trigger = TriggerHandler.parseTrigger(trig, goalCard, true);
        trigger.setOverridingAbility(AbilityFactory.getAbility(eff, goalCard));

        goalCard.addTrigger(trigger);

        human.getZone(ZoneType.Command).add(goalCard);
    }

    @Override
    protected void applyGameOnThread(final Game game) {
        setupMaxPlayerHandSize(game, 7);
        super.applyGameOnThread(game);
        addGoalEnforcement(game);
        game.getRules().setAppliedVariants(Sets.newHashSet(GameType.Puzzle));
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

    public int compareTo(Puzzle pzl) throws ClassCastException {
        if (!(pzl instanceof Puzzle)) {
            throw new ClassCastException("Tried to compare a Puzzle object to a non-Puzzle object.");
        }
        
        return getName().compareTo(((Puzzle)pzl).getName());
    }
}
