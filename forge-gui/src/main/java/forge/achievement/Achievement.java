package forge.achievement;

import org.w3c.dom.Element;

import forge.FThreads;
import forge.GuiBase;
import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.game.Game;
import forge.game.player.Player;
import forge.interfaces.IGuiBase;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.gui.SOptionPane;

public abstract class Achievement {
    private final String displayName, sharedDesc, bronzeDesc, silverDesc, goldDesc;
    private final int bronzeThreshold, silverThreshold, goldThreshold;
    private final boolean checkGreaterThan;
    private String imagePrefix;
    private ISkinImage customImage;
    protected int best, current;

    public Achievement(String displayName0, String sharedDesc0,
            String bronzeDesc0, int bronzeThreshold0,
            String silverDesc0, int silverThreshold0,
            String goldDesc0, int goldThreshold0) {
        displayName = displayName0;
        sharedDesc = sharedDesc0;
        bronzeDesc = bronzeDesc0;
        bronzeThreshold = bronzeThreshold0;
        silverDesc = silverDesc0;
        silverThreshold = silverThreshold0;
        goldDesc = goldDesc0;
        goldThreshold = goldThreshold0;
        checkGreaterThan = goldThreshold0 > silverThreshold0;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getSharedDesc() {
        return sharedDesc;
    }
    public String getBronzeDesc() {
        return bronzeDesc;
    }
    public String getSilverDesc() {
        return silverDesc;
    }
    public String getGoldDesc() {
        return goldDesc;
    }
    public int getBest() {
        return best;
    }
    public boolean earnedGold() {
        if (checkGreaterThan) {
            return best >= goldThreshold;
        }
        return best <= goldThreshold;
    }
    public boolean earnedSilver() {
        if (checkGreaterThan) {
            return best >= silverThreshold;
        }
        return best <= silverThreshold;
    }
    public boolean earnedBronze() {
        if (checkGreaterThan) {
            return best >= bronzeThreshold;
        }
        return best <= bronzeThreshold;
    }

    public void setImagePrefix(String imagePrefix0) {
        imagePrefix = imagePrefix0;
    }

    public ISkinImage getCustomImage() {
        return customImage;
    }

    public void updateCustomImage() {
        int suffix;
        if (earnedGold()) {
            suffix = 3;
        }
        else if (earnedSilver()) {
            suffix = 2;
        }
        else if (earnedBronze()) {
            suffix = 1;
        }
        else {
            customImage = null;
            return;
        }
        final String filename = ForgeConstants.CACHE_ACHIEVEMENT_PICS_DIR + imagePrefix + "_" + suffix + ".png";
        if (FileUtil.doesFileExist(filename)) {
            customImage = GuiBase.getInterface().getUnskinnedIcon(filename);
            return;
        }
        customImage = null;
    }

    protected abstract int evaluate(Player player, Game game);

    protected Player getSingleOpponent(Player player) {
        if (player.getGame().getRegisteredPlayers().size() == 2) {
            for (Player p : player.getGame().getRegisteredPlayers()) {
                if (p.isOpponentOf(player)) {
                    return p;
                }
            }
        }
        return null;
    }

    public void update(IGuiBase gui, Player player) {
        current = evaluate(player, player.getGame());
        if (checkGreaterThan) {
            if (current <= best) { return; }
        }
        else if (current >= best) { return; }

        boolean hadEarnedGold = earnedGold();
        boolean hadEarnedSilver = earnedSilver();
        boolean hadEarnedBronze = earnedBronze();

        best = current;

        String type = null;
        FSkinProp image = null;
        String desc = null;
        if (earnedGold()) {
            if (!hadEarnedGold) {
                type = "Gold";
                image = FSkinProp.IMG_GOLD_TROPHY;
                desc = goldDesc;
            }
        }
        else if (earnedSilver()) {
            if (!hadEarnedSilver) {
                type = "Silver";
                image = FSkinProp.IMG_SILVER_TROPHY;
                desc = silverDesc;
            }
        }
        else if (earnedBronze()) {
            if (!hadEarnedBronze) {
                type = "Bronze";
                image = FSkinProp.IMG_BRONZE_TROPHY;
                desc = bronzeDesc;
            }
        }
        if (type != null) {
            FThreads.invokeInEdtNowOrLater(GuiBase.getInterface(), new Runnable() {
                @Override
                public void run() {
                    updateCustomImage();
                }
            });
            if (sharedDesc != null) {
                desc = sharedDesc + " " + desc;
            }
            SOptionPane.showMessageDialog(gui, "You've earned a " + type + " trophy!\n\n" +
                    displayName + "\n" + desc + ".", "Achievement Earned", image);
        }
    }

    public boolean needSave() {
        return best != 0 || current != 0;
    }

    public void saveToXml(Element el) {
        el.setAttribute("best", String.valueOf(best));
        el.setAttribute("current", String.valueOf(current));
    }

    public void loadFromXml(Element el) {
        best = getIntAttribute(el, "best");
        current = getIntAttribute(el, "current");
    }

    private int getIntAttribute(Element el, String name) {
        String value = el.getAttribute(name);
        if (value.length() > 0) {
            try {
                return Integer.parseInt(value);
            }
            catch (Exception ex) {}
        }
        return 0;
    }

    public abstract String getSubTitle();
}
