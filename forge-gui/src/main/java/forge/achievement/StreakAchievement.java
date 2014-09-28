package forge.achievement;

import org.w3c.dom.Element;

import forge.game.Game;
import forge.game.player.Player;

public abstract class StreakAchievement extends Achievement {
    private int current;

    protected StreakAchievement(String key0, String displayName0, String description0, String flavorText0) {
        super(key0, displayName0, description0, flavorText0, 0);
    }
    //use this constructor for regular tiered achievements
    protected StreakAchievement(String key0, String displayName0, String sharedDesc0,
            String commonDesc0, int commonThreshold0,
            String uncommonDesc0, int uncommonThreshold0,
            String rareDesc0, int rareThreshold0,
            String mythicDesc0, int mythicThreshold0) {
        super(key0, displayName0, sharedDesc0, 0, commonDesc0, commonThreshold0,
                uncommonDesc0, uncommonThreshold0, rareDesc0, rareThreshold0,
                mythicDesc0, mythicThreshold0);
    }

    protected abstract Boolean eval(Player player, Game game);

    @Override
    protected final int evaluate(Player player, Game game) {
        Boolean val = eval(player, game);
        if (val != null) { //null means don't increment or reset
            if (val) {
                current++;
            }
            else {
                current = 0;
            }
        }
        return current;
    }

    @Override
    protected String getNoun() {
        return "Active: " + current; //override here so active streak appears after best
    }
    @Override
    protected boolean pluralizeNoun() {
        return false;
    }

    @Override
    public void saveToXml(Element el) {
        super.saveToXml(el);
        el.setAttribute("current", String.valueOf(current));
    }

    @Override
    public void loadFromXml(Element el) {
        super.loadFromXml(el);
        current = getIntAttribute(el, "current");
    }
}
