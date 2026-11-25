package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.adventure.util.Config;
import forge.assets.FSkinImage;
import forge.gamemodes.rogue.AetherUpgrade;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.AetherEffect;
import forge.gui.FThreads;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.List;

/**
 * The Aetherworks: build and rank them with Echoes, switch them on within the Aether Energy, and buy the next Aether
 * Upgrade with Sparks. A tap picks an Aetherwork; the buttons act on the one picked.
 */
public class RogueAetherScreen extends RogueScreen {
    private static final Color UNUSED = new Color(1, 1, 1, 0.3f);

    private static final Color FLARE = new Color(1f, 0.96f, 0.75f, 0f);
    private static final Color GLOW = new Color(1f, 0.84f, 0.37f, 0f);

    private final List<AetherEffect> works = new ArrayList<>();
    private AetherEffect selected;
    private AetherEffect raised;
    private float raise = 1;
    private float echoesShown = -1;
    private final Content list;
    private final RogueButton reset;
    private final RogueButton toggle;
    private final RogueButton upgrade;
    private final List<RogueButton> tabs;
    private final List<RogueButton> dev = new ArrayList<>();

    public RogueAetherScreen() {
        super(null);
        Localizer localizer = Localizer.getInstance();
        list = add(new Content());
        reset = add(new RogueButton(localizer.getMessage("lblReset"), null, RogueButton.State.NORMAL, this::confirmReset));
        toggle = add(new RogueButton("", null, RogueButton.State.NORMAL, this::toggleSelected));
        upgrade = add(new RogueButton("", null, RogueButton.State.MAIN, this::upgradeSelected));
        tabs = RogueHomeTabs.add(this, RogueHomeTabs.AETHER);
        if (FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED)) {
            dev.add(add(new RogueButton(localizer.getMessage("lblRogueDevMaxAether"), null, RogueButton.State.NORMAL, this::devMaxAether)));
            dev.add(add(new RogueButton(localizer.getMessage("lblRogueDevGainEchoes"), null, RogueButton.State.NORMAL,
                () -> act(() -> RogueMetaProgress.getInstance().addEchoes(10)))));
            dev.add(add(new RogueButton(localizer.getMessage("lblRogueDevGainSparks"), null, RogueButton.State.NORMAL,
                () -> act(() -> RogueMetaProgress.getInstance().addSparks(10)))));
        }
    }

    @Override
    public void onActivate() {
        refresh();
        RogueController.get().showTutorials(List.of(RogueTutorial.AETHER));
    }

    private static RogueMetaProgress progress() {
        return RogueMetaProgress.getInstance();
    }

    private static AetherUpgrade nextUpgrade() {
        RogueMetaProgress progress = progress();
        return progress.isDescensionModeUnlocked() ? AetherUpgrade.forLevel(progress.getAetherUpgradeLevel() + 1) : null;
    }

    private void refresh() {
        RogueMetaProgress progress = progress();
        int level = progress.getAetherUpgradeLevel();
        works.clear();
        for (AetherEffect effect : AetherEffect.values()) {
            if (effect.isAccessibleAt(level)) {
                works.add(effect);
            }
        }
        if (selected == null || !works.contains(selected)) {
            selected = works.isEmpty() ? null : works.get(0);
        }
        Localizer localizer = Localizer.getInstance();
        if (selected == null) {
            toggle.setState(RogueButton.State.DISABLED);
            upgrade.setState(RogueButton.State.DISABLED);
        } else {
            boolean on = progress.getActiveAetherEffects().contains(selected);
            toggle.setText(localizer.getMessage(on ? "lblRogueSwitchOff" : "lblRogueSwitchOn"));
            toggle.setState(progress.canToggleAether(selected) ? RogueButton.State.NORMAL : RogueButton.State.DISABLED);
            int cost = progress.nextRankCost(selected);
            if (cost < 0) {
                upgrade.setText(localizer.getMessage("lblRogueMaxRank"));
                upgrade.setState(RogueButton.State.DISABLED);
            } else {
                int rank = progress.getAetherEffectRank(selected);
                upgrade.setText(localizer.getMessage(rank == 0 ? "lblRogueBuildCost" : "lblRogueUpgradeCost", cost));
                upgrade.setState(progress.canAffordNextRank(selected) ? RogueButton.State.MAIN : RogueButton.State.DISABLED);
            }
        }
        if (!dev.isEmpty()) {
            dev.get(0).setText(localizer.getMessage(level >= AetherUpgrade.getMaxLevel() ? "lblRogueDevResetAether" : "lblRogueDevMaxAether"));
        }
        list.fill();
        revalidate();
    }

    /** Changes to progress can announce achievements, so they run on the flow thread; the page follows. */
    private void act(Runnable change) {
        RogueController.get().submit(() -> {
            change.run();
            FThreads.invokeInEdtLater(this::refresh);
        });
    }

    private void toggleSelected() {
        AetherEffect effect = selected;
        act(() -> {
            if (progress().getActiveAetherEffects().contains(effect)) {
                progress().deactivateAetherEffect(effect);
            } else {
                progress().activateAetherEffect(effect);
            }
        });
    }

    private void upgradeSelected() {
        AetherEffect effect = selected;
        RogueController.get().submit(() -> {
            RogueMetaProgress progress = progress();
            int echoes = progress.getTotalEchoes();
            int before = progress.getAetherEffectRank(effect);
            boolean done = progress.upgradeAetherEffect(effect);
            if (done) {
                SoundSystem.instance.play(SoundEffectType.Artifact, SoundEffectType.Artifact.isSynced());
                if (before == 0) {
                    progress.activateAetherEffect(effect);
                }
            }
            int after = progress.getTotalEchoes();
            FThreads.invokeInEdtLater(() -> {
                refresh();
                if (done) {
                    celebrate(effect, echoes, after);
                }
            });
        });
    }

    /** The Echoes run down while the new rank's pip fills and flares, the tile glows and the rank pops. */
    private void celebrate(AetherEffect effect, int echoesBefore, int echoesAfter) {
        raised = effect;
        RogueMotion.play(0.9f, t -> {
            raise = t;
            echoesShown = echoesBefore + (echoesAfter - echoesBefore) * Interpolation.pow3Out.apply(Math.min(1, t / 0.4f));
        }, () -> {
            raised = null;
            echoesShown = -1;
        });
    }

    /** 0 to 1 over the part of the rank-up between from and to; 1 when it is not this Aetherwork's. */
    private float raising(AetherEffect effect, float from, float to) {
        if (effect != raised) {
            return 1;
        }
        return Math.max(0, Math.min(1, (raise - from) / (to - from)));
    }

    private void buyUpgrade() {
        AetherUpgrade next = nextUpgrade();
        if (next == null) {
            return;
        }
        Localizer localizer = Localizer.getInstance();
        act(() -> {
            if (progress().purchaseAetherUpgrade(next.level)) {
                SoundSystem.instance.play(SoundEffectType.Artifact, SoundEffectType.Artifact.isSynced());
                FThreads.invokeInEdtLater(() -> RogueDialog.show(localizer.getMessage("lblRogueAetherUpgradeUnlocked"),
                    localizer.getMessage("lblRogueUnlockedAetherUpgrade", next.name, next.description), FSkinImage.QUEST_ELIXIR,
                    RogueDialog.Entrance.RAYS, List.of(localizer.getMessage("lblOK")), choice -> { }));
            }
        });
    }

    private void confirmReset() {
        Localizer localizer = Localizer.getInstance();
        RogueDialog.show(localizer.getMessage("lblRogueResetAetherworks"), localizer.getMessage("lblRogueResetAetherworksConfirm"),
            List.of(localizer.getMessage("lblCancel"), localizer.getMessage("lblReset")), choice -> {
                if (choice == 1) {
                    act(() -> {
                        int refunded = progress().resetAetherEffects();
                        if (refunded > 0) {
                            FThreads.invokeInEdtLater(() -> RogueDialog.show(localizer.getMessage("lblRogueAetherworksReset"),
                                localizer.getMessage("lblRogueRefundedEchoes", refunded), List.of(localizer.getMessage("lblOK")), c -> { }));
                        }
                    });
                }
            });
    }

    private void devMaxAether() {
        act(() -> {
            RogueMetaProgress progress = progress();
            int max = AetherUpgrade.getMaxLevel();
            progress.setAetherUpgradeLevel(progress.getAetherUpgradeLevel() >= max ? 0 : max);
        });
    }

    @Override
    protected String background() {
        return "shardalley";
    }

    @Override
    protected float backgroundDim() {
        return 0.2f;
    }

    @Override
    protected int tab() {
        return 2;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        for (RogueButton tab : tabs) {
            tab.setVisible(true);
        }
        List<List<RogueButton>> rows = new ArrayList<>();
        if (!dev.isEmpty()) {
            rows.add(dev);
        }
        rows.add(List.of(toggle, upgrade));
        rows.add(tabs);
        return rows;
    }

    @Override
    protected List<RogueButton> wideRow() {
        List<RogueButton> row = new ArrayList<>();
        row.add(tabs.get(0));
        row.addAll(dev);
        row.add(toggle);
        row.add(upgrade.weight(1.5f));
        for (RogueButton tab : tabs.subList(1, tabs.size())) {
            tab.setVisible(false);
        }
        return row;
    }

    private float detailHeight() {
        float u = RogueLook.unit();
        float w = getWidth() - 30 * u;
        String text = selected == null ? "" : selected.getDescriptionWithAllRanks(progress().getAetherEffectRank(selected), progress().getAetherUpgradeLevel());
        return Math.max(88 * u, RogueLook.wrappedHeight(text, w, 1) + 52 * u);
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        boolean wide = Forge.isLandscapeMode();
        float pw = wide ? 330 * u : w - 6 * u;
        float ph = wide ? h - 6 * u : h - 6 * u - detailHeight() - 4 * u;
        reset.setBounds(3 * u + pw - 66 * u, 8 * u, 58 * u, 22 * u);
        list.setBounds(9 * u, 34 * u, pw - 12 * u, ph - 34 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        boolean wide = Forge.isLandscapeMode();
        RogueMetaProgress progress = progress();
        Localizer localizer = Localizer.getInstance();
        float pw = list.getWidth() + 12 * u;
        float ph = list.getBottom() + 3 * u;
        RogueLook.paper(g, 3 * u, 3 * u, pw, ph);
        float tx = 15 * u;
        float ty = 11 * u;
        String label = localizer.getMessage("lblRogueAetherEnergyLabel");
        RogueLook.text(g, label, tx, ty, 1.1f, RogueLook.INK);
        float ix = tx + RogueLook.textWidth(label, 1.1f) + 6 * u;
        int slots = progress.getActiveAetherEffectSlots();
        int active = progress.getActiveAetherEffectCount();
        float step = 15 * u;
        for (int i = 0; i < slots; i++) {
            RogueLook.sprite(g, Config.instance().getItemSprite("Mana"), ix + i * step, ty - u, 14 * u, 14 * u,
                i < active ? Color.WHITE : UNUSED);
        }
        float cx = ix + slots * step + 3 * u;
        RogueLook.text(g, active + "/" + slots, cx, ty, 1.1f, RogueLook.INK);
        cx += RogueLook.textWidth(active + "/" + slots, 1.1f) + 8 * u;
        cx = currency(g, FSkinImage.QUEST_GOLD, echoesShown >= 0 ? Math.round(echoesShown) : progress.getTotalEchoes(), cx, ty);
        if (progress.isDescensionModeUnlocked()) {
            currency(g, FSkinImage.QUEST_ELIXIR, progress.getTotalSparks(), cx, ty);
        }

        float dx = wide ? pw + 6 * u : 3 * u;
        float dy = wide ? 3 * u : ph + 7 * u;
        float dw = wide ? getWidth() - dx - 3 * u : getWidth() - 6 * u;
        float dh = wide ? list.getBottom() : detailHeight();
        RogueLook.paper(g, dx, dy, dw, dh);
        if (selected == null) {
            return;
        }
        int rank = progress.getAetherEffectRank(selected);
        int level = progress.getAetherUpgradeLevel();
        RogueLook.fit(g, selected.getDisplayName(), dx + 12 * u, dy + 8 * u, 1.3f, RogueLook.INK, dw - 24 * u, Align.left);
        float pop = raising(selected, 0.4f, 0.65f);
        float rankScale = 1 + 0.35f * (float) Math.sin(Math.PI * pop);
        RogueLook.text(g, localizer.getMessage("lblRogueRank", rank, selected.getEffectiveMaxRank(level)), dx + 12 * u, dy + 26 * u, rankScale,
            RogueLook.SOFT);
        RogueLook.wrap(g, selected.getDescriptionWithAllRanks(rank, level), dx + 12 * u, dy + 42 * u, dw - 24 * u, 1, RogueLook.INK, Align.left);
    }

    private static float currency(Graphics g, FSkinImage icon, int amount, float x, float y) {
        float u = RogueLook.unit();
        RogueLook.skin(g, icon, x, y - u, 13 * u);
        String text = String.valueOf(amount);
        RogueLook.text(g, text, x + 15 * u, y, 1.1f, RogueLook.INK);
        return x + 15 * u + RogueLook.textWidth(text, 1.1f) + 8 * u;
    }

    private class Content extends RogueScrollPane {
        private FDisplayObject head;
        private final List<Tile> tiles = new ArrayList<>();

        void fill() {
            clear();
            tiles.clear();
            AetherUpgrade next = nextUpgrade();
            head = progress().isDescensionModeUnlocked() ? next == null ? null : add(new UpgradeCard(next)) : add(new LockedHint());
            for (AetherEffect effect : works) {
                tiles.add(add(new Tile(effect)));
            }
            revalidate();
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float u = RogueLook.unit();
            float y = 0;
            if (head instanceof UpgradeCard card) {
                float h = card.preferredHeight(visibleWidth - 6 * u);
                card.setBounds(3 * u, y, visibleWidth - 6 * u, h);
                y += h + 6 * u;
            } else if (head instanceof LockedHint hint) {
                float h = RogueLook.wrappedHeight(hint.text(), visibleWidth - 18 * u, 0.95f) + 10 * u;
                hint.setBounds(3 * u, y, visibleWidth - 6 * u, h);
                y += h + 6 * u;
            }
            boolean two = Forge.isLandscapeMode();
            float cw = two ? (visibleWidth - 12 * u) / 2 : visibleWidth - 6 * u;
            for (int i = 0; i < tiles.size(); i++) {
                float tx = 3 * u + (two ? (i % 2) * (cw + 6 * u) : 0);
                float ty = y + (two ? i / 2 : i) * 43 * u;
                tiles.get(i).setBounds(tx, ty, cw, 39 * u);
            }
            int rows = two ? (tiles.size() + 1) / 2 : tiles.size();
            return new ScrollBounds(visibleWidth, y + rows * 43 * u);
        }
    }

    private class Tile extends FDisplayObject {
        private final AetherEffect effect;

        Tile(AetherEffect effect) {
            this.effect = effect;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            selected = effect;
            refresh();
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            float h = getHeight();
            RogueMetaProgress progress = progress();
            int rank = progress.getAetherEffectRank(effect);
            if (rank == 0) {
                RogueLook.dashTile(g, 0, 0, w, h);
            } else {
                RogueLook.tile(g, 0, 0, w, h, effect == selected);
            }
            if (rank == 0 && effect == selected) {
                RogueLook.box(g, 0, 0, w, h, 2, RogueLook.TEAL);
            }
            RogueLook.fit(g, effect.getDisplayName(), 8 * u, 4 * u, 1.1f, rank == 0 ? RogueLook.SOFT : RogueLook.DEEP, w - 16 * u, Align.left);
            int max = effect.getEffectiveMaxRank(progress.getAetherUpgradeLevel());
            float fill = raising(effect, 0.15f, 0.45f);
            for (int r = 0; r < max; r++) {
                g.fillRect(RogueLook.INK, 8 * u + r * 16 * u, 24 * u, 13 * u, 6 * u);
                g.fillRect(RogueLook.PAPER, 9 * u + r * 16 * u, 25 * u, 11 * u, 4 * u);
                float part = r < rank - 1 ? 1 : r == rank - 1 ? fill : 0;
                if (part > 0) {
                    g.fillRect(RogueLook.GOLD, 9 * u + r * 16 * u, 25 * u, 11 * u * part, 4 * u);
                }
            }
            float flare = raising(effect, 0.42f, 0.75f);
            if (effect == raised && flare > 0 && flare < 1 && rank > 0) {
                FLARE.a = 0.8f * (1 - flare);
                g.fillCircle(FLARE, 14.5f * u + (rank - 1) * 16 * u, 27 * u, (6 + 10 * flare) * u);
            }
            float glow = raising(effect, 0.45f, 1);
            if (effect == raised && glow < 1) {
                GLOW.a = (float) Math.sin(Math.PI * glow);
                RogueLook.box(g, -2 * u, -2 * u, w + 4 * u, h + 4 * u, 2.5f, GLOW);
            }
            if (progress.getActiveAetherEffects().contains(effect)) {
                String active = Localizer.getInstance().getMessage("lblRogueActiveAetherwork");
                float aw = RogueLook.textWidth(active, 0.9f);
                RogueLook.item(g, "OK", w - aw - 25 * u, 19 * u, 13 * u);
                RogueLook.text(g, active, w - aw - 10 * u, 20 * u, 0.9f, RogueLook.INK);
            }
        }
    }

    /** The next Aether Upgrade, with its cost in Sparks and Echoes. */
    private class UpgradeCard extends FContainer {
        private final AetherUpgrade upgrade;
        private final RogueButton buy;

        UpgradeCard(AetherUpgrade upgrade) {
            this.upgrade = upgrade;
            buy = add(new RogueButton(Localizer.getInstance().getMessage("lblRogueUpgrade"), null,
                progress().canAffordUpgrade(upgrade) ? RogueButton.State.MAIN : RogueButton.State.DISABLED, RogueAetherScreen.this::buyUpgrade));
        }

        float preferredHeight(float w) {
            float u = RogueLook.unit();
            return RogueLook.wrappedHeight(upgrade.description, w - 12 * u, 0.95f) + 56 * u;
        }

        @Override
        protected void doLayout(float width, float height) {
            float u = RogueLook.unit();
            buy.setBounds(width - 86 * u, height - 32 * u, 80 * u, 28 * u);
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            float h = getHeight();
            g.fillRect(RogueLook.PALE, 0, 0, w, h);
            RogueLook.box(g, 0, 0, w, h, 0.8f, RogueLook.SOFT);
            RogueLook.text(g, upgrade.name, 6 * u, 3 * u, 1.05f, RogueLook.DEEP);
            RogueLook.wrap(g, upgrade.description, 6 * u, 19 * u, w - 12 * u, 0.95f, RogueLook.INK, Align.left);
            float x = currency(g, FSkinImage.QUEST_ELIXIR, upgrade.sparkCost, 6 * u, h - 24 * u);
            currency(g, FSkinImage.QUEST_GOLD, upgrade.echoCost, x, h - 24 * u);
        }
    }

    /** Before Descension Mode, why there is nothing more to buy. */
    private static class LockedHint extends FDisplayObject {
        String text() {
            return Localizer.getInstance().getMessage("lblRogueAetherEnergyHint").replace('\n', ' ');
        }

        @Override
        public void draw(Graphics g) {
            float u = RogueLook.unit();
            g.fillRect(RogueLook.PALE, 0, 0, getWidth(), getHeight());
            RogueLook.box(g, 0, 0, getWidth(), getHeight(), 0.8f, RogueLook.SOFT);
            RogueLook.wrap(g, text(), 6 * u, 4 * u, getWidth() - 12 * u, 0.95f, RogueLook.INK, Align.left);
        }
    }
}
