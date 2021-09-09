package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;
import forge.gui.GuiBase;
import forge.item.IPaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.model.FModel;
import forge.util.CardTranslation;
import forge.util.Localizer;

public class PlaneswalkerAchievements extends AchievementCollection {
    public static final PlaneswalkerAchievements instance = new PlaneswalkerAchievements();

    public static ISkinImage getTrophyImage(String planeswalkerName) {
        return GuiBase.getInterface().createLayeredImage(FSkinProp.IMG_SPECIAL_TROPHY, ForgeConstants.CACHE_ACHIEVEMENTS_DIR + "/" + planeswalkerName + ".png", 1);
    }

    private PlaneswalkerAchievements() {
        super("lblPlaneswalkerUltimates", ForgeConstants.ACHIEVEMENTS_DIR + "planeswalkers.xml", false, ForgeConstants.PLANESWALKER_ACHIEVEMENT_LIST_FILE);
    }

    @Override
    protected void addSharedAchievements() {
        //prevent including shared achievements
    }

    protected void add(String cardName0, String displayName0, String flavorText0) {
        add(new PlaneswalkerUltimate(cardName0, displayName0, flavorText0));
    }

    @Override
    public void updateAll(Player player) {
        //only call update achievements for any ultimates activated during the game
        if (player.getOutcome().hasWon()) {
            boolean needSave = false;
            for (String ultimate : player.getAchievementTracker().activatedUltimates) {
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

    private class PlaneswalkerUltimate extends ProgressiveAchievement {
        private PlaneswalkerUltimate(String cardName0, String displayName0, String flavorText0) {
            super(cardName0, displayName0, Localizer.getInstance().getMessage("lblWinGameAfterActivatingCardUltimate", CardTranslation.getTranslatedName(cardName0)), flavorText0);
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
        protected String getNoun() {
            return Localizer.getInstance().getMessage("lblWin");
        }
    }
}
