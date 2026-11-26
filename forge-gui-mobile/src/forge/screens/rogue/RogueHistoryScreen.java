package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.deck.FDeckViewer;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRunHistoryEntry;
import forge.gamemodes.rogue.effect.DescensionLevel;
import forge.gui.FThreads;
import forge.toolbox.FContainer;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.List;

/** Finished runs, newest first, each with its deck. */
public class RogueHistoryScreen extends RogueScreen {
    private static final String VICTORY = "VICTORY";
    private static final String DEFEAT = "DEFEAT";

    private final List<RogueButton> tabs;
    private final RogueButton reset;
    private final Content list;

    public RogueHistoryScreen() {
        super(null);
        Localizer localizer = Localizer.getInstance();
        list = add(new Content());
        reset = add(new RogueButton(localizer.getMessage("lblRogueResetHistory"), null, RogueButton.State.NORMAL, this::confirmReset));
        tabs = RogueHomeTabs.add(this, RogueHomeTabs.HISTORY);
    }

    @Override
    public void onActivate() {
        list.fill(RogueMetaProgress.getInstance().getRunHistory());
        reset.setVisible(!list.rows.isEmpty());
    }

    private void confirmReset() {
        Localizer localizer = Localizer.getInstance();
        RogueDialog.show(localizer.getMessage("lblRogueResetRunHistory"), localizer.getMessage("lblRogueResetHistoryConfirm"),
            List.of(localizer.getMessage("lblCancel"), localizer.getMessage("lblReset")), choice -> {
                if (choice == 1) {
                    RogueController.get().submit(() -> {
                        RogueMetaProgress.getInstance().clearRunHistory();
                        FThreads.invokeInEdtLater(this::onActivate);
                    });
                }
            });
    }

    @Override
    protected String background() {
        return "tavern";
    }

    @Override
    protected float backgroundDim() {
        return 0.3f;
    }

    @Override
    protected int tab() {
        return 4;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(tabs);
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        reset.setBounds(w - 104 * u, 7 * u, 94 * u, 24 * u);
        list.setBounds(x + 8 * u, y + 34 * u, w - 16 * u, h - 39 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        float w = getWidth();
        Localizer localizer = Localizer.getInstance();
        RogueLook.paper(g, 3 * u, 3 * u, w - 6 * u, list.getBottom() + 2 * u);
        RogueLook.text(g, localizer.getMessage("lblRogueRunHistory"), 15 * u, 9 * u, 1.4f, RogueLook.INK);
        if (list.rows.isEmpty()) {
            RogueLook.text(g, localizer.getMessage("lblRogueNoCompletedRunsYet"), 3 * u, list.getTop() + list.getHeight() / 2 - 20 * u,
                1.2f, RogueLook.SOFT, w - 6 * u, Align.center);
        }
    }

    private static class Content extends RogueScrollPane {
        private final List<Row> rows = new ArrayList<>();

        void fill(List<RogueRunHistoryEntry> entries) {
            clear();
            rows.clear();
            for (int i = entries.size() - 1; i >= 0; i--) {
                rows.add(add(new Row(entries.get(i))));
            }
            revalidate();
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float u = RogueLook.unit();
            boolean wide = Forge.isLandscapeMode();
            float gap = 6 * u;
            float cw = wide ? (visibleWidth - 10 * u - gap) / 2 : visibleWidth - 10 * u;
            float h = 0;
            for (Row row : rows) {
                h = Math.max(h, row.preferredHeight(cw));
            }
            float[] colBottom = {0, 0};
            for (int i = 0; i < rows.size(); i++) {
                int col = wide ? i % 2 : 0;
                Row row = rows.get(i);
                row.setBounds(5 * u + col * (cw + gap), colBottom[col], cw, h);
                colBottom[col] += h + gap;
            }
            return new ScrollBounds(visibleWidth, Math.max(colBottom[0], colBottom[1]));
        }
    }

    private static class Row extends FContainer {
        private final RogueRunHistoryEntry entry;
        private final String outcome;
        private final String detail;
        private final String extra;
        private final RogueText path;
        private final RogueButton viewDeck;

        Row(RogueRunHistoryEntry entry) {
            this.entry = entry;
            Localizer localizer = Localizer.getInstance();
            outcome = entry.getShownOutcome();
            detail = entry.getOutcomeDetail();
            List<String> lines = new ArrayList<>();
            DescensionLevel descension = DescensionLevel.forLevel(entry.getDescensionLevel());
            if (descension != null) {
                lines.add(localizer.getMessage("lblRogueHistoryDescension", descension.level, descension.name));
            }
            if (entry.getEndlessLevel() > 0) {
                lines.add(localizer.getMessage("lblRogueEndlessLevel", entry.getEndlessLevel()));
            }
            extra = String.join("   ", lines);
            path = entry.getPath().isEmpty() ? null
                : add(new RogueText(localizer.getMessage("lblRogueHistoryPath", String.join(", ", entry.getPath())), null, 0.85f, RogueLook.SOFT));
            viewDeck = entry.getDeckSnapshot() == null ? null : add(new RogueButton(localizer.getMessage("lblViewDeck"), null,
                RogueButton.State.NORMAL, () -> FDeckViewer.show(entry.getDeckSnapshot())));
        }

        private float textTop() {
            float u = RogueLook.unit();
            float y = 48 * u;
            if (!detail.isEmpty()) {
                y += 14 * u;
            }
            if (!extra.isEmpty()) {
                y += 14 * u;
            }
            return y;
        }

        float preferredHeight(float w) {
            float u = RogueLook.unit();
            float h = textTop() + (path == null ? 0 : path.getPreferredHeight(w - 20 * u) + 3 * u);
            return h + (viewDeck == null ? 6 * u : 36 * u);
        }

        @Override
        protected void doLayout(float width, float height) {
            float u = RogueLook.unit();
            float top = textTop();
            if (path != null) {
                path.setBounds(10 * u, top, width - 20 * u, path.getPreferredHeight(width - 20 * u));
            }
            if (viewDeck != null) {
                viewDeck.setBounds(width - 90 * u, height - 34 * u, 82 * u, 28 * u);
            }
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            float h = getHeight();
            Localizer localizer = Localizer.getInstance();
            if (VICTORY.equals(outcome) || DEFEAT.equals(outcome)) {
                RogueLook.tile(g, 0, 0, w, h, false);
            } else {
                RogueLook.dashTile(g, 0, 0, w, h);
            }
            RogueLook.avatar(g, entry.getAvatarIndex(), 8 * u, 8 * u, 34 * u);
            RogueLook.fit(g, entry.getCommanderName(), 50 * u, 4 * u, 1.15f, RogueLook.INK, w - 58 * u, Align.left);
            String label;
            switch (outcome) {
                case VICTORY -> {
                    RogueLook.item(g, "AWARD", 50 * u, 20 * u, 14 * u);
                    label = localizer.getMessage("lblRogueHistoryVictory");
                }
                case DEFEAT -> {
                    RogueLook.skin(g, FSkinImage.POISON, 50 * u, 20 * u, 13 * u);
                    label = localizer.getMessage("lblRogueHistoryDefeat");
                }
                default -> {
                    RogueLook.item(g, "Exit", 50 * u, 20 * u, 14 * u);
                    label = "ABANDONED".equals(outcome) ? localizer.getMessage("lblRogueHistoryAbandoned") : outcome;
                }
            }
            RogueLook.text(g, label, 68 * u, 21 * u, 1.1f, RogueLook.INK);
            RogueLook.text(g, entry.getTimestamp() + "  " + entry.getFormattedRunTime(), 50 * u, 35 * u, 0.9f, RogueLook.SOFT);
            float y = 50 * u;
            if (!detail.isEmpty()) {
                RogueLook.fit(g, detail, 10 * u, y, 1, RogueLook.INK, w - 20 * u, Align.left);
                y += 14 * u;
            }
            if (!extra.isEmpty()) {
                RogueLook.fit(g, extra, 10 * u, y, 1, RogueLook.INK, w - 20 * u, Align.left);
            }
        }
    }
}
