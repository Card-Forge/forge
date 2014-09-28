package forge.achievement;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.player.Player;
import forge.properties.ForgeConstants;

public class ChallengeAchievements extends AchievementCollection {
    public static final ChallengeAchievements instance = new ChallengeAchievements();

    private ChallengeAchievements() {
        super("Challenges", ForgeConstants.ACHIEVEMENTS_DIR + "challenges.xml", false);
    }

    @Override
    protected void addSharedAchivements() {
        //prevent including shared achievements
    }

    @Override
    protected void addAchievements() {
        add(new NoCreatures());
        add(new NoSpells());
        add(new NoLands());
        add(new Domain());
        add("Chromatic", "Chromatic", "Win a game after casting a 5 color spell", "With great color requirements comes great power.");
        add("Epic", "Epic", "Win a game after resolving a spell with the Epic keyword", "When it's the last spell you ever cast, you better make it count!");
    }

    private void add(String key0, String displayName0, String description0, String flavorText0) {
        add(new ChallengeAchievement(key0, displayName0, description0, flavorText0));
    }

    public static class ChallengeAchievement extends ProgressiveAchievement {
        protected ChallengeAchievement(String key0, String displayName0, String description0, String flavorText0) {
            super(key0, displayName0, description0, flavorText0);
        }

        @Override
        protected final String getNoun() {
            return "Win";
        }

        @Override
        protected boolean eval(Player player, Game game) {
            return player.getOutcome().hasWon() &&
                    player.getAchievementTracker().challengesCompleted.contains(getKey());
        }
    }

    public static abstract class DeckChallengeAchievement extends ChallengeAchievement {
        protected DeckChallengeAchievement(String key0, String displayName0, String condition0, String flavorText0) {
            super(key0, displayName0, "Win a game using a deck " + condition0, flavorText0);
        }

        @Override
        protected final boolean eval(Player player, Game game) {
            if (player.getOutcome().hasWon()) {
                return eval(player.getRegisteredPlayer().getDeck());
            }
            return false;
        }

        protected abstract boolean eval(Deck deck);
    }
}
