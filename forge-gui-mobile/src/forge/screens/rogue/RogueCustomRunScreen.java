package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.effect.CustomRunModification;
import forge.gamemodes.rogue.flow.RogueStart;
import forge.toolbox.FDisplayObject;
import forge.util.Localizer;
import java.util.List;

/** The custom run's modifications, ticked as RogueStart.toggle decides. */
public class RogueCustomRunScreen extends RogueScreen {
    private final RogueNewRunScreen newRun;
    private final Options options = add(new Options());
    private final RogueButton back;
    private final RogueButton commander;
    private final RogueButton descension;
    private final RogueButton start;

    RogueCustomRunScreen(RogueNewRunScreen newRun) {
        super(null);
        this.newRun = newRun;
        newRun.setCustom(true);
        Localizer localizer = Localizer.getInstance();
        back = add(new RogueButton(localizer.getMessage("lblBack"), null, RogueButton.State.NORMAL, () -> {
            newRun.setCustom(false);
            Forge.back();
        }));
        commander = add(new RogueButton(localizer.getMessage("lblCommander"), null, RogueButton.State.NORMAL, Forge::back));
        descension = add(new RogueButton("", null, RogueButton.State.NORMAL, () -> {
            Forge.back();
            newRun.openDescensionSheet();
        }));
        start = add(new RogueButton(localizer.getMessage("lblRogueStartRun"), null, RogueButton.State.MAIN, newRun::begin));
        update();
    }

    @Override
    public void onActivate() {
        update();
    }

    private boolean generatedCommander() {
        return newRun.modifications().stream().anyMatch(CustomRunModification::isGeneratedCommander);
    }

    private void update() {
        Localizer localizer = Localizer.getInstance();
        commander.setState(generatedCommander() ? RogueButton.State.DISABLED : RogueButton.State.NORMAL);
        descension.setText(localizer.getMessage("lblRogueDescensionLevel",
            newRun.descension() == 0 ? localizer.getMessage("lblOff") : String.valueOf(newRun.descension())));
        start.setState(newRun.selection().canBegin(RogueMetaProgress.getInstance()) ? RogueButton.State.MAIN : RogueButton.State.DISABLED);
    }

    @Override
    protected float backgroundDim() {
        return 0.25f;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(List.of(back, commander, descension, start));
    }

    @Override
    protected List<RogueButton> wideRow() {
        back.weight(0.8f);
        commander.weight(1.4f);
        descension.weight(1.2f);
        start.weight(1.6f);
        return List.of(back, commander, descension, start);
    }

    private float headerHeight() {
        return (generatedCommander() ? 64 : 74) * RogueLook.unit();
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        if (Forge.isLandscapeMode()) {
            options.setBounds(142 * u, 9 * u, w - 148 * u, h - 14 * u);
        } else {
            float top = 2 * u + headerHeight() + 2 * u;
            options.setBounds(8 * u, top + 7 * u, w - 16 * u, h - top - 12 * u);
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        Localizer localizer = Localizer.getInstance();
        String limits = localizer.getMessage("lblRogueCustomRunLimitations");
        RogueDeck deck = newRun.deck();
        boolean generated = generatedCommander();
        if (Forge.isLandscapeMode()) {
            float h = getHeight() - 41 * u;
            RogueLook.paper(g, 3 * u, 3 * u, 130 * u, h);
            RogueLook.text(g, localizer.getMessage("lblRogueCustomRun"), 3 * u, 12 * u, 1.4f, RogueLook.INK, 130 * u, Align.center);
            RogueLook.wrap(g, limits, 13 * u, 36 * u, 110 * u, 0.95f, RogueLook.SOFT, Align.center);
            if (generated) {
                RogueLook.wrap(g, localizer.getMessage("lblRogueCommanderChosenAtStart"), 13 * u, 120 * u, 110 * u, 1, RogueLook.INK, Align.center);
            } else if (deck != null) {
                RogueLook.card(g, deck.getStartDeck().getCommanders().get(0), 34 * u, 110 * u, 66 * u, 1);
            }
            RogueLook.paper(g, 136 * u, 3 * u, getWidth() - 139 * u, h);
        } else {
            float hh = headerHeight();
            RogueLook.paper(g, 3 * u, 2 * u, getWidth() - 6 * u, hh);
            RogueLook.text(g, localizer.getMessage("lblRogueCustomRun"), 15 * u, 9 * u, 1.5f, RogueLook.INK);
            RogueLook.wrap(g, limits, 15 * u, 32 * u, getWidth() - (generated ? 32 : 92) * u, 0.95f, RogueLook.SOFT, Align.left);
            if (!generated && deck != null) {
                RogueLook.card(g, deck.getStartDeck().getCommanders().get(0), getWidth() - 62 * u, 9 * u, 44 * u, 1);
            }
            float top = 2 * u + hh + 2 * u;
            RogueLook.paper(g, 3 * u, top, getWidth() - 6 * u, options.getBottom() + 5 * u - top);
        }
    }

    private class Options extends RogueScrollPane {
        Options() {
            for (CustomRunModification modification : CustomRunModification.values()) {
                add(new Option(modification));
            }
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float u = RogueLook.unit();
            boolean two = Forge.isLandscapeMode();
            float cw = two ? visibleWidth / 2 - 8 * u : visibleWidth - 10 * u;
            float left = 5 * u;
            float y1 = 4 * u;
            float y2 = 4 * u;
            int i = 0;
            for (FDisplayObject child : getChildren()) {
                Option option = (Option) child;
                float bh = RogueLook.wrappedHeight(option.modification.getDescription(), cw - 36 * u, 1) + 25 * u;
                boolean right = two && i % 2 == 1;
                option.setBounds(right ? left + cw + 6 * u : left, right ? y2 : y1, cw, bh);
                if (right) {
                    y2 += bh + 5 * u;
                } else {
                    y1 += bh + 5 * u;
                }
                if (!two) {
                    y2 = y1;
                }
                i++;
            }
            return new ScrollBounds(visibleWidth, Math.max(y1, y2));
        }
    }

    private class Option extends FDisplayObject {
        private final CustomRunModification modification;

        Option(CustomRunModification modification) {
            this.modification = modification;
        }

        private boolean forced() {
            return RogueStart.isForced(newRun.modifications(), modification);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (!forced()) {
                boolean tick = !newRun.modifications().contains(modification);
                newRun.setModifications(RogueStart.toggle(newRun.modifications(), modification, tick));
                update();
                revalidate();
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            float h = getHeight();
            boolean on = newRun.modifications().contains(modification);
            boolean forced = forced();
            if (forced) {
                RogueLook.dashTile(g, 0, 0, w, h);
            } else {
                RogueLook.tile(g, 0, 0, w, h, on);
            }
            g.fillRect(forced ? RogueLook.SOFT : RogueLook.INK, 8 * u, 7 * u, 13 * u, 13 * u);
            g.fillRect(RogueLook.PALE, 9.4f * u, 8.4f * u, 10.2f * u, 10.2f * u);
            if (on) {
                RogueLook.item(g, "OK", 7 * u, 4 * u, 15 * u);
            }
            RogueLook.text(g, modification.getDisplayName(), 28 * u, 4 * u, 1.1f, forced ? RogueLook.SOFT : RogueLook.DEEP);
            RogueLook.wrap(g, modification.getDescription(), 28 * u, 19 * u, w - 36 * u, 1, forced ? RogueLook.SOFT : RogueLook.INK, Align.left);
        }
    }
}
