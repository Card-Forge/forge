package forge.achievement;

import org.w3c.dom.Element;

import forge.game.Game;
import forge.game.player.Player;
import forge.util.Lang;

public abstract class ProgressiveAchievement extends Achievement {
    private int current;

    protected ProgressiveAchievement(String key0, String displayName0, String description0, String flavorText0) {
        super(key0, displayName0, description0, flavorText0, 0);
    }
    //use this constructor for regular tiered achievements
    protected ProgressiveAchievement(String key0, String displayName0, String sharedDesc0,
            String commonDesc0, int commonThreshold0,
            String uncommonDesc0, int uncommonThreshold0,
            String rareDesc0, int rareThreshold0,
            String mythicDesc0, int mythicThreshold0) {
        super(key0, displayName0, sharedDesc0, 0, commonDesc0, commonThreshold0,
                uncommonDesc0, uncommonThreshold0, rareDesc0, rareThreshold0,
                mythicDesc0, mythicThreshold0);
    }

    protected abstract boolean eval(Player player, Game game);

    @Override
    protected final int evaluate(Player player, Game game) {
        if (eval(player, game)) {
            current++;
        }
        return current;
    }

    @Override
    public final String getSubTitle() {
        return current + " " + (current != 1 ? Lang.getPlural(getNoun()) : getNoun());
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
