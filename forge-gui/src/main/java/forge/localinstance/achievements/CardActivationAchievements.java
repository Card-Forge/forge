package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;
import forge.gui.GuiBase;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.model.FModel;
import forge.util.CardTranslation;
import forge.util.Localizer;

public class CardActivationAchievements extends AchievementCollection {
    public static final CardActivationAchievements instance = new CardActivationAchievements();

    public static ISkinImage getTrophyImage(String cardName, PaperCard paperCard) {
        return GuiBase.getInterface().createLayeredImage(paperCard, FSkinProp.IMG_SPECIAL_TROPHY, ForgeConstants.CACHE_ACHIEVEMENTS_DIR + "/" + cardName + ".png", 1);
    }

    private CardActivationAchievements() {
        super("lblSpecialCardUltimates", ForgeConstants.ACHIEVEMENTS_DIR + "card-activation.xml", false, ForgeConstants.SPECIAL_CARD_ACHIEVEMENT_LIST_FILE);
    }

    @Override
    protected void addSharedAchievements() {
        //prevent including shared achievements
    }

    protected void add(String cardName0, String displayName0, String flavorText0) {
        add(new CardAbilityUltimate(cardName0, displayName0, flavorText0));
    }

    @Override
    public void updateAll(Player player) {
        //only call update achievements for any ultimates activated during the game
        if (player.getOutcome().hasWon()) {
            boolean needSave = false;
            for (String ultimate : player.getAchievementTracker().activatedNonPWUltimates) {
                Achievement achievement = achievements.get(ultimate);
                if (achievement != null) {
                    achievement.update(player);
                    needSave = true;
                }
            }
            if (needSave) {
                save();
            }
        }
    }

    private class CardAbilityUltimate extends ProgressiveAchievement {
        private CardAbilityUltimate(String cardName0, String displayName0, String flavorText0) {
            super(cardName0, displayName0, Localizer.getInstance().getMessage("lblWinGameAfterActivatingCardUltimate", CardTranslation.getTranslatedName(cardName0)), flavorText0);
        }

        @Override
        protected boolean eval(Player player, Game game) {
            return true; //if this reaches this point, it can be presumed that winning the game after activating the ultimate card ability is accomplished
        }

        @Override
        public IPaperCard getPaperCard() {
            return FModel.getMagicDb().getCommonCards().getCard(getKey());
        }

        @Override
        protected String getNoun() {
            return Localizer.getInstance().getMessage("lblWin");
        }
    }
}
