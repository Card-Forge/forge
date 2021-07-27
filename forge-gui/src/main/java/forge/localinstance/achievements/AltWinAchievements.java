package forge.localinstance.achievements;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.player.Player;
import forge.item.IPaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.CardTranslation;
import forge.util.Localizer;

public class AltWinAchievements extends AchievementCollection {
    public static final AltWinAchievements instance = new AltWinAchievements();

    private AltWinAchievements() {
        super("lblAlternateWinConditions", ForgeConstants.ACHIEVEMENTS_DIR + "altwin.xml", false, ForgeConstants.ALTWIN_ACHIEVEMENT_LIST_FILE);
    }

    @Override
    protected void addSharedAchievements() {
        //prevent including shared achievements
    }

    protected void add(String cardName0, String displayName0, String flavorText0) {
        add(new AltWinAchievement(cardName0, displayName0, flavorText0));
    }

    @Override
    public void updateAll(Player player) {
        //only call update achievement for alternate win condition (if any)
        if (player.getOutcome().hasWon()) {
            String altWinCondition = player.getOutcome().altWinSourceName;
            if (StringUtils.isEmpty(altWinCondition)) {
                Player opponent = player.getSingleOpponent();
                if (opponent == null) { return; }

                altWinCondition = opponent.getOutcome().loseConditionSpell;
                if (StringUtils.isEmpty(altWinCondition)) {
                    return;
                }
            }

            Achievement achievement = achievements.get(altWinCondition);
            if (achievement == null) {
                achievement = achievements.get("Emblem - " + altWinCondition); // indirectly winning through an emblem
            }
            if (achievement != null) {
                achievement.update(player);
                save();
            }
        }
    }

    private class AltWinAchievement extends ProgressiveAchievement {
        private AltWinAchievement(String cardName0, String displayName0, String flavorText0) {
            super(CardTranslation.getTranslatedName(cardName0), displayName0, Localizer.getInstance().getMessage("lblWinGameWithCard", CardTranslation.getTranslatedName(cardName0)), flavorText0);
        }

        @Override
        protected boolean eval(Player player, Game game) {
            return true; //if this reaches this point, it can be presumed that alternate win condition achieved
        }

        @Override
        public IPaperCard getPaperCard() {
            return FModel.getMagicDb().getCommonCards().getCard(getKey());
        }

        @Override
        public String getNoun() {
            return Localizer.getInstance().getMessage("lblWin");
        }
    }
}
