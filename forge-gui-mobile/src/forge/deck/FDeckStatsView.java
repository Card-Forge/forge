package forge.deck;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardFaceSymbols;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;
import forge.util.Utils;

/**
 * Deck statistics dashboard: mana curve, card types and mana identity.
 * Landscape: curve and types side by side with mana identity as a full width row below.
 * Portrait: all three cards stacked vertically (mana identity as a 3x2 grid), scrollable.
 */
public class FDeckStatsView extends FScrollPane {
    private static final Color PERMANENT = accent(5, 150, 105);
    private static final Color SPELL = accent(59, 130, 246);

    //demand (bright) and lands (muted) colours in WUBRGC order
    private static final Color[] DEMAND_COLORS = {
            accent(255, 250, 205), accent(59, 130, 246), accent(120, 120, 140), accent(239, 68, 68), accent(16, 185, 129), accent(156, 163, 175)
    };
    private static final Color[] LAND_COLORS = {
            accent(150, 146, 125), accent(34, 82, 160), accent(80, 80, 98), accent(158, 40, 60), accent(20, 120, 90), accent(90, 92, 105)
    };

    private DeckStatistics stats = new DeckStatistics();
    private boolean landscape;

    private final CurveCard curveCard = add(new CurveCard());
    private final TypesCard typesCard = add(new TypesCard());
    private final IdentityCard identityCard = add(new IdentityCard());

    private static Color rgb(int r, int g, int b) {
        return new Color(r / 255f, g / 255f, b / 255f, 1f);
    }

    private static final float ACCENT_SATURATION = 0.8f; //1 = original colours, lower = more muted

    /** Chart accent colour, desaturated a little so the fixed colours sit better with the skin themes. */
    private static Color accent(int r, int g, int b) {
        float luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
        float rf = luma + (r / 255f - luma) * ACCENT_SATURATION;
        float gf = luma + (g / 255f - luma) * ACCENT_SATURATION;
        float bf = luma + (b / 255f - luma) * ACCENT_SATURATION;
        return new Color(rf, gf, bf, 1f);
    }

    //Neutral colours follow the active skin theme (and adventure mode theme); accent colours below stay fixed.
    private static FSkinColor skin(FSkinColor.Colors normal, FSkinColor.Colors adventure) {
        return FSkinColor.get(Forge.isMobileAdventureMode ? adventure : normal);
    }
    //Adventure screens show their textured backdrop through their windows, so the panel is slightly translucent there.
    //Classic stays fully solid (translucent fills need the piecewise path in Card.fillRounded, see below).
    private static final float ADVENTURE_PANEL_ALPHA = 0.75f;
    private static Color panel() {
        Color c = skin(FSkinColor.Colors.CLR_THEME, FSkinColor.Colors.ADV_CLR_THEME).getColor();
        return new Color(c.r, c.g, c.b, Forge.isMobileAdventureMode ? ADVENTURE_PANEL_ALPHA : 1f);
    }
    private static Color border() { return skin(FSkinColor.Colors.CLR_BORDERS, FSkinColor.Colors.ADV_CLR_OVERLAY).getColor(); }
    private static Color track() { return skin(FSkinColor.Colors.CLR_THEME, FSkinColor.Colors.ADV_CLR_THEME).getContrastColor(44).getColor(); }
    private static Color textMain() { return skin(FSkinColor.Colors.CLR_TEXT, FSkinColor.Colors.ADV_CLR_TEXT).getColor(); }
    private static Color textDim() { return skin(FSkinColor.Colors.CLR_TEXT, FSkinColor.Colors.ADV_CLR_TEXT).alphaColor(0.8f).getColor(); }
    private static Color textMuted() { return skin(FSkinColor.Colors.CLR_TEXT, FSkinColor.Colors.ADV_CLR_TEXT).alphaColor(0.7f).getColor(); }

    private static float titleHeight() { return Utils.scale(26); }
    private static float innerPad() { return Utils.scale(12); }
    private static float rowHeight() { return Utils.scale(30); }
    private static float footerHeight() { return Utils.scale(26); }
    private static float cellHeight() { return Utils.scale(215); }

    private static String msg(String key) {
        return Forge.getLocalizer().getMessage(key);
    }

    public void setDeck(Deck deck) {
        setStatistics(DeckStatistics.fromDeck(deck));
    }

    public void setStatistics(DeckStatistics stats0) {
        stats = stats0 == null ? new DeckStatistics() : stats0;
        revalidate(); //row counts can change the portrait layout height
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        final float pad = Utils.scale(8);
        final float gap = Utils.scale(8);
        landscape = visibleWidth > visibleHeight;

        float totalHeight;
        if (landscape) {
            float colWidth = (visibleWidth - 2 * pad - gap) / 2;
            float availHeight = visibleHeight - 2 * pad - gap;
            float topHeight = Math.max(availHeight * 0.42f, Utils.scale(150));
            float bottomHeight = Math.max(availHeight - topHeight, Utils.scale(250));

            curveCard.setBounds(pad, pad, colWidth, topHeight);
            typesCard.setBounds(pad + colWidth + gap, pad, colWidth, topHeight);
            identityCard.setBounds(pad, pad + topHeight + gap, visibleWidth - 2 * pad, bottomHeight);
            totalHeight = pad + topHeight + gap + bottomHeight + pad;
        }
        else {
            float width = visibleWidth - 2 * pad;
            float curveHeight = Math.max(width * 0.7f, Utils.scale(210));
            float typesHeight = titleHeight() + typesCard.rows().size() * rowHeight() + footerHeight() + innerPad() * 2;
            float identityHeight = titleHeight() + 2 * cellHeight() + innerPad() * 2;

            float y = pad;
            curveCard.setBounds(pad, y, width, curveHeight);
            y += curveHeight + gap;
            typesCard.setBounds(pad, y, width, typesHeight);
            y += typesHeight + gap;
            identityCard.setBounds(pad, y, width, identityHeight);
            y += identityHeight + pad;
            totalHeight = y;
        }
        return new ScrollBounds(visibleWidth, totalHeight);
    }

    //-------------------------------------------------------------------------------------------------------------

    private abstract class Card extends FDisplayObject {

        protected abstract String title();
        protected abstract void drawContent(Graphics g, float x, float y, float w, float h);

        /**
         * Rounded fill that stays correct with translucent colours. Graphics.fillRoundRect overlaps its inner rects, which
         * would double-blend a translucent colour, so translucent fills are built from non-overlapping rects plus four
         * circles clipped to the corner squares.
         */
        private void fillRounded(Graphics g, Color c, float x, float y, float w, float h, float r) {
            r = Math.min(r, Math.min(w, h) / 2);
            if (c.a >= 1f) {
                g.fillRoundRect(c, x, y, w, h, r);
                return;
            }
            x = Math.round(x); y = Math.round(y); w = Math.round(w); h = Math.round(h); r = Math.round(r);
            g.fillRect(c, x + r, y, w - 2 * r, h);
            g.fillRect(c, x, y + r, r, h - 2 * r);
            g.fillRect(c, x + w - r, y + r, r, h - 2 * r);
            fillCorner(g, c, x, y, x + r, y + r, r);
            fillCorner(g, c, x + w - r, y, x + w - r, y + r, r);
            fillCorner(g, c, x, y + h - r, x + r, y + h - r, r);
            fillCorner(g, c, x + w - r, y + h - r, x + w - r, y + h - r, r);
        }

        private void fillCorner(Graphics g, Color c, float boxX, float boxY, float cx, float cy, float r) {
            if (g.startClip(boxX, boxY, r, r)) {
                g.fillCircle(c, cx, cy, r);
            }
            g.endClip(); //always paired with startClip, it also handles a clip that could not be applied
        }

        /** Thin rounded outline: straight edges as rects plus four clipped circle arcs (no stray inner lines). */
        private void drawBorder(Graphics g, Color c, float x, float y, float w, float h, float r) {
            r = Math.min(r, Math.min(w, h) / 2);
            float bw = Math.max(1, Math.round(Utils.scale(1)));
            x = Math.round(x); y = Math.round(y); w = Math.round(w); h = Math.round(h); r = Math.round(r);
            g.fillRect(c, x + r, y, w - 2 * r, bw);
            g.fillRect(c, x + r, y + h - bw, w - 2 * r, bw);
            g.fillRect(c, x, y + r, bw, h - 2 * r);
            g.fillRect(c, x + w - bw, y + r, bw, h - 2 * r);
            float ar = r - bw / 2;
            drawCorner(g, c, bw, x, y, x + r, y + r, r, ar);
            drawCorner(g, c, bw, x + w - r, y, x + w - r, y + r, r, ar);
            drawCorner(g, c, bw, x, y + h - r, x + r, y + h - r, r, ar);
            drawCorner(g, c, bw, x + w - r, y + h - r, x + w - r, y + h - r, r, ar);
        }

        private void drawCorner(Graphics g, Color c, float thickness, float boxX, float boxY, float cx, float cy, float r, float arcRadius) {
            if (g.startClip(boxX, boxY, r, r)) {
                g.drawCircle(thickness, c, cx, cy, arcRadius);
            }
            g.endClip();
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth(), h = getHeight();
            float radius = Math.round(Utils.scale(10));
            fillRounded(g, panel(), 0, 0, w, h, radius);
            drawBorder(g, border(), 0, 0, w, h, radius);

            float inner = innerPad();
            g.drawText(title().toUpperCase(), FSkinFont.get(12), textMuted(), inner, inner, w - 2 * inner, titleHeight(), false, Align.left, true);
            drawContent(g, inner, inner + titleHeight(), w - 2 * inner, h - 2 * inner - titleHeight());
        }
    }

    //-------------------------------------------------------------------------------------------------------------

    private class CurveCard extends Card {
        @Override
        protected String title() {
            return msg("lblManaCurve") + String.format(" (avg %.2f)", stats.averageCmc);
        }

        @Override
        protected void drawContent(Graphics g, float x, float y, float w, float h) {
            FSkinFont small = FSkinFont.get(10);
            float legendH = Utils.scale(16);
            float axisH = Utils.scale(16);
            float topGap = Utils.scale(14);
            float chartH = h - legendH - axisH - topGap;
            if (chartH <= 0) { return; }

            float baseY = y + topGap + chartH;
            float slot = w / DeckStatistics.CURVE_BUCKETS;
            float barW = slot * 0.8f;
            int max = stats.maxCurveValue();

            for (int i = 0; i < DeckStatistics.CURVE_BUCKETS; i++) {
                float cx = x + slot * i + slot / 2;
                int perm = stats.permanentCurve[i];
                int spell = stats.spellCurve[i];

                float permH = chartH * perm / max;
                float spellH = chartH * spell / max;
                if (perm > 0) {
                    g.fillRect(PERMANENT, cx - barW / 2, baseY - permH, barW, permH);
                }
                if (spell > 0) {
                    g.fillRect(SPELL, cx - barW / 2, baseY - permH - spellH, barW, spellH);
                }
                if (perm + spell > 0) {
                    g.drawText(String.valueOf(perm + spell), small, textDim(), cx - slot / 2, baseY - permH - spellH - topGap, slot, topGap, false, Align.center, true);
                }
                String label = i == DeckStatistics.CURVE_BUCKETS - 1 ? i + "+" : String.valueOf(i);
                g.drawText(label, small, textMuted(), cx - slot / 2, baseY + Utils.scale(2), slot, axisH, false, Align.center, true);
            }
            g.fillRect(border(), x, baseY, w, Math.max(1, Utils.scale(1)));

            //legend
            float ly = y + h - legendH;
            float dot = Utils.scale(4);
            float lx = x;
            String perm = msg("lblPermanents");
            String spell = msg("lblSpells");
            g.fillCircle(PERMANENT, lx + dot, ly + legendH / 2, dot);
            float permW = small.getBounds(perm).width;
            g.drawText(perm, small, textDim(), lx + dot * 3, ly, permW + dot, legendH, false, Align.left, true);
            lx += dot * 3 + permW + Utils.scale(16);
            g.fillCircle(SPELL, lx + dot, ly + legendH / 2, dot);
            g.drawText(spell, small, textDim(), lx + dot * 3, ly, w, legendH, false, Align.left, true);
        }
    }

    //-------------------------------------------------------------------------------------------------------------

    private static class TypeRow {
        final String label;
        final int count;
        final Color color;
        TypeRow(String label0, int count0, Color color0) {
            label = label0;
            count = count0;
            color = color0;
        }
    }

    private class TypesCard extends Card {
        List<TypeRow> rows() {
            List<TypeRow> rows = new ArrayList<>();
            rows.add(new TypeRow(msg("lblCreature"), stats.creatures, accent(16, 185, 129)));
            rows.add(new TypeRow(msg("lblInstant"), stats.instants, accent(59, 130, 246)));
            rows.add(new TypeRow(msg("lblSorcery"), stats.sorceries, accent(168, 85, 247)));
            rows.add(new TypeRow(msg("lblArtifact"), stats.artifacts, accent(156, 163, 175)));
            rows.add(new TypeRow(msg("lblEnchantment"), stats.enchantments, accent(245, 158, 11)));
            if (stats.planeswalkers > 0) {
                rows.add(new TypeRow(msg("lblPlaneswalker"), stats.planeswalkers, accent(236, 72, 153)));
            }
            if (stats.battles > 0) {
                rows.add(new TypeRow(msg("lblBattle"), stats.battles, accent(239, 68, 68)));
            }
            if (stats.otherTypes > 0) {
                rows.add(new TypeRow(msg("lblOther"), stats.otherTypes, accent(100, 116, 139)));
            }
            rows.add(new TypeRow(msg("lblLand"), stats.landCount, accent(180, 83, 9)));
            return rows;
        }

        @Override
        protected String title() {
            return msg("lblCardTypes");
        }

        @Override
        protected void drawContent(Graphics g, float x, float y, float w, float h) {
            List<TypeRow> rows = rows();
            FSkinFont labelFont = FSkinFont.get(12);
            FSkinFont valueFont = FSkinFont.get(12);

            float footerH = footerHeight();
            float rowH = Math.min(rowHeight(), (h - footerH) / rows.size());
            float labelW = Utils.scale(92);
            float valueW = Utils.scale(30);
            float gap = Utils.scale(10);
            float trackX = x + labelW + gap;
            float trackW = w - labelW - valueW - 2 * gap;
            float barH = Math.min(Utils.scale(14), rowH * 0.5f);

            int max = 1;
            for (TypeRow row : rows) {
                max = Math.max(max, row.count);
            }

            float ry = y;
            for (TypeRow row : rows) {
                g.drawText(row.label, labelFont, textDim(), x, ry, labelW, rowH, false, Align.right, true);
                float by = ry + (rowH - barH) / 2;
                g.fillRoundRect(track(), trackX, by, trackW, barH, barH / 2);
                float fillW = Math.max(row.count > 0 ? barH : 0, trackW * row.count / max);
                if (fillW > 0) {
                    g.fillRoundRect(row.color, trackX, by, fillW, barH, barH / 2);
                }
                g.drawText(String.valueOf(row.count), valueFont, textMain(), trackX + trackW + gap, ry, valueW, rowH, false, Align.right, true);
                ry += rowH;
            }

            String footer = stats.landCount + " " + msg("lblLands").toLowerCase() + "     " + stats.nonLandCount + " " + msg("lblNonLand").toLowerCase();
            g.drawText(footer, FSkinFont.get(11), textMuted(), x, y + h - footerH, w, footerH, false, Align.left, true);
        }
    }

    //-------------------------------------------------------------------------------------------------------------

    private class IdentityCard extends Card {
        @Override
        protected String title() {
            return msg("lblManaIdentity");
        }

        @Override
        protected void drawContent(Graphics g, float x, float y, float w, float h) {
            int cols = landscape ? DeckStatistics.COLOR_COUNT : 3;
            int rows = landscape ? 1 : 2;
            float cellW = w / cols;
            float cellH = Math.min(h / rows, cellHeight());

            for (int i = 0; i < DeckStatistics.COLOR_COUNT; i++) {
                int col = i % cols;
                int row = i / cols;
                drawCell(g, i, x + col * cellW, y + row * cellH, cellW, cellH);
            }
        }

        private void drawCell(Graphics g, int index, float x, float y, float w, float h) {
            float cx = x + w / 2;
            float barW = w - 2 * Utils.scale(12);
            float barX = cx - barW / 2;
            float barH = Utils.scale(6);
            FSkinFont bigFont = FSkinFont.get(w < Utils.scale(110) ? 18 : 22);
            FSkinFont labelFont = FSkinFont.get(9);
            FSkinFont countFont = FSkinFont.get(10);
            float bigH = Utils.scale(28);
            float labelH = Utils.scale(12);
            float countH = Utils.scale(14);

            float icon = Utils.scale(34);
            CardFaceSymbols.drawSymbol(DeckStatistics.COLOR_SYMBOLS[index], g, cx - icon / 2, y, icon, icon);
            float cy = y + icon + Utils.scale(4);

            int pipPct = DeckStatistics.percent(stats.pips[index], stats.totalPips);
            cy = drawMetric(g, pipPct, stats.pips[index] + " pips", msg("lblDemand"), textMain(), DEMAND_COLORS[index], cx, barX, barW, barH, cy, bigFont, labelFont, countFont, bigH, labelH, countH);

            cy += Utils.scale(6);
            int landPct = DeckStatistics.percent(stats.landSources[index], stats.totalLandSources);
            drawMetric(g, landPct, stats.landSources[index] + " " + msg("lblLands").toLowerCase(), msg("lblLands"), textDim(), LAND_COLORS[index], cx, barX, barW, barH, cy, bigFont, labelFont, countFont, bigH, labelH, countH);
        }

        private float drawMetric(Graphics g, int pct, String countText, String label, Color pctColor, Color barColor,
                                 float cx, float barX, float barW, float barH, float cy,
                                 FSkinFont bigFont, FSkinFont labelFont, FSkinFont countFont,
                                 float bigH, float labelH, float countH) {
            g.drawText(pct + "%", bigFont, pctColor, cx - barW / 2, cy, barW, bigH, false, Align.center, true);
            cy += bigH;
            g.drawText(label.toUpperCase(), labelFont, textMuted(), cx - barW / 2, cy, barW, labelH, false, Align.center, true);
            cy += labelH + Utils.scale(3);
            g.fillRoundRect(track(), barX, cy, barW, barH, barH / 2);
            if (pct > 0) {
                g.fillRoundRect(barColor, barX, cy, Math.max(barH, barW * pct / 100f), barH, barH / 2);
            }
            cy += barH + Utils.scale(3);
            g.drawText(countText, countFont, textMuted(), cx - barW / 2, cy, barW, countH, false, Align.center, true);
            return cy + countH;
        }
    }
}
