package forge.ai.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;

import forge.ai.ComputerUtilCard;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;
import forge.game.spellability.TargetRestrictions;

public class PossibleTargetSelector {
    private final SpellAbility sa;
    private SpellAbility targetingSa;
    private int targetingSaIndex;
    private int maxTargets;
    private TargetRestrictions tgt;
    private int targetIndex;
    private List<GameObject> validTargets;

    public static class Targets {
        final int targetingSaIndex;
        final int originalTargetCount;
        final int targetIndex;
        final String description;
        
        private Targets(int targetingSaIndex, int originalTargetCount, int targetIndex, String description)  {
            this.targetingSaIndex = targetingSaIndex;
            this.originalTargetCount = originalTargetCount;
            this.targetIndex = targetIndex;
            this.description = description;

            if (targetIndex < 0 || targetIndex >= originalTargetCount) {
                throw new IllegalArgumentException("Invalid targetIndex=" + targetIndex);
            }
        }
        
        @Override
        public String toString() {
            return description;
        }
    }

    public PossibleTargetSelector(Game game, Player player, SpellAbility sa) {
        this.sa = sa;
        chooseTargetingSubAbility();
        this.targetIndex = 0;
        this.validTargets = new ArrayList<GameObject>();
        generateValidTargets(player);
    }

    private void generateValidTargets(Player player) {
        if (targetingSa == null) {
            return;
        }
        tgt = targetingSa.getTargetRestrictions();
        maxTargets = tgt.getMaxTargets(sa.getHostCard(), targetingSa);

        SimilarTargetSkipper skipper = new SimilarTargetSkipper();
        for (GameObject o : tgt.getAllCandidates(sa, true)) {
            if (maxTargets == 1 && skipper.shouldSkipTarget(o)) {
                continue;
            }
            validTargets.add(o);
        }
        targetingSa.resetTargets();
        sa.setActivatingPlayer(player);
    }

    private static class SimilarTargetSkipper {
        private ArrayListMultimap<String, Card> validTargetsMap = ArrayListMultimap.<String, Card>create();
        private HashMap<Card, String> cardTypeStrings = new HashMap<Card, String>();
        private HashMap<Card, Integer> creatureScores;

        private int getCreatureScore(Card c) {
            if (creatureScores != null) {
                Integer score = creatureScores.get(c);
                if (score != null) {
                    return score;
                }
            } else  {
                creatureScores = new HashMap<Card, Integer>();
            }
            
            int score = ComputerUtilCard.evaluateCreature(c);
            creatureScores.put(c, score);
            return score;
        }

        private String getTypeString(Card c) {
            String str = cardTypeStrings.get(c);
            if (str != null) {
                return str;
            }
            str = c.getType().toString();
            cardTypeStrings.put(c, str);
            return str;
        }

        public boolean shouldSkipTarget(GameObject o) {
            // TODO: Support non-card targets, such as spells on the stack.
            if (!(o instanceof Card)) {
                return false;
            }

            Card c = (Card) o;
            for (Card existingTarget : validTargetsMap.get(c.getName())) {
                // Note: Checks are ordered from cheapest to more expensive ones. For example, type equals()
                // ends up calling toString() on the type object and is more expensive than the checks above it.
                if (c.getController() != c.getController() || c.getOwner() != existingTarget.getOwner()) {
                    continue;
                }
                if (c.getSpellAbilities().size() != existingTarget.getSpellAbilities().size()) {
                    continue;
                }
                // Note: This doesn't just do equals() on the types because a) it doesn't exist and b) if
                // it existed and just used toString() comparison it would be less efficient than doing it
                // in this class, which caches the strings.
                if (!getTypeString(existingTarget).equals(getTypeString(c))) {
                    continue;
                }
                if (c.isCreature() && getCreatureScore(c) != getCreatureScore(existingTarget)) {
                    continue;
                }
                return true;
            }
            validTargetsMap.put(c.getName(), c);
            return false;
        }
    }

    private static boolean conditionsAreMet(SpellAbility saOrSubSa) {
        SpellAbilityCondition conditions = saOrSubSa.getConditions();
        return conditions == null || conditions.areMet(saOrSubSa);
    }

    private void chooseTargetingSubAbility() {
        // TODO: This needs to handle case where multiple sub-abilities each have targets.
        SpellAbility saOrSubSa = sa;
        int index = 0;
        do {
            if (saOrSubSa.usesTargeting() && conditionsAreMet(saOrSubSa)) {
                targetingSaIndex = index;
                targetingSa = saOrSubSa;
                return;
            }
            saOrSubSa = saOrSubSa.getSubAbility();
            index++;
        } while (saOrSubSa != null);
    }

    public boolean hasPossibleTargets() {
        return !validTargets.isEmpty();
    }

    private void selectTargetsByIndex(int index) {
        targetingSa.resetTargets();

        // TODO: smarter about multiple targets, etc...
        while (targetingSa.getTargets().getNumTargeted() < maxTargets && index < validTargets.size()) {
            targetingSa.getTargets().add(validTargets.get(index++));
        }

        // Divide up counters, since AI is expected to do this. For now,
        // divided evenly with left-overs going to the first target.
        if (targetingSa.hasParam("DividedAsYouChoose")) {
            final int targetCount = targetingSa.getTargets().getTargetCards().size();
            if (targetCount > 0) {
                final String amountStr = targetingSa.getParam("CounterNum");
                final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, targetingSa);
                final int amountPerCard = amount / targetCount;
                int amountLeftOver = amount - (amountPerCard * targetCount);
                final TargetRestrictions tgtRes = targetingSa.getTargetRestrictions();
                for (GameObject target : targetingSa.getTargets().getTargets()) {
                    tgtRes.addDividedAllocation(target, amountPerCard + amountLeftOver);
                    amountLeftOver = 0;
                }
            }
        }
    }

    public Targets getLastSelectedTargets() {
        return new Targets(targetingSaIndex, validTargets.size(), targetIndex - 1, targetingSa.getTargets().getTargetedString());
    }

    public boolean selectTargets(Targets targets) {
        if (targets.originalTargetCount != validTargets.size() || targets.targetingSaIndex != targetingSaIndex) {
            return false;
        }
        selectTargetsByIndex(targets.targetIndex);
        return true;
    }
 
    public boolean selectNextTargets() {
        if (targetIndex >= validTargets.size()) {
            return false;
        }
        selectTargetsByIndex(targetIndex);
        targetIndex++;
        return true;
    }
}
