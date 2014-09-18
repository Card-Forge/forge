package forge.achievement;

import org.w3c.dom.Element;

import forge.GuiBase;
import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.game.Game;
import forge.game.player.Player;
import forge.interfaces.IGuiBase;
import forge.util.gui.SOptionPane;

public abstract class Achievement {
    private final String displayName, sharedDesc, commonDesc, uncommonDesc, rareDesc, mythicDesc;
    private final int commonThreshold, uncommonThreshold, rareThreshold, mythicThreshold;
    private final boolean checkGreaterThan;
    private final FSkinProp overlayImage;
    private ISkinImage image;
    protected int best, current;

    //use this constructor for special achievements without tiers
    protected Achievement(String displayName0, String description0, FSkinProp overlayImage0) {
        this(displayName0, description0, null, 1, null, 1, null, 1, null, 1, overlayImage0);
    }
    //use this constructor for regular tiered achievements
    protected Achievement(String displayName0, String sharedDesc0,
            String commonDesc0, int commonThreshold0,
            String uncommonDesc0, int uncommonThreshold0,
            String rareDesc0, int rareThreshold0,
            String mythicDesc0, int mythicThreshold0,
            FSkinProp overlayImage0) {
        displayName = displayName0;
        sharedDesc = sharedDesc0;
        commonDesc = commonDesc0;
        commonThreshold = commonThreshold0;
        uncommonDesc = uncommonDesc0;
        uncommonThreshold = uncommonThreshold0;
        rareDesc = rareDesc0;
        rareThreshold = rareThreshold0;
        mythicDesc = mythicDesc0;
        mythicThreshold = mythicThreshold0;
        overlayImage = overlayImage0;
        checkGreaterThan = rareThreshold0 > uncommonThreshold0;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getSharedDesc() {
        return sharedDesc;
    }
    public String getCommonDesc() {
        return commonDesc;
    }
    public String getUncommonDesc() {
        return uncommonDesc;
    }
    public String getRareDesc() {
        return rareDesc;
    }
    public String getMythicDesc() {
        return mythicDesc;
    }
    public ISkinImage getImage() {
        if (image == null) {
            updateTrophyImage();
        }
        return image;
    }
    private boolean earnedSpecial() {
        return (mythicThreshold == commonThreshold && best > 1);
    }
    public boolean earnedMythic() {
        if (checkGreaterThan) {
            return best >= mythicThreshold;
        }
        return best <= mythicThreshold;
    }
    public boolean earnedRare() {
        if (checkGreaterThan) {
            return best >= rareThreshold;
        }
        return best <= rareThreshold;
    }
    public boolean earnedUncommon() {
        if (checkGreaterThan) {
            return best >= uncommonThreshold;
        }
        return best <= uncommonThreshold;
    }
    public boolean earnedCommon() {
        if (checkGreaterThan) {
            return best >= commonThreshold;
        }
        return best <= commonThreshold;
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

    private void updateTrophyImage() {
        FSkinProp background;
        float opacity = 1;
        if (earnedSpecial()) {
            background = FSkinProp.IMG_SPECIAL_TROPHY;
        }
        else if (earnedMythic()) {
            background = FSkinProp.IMG_MYTHIC_TROPHY;
        }
        else if (earnedRare()) {
            background = FSkinProp.IMG_RARE_TROPHY;
        }
        else if (earnedUncommon()) {
            background = FSkinProp.IMG_UNCOMMON_TROPHY;
            //0.8f; //TODO: fade out slightly until rare earned
        }
        else if (earnedCommon()) {
            background = FSkinProp.IMG_COMMON_TROPHY;
            //0.6f; //TODO: fade out a bit more until uncommon earned
        }
        else {
            opacity = 0.25f; //fade out if achievement hasn't been earned yet
            if (mythicThreshold == commonThreshold) {
                background = FSkinProp.IMG_SPECIAL_TROPHY;
            }
            else {
                background = FSkinProp.IMG_COMMON_TROPHY;
            }
        }
        image = GuiBase.getInterface().createLayeredImage(background, overlayImage, opacity);
    }

    public void update(IGuiBase gui, Player player) {
        current = evaluate(player, player.getGame());
        if (checkGreaterThan) {
            if (current <= best) { return; }
        }
        else if (current >= best) { return; }

        boolean hadEarnedMythic = earnedMythic();
        boolean hadEarnedRare = earnedRare();
        boolean hadEarnedUncommon = earnedUncommon();
        boolean hadEarnedCommon = earnedCommon();

        best = current;

        String type = null;
        String desc = null;
        if (earnedMythic()) {
            if (!hadEarnedMythic) {
                type = "Mythic";
                desc = mythicDesc;
            }
        }
        else if (earnedRare()) {
            if (!hadEarnedRare) {
                type = "Rare";
                desc = rareDesc;
            }
        }
        else if (earnedUncommon()) {
            if (!hadEarnedUncommon) {
                type = "Uncommon";
                desc = uncommonDesc;
            }
        }
        else if (earnedCommon()) {
            if (!hadEarnedCommon) {
                type = "Common";
                desc = commonDesc;
            }
        }
        if (type != null) {
            if (image != null) { //only update image if it has already been initialized
                updateTrophyImage();
            }
            if (sharedDesc != null) {
                desc = sharedDesc + " " + desc;
            }
            SOptionPane.showMessageDialog(gui, "You've earned a " + type + " trophy!\n\n" +
                    displayName + "\n" + desc + ".", "Achievement Earned", overlayImage);
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
