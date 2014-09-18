package forge.achievement;

import org.w3c.dom.Element;

import forge.assets.FSkinProp;
import forge.game.Game;
import forge.game.player.Player;
import forge.interfaces.IGuiBase;
import forge.util.gui.SOptionPane;

public abstract class Achievement {
    private final String displayName, sharedDesc, commonDesc, uncommonDesc, rareDesc, mythicDesc;
    private final int commonThreshold, uncommonThreshold, rareThreshold, mythicThreshold;
    private final boolean checkGreaterThan;
    private final TrophyDisplay trophyDisplay;
    protected int best, current;

    public class TrophyDisplay {
        private final FSkinProp overlay;
        private FSkinProp background;
        private float backgroundOpacity, overlayOpacity;

        private TrophyDisplay(FSkinProp overlay0) {
            overlay = overlay0;
            background = mythicThreshold == commonThreshold ? FSkinProp.IMG_SPECIAL_TROPHY : FSkinProp.IMG_COMMON_TROPHY;
            backgroundOpacity = 0.25f; //fade out heavily if achievement not earned
            overlayOpacity = backgroundOpacity;
        }

        public FSkinProp getOverlay() {
            return overlay;
        }

        public FSkinProp getBackground() {
            return background;
        }

        public float getBackgroundOpacity() {
            return backgroundOpacity;
        }

        public float getOverlayOpacity() {
            return overlayOpacity;
        }

        private void update() {
            if (earnedSpecial()) {
                overlayOpacity = 1;
                backgroundOpacity = 1;
            }
            else if (earnedMythic()) {
                background = FSkinProp.IMG_MYTHIC_TROPHY;
                overlayOpacity = 1;
                backgroundOpacity = 1;
            }
            else if (earnedRare()) {
                background = FSkinProp.IMG_RARE_TROPHY;
                overlayOpacity = 1;
                backgroundOpacity = 1;
            }
            else if (earnedUncommon()) {
                background = FSkinProp.IMG_UNCOMMON_TROPHY;
                overlayOpacity = 1; //0.8f; //TODO: fade out slightly until rare earned
                backgroundOpacity = 1;
            }
            else if (earnedCommon()) {
                overlayOpacity = 1; //0.6f; //TODO: fade out a bit more until uncommon earned
                backgroundOpacity = 1;
            }
        }
    }

    //use this constructor for special achievements without tiers
    protected Achievement(String displayName0, String description0, FSkinProp image0) {
        this(displayName0, description0, null, 1, null, 1, null, 1, null, 1, image0);
    }
    //use this constructor for regular tiered achievements
    protected Achievement(String displayName0, String sharedDesc0,
            String commonDesc0, int commonThreshold0,
            String uncommonDesc0, int uncommonThreshold0,
            String rareDesc0, int rareThreshold0,
            String mythicDesc0, int mythicThreshold0,
            FSkinProp image0) {
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
        trophyDisplay = new TrophyDisplay(image0);
        checkGreaterThan = rareThreshold0 > uncommonThreshold0;
    }

    public String getDisplayName() {
        return displayName;
    }
    public TrophyDisplay getTrophyDisplay() {
        return trophyDisplay;
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
            trophyDisplay.update();
            if (sharedDesc != null) {
                desc = sharedDesc + " " + desc;
            }
            SOptionPane.showMessageDialog(gui, "You've earned a " + type + " trophy!\n\n" +
                    displayName + "\n" + desc + ".", "Achievement Earned", trophyDisplay.getOverlay());
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
        trophyDisplay.update();
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
