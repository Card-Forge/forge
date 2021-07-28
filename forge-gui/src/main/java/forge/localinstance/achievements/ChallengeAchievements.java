package forge.localinstance.achievements;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;
import forge.localinstance.properties.ForgeConstants;
import forge.util.Localizer;

public class ChallengeAchievements extends AchievementCollection {
    public static final ChallengeAchievements instance = new ChallengeAchievements();

    private ChallengeAchievements() {
        super("lblChallenges", ForgeConstants.ACHIEVEMENTS_DIR + "challenges.xml", false);
    }

    @Override
    protected void addSharedAchievements() {
        //prevent including shared achievements
    }

    @Override
    protected void addAchievements() {
        add(new NoCreatures());
        add(new NoSpells());
        add(new NoLands());
        add(new Domain());
        add("Chromatic", Localizer.getInstance().getMessage("lblChromatic"), 
            Localizer.getInstance().getMessage("lblWinGameAfterCasting5CSpell"),
            Localizer.getInstance().getMessage("lblGreatColorComesPower")
        );
        add("Epic", Localizer.getInstance().getMessage("lblEpic"),
            Localizer.getInstance().getMessage("lblWinGameAfterResolvingWithEpicSpell"),
            Localizer.getInstance().getMessage("lblWhenItsYouLastSpellBetterMakeCount")
        );
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
            return Localizer.getInstance().getMessage("lblWin");
        }

        @Override
        protected boolean eval(Player player, Game game) {
            if (!ChallengeAchievements.checkValidGameMode(game)) {
                return false;
            }
            return player.getOutcome().hasWon() &&
                    player.getAchievementTracker().challengesCompleted.contains(getKey());
        }
    }

    public static abstract class DeckChallengeAchievement extends ChallengeAchievement {
        protected DeckChallengeAchievement(String key0, String displayName0, String condition0, String flavorText0) {
            super(key0, displayName0, Localizer.getInstance().getMessage("lblWinGameUsingTargetDeck", condition0), flavorText0);
        }

        @Override
        protected final boolean eval(Player player, Game game) {
            if (!ChallengeAchievements.checkValidGameMode(game)) {
                return false;
            }
            if (player.getOutcome().hasWon()) {
                return eval(player.getRegisteredPlayer().getDeck());
            }
            return false;
        }

        protected abstract boolean eval(Deck deck);
    }

    public static boolean checkValidGameMode(final Game game) {
        // these modes use a fixed pre-defined deck format, so challenge achievements don't apply in them
        return !game.getRules().hasAppliedVariant(GameType.MomirBasic) && !game.getRules().hasAppliedVariant(GameType.MoJhoSto)
                && !game.getRules().hasAppliedVariant(GameType.Puzzle);
    }
}
