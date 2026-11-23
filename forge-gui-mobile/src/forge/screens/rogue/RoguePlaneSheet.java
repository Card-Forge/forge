package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.flow.RogueFlow;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.item.PaperCard;
import forge.toolbox.FContainer;
import forge.toolbox.FOverlay;
import forge.util.Localizer;

/** The chosen plane: its planebound, art, text and rewards, with Reroll Plane and Enter. */
public class RoguePlaneSheet extends FContainer {
    private final RogueRun run;
    private final NodePlanebound node;
    private final RogueFlow.Reroll reroll;
    private final RogueButton close;
    private final RogueButton rerollButton;
    private final RogueButton enter;

    public RoguePlaneSheet(RogueRun run, NodePlanebound node, RogueFlow.Reroll reroll, Runnable onReroll, Runnable onEnter) {
        this.run = run;
        this.node = node;
        this.reroll = reroll;
        Localizer localizer = Localizer.getInstance();
        close = add(new RogueButton(localizer.getMessage("lblClose"), null, RogueButton.State.NORMAL, () -> FOverlay.getTopOverlay().hide()));
        rerollButton = add(new RogueButton(localizer.getMessage("lblRogueRerollPlaneCount", reroll.left()), null,
            reroll.enabled() ? RogueButton.State.NORMAL : RogueButton.State.DISABLED, onReroll));
        rerollButton.setVisible(reroll.left() > 0);
        enter = add(new RogueButton(localizer.getMessage("lblRogueEnterNode"), null, RogueButton.State.MAIN, onEnter));
    }

    @Override
    protected void doLayout(float width, float height) {
        float u = RogueLook.unit();
        float y = height - 30 * u;
        float rest = width - (rerollButton.isVisible() ? 118 * u : 0);
        close.setBounds(0, y, rest * 0.45f, 30 * u);
        rerollButton.setBounds(rest * 0.45f, y, 118 * u, 30 * u);
        enter.setBounds(rest * 0.45f + (rerollButton.isVisible() ? 118 * u : 0), y, rest * 0.55f, 30 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        float u = RogueLook.unit();
        float w = getWidth();
        boolean wide = Forge.isLandscapeMode();
        Localizer localizer = Localizer.getInstance();
        RoguePlanebound planebound = node.getRoguePlanebound();
        boolean down = run.getPath().isFaceDown(run, node);
        float y = 0;
        RogueLook.avatar(g, down ? 125 : planebound.avatarIndex(), u, u, 30 * u);
        RogueLook.fit(g, down ? "???" : planebound.planeboundName(), 40 * u, y, 1.3f, RogueLook.INK, w - 40 * u, Align.left);
        RogueLook.fit(g, down ? "???" : planebound.planeName(), 40 * u, y + 17 * u, 1, RogueLook.SOFT, w - 40 * u, Align.left);
        y += 38 * u;
        PaperCard plane = CodexHelper.getPlaneCard(planebound);
        float artHeight = (wide ? 56 : 150) * u;
        if (!down && plane != null) {
            g.fillRect(RogueLook.INK, -u, y - u, w + 2 * u, artHeight + 2 * u);
            RogueLook.art(g, plane, 0, y, w, artHeight, 1);
            y += artHeight + 6 * u;
            RogueLook.text(g, plane.getName(), 0, y, 1.15f, RogueLook.DEEP);
            y += 16 * u;
            y += RogueLook.wrap(g, plane.getRules().getOracleText().replace("\\n", "\n"), 0, y, w, wide ? 0.85f : 1,
                RogueLook.INK, Align.left) + 4 * u;
        } else {
            y += RogueLook.wrap(g, localizer.getMessage("lblRogueHiddenPlanes"), 0, y, w, 1, RogueLook.SOFT, Align.left) + 4 * u;
        }
        RogueLook.line(g, 0, y, w, y, 0.8f, RogueLook.SOFT);
        y += 5 * u;
        RogueLook.item(g, "Life", 0, y, 13 * u);
        RogueLook.text(g, String.valueOf(node.getPlaneboundLife(run.getPath().countPlaneboundRowsUpTo(node.getRowIndex()))), 16 * u, y, 1, RogueLook.INK);
        RogueLook.skin(g, FSkinImage.QUEST_COIN, 48 * u, y, 13 * u);
        RogueLook.text(g, String.valueOf(node.getGoldReward()), 64 * u, y, 1, RogueLook.INK);
        RogueLook.skin(g, FSkinImage.QUEST_GOLD, 84 * u, y, 13 * u);
        RogueLook.text(g, String.valueOf(node.getEchoReward()), 100 * u, y, 1, RogueLook.INK);
        RogueLook.text(g, localizer.getMessage("lblRogueCardReward"), 122 * u, y, 1, RogueLook.INK);
        if (reroll.left() > 0 && !reroll.enabled()) {
            RogueLook.wrap(g, reroll.reason(), 0, y + 20 * u, w, 1, RogueLook.SOFT, Align.left);
        }
    }
}
