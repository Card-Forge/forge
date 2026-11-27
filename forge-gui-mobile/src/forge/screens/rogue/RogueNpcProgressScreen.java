package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.npc.NPC;
import forge.gui.FThreads;
import forge.gamemodes.rogue.npc.NPCEncounterComposite;
import forge.gamemodes.rogue.npc.NPCEncounterComposite.LevelChoice;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Developer tool: set each NPC's stored progress level. The first choice keeps the current level. */
public class RogueNpcProgressScreen extends RogueScreen {
    private final Map<NPC, List<LevelChoice>> choices = new EnumMap<>(NPC.class);
    private final Map<NPC, LevelChoice> picked = new EnumMap<>(NPC.class);
    private final Map<NPC, RogueButton> selectors = new EnumMap<>(NPC.class);
    private final RogueButton cancel;
    private final RogueButton ok;

    public RogueNpcProgressScreen() {
        super(null);
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        for (NPC npc : NPC.values()) {
            List<LevelChoice> options = NPCEncounterComposite.levelChoices(npc, progress.getNPCLevel(npc.id));
            if (options.isEmpty()) {
                continue;
            }
            choices.put(npc, options);
            picked.put(npc, options.get(0));
            selectors.put(npc, add(new RogueButton(options.get(0).label(), null, RogueButton.State.NORMAL, () -> pick(npc))));
        }
        Localizer localizer = Localizer.getInstance();
        cancel = add(new RogueButton(localizer.getMessage("lblCancel"), null, RogueButton.State.NORMAL, Forge::back));
        ok = add(new RogueButton(localizer.getMessage("lblOK"), null, RogueButton.State.MAIN, this::save));
    }

    private void pick(NPC npc) {
        List<RogueRunMenu.Item> items = new ArrayList<>();
        for (LevelChoice choice : choices.get(npc)) {
            items.add(new RogueRunMenu.Item(choice.label(), () -> {
                picked.put(npc, choice);
                selectors.get(npc).setText(choice.label());
            }));
        }
        RogueRunMenu.show(items, selectors.get(npc).getTop());
    }

    private void save() {
        Map<NPC, LevelChoice> levels = new EnumMap<>(picked);
        RogueController.get().submit(() -> {
            RogueMetaProgress progress = RogueMetaProgress.getInstance();
            for (Map.Entry<NPC, LevelChoice> entry : levels.entrySet()) {
                progress.setNPCLevel(entry.getKey().id, entry.getValue().level());
            }
            FThreads.invokeInEdtLater(() -> {
                Localizer localizer = Localizer.getInstance();
                Forge.back();
                RogueDialog.show(localizer.getMessage("lblRogueDevNpcProgressTitle"), localizer.getMessage("lblRogueNpcProgressUpdated"),
                    List.of(localizer.getMessage("lblOK")), choice -> { });
            });
        });
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(List.of(cancel, ok));
    }

    private float[] box() {
        float u = RogueLook.unit();
        float w = Math.min(getWidth() - 6 * u, 360 * u);
        return new float[]{(getWidth() - w) / 2, w};
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        float[] box = box();
        float row = 64 * u;
        for (RogueButton selector : selectors.values()) {
            selector.setBounds(box[0] + box[1] / 2, row, box[1] / 2 - 14 * u, 28 * u);
            row += 34 * u;
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        float[] box = box();
        float x = box[0];
        float w = box[1];
        Localizer localizer = Localizer.getInstance();
        RogueLook.paper(g, x, 3 * u, w, cancel.getTop() - 6 * u);
        RogueLook.text(g, localizer.getMessage("lblRogueSetNPCProgressLevels"), x, 12 * u, 1.4f, RogueLook.INK, w, Align.center);
        RogueLook.wrap(g, localizer.getMessage("lblRogueAdjustStoredNPCProgressionValuesForDevelopment"), x + 14 * u, 34 * u, w - 28 * u,
            1, RogueLook.SOFT, Align.center);
        for (Map.Entry<NPC, RogueButton> entry : selectors.entrySet()) {
            float y = entry.getValue().getTop();
            RogueLook.avatar(g, entry.getKey().avatarIndex, x + 16 * u, y + 2 * u, 24 * u);
            RogueLook.fit(g, entry.getKey().name, x + 50 * u, y + 8 * u, 1, RogueLook.INK, w / 2 - 54 * u, Align.left);
        }
    }
}
