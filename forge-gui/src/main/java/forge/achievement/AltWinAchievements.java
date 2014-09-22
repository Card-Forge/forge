package forge.achievement;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.player.Player;
import forge.interfaces.IGuiBase;
import forge.properties.ForgeConstants;

public class AltWinAchievements extends AchievementCollection {
    public static final AltWinAchievements instance = new AltWinAchievements();

    private AltWinAchievements() {
        super("Alternate Win Conditions", ForgeConstants.ACHIEVEMENTS_DIR + "altwin.xml", false);
    }

    @Override
    protected void addSharedAchivements() {
        //prevent including shared achievements
    }

    @Override
    protected void addAchievements() {
        add("Azor's Elocutors", "The Filibuster", "Talk might be cheap, but it can buy you victory!");
        add("Barren Glory", "The Clean Slate", "When you have nothing, you can lose nothing... so you can win everything!");
        add("Battle of Wits", "The Great Library", "So many answers, so little time to look through them...");
        add("Biovisionary", "The Clique", "And now my... I mean our plan is complete!");
        add("Chance Encounter", "The Accident", "This victory was brought to you by a series of fortunate events.");
        add("Coalition Victory", "The Teamwork", "Let's all be friends!");
        add("Darksteel Reactor", "The Machine", "What are you going to do with all this power? Whatever you want!");
        add("Epic Struggle", "The Army", "Let's just trample them into the ground already!");
        add("Felidar Sovereign", "The Cat's Life", "Just wait for his other eight lives!");
        add("Helix Pinnacle", "The Tower", "The view from the top is great!");
        add("Hellkite Tyrant", "The Hoard", "You made your bed of treasure, now lie in it!");
        add("Laboratory Maniac", "The Insanity", "No more questions? I'm omniscient now!");
        add("Mayael's Aria", "The Gargantuan", "Just my shadow weighs a ton!");
        add("Maze's End", "The Labyrinth", "What? No bossfight?");
        add("Mortal Combat", "The Boneyard", "So peaceful...");
        add("Near-Death Experience", "The Edge", "Phew... I thought I was going to die!");
    }

    private void add(String cardName0, String displayName0, String flavorText0) {
        add(new AltWinAchievement(cardName0, displayName0, flavorText0));
    }

    @Override
    public void updateAll(IGuiBase gui, Player player) {
        //only call update on achievement for alternate win condition (if any)
        if (player.getOutcome().hasWon()) {
            String altWinCondition = player.getOutcome().altWinSourceName;
            if (!StringUtils.isEmpty(altWinCondition)) {
                Achievement achievement = achievements.get(altWinCondition);
                if (achievement != null) {
                    achievement.update(gui, player);
                    save();
                }
            }
        }
    }

    private class AltWinAchievement extends Achievement {
        private AltWinAchievement(String cardName0, String displayName0, String flavorText0) {
            super(cardName0, displayName0, "Win a game with " + cardName0, flavorText0);
        }

        @Override
        protected int evaluate(Player player, Game game) {
            return current + 1; //if this reaches this point, it can be presumed that alternate win condition achieved
        }

        @Override
        public String getSubTitle() {
            return current + " Win" + (current != 1 ? "s" : "");
        }
    }
}
