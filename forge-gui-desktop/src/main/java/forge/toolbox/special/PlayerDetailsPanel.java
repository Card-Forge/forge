package forge.toolbox.special;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.*;

import forge.game.zone.ZoneType;
import forge.gui.FThreads;
import org.apache.commons.lang3.StringUtils;

import forge.card.mana.ManaAtom;
import forge.game.player.PlayerView;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinFont;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.trackable.TrackableProperty;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.text.WordUtils;

public class PlayerDetailsPanel extends JPanel {
    private static final long serialVersionUID = -6531759554646891983L;

    private final PlayerView player;

    // Info labels
    private final Map<ZoneType, DetailLabelZone> zoneLabels = new EnumMap<>(ZoneType.class);
    private final List<DetailLabelMana> manaLabels = new ArrayList<>();
    private final DetailLabelExtra extraLabel;

    // Arena board: zones drawn as card piles, created on first use
    private final Map<ZoneType, ArenaZonePile> arenaPiles = new EnumMap<>(ZoneType.class);
    private boolean arenaMode;
    private Function<ZoneType, Runnable> zoneActionFactory;
    private BiConsumer<ZoneType, MouseEvent> zoneRightClick;

    public PlayerDetailsPanel(final PlayerView player, final EnumSet<ZoneType> supportedZones) {
        this.player = player;

        zoneLabels.put(ZoneType.Hand, new DetailLabelZone(ZoneType.Hand, "lblHandNOfMax", PlayerView::getMaxHandString));
        zoneLabels.put(ZoneType.Graveyard, new DetailLabelZone(ZoneType.Graveyard, "lblGraveyardNCardsNTypes", p -> p.getZoneTypes(TrackableProperty.Graveyard)));
        zoneLabels.put(ZoneType.Library, new DetailLabelZone(ZoneType.Library, "lblLibraryNCards"));
        zoneLabels.put(ZoneType.Exile, new DetailLabelZone(ZoneType.Exile, "lblExileNCards"));
        zoneLabels.put(ZoneType.Flashback, new DetailLabelZone(ZoneType.Flashback, "lblFlashbackNCards"));
        zoneLabels.put(ZoneType.Command, new DetailLabelZone(ZoneType.Command, "lblCommandZoneNCards"));
        //zoneLabels.put(ZoneType.Ante, new DetailLabelZone(ZoneType.Ante, "lblAnteZoneNCards"));
        zoneLabels.put(ZoneType.Sideboard, new DetailLabelZone(ZoneType.Sideboard, "lblSideboardNCards"));

        manaLabels.add(new DetailLabelMana("W", "lblWhiteManaOfN"));
        manaLabels.add(new DetailLabelMana("U", "lblBlueManaOfN"));
        manaLabels.add(new DetailLabelMana("B", "lblBlackManaOfN"));
        manaLabels.add(new DetailLabelMana("R", "lblRedManaOfN"));
        manaLabels.add(new DetailLabelMana("G", "lblGreenManaOfN"));
        manaLabels.add(new DetailLabelMana("C", "lblColorlessManaOfN"));

        EnumSet<ZoneType> extraZoneTypes = EnumSet.copyOf(supportedZones);
        extraZoneTypes.removeAll(zoneLabels.keySet());
        extraLabel = new DetailLabelExtra(extraZoneTypes);

        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, wrap"));
        populateDetails();

        updateZones();
        updateManaPool();
    }

    public static FSkinProp iconFromZone(ZoneType zoneType) {
        return FSkinProp.iconFromZone(zoneType, false);
    }

    /** Adds various labels to pool area JPanel container. */
    private void populateDetails() {
        final SkinnedPanel row1 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row2 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row3 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row4 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row5 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row6 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row7 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));

        row1.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row2.setOpaque(false);
        row3.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row4.setOpaque(false);
        row5.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row6.setOpaque(false);
        row7.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));

        // Hand, library, graveyard, exile, flashback, command
        final String constraintsCell = "w 50%-4px!, h 100%!, gapleft 2px, gapright 2px";

        row1.add(zoneLabels.get(ZoneType.Hand), constraintsCell);
        row1.add(zoneLabels.get(ZoneType.Library), constraintsCell);

        row2.add(zoneLabels.get(ZoneType.Graveyard), constraintsCell);
        row2.add(zoneLabels.get(ZoneType.Exile), constraintsCell);

        row3.add(zoneLabels.get(ZoneType.Flashback), constraintsCell);
        row3.add(zoneLabels.get(ZoneType.Command), constraintsCell);

        row4.add(extraLabel, constraintsCell);
        row4.add(zoneLabels.get(ZoneType.Sideboard), constraintsCell);

        row5.add(manaLabels.get(0), constraintsCell);
        row5.add(manaLabels.get(1), constraintsCell);

        row6.add(manaLabels.get(2), constraintsCell);
        row6.add(manaLabels.get(3), constraintsCell);

        row7.add(manaLabels.get(4), constraintsCell);
        row7.add(manaLabels.get(5), constraintsCell);

        final String constraintsRow = "w 100%!, h 14%!";
        add(row1, constraintsRow + ", gap 0 0 2% 0");
        add(row2, constraintsRow);
        add(row3, constraintsRow);
        add(row4, constraintsRow);
        add(row5, constraintsRow);
        add(row6, constraintsRow);
        add(row7, constraintsRow);
    }

    /**
     * Arena board: library, graveyard and exile (and the command zone in Commander) as
     * card piles, with slim chips beside them for the hand count and the less common
     * zones, shown only when not empty. Compact bars leave the chips out.
     */
    public JPanel createArenaZones(final Supplier<Iterable<PlayerView>> viewers, final boolean withCommand, final boolean compact) {
        arenaMode = true;
        final JPanel pnl = new JPanel(new MigLayout("insets 0, gap 4, hidemode 3"));
        pnl.setOpaque(false);
        final String pileSize = compact ? "w 58!, h 100%!" : "w 74!, h 100%!";
        if (withCommand) {
            pnl.add(getArenaPile(ZoneType.Command, viewers), pileSize + ", gapright 6");
        }
        for (final ZoneType zone : new ZoneType[] {ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile}) {
            pnl.add(getArenaPile(zone, viewers), pileSize);
        }
        if (compact) {
            updateZones();
            return pnl;
        }
        final JPanel chips = new JPanel(new MigLayout("insets 0, gap 2, wrap 2, hidemode 3"));
        chips.setOpaque(false);
        for (final ZoneType zone : new ZoneType[] {ZoneType.Hand, ZoneType.Flashback, ZoneType.Command, ZoneType.Sideboard}) {
            chips.add(zoneLabels.get(zone), "w 54!, h 24!");
        }
        chips.add(extraLabel, "w 54!, h 24!");
        pnl.add(chips, "aligny center");
        updateZones();
        return pnl;
    }

    /** An opponent's hand as a fan of card backs. */
    public JComponent createArenaHandFan(final Supplier<Iterable<PlayerView>> viewers) {
        arenaMode = true;
        return getArenaPile(ZoneType.Hand, viewers);
    }

    /** Floating mana: one chip per colour, visible only while that colour is in the pool. */
    public JPanel createArenaManaPool() {
        arenaMode = true;
        final JPanel pnl = new JPanel(new MigLayout("insets 0, gap 3, hidemode 3"));
        pnl.setOpaque(false);
        for (final DetailLabelMana label : manaLabels) {
            pnl.add(label, "w 48!, h 28!");
        }
        updateManaPool();
        return pnl;
    }

    private ArenaZonePile getArenaPile(final ZoneType zone, final Supplier<Iterable<PlayerView>> viewers) {
        ArenaZonePile pile = arenaPiles.get(zone);
        if (pile == null) {
            pile = new ArenaZonePile(player, zone, viewers);
            arenaPiles.put(zone, pile);
            if (zoneActionFactory != null) {
                addZoneMouseActions(pile, zone);
            }
        }
        pile.update();
        return pile;
    }

    public Component getLblLibrary() {
        return zoneLabels.get(ZoneType.Library);
    }

    /**
     * Handles observer update of player Zones - hand, graveyard, etc.
     */
    public void updateZones() {
        for(DetailLabelZone zone : this.zoneLabels.values())
            zone.onContentUpdate();
        extraLabel.onContentUpdate();
        if (arenaMode) {
            for (final Map.Entry<ZoneType, DetailLabelZone> e : zoneLabels.entrySet()) {
                e.getValue().setVisible(e.getKey() == ZoneType.Hand || player.getZoneSize(e.getKey()) > 0);
            }
            for (final ArenaZonePile pile : arenaPiles.values()) {
                pile.update();
            }
        }
    }

    /**
     * Handles observer update of the mana pool.
     */
    public void updateManaPool() {
        for (final DetailLabelMana label : manaLabels) {
            label.onContentUpdate();
            if (arenaMode) {
                label.setVisible(player.getMana(ManaAtom.fromName(label.color)) > 0);
            }
        }
    }

    public void setupMouseActions(Function<ZoneType, Runnable> zoneActionFactory,
                                   BiConsumer<ZoneType, MouseEvent> zoneRightClick,
                                   Function<Byte, Boolean> manaAction) {
        this.zoneActionFactory = zoneActionFactory;
        this.zoneRightClick = zoneRightClick;

        // Detail label listeners
        for(Map.Entry<ZoneType, DetailLabelZone> zoneEntry : zoneLabels.entrySet()) {
            addZoneMouseActions(zoneEntry.getValue(), zoneEntry.getKey());
        }
        for (final Map.Entry<ZoneType, ArenaZonePile> pileEntry : arenaPiles.entrySet()) {
            addZoneMouseActions(pileEntry.getValue(), pileEntry.getKey());
        }

        for (final DetailLabelMana label : manaLabels) {
            label.addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftClick(final MouseEvent e) {
                    //if shift key down, keep using mana until it runs out or no longer can be put towards the cost
                    final Byte mana = ManaAtom.fromName(label.color);
                    do {manaAction.apply(mana);}
                    while (e.isShiftDown());
                }
            });
        }

        this.extraLabel.setupMouseActions(zoneActionFactory);
    }

    private void addZoneMouseActions(final Component target, final ZoneType zone) {
        final Runnable action = zoneActionFactory.apply(zone);
        target.addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftClick(MouseEvent e) {
                action.run();
            }
            @Override
            public void onRightClick(MouseEvent e) {
                if (zoneRightClick != null) {
                    zoneRightClick.accept(zone, e);
                }
            }
        });
    }

    private static abstract class DetailLabel extends FLabel {
        public DetailLabel(final FSkinProp icon) {
            super(new FLabel.Builder().icon(FSkin.getImage(icon))
                    .opaque(false).fontSize(14).hoverable()
                    .fontStyle(Font.BOLD).iconInBackground()
                    .fontAlign(SwingConstants.RIGHT));
        }

        public abstract void onContentUpdate();

        @Override
        public void setText(final String text) {
            super.setText(text);
            autoSizeFont();
        }

        @Override
        protected void resetIcon() {
            super.resetIcon();
            autoSizeFont();
        }

        private void autoSizeFont() {
            final String text = getText();
            if (StringUtils.isEmpty(text)) { return; }

            final Graphics g = getGraphics();
            if (g == null) { return; }

            final int max = getMaxTextWidth();

            SkinFont font = null;
            for (int fontSize = 14; fontSize > 5; fontSize--) {
                font = FSkin.getBoldFont(fontSize);
                if (font.measureTextWidth(g, text) <= max) {
                    break;
                }
            }
            setFont(font);
        }
    }

    private class DetailLabelNumeric extends DetailLabel {
        private final String tooltipFormat;
        private final Function<PlayerView, Object> tooltipExtraArg;
        private final Function<PlayerView, Integer> countFunction;
        public DetailLabelNumeric(final FSkinProp icon, final String tooltipLabel,
                           Function<PlayerView, Integer> countFunction) {
            this(icon, tooltipLabel, countFunction, null);
        }

        public DetailLabelNumeric(final FSkinProp icon, final String tooltipLabel,
                            Function<PlayerView, Integer> countFunction, Function<PlayerView, Object> toolTipExtraArg) {
            super(icon);

            this.countFunction = countFunction;
            //Format in one or two format args depending on if we have a second parameter.
            Object[] placeholders = toolTipExtraArg != null ? new Object[]{"%d", "%s"} : new Object[]{"%d"};
            this.tooltipFormat = Localizer.getInstance().getMessage(tooltipLabel, placeholders);
            this.tooltipExtraArg = toolTipExtraArg;
            setFocusable(false);
        }

        public void onContentUpdate() {
            int count = countFunction.apply(player);
            this.setText(String.valueOf(count));

            if(this.tooltipExtraArg == null)
                setToolTipText(String.format(tooltipFormat, count));
            else
                setToolTipText(String.format(tooltipFormat, count, tooltipExtraArg.apply(player)));
        }
    }

    private class DetailLabelZone extends DetailLabelNumeric {
        public final ZoneType zone;

        public DetailLabelZone(ZoneType zone, String toolTipLabel) {
            this(zone, toolTipLabel, null);
        }
        private DetailLabelZone(ZoneType zone, String toolTipLabel, Function<PlayerView, Object> toolTipExtraArg) {
            super(iconFromZone(zone), toolTipLabel, (PlayerView p) -> p.getZoneSize(zone), toolTipExtraArg);
            this.zone = zone;
        }
    }

    private class DetailLabelMana extends DetailLabelNumeric {
        public final String color;

        public DetailLabelMana(String color, String toolTipLabel) {
            super(FSkinProp.MANA_IMG.get(color), toolTipLabel, (PlayerView p) -> p.getMana(ManaAtom.fromName(color)));
            this.color = color;
        }
    }

    private class DetailLabelExtra extends DetailLabel {
        final EnumSet<ZoneType> supportedZones;
        final EnumMap<ZoneType, JMenuItem> featuredZones;
        Function<ZoneType, Runnable> zoneActionFactory;

        private final JPopupMenu popupMenu;

        public DetailLabelExtra(EnumSet<ZoneType> supportedZones) {
            super(FSkinProp.IMG_STAR_OUTLINE);

            this.supportedZones = supportedZones;
            this.featuredZones = new EnumMap<>(ZoneType.class);

            String lblExtraZones = Localizer.getInstance().getMessage("lblExtraZones");
            this.popupMenu = new JPopupMenu(lblExtraZones);
            this.setToolTipText(lblExtraZones);
        }

        public void setupMouseActions(Function<ZoneType, Runnable> zoneActionFactory) {
            this.zoneActionFactory = zoneActionFactory;
            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftClick(MouseEvent e) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            });
        }

        @Override
        public void onContentUpdate() {
            FThreads.assertExecutedByEdt(true);
            if (zoneActionFactory != null) {
                for (ZoneType zone : supportedZones) {
                    int count = player.getZoneSize(zone);
                    if (!featuredZones.containsKey(zone)) {
                        if (count == 0)
                            continue;
                        this.addZone(zone);
                    }
                    featuredZones.get(zone).setText(getZoneLabelText(zone));
                }
            }
            if(supportedZones.isEmpty()) {
                setEnabled(false);
                setText("-");
            }
            else {
                setEnabled(true);
                setText("+");
            }
        }

        private String getZoneLabelText(ZoneType zone) {
            int count = player.getZoneSize(zone);
            String zoneName = WordUtils.capitalize(zone.getTranslatedName());
            return String.format("%s (%d)", zoneName, count);
        }

        private void addZone(ZoneType zone) {
            if(featuredZones.containsKey(zone))
                return;
            FSkin.SkinnedMenuItem menuItem = new FSkin.SkinnedMenuItem(getZoneLabelText(zone));
            menuItem.setIcon(FSkin.getImage(iconFromZone(zone)).resize(18, 18));
            Runnable zoneAction = zoneActionFactory.apply(zone);
            menuItem.addActionListener(event -> zoneAction.run());
            popupMenu.add(menuItem);
            featuredZones.put(zone, menuItem);

            //Add Junkyard if game wants it.
            if(zone == ZoneType.ContraptionDeck || zone == ZoneType.AttractionDeck)
                addZone(ZoneType.Junkyard);
        }
    }
}
