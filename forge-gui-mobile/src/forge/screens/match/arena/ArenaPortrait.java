package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FImageComplex;
import forge.assets.FSkinFont;
import forge.game.GameView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.screens.match.MatchController;
import forge.screens.match.views.VAvatar;
import forge.util.AITimeoutTracker;

/**
 * A player's portrait on the Arena board (port of the Swing VField avatar area and ArenaArt's portrait frame, ring and
 * life plate): the portrait in a circle on a steel plinth whose wings carry the phase ladder, a sunburst behind it that
 * comes up and breathes while it is this player's turn, a gold ring that lights up in the colour of what the player
 * can be now (picked, chosen, aimed at, under the pointer), and the life total on a plate below that counts its way to
 * a new figure while the difference floats off. Everything is drawn round the portrait's own square (the view's bounds);
 * the plinth and the sunburst reach well beyond it.
 */
public final class ArenaPortrait implements ArenaSkin<VAvatar> {
    /** What a portrait remembers between frames. */
    public static final class State {
        final ArenaMotion.Fade turnLight = new ArenaMotion.Fade(0f);
        final ArenaMotion.Fade ringMix = new ArenaMotion.Fade(1f);
        int ring = -1, ringWas = -1;
        int lifeSeen = Integer.MIN_VALUE, lifeShown;
        ArenaMotion.Tween lifeRoll;
        final ArenaMotion.Fade lifePop = new ArenaMotion.Fade(0f), lifeFlash = new ArenaMotion.Fade(0f), floatUp = new ArenaMotion.Fade(1f);
        boolean flashLost;
        String floatText;
        String lifeText = "";
        int lifeTextFor = Integer.MIN_VALUE;
        String hintText;
        // the chips round the life plate: commander damage on the left, counters and designations on the right
        final ArenaPlayerInfo.Row damageRow = new ArenaPlayerInfo.Row(), infoRow = new ArenaPlayerInfo.Row();
    }

    private static final int RING_CHOSEN = 0, RING_SELECTABLE = 1, RING_TARGET = 2, RING_HOVER = 3;
    private static final Color[] RING = {ArenaTheme.CHOSEN, ArenaTheme.SELECTABLE, ArenaTheme.rgb(0xBFEFFF), ArenaTheme.HOVER};
    private static final Color LIFE_LOST = ArenaTheme.rgb(0xFF6A5C), LIFE_GAINED = ArenaTheme.rgb(0x7BEA8B);
    private static final Color LOW_TOP = ArenaTheme.rgb(0xFF8A7A), LOW_BOTTOM = ArenaTheme.rgb(0xF0352E), LIFE_BOTTOM = ArenaTheme.rgb(0xE9E2D2);
    private static final Color STEEL_TOP = ArenaTheme.rgb(0x4A4D50), STEEL_BOTTOM = ArenaTheme.rgb(0x212327), STEEL_ACTIVE_TOP = ArenaTheme.rgb(0x786C56),
            STEEL_ACTIVE_BOTTOM = ArenaTheme.rgb(0x2E2A24), BRASS = ArenaTheme.rgb(0xB89A5E), BRASS_IDLE = ArenaTheme.rgb(0x6E6C66);
    private static final Color PLATE_TOP = ArenaTheme.rgb(0x1B1E24), PLATE_BOTTOM = ArenaTheme.rgb(0x0B0C10), PLATE_RIM_TOP = ArenaTheme.rgb(0xC9B07A),
            PLATE_RIM_BOTTOM = ArenaTheme.rgb(0x5E4E30);

    private static final Color BUBBLE = ArenaTheme.rgba(10, 12, 18, 225);
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color(), S4 = new Color();
    private static Texture sunburst;

    @Override
    public boolean draw(final Graphics g, final VAvatar v) {
        final PlayerView player = v.getPlayer();
        final State st = v.getArenaState();
        final float d = Math.min(v.getWidth(), v.getHeight());
        final float cx = v.getWidth() / 2f, cy = v.getHeight() / 2f;
        final boolean opponent = v.isArenaOpponent();
        final GameView game = MatchController.instance.getGameView();
        final boolean active = game != null && game.getPlayerTurn() == player && !game.isGameOver();

        // the sunburst behind it all while it is this player's turn, breathing once it is up
        if (st.turnLight.goal() != (active ? 1f : 0f)) {
            st.turnLight.to(active ? 1f : 0f, active ? 420 : 300);
        }
        final float light = st.turnLight.get();
        if (light > 0f) {
            final float breath = active && !st.turnLight.isMoving() ? 0.82f + 0.18f * ArenaMotion.pulse(3f) : 1f;
            drawSunburst(g, cx, cy, d, light * breath);
        }
        if (d >= ArenaBoard.windowH() * 0.09f) { //a seat's small portrait has no ladder, so no wings to carry it
            drawPlinth(g, cx, cy, d, light, opponent);
        }

        // the portrait itself, in a circle
        drawPicture(g, v.getImage(), cx, cy, d * 0.92f, player.getHasLost());

        // the ring and its light
        final int ring = ringFor(player, v.isHovered());
        if (ring != st.ring) {
            st.ringWas = st.ring;
            st.ring = ring;
            st.ringMix.set(0f);
            st.ringMix.to(1f, 180);
        }
        final float mix = st.ringMix.get();
        final float breath = ring == RING_SELECTABLE ? 0.6f + 0.4f * ArenaMotion.pulse(1.5f) : 1f;
        ArenaArt.portraitRing(g, cx, cy, d, null, 0f);
        if (st.ringWas >= 0 && mix < 1f) {
            ArenaArt.portraitRing(g, cx, cy, d, RING[st.ringWas], 0.85f * (1f - mix));
        }
        if (ring >= 0) {
            ArenaArt.portraitRing(g, cx, cy, d, RING[ring], 0.92f * mix * breath);
        }

        // the life total: it counts its way to the new figure, the plate jumps, the ring flashes, the difference floats off
        noticeLife(st, player.getLife());
        final float flash = st.lifeFlash.get();
        if (flash > 0f) {
            ArenaArt.portraitRing(g, cx, cy, d, st.flashLost ? LIFE_LOST : LIFE_GAINED, flash);
        }
        final boolean seat = d < ArenaBoard.windowH() * 0.09f;
        final float plateH = Math.max(ArenaTheme.px(22), Math.round(d * (seat ? 0.58f : 0.42f)));
        drawLifePlate(g, st, cx, cy + d / 2f, plateH);
        if (!seat) { //a seat's chips go under its name plate: beside its small plate they would run into its hand and piles
            final float chipH = Math.round(plateH * CHIP_SHARE), gap = plateH * 0.75f + ArenaTheme.px(6);
            // with seats round the table, it says when you play next as it does on theirs
            final int lead = ArenaBoard.isSeatGame() && ArenaBoard.nextTurnPlayer() == player ? ArenaPlayerInfo.LEAD_NEXT : ArenaPlayerInfo.LEAD_NONE;
            ArenaPlayerInfo.draw(g, st.damageRow, player, ArenaPlayerInfo.CMDR, ArenaPlayerInfo.LEAD_NONE, null, cx - gap, cy + d / 2f, chipH, true, 0);
            ArenaPlayerInfo.draw(g, st.infoRow, player, ArenaPlayerInfo.COUNTERS | ArenaPlayerInfo.STATUS, lead, null, cx + gap, cy + d / 2f, chipH,
                    false, 0);
        }
        final FSkinFont hf = ArenaCards.evenFont(Math.max(ArenaTheme.px(18), plateH * HINT_SHARE));
        final float hintTop = opponent ? cy + d / 2f + plateH * 0.6f + ArenaTheme.px(4) : cy - d / 2f - ArenaArt.hintHeight(hf) - ArenaTheme.px(8);
        if (ArenaBoard.isDeclaringAttack() && opponent && (ring == RING_CHOSEN || ring == RING_SELECTABLE)) {
            // while you declare attackers every opponent says, without hovering, where your attack goes and that it can go
            // to them instead (the prompt asks for the same in words)
            ArenaArt.hint(g, attackPlate(ring == RING_CHOSEN), hf, cx, hintTop, -v.screenPos.x, ArenaBoard.windowW() - v.screenPos.x);
        } else if (ring == RING_SELECTABLE && isPointerOver(v)) { // what the breathing ring means: it can be clicked, for what
            ArenaArt.hint(g, hintText(st, player), hf, cx, hintTop, -v.screenPos.x, ArenaBoard.windowW() - v.screenPos.x);
        }

        final float up = st.floatUp.get();
        if (up < 1f && st.floatText != null) {
            final FSkinFont f = ArenaCards.evenFont(Math.max(ArenaTheme.px(26), d * 0.3f));
            final float a = 1f - up * up;
            final float y = cy - d * 0.1f - d * 0.55f * ArenaMotion.OUT.at(up);
            ArenaArt.textOutlined(g, st.floatText, f, ArenaTheme.alpha(st.flashLost ? LIFE_LOST : LIFE_GAINED, a, S1), cx - d, y - f.getLineHeight() / 2f, 2 * d,
                    f.getLineHeight(), Align.center);
        }
        drawThinking(g, player, cx, cy, d);
        return true;
    }

    /** Whether the mouse is over the portrait now (the hover flag can outlive a click elsewhere). */
    private static boolean isPointerOver(final VAvatar v) {
        return Forge.magnify && v.screenPos.contains(Forge.mouseMovedX, Forge.mouseMovedY);
    }

    /** The hint plate's text against the life plate's height. */
    private static final float HINT_SHARE = 0.36f;
    private static String attackHere, clickToAttack;

    /** "Your attack goes here" for the defender being filled, "Click to attack" for the others; made once. */
    static String attackPlate(final boolean current) {
        if (attackHere == null) {
            attackHere = Forge.getLocalizer().getMessage("lblArenaAttackHere");
            clickToAttack = Forge.getLocalizer().getMessage("lblArenaClickToAttack");
        }
        return current ? attackHere : clickToAttack;
    }

    /** The share of the life plate's height a chip beside it takes. */
    private static final float CHIP_SHARE = 0.56f;

    /** "Click to choose X" (a target, a player to pick); made once per portrait. */
    private static String hintText(final State st, final PlayerView player) {
        if (st.hintText == null) {
            st.hintText = Forge.getLocalizer().getMessage("lblArenaHintChoose", player.getName());
        }
        return st.hintText;
    }

    private static int ringFor(final PlayerView player, final boolean hovered) {
        final MatchController mc = MatchController.instance;
        if (mc.isHighlighted(player)) {
            return RING_CHOSEN;
        }
        if (mc.isSelectable(player) || ArenaBoard.isAttackOption(player)) {
            return RING_SELECTABLE;
        }
        if (isStackTarget(player)) {
            return RING_TARGET;
        }
        return hovered ? RING_HOVER : -1;
    }

    /** Whether what is on top of the stack is aimed at this player. */
    static boolean isStackTarget(final PlayerView player) {
        final GameView game = MatchController.instance.getGameView();
        StackItemView item = game == null ? null : game.peekStack();
        while (item != null) {
            if (item.getTargetPlayers() != null) {
                for (final PlayerView target : item.getTargetPlayers()) {
                    if (player.equals(target)) {
                        return true;
                    }
                }
            }
            item = item.getSubInstance();
        }
        return false;
    }

    private static void noticeLife(final State st, final int life) {
        if (st.lifeSeen != Integer.MIN_VALUE && life != st.lifeSeen) {
            final int delta = life - st.lifeSeen;
            final int from = st.lifeShown;
            if (st.lifeRoll != null) {
                st.lifeRoll.cancel();
            }
            st.lifeRoll = ArenaMotion.play(Math.min(800, 320 + Math.abs(delta) * 40), ArenaMotion.OUT, t -> st.lifeShown = Math.round(ArenaMotion.lerp(from, life, t)));
            st.lifePop.set(1f);
            st.lifePop.to(0f, 460);
            st.flashLost = delta < 0;
            st.lifeFlash.set(1f);
            st.lifeFlash.to(0f, 560);
            st.floatText = (delta > 0 ? "+" : "") + delta;
            st.floatUp.set(0f);
            st.floatUp.to(1f, 1100, 0, ArenaMotion.LINEAR, null);
        } else if (st.lifeRoll == null || st.lifeRoll.isOver()) {
            st.lifeShown = life;
        }
        st.lifeSeen = life;
        if (st.lifeTextFor != st.lifeShown) {
            st.lifeTextFor = st.lifeShown;
            st.lifeText = String.valueOf(st.lifeShown);
        }
    }

    /** The steel plinth the portrait sits on: two wings that carry the phase ladder, brass along their edges. */
    private static void drawPlinth(final Graphics g, final float cx, final float cy, final float d, final float light, final boolean opponent) {
        final Color top = ArenaTheme.mix(STEEL_TOP, STEEL_ACTIVE_TOP, light, S1);
        final Color bottom = ArenaTheme.mix(STEEL_BOTTOM, STEEL_ACTIVE_BOTTOM, light, S2);
        final Color brass = ArenaTheme.mix(BRASS_IDLE, BRASS, light, S3);
        // the long blades, running out to a point either side
        final float beamH = Math.round(d * 0.15f), beamW = Math.round(d * 3.3f);
        final float by = cy - beamH / 2f + (opponent ? -d * 0.04f : d * 0.04f);
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.6f, S4), cx - beamW / 2f, by + ArenaTheme.px(3), beamW, beamH, beamH / 2f, ArenaTheme.px(8));
        g.fillSmoothRoundRectGradient(top, bottom, true, cx - beamW / 2f, by, beamW, beamH, beamH / 2f);
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.2f)), brass, cx - beamW / 2f, by, beamW, beamH, beamH / 2f);
        // the shoulders, a broader plate under the collar
        final float shH = Math.round(d * 0.34f), shW = Math.round(d * 1.85f);
        final float sy = cy - shH / 2f + (opponent ? -d * 0.06f : d * 0.06f);
        g.fillSmoothRoundRectGradient(top, bottom, true, cx - shW / 2f, sy, shW, shH, shH * 0.45f);
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.4f)), brass, cx - shW / 2f, sy, shW, shH, shH * 0.45f);
        // the collar round the portrait
        final float collar = d * 1.12f;
        g.fillSmoothRoundRectGradient(ArenaTheme.mix(top, ArenaTheme.WHITE, 0.08f, S4), bottom, true, cx - collar / 2f, cy - collar / 2f, collar, collar, collar / 2f);
        g.drawSmoothCircle(Math.max(1f, ArenaTheme.px(1.5f)), brass, cx, cy, collar / 2f);
    }

    private static void drawPicture(final Graphics g, final FImage image, final float cx, final float cy, final float d, final boolean lost) {
        if (image instanceof FImageComplex) {
            final TextureRegion r = ((FImageComplex) image).getTextureRegion();
            if (r != null) {
                ArenaArt.circleImage(g, ArenaArt.squareOf(r), cx, cy, d);
                if (lost) {
                    g.fillSmoothCircle(ArenaTheme.alpha(ArenaTheme.BLACK, 0.55f, S1), cx, cy, d / 2f);
                }
                return;
            }
        }
        if (image != null) {
            // not a plain picture (card art drawn by itself): the square, its corners hidden under a dark collar
            image.draw(g, cx - d / 2f, cy - d / 2f, d, d);
            final float corner = d * 0.21f;
            g.drawSmoothCircle(corner, ArenaTheme.NIGHT, cx, cy, d / 2f + corner);
        }
    }

    /** The plate under the portrait with the life total on it; red at 5 or less. */
    private static void drawLifePlate(final Graphics g, final State st, final float cx, final float cy, final float plateH) {
        final float pop = st.lifePop.get();
        final float grow = 1f + 0.22f * pop;
        final float h = plateH * grow, w = h * 1.5f;
        final float x = cx - w / 2f, y = cy - h / 2f;
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.7f, S1), x, y + h * 0.08f, w, h, h * 0.42f, h * 0.12f);
        g.fillSmoothRoundRectGradient(PLATE_RIM_TOP, PLATE_RIM_BOTTOM, true, x, y, w, h, h * 0.42f);
        final float rim = Math.max(1.5f, h * 0.07f);
        g.fillSmoothRoundRectGradient(PLATE_TOP, PLATE_BOTTOM, true, x + rim, y + rim, w - 2 * rim, h - 2 * rim, h * 0.42f - rim);
        final String life = st.lifeText;
        FSkinFont f = ArenaCards.evenFont(h * 0.7f);
        if (ArenaArt.textWidth(life, f) > w * 0.72f) {
            f = ArenaCards.evenFont(h * 0.7f * w * 0.72f / Math.max(1f, ArenaArt.textWidth(life, f)));
        }
        final boolean low = st.lifeShown <= 5;
        final float o = Math.max(1f, h / 26f);
        g.drawText(life, f, ArenaTheme.alpha(ArenaTheme.BLACK, 0.55f, S1), x, y + o, w, h, false, Align.center, true);
        ArenaArt.textBold(g, life, f, low ? LOW_TOP : ArenaTheme.WHITE, x, y, w, h, Align.center);
    }

    /** Arena shows an opponent who is taking long to decide as a speech bubble with rippling dots beside the portrait. */
    private static void drawThinking(final Graphics g, final PlayerView player, final float cx, final float cy, final float d) {
        final GameView gv = MatchController.instance.getGameView();
        if (gv == null) {
            return;
        }
        final float f = AITimeoutTracker.getRemainingFraction(AITimeoutTracker.key(gv.getId(), player.getId()));
        if (f < 0f) {
            return;
        }
        final float bh = Math.max(ArenaTheme.px(16), d / 4f), bw = bh * 2f;
        final float bx = cx + d / 2f - bw / 3f, by = cy - d / 2f;
        g.fillSmoothRoundRect(BUBBLE, bx, by, bw, bh, bh / 2f);
        final float dot = Math.max(ArenaTheme.px(3), bh / 5f);
        final float beat = ArenaMotion.seconds() * 2f * (float) Math.PI / 0.9f;
        ArenaMotion.requestFrame();
        for (int i = 0; i < 3; i++) {
            final float hop = (float) Math.max(0d, Math.sin(beat - i * 0.9d)) * dot * 0.8f;
            g.fillSmoothCircle(ArenaTheme.WHITE, bx + bw / 2f - dot * 1.5f + i * dot * 1.5f, by + bh / 2f - hop, dot / 2f);
        }
    }

    /** A warm sunburst behind the portrait: rays and a glow, made once into a texture and drawn faded. */
    private static void drawSunburst(final Graphics g, final float cx, final float cy, final float d, final float alpha) {
        final Texture t = sunburst();
        if (t == null) {
            ArenaArt.halo(g, cx, cy, d * 1.4f, ArenaTheme.GOLD_BRIGHT, 0.5f * alpha);
            return;
        }
        final float s = d * 3.4f;
        g.drawImageFaded(t, cx - s / 2f, cy - s / 2f, s, s, alpha);
    }

    private static Texture sunburst() {
        if (sunburst != null) {
            return sunburst;
        }
        try {
            final int n = 384;
            final Pixmap p = new Pixmap(n, n, Pixmap.Format.RGBA8888);
            p.setBlending(Pixmap.Blending.None);
            final float half = n / 2f;
            final int rays = 22;
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    final float dx = x + 0.5f - half, dy = y + 0.5f - half;
                    final float r = (float) Math.sqrt(dx * dx + dy * dy) / half; // 0 centre, 1 edge
                    if (r >= 1f) {
                        p.drawPixel(x, y, 0);
                        continue;
                    }
                    final double ang = Math.atan2(dy, dx);
                    // rays of two lengths, soft edged, fading out towards their tips
                    final float ray = (float) Math.pow(0.5 + 0.5 * Math.cos(ang * rays), 6.0);
                    final float longRay = (float) Math.pow(0.5 + 0.5 * Math.cos(ang * rays / 2.0), 2.0);
                    final float reach = 0.55f + 0.42f * longRay;
                    final float fall = Math.max(0f, 1f - r / reach);
                    final float glow = (float) Math.exp(-r * r * 9.0);
                    float a = ray * fall * fall * 0.9f + glow * 0.75f;
                    a = Math.min(1f, a) * Math.max(0f, Math.min(1f, (1f - r) * 6f));
                    final float warm = Math.min(1f, glow * 1.4f);
                    final int cr = 255, cg = Math.round(196 + 40 * warm), cb = Math.round(110 + 90 * warm);
                    p.drawPixel(x, y, (cr << 24) | (cg << 16) | (cb << 8) | Math.round(a * 255f));
                }
            }
            sunburst = new Texture(p);
            sunburst.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            p.dispose();
        } catch (final RuntimeException e) {
            e.printStackTrace();
            sunburst = null;
        }
        return sunburst;
    }
}
