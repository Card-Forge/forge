package forge.gamemodes.puzzle;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;

import forge.ai.GameState;
import forge.game.Game;
import forge.game.GameType;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;

public class Puzzle extends GameState implements InventoryItem, Comparable<Puzzle> {
    String name;
    String filename;
    String goal;
    String url;
    String difficulty;
    String description;
    String targets;
    int targetCount = 1;
    int turns;
    boolean completed;

    public Puzzle(Map<String, List<String>> puzzleLines) {
        this(puzzleLines, "", false);
    }

    public Puzzle(Map<String, List<String>> puzzleLines, String filename, boolean completed) {
        loadMetaData(puzzleLines.get("metadata"));
        loadGameState(puzzleLines.get("state"));
        // Generate goal enforcement
        this.filename = filename;
        this.completed = completed;
    }

    private void loadMetaData(List<String> metadataLines) {
        for(String line : metadataLines) {
            String[] split = line.split(":", 2);
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
            } else if ("TargetCount".equalsIgnoreCase(split[0])) {
                this.targetCount = Integer.parseInt(split[1]);
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
            p.setStartingHandSize(maxHandSize);
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
        goalCard.setImmutable(true);
        goalCard.setOracleText(getGoalDescription());

        int turnCorr = 0;
        if (game.getPhaseHandler().getPhase() == PhaseType.CLEANUP) {
            turnCorr = 1;
        }

        int turnLimit = turns + turnCorr;

        // Default goal: win the game; lose on turn X
        String trig = "Mode$ Phase | Phase$ Cleanup | TriggerZones$ Command | Static$ True | " +
                "TurnCount$ " + turnLimit + " | TriggerDescription$ At the beginning of your cleanup step on the specified turn, you lose the game.";
        String eff = "DB$ LosesGame | Defined$ You";

        switch(goal.toLowerCase()) {
            case "win":
                // Handled as default
                break;
            case "survive":
                trig = "Mode$ Phase | Phase$ Upkeep | TriggerZones$ Command | Static$ True | " +
                       "TurnCount$ " + (turnLimit + 1) + " | TriggerDescription$ At the beginning of the upkeep step on the specified turn, you win the game.";
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
            case "put the specified permanent on the battlefield":
            case "play the specified permanent":
                if (targets == null) {
                    System.err.println("Error: target was not specified for the puzzle with an OTB permanent objective!");
                    break;
                }
                String trigPlay = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCards$ " + targets + " | " +
                        "Static$ True | TriggerDescription$ When the specified permanent enters the battlefield, you win the game.";
                String effPlay = "DB$ WinsGame | Defined$ You | ConditionCheckSVar$ PermCount | ConditionSVarCompare$ GE" + targetCount;
                final Trigger triggerPlay = TriggerHandler.parseTrigger(trigPlay, goalCard, true);
                triggerPlay.setOverridingAbility(AbilityFactory.getAbility(effPlay, goalCard));
                goalCard.addTrigger(triggerPlay);

                String countPerm = "Count$Valid " + targets;
                goalCard.setSVar("PermCount", countPerm);
                break;
            case "gain control of specified permanents":
                if (targets == null) {
                    targets = "Card.inZoneBattlefield+OppCtrl"; // by default, gain control of all opponent's permanents
                }
                String trigClear = "Mode$ ChangesController | ValidCards$ " + targets + " | Static$ True | " +
                        "TriggerDescription$ When the last permanent controlled by the opponent leaves the battlefield, you win the game.";
                String effClear = "DB$ WinsGame | Defined$ You | ConditionCheckSVar$ PermCount | ConditionSVarCompare$ EQ0";
                final Trigger triggerClear = TriggerHandler.parseTrigger(trigClear, goalCard, true);
                triggerClear.setOverridingAbility(AbilityFactory.getAbility(effClear, goalCard));
                goalCard.addTrigger(triggerClear);

                String countOTB = "Count$Valid " + targets;
                goalCard.setSVar("PermCount", countOTB);
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

    public boolean getCompleted() { return completed; }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.completed) {
            sb.append("[COMPLETED] ");
        }
        sb.append(name);
        return sb.toString();
    }

    public int compareTo(Puzzle pzl) throws ClassCastException {
        if (!(pzl instanceof Puzzle)) {
            throw new ClassCastException("Tried to compare a Puzzle object to a non-Puzzle object.");
        }

        if (this.completed == pzl.getCompleted()) {
            return getName().compareTo(pzl.getName());
        } else if (this.completed) {
            return 1;
        } else {
            return -1;
        }
    }

    public boolean savePuzzleSolve(final boolean completed) {
        if (!completed) {
            return false;
        }

        File directory = new File(ForgeConstants.USER_PUZZLE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File store = new File(ForgeConstants.USER_PUZZLE_DIR, filename + PuzzleIO.SUFFIX_COMPLETE);
        if (!store.exists()) {
            try {
                store.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.completed = true;
        }
        return true;
    }
}
