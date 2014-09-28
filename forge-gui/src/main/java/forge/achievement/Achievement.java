package forge.achievement;

import org.w3c.dom.Element;

import forge.GuiBase;
import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.game.Game;
import forge.game.player.Player;
import forge.interfaces.IGuiBase;
import forge.item.IPaperCard;
import forge.properties.ForgeConstants;
import forge.util.Lang;

public abstract class Achievement {
    private final String key, displayName, sharedDesc, commonDesc, uncommonDesc, rareDesc, mythicDesc;
    private final int commonThreshold, uncommonThreshold, rareThreshold, mythicThreshold;
    private final boolean checkGreaterThan;
    protected final int defaultValue;
    private ISkinImage image;
    private int best;

    //use this constructor for special achievements without tiers
    protected Achievement(String key0, String displayName0, String description0, String flavorText0, int defaultValue0) {
        this(key0, displayName0, description0, defaultValue0, null, 1, null, 1, null, 1, "(" + flavorText0 + ")", 1); //pass flavor text as mythic description so it appears below description faded out
    }
    //use this constructor for regular tiered achievements
    protected Achievement(String key0, String displayName0, String sharedDesc0, int defaultValue0,
            String commonDesc0, int commonThreshold0,
            String uncommonDesc0, int uncommonThreshold0,
            String rareDesc0, int rareThreshold0,
            String mythicDesc0, int mythicThreshold0) {
        key = key0;
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
        checkGreaterThan = rareThreshold0 >= uncommonThreshold0;
        best = defaultValue0;
        defaultValue = defaultValue0;
    }

    public String getKey() {
        return key;
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
    public int getBest() {
        return best;
    }
    public boolean isSpecial() {
        return mythicThreshold == commonThreshold;
    }
    private boolean earnedSpecial() {
        return (isSpecial() && best > 0);
    }
    public boolean earnedMythic() {
        if (isSpecial()) { return false; }
        if (checkGreaterThan) {
            return best >= mythicThreshold;
        }
        return best <= mythicThreshold;
    }
    public boolean earnedRare() {
        if (isSpecial()) { return false; }
        if (checkGreaterThan) {
            return best >= rareThreshold;
        }
        return best <= rareThreshold;
    }
    public boolean earnedUncommon() {
        if (isSpecial()) { return false; }
        if (checkGreaterThan) {
            return best >= uncommonThreshold;
        }
        return best <= uncommonThreshold;
    }
    public boolean earnedCommon() {
        if (isSpecial()) { return false; }
        if (checkGreaterThan) {
            return best >= commonThreshold;
        }
        return best <= commonThreshold;
    }

    //get card associated with this achievement if any
    public IPaperCard getPaperCard() {
        return null;
    }

    protected abstract int evaluate(Player player, Game game);

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
        }
        else if (earnedCommon()) {
            background = FSkinProp.IMG_COMMON_TROPHY;
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
        image = GuiBase.getInterface().createLayeredImage(background, ForgeConstants.CACHE_ACHIEVEMENTS_DIR + "/" + key + ".png", opacity);
    }

    public int update(IGuiBase gui, Player player) {
        int value = evaluate(player, player.getGame());
        if (checkGreaterThan) {
            if (value <= best) { return value; }
        }
        else if (value >= best) { return value; }

        boolean hadEarnedSpecial = earnedSpecial();
        boolean hadEarnedMythic = earnedMythic();
        boolean hadEarnedRare = earnedRare();
        boolean hadEarnedUncommon = earnedUncommon();
        boolean hadEarnedCommon = earnedCommon();

        best = value;

        if (earnedSpecial()) {
            if (!hadEarnedSpecial) {
                updateTrophyImage();
                gui.showImageDialog(image, displayName + "\n" + sharedDesc + "\n" + mythicDesc, "Achievement Earned");
            }
            return value;
        }

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
            updateTrophyImage();
            if (sharedDesc != null) {
                desc = sharedDesc + " " + desc;
            }
            gui.showImageDialog(image, displayName + " (" + type + ")\n" + desc, "Achievement Earned");
        }
        return value;
    }

    public final boolean needSave() {
        return best != defaultValue;
    }

    public void saveToXml(Element el) {
        el.setAttribute("best", String.valueOf(best));
    }

    public void loadFromXml(Element el) {
        best = getIntAttribute(el, "best");
    }

    protected int getIntAttribute(Element el, String name) {
        String value = el.getAttribute(name);
        if (value.length() > 0) {
            try {
                return Integer.parseInt(value);
            }
            catch (Exception ex) {}
        }
        return 0;
    }

    protected abstract String getNoun();

    protected boolean pluralizeNoun() {
        return best != 1;
    }
    protected boolean displayNounBefore() {
        return false;
    }

    public String getSubTitle() {
        if (best != defaultValue) {
            if (displayNounBefore()) {
                return "Best: " + getNoun() + " " + best;
            }
            return "Best: " + best + " " + (pluralizeNoun() ? Lang.getPlural(getNoun()) : getNoun());
        }
        return null;
    }
}
