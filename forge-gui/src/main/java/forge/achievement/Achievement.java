package forge.achievement;

import org.w3c.dom.Element;

import forge.assets.FSkinProp;
import forge.game.Game;
import forge.game.player.Player;
import forge.interfaces.IGuiBase;
import forge.util.gui.SOptionPane;

public abstract class Achievement {
    private final String displayName, bronzeDesc, silverDesc, goldDesc;
    private final int bronzeThreshold, silverThreshold, goldThreshold;
    private final boolean showBest, showCurrent, checkGreaterThan;
    protected int best, current;

    public Achievement(String displayName0,
            boolean showBest0, boolean showCurrent0,
            String bronzeDesc0, int bronzeThreshold0,
            String silverDesc0, int silverThreshold0,
            String goldDesc0, int goldThreshold0) {
        displayName = displayName0;
        showBest = showBest0;
        showCurrent = showCurrent0;
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

    protected abstract int evaluate(Player player, Game game, int current); 

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
        current = evaluate(player, player.getGame(), current);
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
            SOptionPane.showMessageDialog(gui, "You've earned a " + type + " trophy!\n\n" +
                    displayName + " - " + desc, "Achievement Earned", image);
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

    public String getSubTitle() {
        if (showBest) {
            if (showCurrent) {
                return "Best: " + best + " Current: " + current;
            }
            return "Best: " + best;
        }
        else if (showCurrent) {
            return "Current: " + current;
        }
        return null;
    }
}
