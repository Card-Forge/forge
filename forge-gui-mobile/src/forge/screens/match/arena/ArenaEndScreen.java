package forge.screens.match.arena;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FImageComplex;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardAvatarImage;
import forge.card.CardRenderer;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameView;
import forge.game.player.PlayerView;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.screens.FScreen;
import forge.screens.match.MatchController;
import forge.toolbox.FButton;
import forge.toolbox.FOverlay;
import forge.toolbox.FTextArea;
import forge.util.Lang;

/**
 * The end of a game in the Arena look (port of the Swing {@code ArenaEndScreen}): after a beat, so that the last blow
 * lands and the life total runs down on the board itself, the board dims and the scene comes in over it: rays of light
 * and rising embers behind a medallion with the player's portrait set in a laurel wreath (gold for a victory, cold
 * pewter otherwise), a banner of cast metal ("Victory", "Defeat", or the plain title for a draw and other people's
 * games), one score line, and the screen's buttons as Arena pills in the shared slot. The orange pill goes on to the
 * next thing to play (the next game when there is one, else a new match, quitting only when nothing else is left);
 * the others stand beside it. The game log waits behind a dim round button in the corner, "View Battlefield" steps
 * aside to the board. Leaving holds a picture of the screen over the change and fades it once the next screen is up.
 *
 * <p>Owned by {@code ViewWinLose}, which keeps its own buttons (the mode's controllers set their text, visibility and
 * enablement and run their actions): the pills mirror them and press them. Render thread only.</p>
 */
public final class ArenaEndScreen {
    public enum Outcome { VICTORY, DEFEAT, NEUTRAL }

    // ---- layout (Swing ArenaEndScreen): shares of the height, unless said otherwise ------------------------------
    private static final float MEDALLION_Y = 0.355f, BANNER_Y = 0.595f, SCORE_Y = 0.722f, BANNER_SIZE = 0.12f;
    private static final float BUTTON_CY = 0.81f, BUTTON_H = 0.058f, BUTTON_W = 0.14f /* of the width */;
    private static final float VIEW_W = 0.125f /* of the width */, TITLE_CY = 0.09f;
    private static final float BURST_RADIUS = 0.35f /* of the width */;
    private static final int EMBERS = 30;
    private static final int BEAT_MS = 650, ENTER_MS = 560, RETURN_MS = 280;

    private static final Color BACKDROP = ArenaTheme.rgb(0x040507);
    private static final Color LOG_FILL = ArenaTheme.rgba(10, 11, 14, 245), LOG_RIM = ArenaTheme.rgba(255, 255, 255, 40);
    private static final Color SCORE = ArenaTheme.rgb(0xF4F1EA);
    private static final Color LETTER_EDGE = ArenaTheme.rgb(0x24160A), RIM_LIGHT = ArenaTheme.rgb(0xFFFCF0);
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color(), S4 = new Color();
    private static final Color[] STOPS3 = new Color[3];
    private static final float[] METAL_STOPS = {0f, 0.45f, 1f};

    private final ViewHost host;
    private final Outcome outcome;
    private final String banner;
    private final String score;
    private final PlayerView portraitOf;
    private final FButton btnNext, btnRestart, btnQuit;
    private final FTextArea log;

    private final Pill[] row = {new Pill(), new Pill(), new Pill()};
    private final Pill pillView = new Pill(), pillCopy = new Pill();
    private final Rectangle logButton = new Rectangle(), logPanel = new Rectangle();
    private final ArenaMotion.Fade logLit = new ArenaMotion.Fade(0f);
    private boolean logShown;
    private int focus = -1; // which of the row has the keyboard (-1: the primary one)
    private Pill pressed;
    private float pointerX = -1, pointerY = -1;

    private boolean arrived;
    private ArenaMotion.Tween beat;
    private final ArenaMotion.Fade fade = new ArenaMotion.Fade(0f);
    private float lw = -1, lh = -1;
    private final String[] shownText = new String[3];
    private final boolean[] shownOn = new boolean[3];

    // the portrait, cut square once it can be had
    private TextureRegion portrait;
    private FImage portraitImage;
    private long portraitTried = -1000L;

    /** What the screen asks of the view it draws for. */
    public interface ViewHost {
        /** Hides the view (the board shows; the game menu brings the screen back). */
        void hideView();

        float getWidth();

        float getHeight();
    }

    public ArenaEndScreen(final ViewHost host0, final GameView game, final FButton next, final FButton restart, final FButton quit,
            final FTextArea log0) {
        host = host0;
        btnNext = next;
        btnRestart = restart;
        btnQuit = quit;
        log = log0;
        outcome = outcomeOf(game);
        banner = bannerOf(game, outcome);
        score = scoreOf(game);
        portraitOf = portraitOf(game);
        pillView.text = Forge.getLocalizer().getMessage("lblArenaViewBattlefield");
        pillCopy.text = Forge.getLocalizer().getMessage("btnCopyToClipboard");
        log.setFont(ArenaTheme.font(20));
        log.setTextColor(FSkinColor.getStandardColor(ArenaTheme.TEXT));
        log.setVisible(false);
    }

    // ---- what it says -------------------------------------------------------------------------------------------------

    private static Outcome outcomeOf(final GameView game) {
        final String winner = game.getWinningPlayerName();
        if (winner == null || winner.isEmpty() || game.getWinningTeam() != -1 || !MatchController.instance.hasLocalPlayers()) {
            return Outcome.NEUTRAL;
        }
        for (final PlayerView p : MatchController.instance.getLocalPlayers()) {
            if (winner.equals(p.getName())) {
                return Outcome.VICTORY;
            }
        }
        return Outcome.DEFEAT;
    }

    /** "Victory" or "Defeat" for the player at this screen; the usual title for draws, team games and other people's games. */
    private static String bannerOf(final GameView game, final Outcome outcome) {
        switch (outcome) {
            case VICTORY: return Forge.getLocalizer().getMessage("lblArenaVictory");
            case DEFEAT: return Forge.getLocalizer().getMessage("lblArenaDefeat");
            default:
                final String winner = game.getWinningPlayerName();
                if (winner == null || winner.isEmpty()) {
                    return Forge.getLocalizer().getMessage("lblItsADraw");
                }
                if (game.getWinningTeam() != -1) {
                    return Forge.getLocalizer().getMessage("lblTeamWon").replace("%s", String.valueOf(game.getWinningTeam()));
                }
                return Forge.getLocalizer().getMessage("lblWinnerWon").replace("%s", winner);
        }
    }

    private static final Pattern TWO_PLAYERS = Pattern.compile("^(.+?):\\s*(\\d+)\\s+(.+?):\\s*(\\d+)$");

    /** "James  1 – 0  Opponent" for two players; any other match result as the log words it. */
    private static String scoreOf(final GameView game) {
        String s = "";
        for (final GameLogEntry o : game.getGameLog().getLogEntriesExact(GameLogEntryType.MATCH_RESULTS)) {
            s = o.message().replaceAll("\\[[^\\]]*\\]", "").trim();
        }
        final Matcher two = TWO_PLAYERS.matcher(s);
        // the skin's font has the long dash (FSkinFont adds it) but not the en dash Swing used
        return two.matches() ? two.group(1) + "   " + two.group(2) + " — " + two.group(4) + "   " + two.group(3) : s;
    }

    /** Whose portrait the medallion holds: the player at this screen, else the winner, else the first player. */
    private static PlayerView portraitOf(final GameView game) {
        if (MatchController.instance.hasLocalPlayers()) {
            for (final PlayerView p : MatchController.instance.getLocalPlayers()) {
                return p;
            }
        }
        final String winner = game.getWinningPlayerName();
        PlayerView first = null;
        for (final PlayerView p : game.getPlayers()) {
            if (first == null) {
                first = p;
            }
            if (p.getName() != null && p.getName().equals(winner)) {
                return p;
            }
        }
        return first;
    }

    // ---- coming and going ---------------------------------------------------------------------------------------------

    /** The view has been shown: the first time it waits a beat, then comes in over the board; later it comes back at once. */
    public void onShown() {
        if (arrived) {
            fade.set(0.2f);
            fade.to(1f, RETURN_MS, 0, ArenaMotion.LINEAR, null);
            return;
        }
        if (beat != null) {
            return;
        }
        prepareArt();
        beat = ArenaMotion.play(BEAT_MS, ArenaMotion.LINEAR, null, () -> {
            beat = null;
            arrived = true;
            fade.set(0f);
            fade.to(1f, ENTER_MS, 0, ArenaMotion.LINEAR, null);
        });
    }

    public void onHidden() {
        setLogShown(false);
    }

    /** Whether it has come in (until then it shows nothing and takes no clicks). */
    public boolean isReady() {
        return arrived;
    }

    private float scene() {
        return arrived ? ArenaMotion.clamp01(fade.get() / 0.55f) : 0f;
    }

    private float content() {
        return arrived ? ArenaMotion.clamp01((fade.get() - 0.2f) / 0.8f) : 0f;
    }

    // ---- layout -------------------------------------------------------------------------------------------------------

    /** Lays the screen out for the view's size: the view's own children sit out of sight, the log in its panel when shown. */
    public void layout(final float w, final float h) {
        lw = w;
        lh = h;
        btnNext.setBounds(-1, -1, 0, 0);
        btnRestart.setBounds(-1, -1, 0, 0);
        btnQuit.setBounds(-1, -1, 0, 0);
        placeRow(w, h);
        final float bh = Math.round(h * BUTTON_H);
        final float vw = Math.round(w * VIEW_W);
        pillView.bounds.set(w - vw - Math.round(w * 0.024f), Math.round(h * TITLE_CY - bh / 2f), vw, bh);
        final float icon = bh * 0.82f;
        logButton.set(pillView.bounds.x - icon - Math.round(w * 0.012f), pillView.bounds.y + (bh - icon) / 2f, icon, icon);
        logPanel.set(Math.round(w * 0.27f), Math.round(h * 0.09f), Math.round(w * 0.46f), Math.round(h * 0.66f));
        final float pad = Math.round(h * 0.022f), copyH = Math.round(h * 0.045f);
        final float copyW = Math.max(copyH * 4f, ArenaArt.textWidth(pillCopy.text, pillFont(copyH)) + copyH * 1.6f);
        pillCopy.bounds.set(logPanel.x + logPanel.width - pad - copyW, logPanel.y + logPanel.height - pad - copyH, copyW, copyH);
        log.setBounds(logPanel.x + pad, logPanel.y + pad, logPanel.width - 2 * pad, logPanel.height - 2 * pad - copyH - pad / 2f);
        log.setVisible(logShown);
    }

    /** The primary button: the next game when it can be played, else a new match; quitting only when nothing else is left. */
    private FButton primary() {
        if (btnNext.isVisible() && btnNext.isEnabled()) {
            return btnNext;
        }
        if (btnRestart.isVisible() && btnRestart.isEnabled()) {
            return btnRestart;
        }
        return btnQuit;
    }

    /** The row of pills, all one size, centred in the shared slot; a button that cannot be pressed is not offered. */
    private void placeRow(final float w, final float h) {
        final FButton main = primary();
        final FButton[] buttons = {btnNext, btnRestart, btnQuit};
        int count = 0;
        for (int i = 0; i < 3; i++) {
            final FButton b = buttons[i];
            final Pill p = row[i];
            p.button = b;
            p.primary = b == main;
            p.visible = b == main || b.isVisible() && b.isEnabled();
            p.text = b.getText() == null ? "" : b.getText();
            shownText[i] = b.getText();
            shownOn[i] = b.isVisible() && b.isEnabled();
            if (p.visible) {
                count++;
            }
        }
        final float bh = Math.round(h * BUTTON_H), gap = Math.round(w * 0.012f);
        float bw = Math.round(w * BUTTON_W);
        if (count * bw + (count - 1) * gap > w * 0.92f) {
            bw = Math.round((w * 0.92f - (count - 1) * gap) / count);
        }
        //clear of the local portrait and life total, which show low in the middle of the board
        float x = ArenaDialogKit.clearOfPortrait(w / 2f, w, count * bw + (count - 1) * gap);
        final float y = Math.round(h * BUTTON_CY - bh / 2f);
        for (final Pill p : row) {
            if (p.visible) {
                p.bounds.set(Math.round(x), y, bw, bh);
                x += bw + gap;
            } else {
                p.bounds.set(0, 0, 0, 0);
            }
        }
        if (focus >= 0 && !row[focus].visible) {
            focus = -1;
        }
    }

    /** The mode's controller may change the buttons at any time (a quest's rewards, a tournament's next match). */
    private void followButtons() {
        final FButton[] buttons = {btnNext, btnRestart, btnQuit};
        for (int i = 0; i < 3; i++) {
            final String t = buttons[i].getText();
            final boolean on = buttons[i].isVisible() && buttons[i].isEnabled();
            if (on != shownOn[i] || (t == null ? shownText[i] != null : !t.equals(shownText[i]))) {
                placeRow(lw, lh);
                return;
            }
        }
    }

    // ---- drawing ------------------------------------------------------------------------------------------------------

    /** Under the view's children: the dimmed board, the light, the medallion, the banner and the score, the log's panel. */
    public void drawScene(final Graphics g) {
        final float w = host.getWidth(), h = host.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (w != lw || h != lh) {
            layout(w, h);
        }
        followButtons();
        final float s = scene();
        if (s <= 0f) {
            return;
        }
        g.fillRect(ArenaTheme.alpha(BACKDROP, 0.6f * s, S1), 0, 0, w, h);
        ArenaArt.vignette(g, 0, 0, w, h, Math.min(w, h) * 0.34f, 0.45f * s);

        final float cx = w / 2f, cy = Math.round(h * MEDALLION_Y);
        final float d = portraitSize(h);
        final float old = g.getfloatAlphaComposite();
        g.setAlphaComposite(old * s);
        try {
            drawBurst(g, cx, cy, w);
            drawEmbers(g, cx, cy, d);
            drawTrophy(g, cx, cy, d, outcome == Outcome.VICTORY);
            drawPortrait(g, cx, cy, d);
            drawBanner(g, cx, Math.round(h * BANNER_Y), w, h);
            if (!score.isEmpty()) {
                final FSkinFont f = ArenaTheme.fontPx(Math.min(72, Math.round(h * 0.03f)));
                final float lh0 = f.getLineHeight();
                ArenaArt.textOutlined(g, score, f, SCORE, 0, Math.round(h * SCORE_Y - lh0 / 2f), w, lh0, Align.center);
            }
        } finally {
            g.setAlphaComposite(old);
        }
        if (logShown) {
            final float r = h * 0.015f;
            ArenaArt.shadow(g, logPanel.x, logPanel.y, logPanel.width, logPanel.height, r, ArenaTheme.px(22), 0.6f, ArenaTheme.px(6));
            g.fillSmoothRoundRect(LOG_FILL, logPanel.x, logPanel.y, logPanel.width, logPanel.height, r);
            g.drawSmoothRoundRect(1f, LOG_RIM, logPanel.x, logPanel.y, logPanel.width, logPanel.height, r);
        }
    }

    /** Over the view's children: the pills, the log button and View Battlefield, rising into place with the entrance. */
    public void drawFront(final Graphics g) {
        final float w = host.getWidth(), h = host.getHeight();
        final float c = content();
        if (c <= 0f || w <= 0 || h <= 0) {
            return;
        }
        final float rise = Math.round((1f - c) * h * 0.025f);
        final float old = g.getfloatAlphaComposite();
        g.setAlphaComposite(old * c);
        try {
            final Pill focused = focusedPill();
            for (final Pill p : row) {
                // the keyboard's choice is ringed once keys are used (Enter presses the primary pill anyway)
                p.draw(g, rise, p == focused && (keysUsed || Forge.lastInputWasController()));
            }
            pillView.draw(g, rise, false);
            if (logShown) {
                pillCopy.draw(g, 0f, false);
            }
            drawLogButton(g, rise);
        } finally {
            g.setAlphaComposite(old);
        }
        if (beat != null || fade.isMoving() || embersMove()) {
            ArenaMotion.requestFrame();
        }
    }

    private boolean keysUsed;

    private Pill focusedPill() {
        if (focus >= 0 && row[focus].visible) {
            return row[focus];
        }
        for (final Pill p : row) {
            if (p.visible && p.primary) {
                return p;
            }
        }
        return null;
    }

    /** A small round button with a drawn scroll, dim until the pointer comes to it (Swing LogButton). */
    private void drawLogButton(final Graphics g, final float rise) {
        final boolean over = logButton.contains(pointerX, pointerY);
        if (logLit.goal() != (over || logShown ? 1f : 0f)) {
            logLit.to(over || logShown ? 1f : 0f, ArenaMotion.HOVER_MS);
        }
        final float t = logLit.get();
        final float d = logButton.width, r = d / 2f, cx = logButton.x + r, cy = logButton.y + r + rise;
        g.fillSmoothCircle(ArenaTheme.rgbaInto(8, 9, 12, ArenaMotion.lerp(90, 235, t), S1), cx, cy, r);
        g.drawSmoothCircle(Math.max(1f, ArenaTheme.px(1.2f)), ArenaTheme.alpha(ArenaTheme.WHITE, ArenaMotion.lerp(56, 210, t) / 255f, S2), cx, cy, r - 0.5f);
        final Color icon = ArenaTheme.alpha(ArenaTheme.WHITE, ArenaMotion.lerp(110, 255, t) / 255f, S3);
        final float size = d * 0.6f, iw = size * 0.66f, ih = size * 0.82f, ix = cx - iw / 2f, iy = cy - ih / 2f;
        final float line = Math.max(1f, size * 0.085f);
        g.drawSmoothRoundRect(line, icon, ix, iy, iw, ih, size * 0.1f);
        for (int k = 0; k < 3; k++) {
            final float ly = iy + ih * (0.3f + 0.2f * k);
            g.fillSmoothRoundRect(icon, ix + iw * 0.22f, ly, iw * (k == 2 ? 0.38f : 0.56f), line, line / 2f);
        }
    }

    /** A portrait as large as the layout allows (Swing: 18% of the height, at least 14%). */
    private static float portraitSize(final float h) {
        return Math.round(h * 0.17f);
    }

    // ---- the light: rays and embers -----------------------------------------------------------------------------------

    private static Texture burstTexture;
    private static int burstSide, burstColour;
    private static boolean burstPending;

    private Color coreColour() {
        switch (outcome) {
            case VICTORY: return CORE_VICTORY;
            case DEFEAT: return CORE_DEFEAT;
            default: return CORE_NEUTRAL;
        }
    }

    private static final Color CORE_VICTORY = ArenaTheme.rgb(0xFFC860), CORE_DEFEAT = ArenaTheme.rgb(0x7E93B8), CORE_NEUTRAL = ArenaTheme.rgb(0xC9CDD6);
    private static final Color EMBER_VICTORY = ArenaTheme.rgb(0xFFC766), EMBER_DEFEAT = ArenaTheme.rgb(0xA9B4C8), EMBER_NEUTRAL = ArenaTheme.rgb(0xD8D4C8);
    private static final Color EMBER_CORE = ArenaTheme.rgb(0xFFF3D0);

    /** Works out the rays (on another thread: they are a per-pixel picture) and the banner's letters, ahead of the entrance. */
    private void prepareArt() {
        final float w = Forge.getScreenWidth(), h = Forge.getScreenHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        // the rays at half the window's resolution, a square round the medallion as wide as their reach
        final int side = Math.max(64, Math.round(w * BURST_RADIUS));
        final Color core = coreColour();
        final int colour = Color.rgba8888(core);
        if ((burstTexture == null || burstSide != side || burstColour != colour) && !burstPending) {
            burstPending = true;
            final Thread painter = new Thread(() -> {
                Pixmap pm = null;
                try {
                    pm = burstPixmap(side, core);
                } catch (final RuntimeException e) {
                    e.printStackTrace(); // the end screen shows without its rays
                }
                final Pixmap made = pm;
                Gdx.app.postRunnable(() -> {
                    burstPending = false;
                    if (made == null) {
                        return;
                    }
                    if (burstTexture != null) {
                        burstTexture.dispose();
                    }
                    burstTexture = new Texture(made);
                    burstTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    burstSide = side;
                    burstColour = colour;
                    made.dispose();
                    ArenaMotion.requestFrame();
                });
            }, "Arena end rays");
            painter.setDaemon(true);
            painter.start();
        }
        BigText.prepare(banner, Math.round(h * BANNER_SIZE), w * 0.8f, h * 0.05f);
    }

    /**
     * Soft rays from the middle of a square picture {@code side} pixels across (Swing paintVictoryBurst): about 25%
     * opaque near the middle (drawn twice in Swing; baked in here), reaching nothing at its edge, with a little noise
     * against banding. Pure arithmetic: safe off the render thread.
     */
    private static Pixmap burstPixmap(final int side, final Color core) {
        final float[] broad = spokeTable(2048, 0x71C, 13, 0.012f, 0.028f);
        final float[] fine = spokeTable(2048, 0x9A3, 34, 0.0030f, 0.0080f);
        final int cr = Math.round(core.r * 255), cg = Math.round(core.g * 255), cb = Math.round(core.b * 255);
        final float o = side / 2f, radius = side / 2f;
        final Pixmap pm = new Pixmap(side, side, Pixmap.Format.RGBA8888);
        final ByteBuffer buf = pm.getPixels();
        final byte[] line = new byte[side * 4];
        int seed = 0x13579B;
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                final float dx = x + 0.5f - o, dy = y + 0.5f - o;
                final float e = (float) Math.sqrt(dx * dx + dy * dy) / radius;
                int alpha = 0;
                float heat = 0f;
                if (e < 1f) {
                    final float th = (atan2f(dy, dx) / 6.2831855f + 0.5f) * 2047f;
                    final float ray = broad[(int) th] * 0.72f + fine[(int) th] * 0.48f;
                    final float fall = (1f - e) * (1f - e) * (1f + 2f * e);
                    final float heart = sat(1f - e * 3.4f);
                    float al = 0.25f * fall * (0.30f + 0.70f * sat(ray)) + heart * heart * 0.16f;
                    al = 1f - (1f - al) * (1f - al); // Swing drew it twice
                    seed ^= seed << 13;
                    seed ^= seed >>> 17;
                    seed ^= seed << 5;
                    alpha = clampByte(al * 255f + ((seed & 0xFF) / 255f) * Math.min(1f, fall * 8f));
                    heat = sat(1f - e * 1.8f);
                }
                final int i = x * 4;
                line[i] = (byte) Math.round(cr + (255 - cr) * heat * 0.55f);
                line[i + 1] = (byte) Math.round(cg + (255 - cg) * heat * 0.55f);
                line[i + 2] = (byte) Math.round(cb + (255 - cb) * heat * 0.55f);
                line[i + 3] = (byte) alpha;
            }
            buf.put(line);
        }
        buf.flip();
        return pm;
    }

    /** Evenly spread spokes with jittered angle, width and strength; values 0 to 1 around the circle (Swing spokeTable). */
    private static float[] spokeTable(final int n, final long seed, final int rays, final float minW, final float maxW) {
        final Random r = new Random(seed);
        final float[] t = new float[n];
        for (int i = 0; i < rays; i++) {
            final float at = (i + 0.5f + (r.nextFloat() - 0.5f) * 0.6f) / rays * n;
            final float width = n * (minW + r.nextFloat() * (maxW - minW));
            final float gain = 0.30f + r.nextFloat() * 0.70f;
            for (int k = 0; k < n; k++) {
                float dist = Math.abs(k - at);
                dist = Math.min(dist, n - dist);
                final float q = dist / width;
                if (q < 3f) {
                    t[k] = Math.max(t[k], gain * (float) Math.exp(-q * q));
                }
            }
        }
        return t;
    }

    private static float sat(final float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    private static int clampByte(final float v) {
        return v <= 0f ? 0 : v >= 255f ? 255 : (int) v;
    }

    private static float atan2f(final float y, final float x) {
        final float ax = Math.abs(x), ay = Math.abs(y);
        if (ax == 0f && ay == 0f) {
            return 0f;
        }
        final float t = Math.min(ax, ay) / Math.max(ax, ay);
        final float s = t * t;
        float r = ((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * t + t;
        if (ay > ax) {
            r = 1.57079637f - r;
        }
        if (x < 0f) {
            r = 3.14159274f - r;
        }
        return y < 0f ? -r : r;
    }

    private void drawBurst(final Graphics g, final float cx, final float cy, final float w) {
        if (burstTexture == null) {
            return;
        }
        final float r = w * BURST_RADIUS;
        g.drawImage(burstTexture, cx - r, cy - r, 2 * r, 2 * r);
    }

    // the embers' places: how far up their climb, how far to the side, their size and strength (Swing paintEmbers)
    private static final float[] EMBER_CLIMB = new float[EMBERS], EMBER_SIDE = new float[EMBERS], EMBER_SIZE = new float[EMBERS],
            EMBER_GLOW = new float[EMBERS], EMBER_TRAIL = new float[EMBERS], EMBER_SPEED = new float[EMBERS];

    static {
        final Random r = new Random(0xE3BE25L);
        for (int i = 0; i < EMBERS; i++) {
            EMBER_CLIMB[i] = r.nextFloat();
            EMBER_SIDE[i] = (i % 2 == 0 ? -1f : 1f) * (0.08f + 0.92f * r.nextFloat());
            EMBER_SIZE[i] = 0.007f + 0.015f * r.nextFloat() * r.nextFloat();
            EMBER_GLOW[i] = 0.55f + 0.45f * r.nextFloat();
            EMBER_TRAIL[i] = 2.5f + 2.5f * r.nextFloat();
            EMBER_SPEED[i] = 1f / (7f + 5f * r.nextFloat());
        }
    }

    /** Embers drift up on a desktop, where the screen keeps drawing anyway; elsewhere they stay put, costing nothing. */
    private static boolean embersMove() {
        return GuiBase.getInterface() != null && GuiBase.getInterface().isRunningOnDesktop();
    }

    /** Small soft sparks rising behind the medallion, each with a faint trail beneath it, thinning out as they climb. */
    private void drawEmbers(final Graphics g, final float cx, final float cy, final float size) {
        final Color c = outcome == Outcome.VICTORY ? EMBER_VICTORY : outcome == Outcome.DEFEAT ? EMBER_DEFEAT : EMBER_NEUTRAL;
        final float strength = outcome == Outcome.VICTORY ? 1f : 0.6f;
        final float now = embersMove() ? ArenaMotion.seconds() : 0f;
        for (int i = 0; i < EMBERS; i++) {
            float climb = EMBER_CLIMB[i] + now * EMBER_SPEED[i];
            climb -= (float) Math.floor(climb);
            float dx = EMBER_SIDE[i] * size * (2.5f - 0.8f * climb);
            final float dy = size * (0.9f - 2.5f * climb);
            final float radius = size * EMBER_SIZE[i];
            // they come out of nothing and go into nothing, rather than appearing at the foot of their climb
            final float life = sat(climb / 0.08f) * (1f - sat((climb - 0.85f) / 0.15f));
            final float alpha = strength * (0.95f - 0.55f * climb) * EMBER_GLOW[i] * life;
            if (dx * dx + dy * dy < size * size * 0.42f) {
                dx = dx < 0f ? dx - size * 0.72f : dx + size * 0.72f; // clear of the picture
            }
            if (alpha <= 0.01f) {
                continue;
            }
            final float x = cx + dx, y = cy + dy, halo = radius * 2.8f, trail = radius * EMBER_TRAIL[i];
            g.fillGradientRect(ArenaTheme.alpha(c, 0.25f * alpha, S1), ArenaTheme.alpha(c, 0f, S2), true, x - radius * 0.32f, y, radius * 0.64f, trail);
            g.drawSoftShadow(ArenaTheme.alpha(c, 0.66f * alpha, S1), x - radius, y - radius, 2 * radius, 2 * radius, radius, halo - radius);
            g.fillSmoothCircle(ArenaTheme.alpha(EMBER_CORE, alpha, S2), x, y, radius * 0.55f);
        }
    }

    // ---- the medallion ------------------------------------------------------------------------------------------------

    private static final Color[] GOLD = {ArenaTheme.rgb(0xFFE8AE), ArenaTheme.rgb(0xB88E3C), ArenaTheme.rgb(0x6E5024)};
    private static final Color[] PEWTER = {ArenaTheme.rgb(0xBAC2CA), ArenaTheme.rgb(0x666C72), ArenaTheme.rgb(0x3C4046)};
    private static final Color GOLD_HALO = ArenaTheme.rgb(0xFFBE5A);
    private static final Color GOLD_LINE = ArenaTheme.rgb(0x2A1B0A), PEWTER_LINE = ArenaTheme.rgb(0x15171A);

    /**
     * The medallion behind a round portrait of diameter d centred on cx, cy (Swing paintTrophyFrame, on the GPU): a
     * reeded ring inside a laurel wreath, a spearhead crest above and a tied ribbon below. Gold for a victory, pewter
     * otherwise; everything within about 0.95 d of the middle.
     */
    private static void drawTrophy(final Graphics g, final float cx, final float cy, final float d, final boolean victory) {
        final Color[] m = victory ? GOLD : PEWTER;
        final Color line = victory ? GOLD_LINE : PEWTER_LINE;
        // it stands off the board: one wide soft shadow under all of it, a warm halo for a victory
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.5f, S1), cx - 0.8f * d, cy - 0.78f * d, 1.6f * d, 1.6f * d, 0.8f * d, 0.12f * d);
        if (victory) {
            g.drawGlow(ArenaTheme.alpha(GOLD_HALO, 0.38f, S1), cx - 0.63f * d, cy - 0.63f * d, 1.26f * d, 1.26f * d, 0.63f * d, 0.07f * d);
        }
        // the ribbon's two swallowtails and its knot, under the wreath
        for (int s = -1; s <= 1; s += 2) {
            leaf(g, cx + s * 0.03f * d, cy + 0.70f * d, 0.27f * d, 0.06f * d, (float) Math.toRadians(90 - s * 48), m[1], m[2], line, 0.75f);
        }
        // the wreath, one side at a time: leaves in two rows along a stem, alternately under and over
        for (int pass = 0; pass < 2; pass++) {
            for (int s = -1; s <= 1; s += 2) {
                wreath(g, cx, cy, d, s, pass == 1, m, line);
            }
        }
        // the crest: a spearhead and a blade leaning out to each side
        for (int s = -1; s <= 1; s += 2) {
            leaf(g, cx + s * 0.05f * d, cy - 0.64f * d, 0.20f * d, 0.045f * d, (float) Math.toRadians(-90 + s * 34), m[0], m[1], line, 1f);
        }
        leaf(g, cx, cy - 0.60f * d, 0.35f * d, 0.06f * d, (float) Math.toRadians(-90), m[0], m[1], line, 1f);
        g.fillSmoothRoundRect(ArenaTheme.alpha(m[1], 1f, S1), cx - 0.1f * d, cy + 0.67f * d, 0.2f * d, 0.1f * d, 0.05f * d);
        g.drawSmoothRoundRect(Math.max(1f, d * 0.008f), line, cx - 0.1f * d, cy + 0.67f * d, 0.2f * d, 0.1f * d, 0.05f * d);

        // the ring: its cast shadow, the cast metal lit from above, a dark line at its edge
        final float rr = 0.628f * d;
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.66f, S1), cx - rr, cy - rr + 0.03f * d, 2 * rr, 2 * rr, rr, 0.03f * d);
        STOPS3[0] = m[0];
        STOPS3[1] = m[1];
        STOPS3[2] = m[2];
        g.drawArenaShape(Graphics.SHAPE_FILL, cx - rr, cy - rr, 2 * rr, 2 * rr, rr, 0f, STOPS3, METAL_STOPS, true);
        g.drawSmoothCircle(Math.max(1.5f, d * 0.012f), line, cx, cy, rr);
        // reeding round the ring, like the milled edge of a coin
        final float reed = 0.587f * d, dot = Math.max(1f, d * 0.0075f);
        for (int i = 0; i < 64; i++) {
            final double a = i * Math.PI * 2 / 64;
            final float px = cx + (float) Math.sin(a) * reed, py = cy - (float) Math.cos(a) * reed;
            g.fillSmoothCircle(ArenaTheme.alpha(m[2], 0.85f, S1), px, py + dot * 0.4f, dot);
            g.fillSmoothCircle(ArenaTheme.alpha(m[0], 0.55f, S2), px, py - dot * 0.3f, dot * 0.6f);
        }
        // the bezel the picture sits in: sunk, so dark at the top and lit at the foot
        final float br = 0.532f * d;
        g.drawArenaShape(Graphics.SHAPE_RING, cx - br, cy - br, 2 * br, 2 * br, br, Math.max(2f, d * 0.03f), m[2], m[0], true);
        // a gleam on the upper left of the metal
        ArenaArt.haloEllipse(g, cx - 0.36f * d, cy - 0.42f * d, 0.16f * d, 0.07f * d, ArenaTheme.WHITE, victory ? 0.35f : 0.22f);
    }

    /** One side of the wreath ({@code s} -1 left, 1 right), the leaves under the stem or those over it. */
    private static void wreath(final Graphics g, final float cx, final float cy, final float d, final int s, final boolean over,
            final Color[] m, final Color line) {
        final int n = 11;
        for (int i = 0; i < n; i++) {
            final float t = i / (float) (n - 1);
            final double a = Math.toRadians(168 - t * 146);
            final double rad = 0.742 - t * 0.022;
            final float x = (float) (Math.sin(a) * rad), y = (float) (-Math.cos(a) * rad);
            // the stem's direction, heading up towards the crest (right side; the left is its mirror)
            final double tang = Math.atan2(-Math.sin(a), -Math.cos(a));
            final float len = (0.235f - t * 0.085f) * d, wid = (0.060f - t * 0.022f) * d;
            final double outA = tang + 0.56, inA = tang - 0.50;
            final boolean outerOver = i % 2 == 0;
            if (outerOver == over) {
                leaf(g, cx + s * x * d, cy + y * d, len, wid, mirror(outA, s), over ? m[0] : m[1], over ? m[1] : m[2], line, 1f);
            }
            if (outerOver != over) {
                leaf(g, cx + s * x * d, cy + y * d, len * 0.70f, wid * 0.82f, mirror(inA, s), over ? m[0] : m[1], over ? m[1] : m[2], line, 1f);
            }
            if (over && i == n - 1) {
                // one last leaf finishes the branch, pointing at the crest
                leaf(g, cx + s * x * d, cy + y * d, len * 0.92f, wid * 0.86f, mirror(tang - 0.06, s), m[0], m[1], line, 1f);
            }
        }
    }

    /** An angle of the right side, for side s (the left side is the mirror image). */
    private static float mirror(final double angle, final int s) {
        return (float) (s > 0 ? angle : Math.PI - angle);
    }

    private static final Color[] LEAF = new Color[3];
    private static final float[] LEAF_STOPS = {0f, 0.5f, 1f};

    /**
     * A leaf (or any pointed blade) from its base at bx, by along {@code angle} (radians, y down), {@code len} long and
     * {@code wid} wide either side of its middle: a capsule turned into place, lit along its upper edge, with a dark
     * line round it and a bright rib.
     */
    private static void leaf(final Graphics g, final float bx, final float by, final float len, final float wid, final float angle,
            final Color light, final Color dark, final Color line, final float alpha) {
        final float ca = (float) Math.cos(angle), sa = (float) Math.sin(angle);
        final float mx = bx + ca * len / 2f, my = by + sa * len / 2f;
        final float h = Math.max(2f, wid * 1.7f);
        g.startRotateTransform(mx, my, -(float) Math.toDegrees(angle));
        try {
            LEAF[0] = ArenaTheme.alpha(light, alpha, S1);
            LEAF[1] = ArenaTheme.mix(light, dark, 0.55f, S2);
            LEAF[1].a = alpha;
            LEAF[2] = ArenaTheme.alpha(dark, alpha, S3);
            g.drawArenaShape(Graphics.SHAPE_FILL, mx - len / 2f, my - h / 2f, len, h, h / 2f, 0f, LEAF, LEAF_STOPS, true);
            g.drawSmoothRoundRect(Math.max(1f, h * 0.09f), ArenaTheme.alpha(line, 0.85f * alpha, S4), mx - len / 2f, my - h / 2f, len, h, h / 2f);
            final float rib = Math.max(1f, h * 0.08f);
            g.fillSmoothRoundRect(ArenaTheme.alpha(light, 0.55f * alpha, S4), mx - len * 0.38f, my - rib / 2f, len * 0.7f, rib, rib / 2f);
        } finally {
            g.endTransform();
        }
    }

    /** The portrait cut to a circle, with a thin dark line between it and the metal; a dark disc while it loads. */
    private void drawPortrait(final Graphics g, final float cx, final float cy, final float d) {
        final TextureRegion region = portraitRegion();
        final float r = d / 2f;
        if (region == null) {
            g.fillSmoothCircle(ArenaTheme.rgbaInto(20, 17, 13, 255, S1), cx, cy, r);
        } else {
            ArenaArt.circleImage(g, region, cx, cy, d);
        }
        g.drawSmoothCircle(Math.max(1.5f, d * 0.012f), ArenaTheme.rgbaInto(26, 18, 10, 255, S1), cx, cy, r);
        // the picture sits under glass: a soft shade at its rim
        g.drawArenaShape(Graphics.SHAPE_RING, cx - r, cy - r, d, d, r, Math.max(2f, d * 0.06f),
                ArenaTheme.alpha(ArenaTheme.BLACK, 0.45f, S2), ArenaTheme.alpha(ArenaTheme.BLACK, 0.15f, S3), true);
    }

    /** The portrait's picture as a centred square region, once it has loaded (tried again now and then until it has). */
    private TextureRegion portraitRegion() {
        if (portrait != null || portraitOf == null) {
            return portrait;
        }
        final long frame = ArenaMotion.frame();
        if (frame - portraitTried < 30) {
            return null;
        }
        portraitTried = frame;
        if (portraitImage == null) {
            portraitImage = MatchController.getPlayerAvatar(portraitOf);
        }
        FImage img = portraitImage;
        if (img instanceof CardAvatarImage && portraitOf.getAvatarCardImageKey() != null) {
            img = CardRenderer.getCardArt(portraitOf.getAvatarCardImageKey(), false, false, false, false, false, false, false, false, true, false);
        }
        if (img instanceof FImageComplex) {
            final TextureRegion whole = ((FImageComplex) img).getTextureRegion();
            if (whole != null && whole.getRegionWidth() > 0 && whole.getRegionHeight() > 0) {
                final int rw = whole.getRegionWidth(), rh = whole.getRegionHeight(), side = Math.min(rw, rh);
                portrait = new TextureRegion(whole, (rw - side) / 2, (rh - side) / 2, side, side);
            }
        }
        return portrait;
    }

    // ---- the banner ---------------------------------------------------------------------------------------------------

    private static final Color[] BANNER_VICTORY = {ArenaTheme.rgb(0xFFE9A8), ArenaTheme.rgb(0xD9A441)};
    private static final Color[] BANNER_DEFEAT = {ArenaTheme.rgb(0xEEF1F6), ArenaTheme.rgb(0x9AA6BA)};
    private static final Color[] BANNER_NEUTRAL = {ArenaTheme.rgb(0xFFFFFF), ArenaTheme.rgb(0xB9BDC6)};
    private static final Color RULE_COOL = ArenaTheme.rgb(0xC4CEDF);

    /** The banner: a soft light behind the word, its shadow, a crisp dark line round the letters, the letters as cast metal. */
    private void drawBanner(final Graphics g, final float cx, final float cy, final float w, final float h) {
        final Color[] metal = outcome == Outcome.VICTORY ? BANNER_VICTORY : outcome == Outcome.DEFEAT ? BANNER_DEFEAT : BANNER_NEUTRAL;
        final BigText text = BigText.get(banner);
        final float textH, textW, top;
        if (text == null) {
            // the large letters could not be made: the skin's font, as large as it comes
            final FSkinFont f = ArenaTheme.fontPx(72);
            textH = f.getLineHeight();
            textW = ArenaArt.textWidth(banner, f);
            top = cy - textH / 2f;
            g.drawGlow(ArenaTheme.alpha(coreColour(), 0.35f, S1), cx - textW / 2f, top + textH * 0.2f, textW, textH * 0.6f, textH * 0.3f, textH * 0.4f);
            ArenaArt.textOutlined(g, banner, f, metal[0], 0, top, w, textH, Align.center);
        } else {
            textH = text.capHeight;
            textW = text.width;
            top = cy - textH / 2f;
            // a wide haze of light behind the word, so that the letters stay crisp
            g.drawSoftShadow(ArenaTheme.alpha(coreColour(), 0.22f, S1), cx - textW / 2f, top, textW, textH, textH * 0.3f, textH * 0.45f);
            text.draw(g, cx - textW / 2f, top, metal[0], metal[1], h);
        }
        final float ruleY = Math.round(cy + textH / 2f + h * BANNER_SIZE * 0.32f);
        final float ruleW = Math.min(w * 0.4f, Math.max(textW * 1.3f, w * 0.25f));
        ArenaArt.divider(g, cx - ruleW / 2f, ruleY, ruleW, ArenaTheme.alpha(outcome == Outcome.VICTORY ? ArenaTheme.GOLD_BRIGHT : RULE_COOL, 0.85f, S1),
                Math.max(1f, h * 0.0016f));
    }

    /**
     * Large letters made for one word at one size: the skin's own typeface rendered at the size it is shown at (the
     * skin's fonts stop at 72 px and are scaled up beyond that, which blurs them), shaded top to bottom per vertex.
     */
    private static final class BigText {
        private static BigText current;

        final String text;
        final int px;
        final BitmapFont font;
        final BitmapFontCache cache;
        final float width, capHeight, scale;

        private BigText(final String text, final int px, final BitmapFont font, final float scale) {
            this.text = text;
            this.px = px;
            this.font = font;
            this.scale = scale;
            cache = new BitmapFontCache(font, font.usesIntegerPositions());
            final GlyphLayout layout = cache.setText(text, 0, 0);
            width = layout.width;
            capHeight = font.getCapHeight();
        }

        static BigText get(final String text) {
            return current != null && current.text.equals(text) ? current : null;
        }

        /** Makes the letters for this word at {@code px} logical pixels, smaller if it would be wider than {@code most}. */
        static void prepare(final String text, final int px, final float most, final float least) {
            if (current != null && current.text.equals(text) && current.px == px) {
                return;
            }
            try {
                BigText made = make(text, px);
                if (made != null && made.width > most && px > least) {
                    final int fit = Math.max(Math.round(least), (int) Math.floor(px * most / made.width));
                    made.font.dispose();
                    made = make(text, fit);
                }
                if (current != null) {
                    current.font.dispose();
                }
                current = made;
            } catch (final RuntimeException e) {
                e.printStackTrace(); // the banner falls back to the skin's font
                current = null;
            }
        }

        private static BigText make(final String text, final int px) {
            final FileHandle ttf;
            if (Lang.initInstance(Forge.locale).getFontFile() != null && !Forge.forcedEnglishonCJKMissing) {
                ttf = Gdx.files.absolute(ForgeConstants.FONTS_DIR + Forge.CJK_Font + ".ttf");
            } else {
                ttf = FSkin.getSkinFile("font1.ttf");
            }
            if (ttf == null || !ttf.exists()) {
                return null;
            }
            // in framebuffer pixels, so that a scaled (HiDPI) window shows them crisp too
            final float density = Gdx.graphics == null || Forge.getScreenHeight() <= 0 ? 1f
                    : Math.max(1f, Gdx.graphics.getBackBufferHeight() / (float) Forge.getScreenHeight());
            final FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttf);
            try {
                final FreeTypeFontParameter p = new FreeTypeFontParameter();
                p.size = Math.max(8, Math.round(px * density));
                final StringBuilder chars = new StringBuilder();
                text.codePoints().distinct().forEach(chars::appendCodePoint);
                chars.append("Hx"); // the cap height comes from these
                p.characters = chars.toString();
                p.minFilter = Texture.TextureFilter.Linear;
                p.magFilter = Texture.TextureFilter.Linear;
                p.kerning = true;
                final BitmapFont font = generator.generateFont(p);
                font.setUseIntegerPositions(false);
                font.getData().setScale(1f / density);
                return new BigText(text, px, font, density);
            } finally {
                generator.dispose();
            }
        }

        /**
         * Draws the word with its top-left at x, top (Forge coordinates): its shadow, a dark line round it, then the
         * letters shaded from {@code light} at the top to {@code deep} at the foot, with a pale rim along their tops.
         */
        void draw(final Graphics g, final float x, final float top, final Color light, final Color deep, final float h) {
            final float a = g.getfloatAlphaComposite();
            final float gx = g.adjustX(x), gTop = g.adjustY(top, 0);
            // the cache holds the word with its top at y 0; glyph boxes run below it
            final float o = Math.max(1.5f, h * 0.0024f);
            pass(g, gx, gTop - 2f, ArenaTheme.BLACK, ArenaTheme.BLACK, 0.66f * a);
            for (int k = 0; k < 8; k++) {
                final double ang = k * Math.PI / 4;
                pass(g, gx + (float) Math.cos(ang) * o, gTop + (float) Math.sin(ang) * o, LETTER_EDGE, LETTER_EDGE, a);
            }
            final Color lit = ArenaTheme.mix(light, ArenaTheme.WHITE, 0.35f, S3);
            pass(g, gx, gTop, lit, deep, a);
            // a rim of near white along the tops of the letters
            g.startClip(x, top, width + 4f, capHeight * 0.16f);
            pass(g, gx, gTop, RIM_LIGHT, RIM_LIGHT, 0.75f * a);
            g.endClip();
        }

        /** One drawing of the word at gx, gy (batch coordinates), shaded per vertex from {@code top} to {@code bottom}. */
        private void pass(final Graphics g, final float gx, final float gy, final Color top, final Color bottom, final float alpha) {
            cache.setPosition(gx, gy);
            final float[] v = cache.getVertices(0);
            final int count = cache.getVertexCount(0);
            final float yTop = gy, yBottom = gy - capHeight * 1.25f;
            for (int i = 0; i + 4 < count; i += 5) {
                final float t = sat((yTop - v[i + 1]) / Math.max(1f, yTop - yBottom));
                v[i + 2] = Color.toFloatBits(top.r + (bottom.r - top.r) * t, top.g + (bottom.g - top.g) * t, top.b + (bottom.b - top.b) * t, alpha);
            }
            cache.draw(g.getBatch());
        }
    }

    /** Lets go of the art it keeps for the next end screen (the rays and the banner's letters). Render thread. */
    public static void disposeArt() {
        if (burstTexture != null) {
            burstTexture.dispose();
            burstTexture = null;
        }
        if (BigText.current != null) {
            BigText.current.font.dispose();
            BigText.current = null;
        }
    }

    // ---- input --------------------------------------------------------------------------------------------------------

    /** The pointer moved over the view (local coordinates). */
    public void pointer(final float x, final float y) {
        if (x == pointerX && y == pointerY) {
            return;
        }
        pointerX = x;
        pointerY = y;
        for (final Pill p : row) {
            p.setHover(p.contains(x, y));
        }
        pillView.setHover(pillView.contains(x, y));
        pillCopy.setHover(logShown && pillCopy.contains(x, y));
        ArenaMotion.requestFrame();
    }

    private Pill pillAt(final float x, final float y) {
        for (final Pill p : row) {
            if (p.contains(x, y)) {
                return p;
            }
        }
        if (pillView.contains(x, y)) {
            return pillView;
        }
        return logShown && pillCopy.contains(x, y) ? pillCopy : null;
    }

    public boolean press(final float x, final float y) {
        if (!arrived) {
            return true;
        }
        pressed = pillAt(x, y);
        if (pressed != null) {
            pressed.pressed = true;
        }
        return true;
    }

    public boolean release(final float x, final float y) {
        if (pressed != null) {
            pressed.pressed = false;
            pressed = null;
        }
        return true;
    }

    public boolean tap(final float x, final float y, final int count) {
        if (!arrived || content() < 0.5f) {
            return true;
        }
        final Pill p = pillAt(x, y);
        if (p == pillView) {
            ArenaCurtain.fadeOver(ArenaMotion.CLOSE_MS);
            host.hideView();
            return true;
        }
        if (p == pillCopy) {
            Forge.getClipboard().setContents(log.getText());
            return true;
        }
        if (p != null) {
            press(p);
            return true;
        }
        if (logButton.contains(x, y)) {
            toggleLog();
            return true;
        }
        if (logShown && !logPanel.contains(x, y)) {
            toggleLog(); // a click away from the log puts it away
        }
        return true;
    }

    /** Arrow keys come as flings: left and right move the keyboard's choice along the row. */
    public boolean fling(final float velocityX, final float velocityY) {
        if (arrived && Math.abs(velocityX) > Math.abs(velocityY)) {
            moveFocus(velocityX > 0 ? -1 : 1);
        }
        return true;
    }

    public boolean keyDown(final int keyCode) {
        if (!arrived) {
            return keyCode != Keys.ESCAPE && keyCode != Keys.BACK; // nothing yet; Esc still has its usual meaning
        }
        switch (keyCode) {
            case Keys.ENTER:
            case Keys.NUMPAD_ENTER:
            case Keys.SPACE:
            case Keys.BUTTON_A: {
                final Pill p = focusedPill();
                if (p != null) {
                    press(p);
                }
                return true;
            }
            case Keys.ESCAPE:
            case Keys.BACK:
            case Keys.BUTTON_B:
                if (Forge.endKeyInput()) {
                    return true;
                }
                if (logShown) {
                    toggleLog();
                    return true;
                }
                for (final Pill p : row) {
                    if (p.button == btnQuit && p.visible) {
                        press(p);
                        return true;
                    }
                }
                return true;
            case Keys.LEFT:
                moveFocus(-1);
                return true;
            case Keys.RIGHT:
            case Keys.TAB:
                moveFocus(1);
                return true;
            case Keys.L:
                toggleLog();
                return true;
            case Keys.V:
                ArenaCurtain.fadeOver(ArenaMotion.CLOSE_MS);
                host.hideView();
                return true;
            default:
                return false;
        }
    }

    private void moveFocus(final int dir) {
        keysUsed = true;
        final Pill now = focusedPill();
        int i = 0;
        for (int k = 0; k < row.length; k++) {
            if (row[k] == now) {
                i = k;
            }
        }
        for (int step = 0; step < row.length; step++) {
            i = (i + dir + row.length) % row.length;
            if (row[i].visible) {
                focus = i;
                break;
            }
        }
        ArenaMotion.requestFrame();
    }

    /**
     * Presses one of the screen's buttons. Whatever it leads to (the next game, a new match, the menus) comes up under
     * a picture of this screen, which fades once the next screen is up (or after three seconds whatever happens).
     */
    private void press(final Pill p) {
        if (p.button == null || !p.button.isEnabled()) {
            return;
        }
        final FScreen from = Forge.getCurrentScreen();
        final FOverlay before = FOverlay.getTopOverlay();
        if (ArenaCurtain.cover()) {
            final boolean[] released = {false};
            final int[] after = {0};
            ArenaMotion.play(ArenaMotion.HOLD_MOST_MS, ArenaMotion.LINEAR, t -> {
                if (released[0]) {
                    return;
                }
                // a question the button raised must not wait under the picture
                final FOverlay top = FOverlay.getTopOverlay();
                final boolean asked = top != null && top != before && top.isVisible();
                if (asked || Forge.getCurrentScreen() != from && ++after[0] >= 4) {
                    released[0] = true;
                    ArenaCurtain.release(asked ? ArenaMotion.CLOSE_MS : ArenaMotion.SCREEN_FADE_LONG_MS);
                }
            }, () -> {
                if (!released[0]) {
                    ArenaCurtain.release(ArenaMotion.SCREEN_FADE_MS);
                }
            });
        }
        p.button.trigger();
    }

    private void toggleLog() {
        ArenaCurtain.fadeOver(220); // the log comes and goes through a quick fade
        setLogShown(!logShown);
    }

    private void setLogShown(final boolean shown) {
        logShown = shown;
        log.setVisible(shown);
        if (shown) {
            log.setScrollTop(0);
        }
        pillCopy.setHover(false);
    }

    // ---- the pills ----------------------------------------------------------------------------------------------------

    private static FSkinFont pillFont(final float h) {
        return ArenaTheme.fontPx(Math.round(Math.min(h * 0.42f, 40f)));
    }

    /** One of the screen's buttons as an Arena pill, mirroring a button of the view (or standing alone). */
    private static final class Pill {
        private static final Color FOCUS = new Color();
        final Rectangle bounds = new Rectangle();
        FButton button;
        String text = "";
        boolean primary, visible = true, hover, pressed;
        private final ArenaMotion.Fade lit = new ArenaMotion.Fade(0f);
        private FSkinFont font;
        private String fontFor;
        private float fontH;

        boolean contains(final float x, final float y) {
            return visible && bounds.width > 0 && bounds.contains(x, y);
        }

        void setHover(final boolean b) {
            if (b != hover) {
                hover = b;
                lit.to(b ? 1f : 0f, b ? 110 : 180);
            }
        }

        void draw(final Graphics g, final float rise, final boolean focused) {
            if (!visible || bounds.width <= 0) {
                return;
            }
            if (font == null || fontH != bounds.height || !text.equals(fontFor)) {
                // the largest of a few sizes at which the words fit
                fontFor = text;
                fontH = bounds.height;
                int px = Math.round(Math.min(bounds.height * 0.42f, 40f));
                font = ArenaTheme.fontPx(px);
                while (px > 10 && ArenaArt.textWidth(text, font) > bounds.width - bounds.height * 0.8f) {
                    px -= 2;
                    font = ArenaTheme.fontPx(px);
                }
            }
            if (focused) {
                // the keyboard's choice glows, so it can be found at a glance
                final float pad = ArenaArt.buttonPad(bounds.height);
                g.drawGlow(ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, 0.75f, FOCUS), bounds.x + pad, bounds.y + rise + pad, bounds.width - 2 * pad,
                        bounds.height - 2 * pad, (bounds.height - 2 * pad) / 2f, bounds.height * 0.32f);
            }
            ArenaArt.button(g, text, font, bounds.x, bounds.y + rise, bounds.width, bounds.height, primary, lit.get() > 0.5f, pressed, focused,
                    false, button == null || button.isEnabled());
        }
    }
}
