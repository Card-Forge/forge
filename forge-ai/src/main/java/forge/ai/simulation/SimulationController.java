package forge.ai.simulation;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.spellability.SpellAbility;

public class SimulationController {
    private static int MAX_DEPTH = 2;
    
    private int recursionDepth;
    
    public SimulationController() {
    }
    
    public boolean shouldRecurse() {
        return recursionDepth < MAX_DEPTH;
    }
    
    public void push(SpellAbility sa) {
        GameSimulator.debugPrint("Recursing DEPTH=" + recursionDepth);
        GameSimulator.debugPrint("  With: " + sa);
        recursionDepth++;
    }

    public void pop(Score score, SpellAbility nextSa) {
        recursionDepth--;
        GameSimulator.debugPrint("DEPTH"+recursionDepth+" best score " + score + " " + nextSa);
    }

    public void printState(Score score, SpellAbility origSa) {
        for (int i = 0; i < recursionDepth; i++)
            System.err.print("  ");
        System.err.println(recursionDepth + ": [" + score.value + "] " + SpellAbilityPicker.abilityToString(origSa));
    }
}
