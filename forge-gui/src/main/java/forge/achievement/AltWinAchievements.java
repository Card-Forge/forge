package forge.achievement;

import org.apache.commons.lang3.StringUtils;

import forge.assets.FSkinProp;
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
        add("Azor's Elocutors", "The Filibuster", "Talk might be cheap, but it can buy you victory!", FSkinProp.IMG_THE_FILIBUSTER);
        add("Barren Glory", "The Clean Slate", "When you have nothing, you can lose nothing... so you can win everything!", FSkinProp.IMG_THE_CLEAN_SLATE);
        add("Battle of Wits", "The Huge Library", "So many answers, so little time to look through them...", FSkinProp.IMG_THE_LIBRARY_OF_CONGRESS);
        add("Biovisionary", "The Clique", "And now my... I mean our plan is complete!", FSkinProp.IMG_THE_CLIQUE);
        add("Chance Encounter", "The Accident", "This victory was brought to you by a series of fortunate events.", FSkinProp.IMG_THE_ACCIDENT);
        add("Coalition Victory", "The Teamwork", "Let's all be friends!", FSkinProp.IMG_THE_TEAMWORK);
        add("Darksteel Reactor", "The Machine", "What are you going to do with all this power? Whatever you want!", FSkinProp.IMG_THE_MACHINE);
        add("Epic Struggle", "The Army", "Let's just trample them into the ground already!", FSkinProp.IMG_THE_ARMY);
        add("Felidar Sovereign", "The Cat's Life", "Just wait for his other eight lives!", FSkinProp.IMG_THE_CATS_LIFE);
        add("Helix Pinnacle", "The Tower", "The view from the top is great!", FSkinProp.IMG_THE_TOWER);
        add("Hellkite Tyrant", "The Hoard", "You made your bed of treasure, now lie in it!", FSkinProp.IMG_THE_HOARD);
        add("Laboratory Maniac", "The Insanity", "No more questions? I'm omniscient now!", FSkinProp.IMG_THE_INSANITY);
        add("Mayael's Aria", "The Gargantuan", "Just my shadow weighs a ton!", FSkinProp.IMG_THE_GARGANTUAN);
        add("Maze's End", "The Labyrinth", "What? No bossfight?", FSkinProp.IMG_THE_LABYRINTH);
        add("Mortal Combat", "The Boneyard", "So peaceful...", FSkinProp.IMG_THE_BONEYARD);
        add("Near-Death Experience", "The Edge", "Phew... I thought I was going to die!", FSkinProp.IMG_THE_EDGE);
    }

    private void add(String cardName0, String displayName0, String flavorText0, FSkinProp overlayImage0) {
        add(getKey(cardName0), new AltWinAchievement(displayName0, cardName0, flavorText0, overlayImage0));
    }

    private String getKey(String cardName0) {
        return cardName0.replaceAll(" ", "");
    }

    @Override
    public void updateAll(IGuiBase gui, Player player) {
        //only call update on achievement for alternate win condition (if any)
        if (player.getOutcome().hasWon()) {
            String altWinCondition = player.getOutcome().altWinSourceName;
            if (!StringUtils.isEmpty(altWinCondition)) {
                Achievement achievement = achievements.get(getKey(altWinCondition));
                if (achievement != null) {
                    achievement.update(gui, player);
                    save();
                }
            }
        }
    }

    private class AltWinAchievement extends Achievement {
        private AltWinAchievement(String displayName0, String cardName0, String flavorText0, FSkinProp overlayImage0) {
            super(displayName0, "Win a game with " + cardName0, flavorText0, overlayImage0);
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
