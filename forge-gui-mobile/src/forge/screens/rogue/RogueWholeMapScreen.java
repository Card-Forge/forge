package forge.screens.rogue;

import forge.Forge;
import forge.Graphics;
import forge.gamemodes.rogue.RogueRun;
import forge.util.Localizer;
import java.util.List;
import java.util.Set;

/** All rows of the run's path at once. */
public class RogueWholeMapScreen extends RogueScreen {
    private final RogueMapView map = add(new RogueMapView(i -> { }, true));
    private final RogueButton back;

    public RogueWholeMapScreen() {
        super(null);
        back = add(new RogueButton(Localizer.getInstance().getMessage("lblBack"), null, RogueButton.State.MAIN, Forge::back));
    }

    @Override
    public void onActivate() {
        RogueRun run = RogueController.get().flow().run();
        if (run != null && run.getCurrentNode() != null) {
            map.setRun(run, Set.copyOf(run.getPath().getVisibleNodesInCurrentRow(run.getCurrentNode().getRowIndex(),
                RogueController.get().flow().pathContext())));
        }
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(List.of(back));
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        map.setBounds(x + 8 * u, y + 8 * u, w - 16 * u, h - 14 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        RogueLook.paper(g, 3 * u, 3 * u, getWidth() - 6 * u, map.getBottom() + 3 * u);
    }
}
