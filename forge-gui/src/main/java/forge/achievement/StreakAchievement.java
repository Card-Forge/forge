package forge.achievement;

import org.w3c.dom.Element;

import forge.game.Game;
import forge.game.player.Player;

public abstract class StreakAchievement extends Achievement {
    private int current;

    //use this constructor for regular tiered achievements
    protected StreakAchievement(final String key0, final String displayName0, final String sharedDesc0,
            final String commonDesc0, final int commonThreshold0,
            final String uncommonDesc0, final int uncommonThreshold0,
            final String rareDesc0, final int rareThreshold0,
            final String mythicDesc0, final int mythicThreshold0) {
        super(key0, displayName0, sharedDesc0, 0, commonDesc0, commonThreshold0,
                uncommonDesc0, uncommonThreshold0, rareDesc0, rareThreshold0,
                mythicDesc0, mythicThreshold0);
    }

    protected abstract Boolean eval(Player player, Game game);

    @Override
    protected final int evaluate(final Player player, final Game game) {
        final Boolean val = eval(player, game);
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
    public void saveToXml(final Element el) {
        super.saveToXml(el);
        el.setAttribute("current", String.valueOf(current));
    }

    @Override
    public void loadFromXml(final Element el) {
        super.loadFromXml(el);
        current = getIntAttribute(el, "current");
    }
}
