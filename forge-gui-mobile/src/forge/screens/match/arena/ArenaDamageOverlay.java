package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.card.MagicColor;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.player.PlayerView;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;
import forge.util.CardTranslation;

/**
 * Splitting an amount among targets, in the Arena dialog language: combat damage among an attacker's blockers (and
 * what tramples over to the player or planeswalker), and an effect's damage, counters or mana among its targets. The
 * source card stands on the right; each target is a card (or a portrait for a player, a symbol for a colour) with its
 * share in a gold plate under it between two round buttons. A click on a target or its + adds one, its - or the wheel
 * down takes one back; the arrow keys walk the targets and Up/Down add and take. The rules of the classic
 * VAssignCombatDamage / VAssignGenericAmount dialogs are kept exactly: lethal damage to each blocker in order before
 * the next (unless the order may be ignored), Auto, Reset and Skip as they offer them.
 */
public final class ArenaDamageOverlay extends ArenaDialogOverlay {
    private static final float CARD_H = 0.34f;
    private static final float ROW_CY = 0.43f;

    private static final Color PLATE_TOP = ArenaTheme.rgb(0x2A2C33), PLATE_BOTTOM = ArenaTheme.rgb(0x0C0D11);
    private static final Color LETHAL = ArenaTheme.rgb(0xFF6A4D), PORTRAIT_BACK = ArenaTheme.rgb(0x101622);
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color();
    private static final Rectangle SOURCE = new Rectangle();

    /** What the shares go to. */
    private final class Target {
        final CardView card;      // a card target, or null for the player or planeswalker attacked (combat) / a non-card target
        final Object entity;      // generic: what the share is keyed by in the answer
        final int max;            // generic: the most it may take
        final String name;
        final FImage image;       // a player's portrait or a colour's symbol
        int amount;
        boolean hovered;
        final ArenaMotion.Fade hover = new ArenaMotion.Fade(0f);
        final ArenaMotion.Fade bump = new ArenaMotion.Fade(0f);
        float cx;
        final Rectangle minus = new Rectangle(), plus = new Rectangle();

        Target(final CardView card, final Object entity, final int max, final String name, final FImage image) {
            this.card = card;
            this.entity = entity;
            this.max = max;
            this.name = name;
            this.image = image;
        }
    }

    private final boolean combat;
    private final CardView source;
    private final int total;
    private final List<Target> targets = new ArrayList<>();
    private final ArenaDialogKit.Pill btnDone = new ArenaDialogKit.Pill(), btnAuto = new ArenaDialogKit.Pill(),
            btnReset = new ArenaDialogKit.Pill(), btnSkip = new ArenaDialogKit.Pill();
    private Runnable whenClosed;
    private boolean finished;
    private Target focus;
    private float cw, ch, rowCy;
    private String title, detail, line;

    // combat
    private GameEntityView defender;
    private boolean deathtouch, divide, trample, infect, overrideOrder, skip;
    // generic
    private boolean atLeastOne;
    private String amountLabel;

    // ---- opening ----------------------------------------------------------------------------------------------------

    /**
     * Combat damage of {@code attacker} among {@code blockers} (and over them to {@code defender} with trample): tells
     * {@code done} the shares, or null if the player chose Skip. Render thread. False if the Arena look is not on the
     * match screen (then nothing opened).
     */
    public static boolean assignCombatDamage(final CardView attacker, final List<CardView> blockers, final int damage,
            final GameEntityView defender, final boolean overrideOrder, final boolean maySkip, final Consumer<Map<CardView, Integer>> done) {
        if (!ArenaTheme.isMatchBoard() || attacker == null || blockers == null || blockers.isEmpty()) {
            return false;
        }
        final ArenaDamageOverlay o = new ArenaDamageOverlay(true, attacker, damage);
        o.defender = defender;
        o.deathtouch = attacker.getCurrentState().hasDeathtouch();
        o.divide = attacker.getCurrentState().hasDivideDamage();
        o.infect = attacker.getCurrentState().hasInfect();
        o.trample = defender != null && attacker.getCurrentState().hasTrample();
        o.overrideOrder = overrideOrder;
        for (final CardView blocker : blockers) {
            o.targets.add(o.new Target(blocker, blocker, Integer.MAX_VALUE, cardName(blocker), null));
        }
        if (o.trample || (o.divide && overrideOrder)) {
            // what tramples over goes to what was attacked
            if (defender instanceof CardView) {
                o.targets.add(o.new Target((CardView) defender, null, Integer.MAX_VALUE, cardName((CardView) defender), null));
            } else if (defender instanceof PlayerView) {
                final PlayerView p = (PlayerView) defender;
                o.targets.add(o.new Target(null, null, Integer.MAX_VALUE, p.getName(), MatchController.getPlayerAvatar(p)));
            } else {
                o.targets.add(o.new Target(null, null, Integer.MAX_VALUE, String.valueOf(defender), null));
            }
        }
        o.title = Forge.getLocalizer().getMessageorUseDefault("lblArenaAssignDamage", "Assign Damage");
        o.detail = Forge.getLocalizer().getMessage("lbLAssignDamageDealtBy").replace("%s", cardName(attacker));
        o.btnAuto.text = Forge.getLocalizer().getMessage("lblAuto");
        o.btnAuto.action = () -> {
            o.resetAssigned();
            o.initialAssignDamage(true);
            o.finish();
        };
        o.btnSkip.visible = maySkip;
        o.btnSkip.text = Forge.getLocalizer().getMessage("lblSkip");
        o.btnSkip.action = () -> {
            o.skip = true;
            o.finish();
        };
        o.btnReset.action = () -> {
            o.resetAssigned();
            o.initialAssignDamage(false);
        };
        o.whenClosed = () -> {
            if (o.skip) {
                done.accept(null);
                return;
            }
            final Map<CardView, Integer> result = new HashMap<>();
            for (final Target t : o.targets) {
                result.put(t.entity == null ? null : (CardView) t.entity, t.amount);
            }
            done.accept(result);
        };
        o.initialAssignDamage(false);
        o.openOverlay();
        return true;
    }

    /**
     * {@code amount} of {@code amountLabel} from {@code effectSource} among {@code targets} (each with the most it may
     * take): tells {@code done} the shares. Render thread. False if the Arena look is not on the match screen.
     */
    public static boolean assignGenericAmount(final CardView effectSource, final Map<Object, Integer> targets, final int amount,
            final boolean atLeastOne, final String amountLabel, final Consumer<Map<Object, Integer>> done) {
        if (!ArenaTheme.isMatchBoard() || targets == null || targets.isEmpty()) {
            return false;
        }
        final ArenaDamageOverlay o = new ArenaDamageOverlay(false, effectSource, amount);
        o.atLeastOne = atLeastOne;
        o.amountLabel = amountLabel;
        for (final Map.Entry<Object, Integer> e : targets.entrySet()) {
            final Object entity = e.getKey();
            final int max = e.getValue() == null ? Integer.MAX_VALUE : e.getValue();
            if (entity instanceof CardView) {
                o.targets.add(o.new Target((CardView) entity, entity, max, cardName((CardView) entity), null));
            } else if (entity instanceof PlayerView) {
                final PlayerView p = (PlayerView) entity;
                o.targets.add(o.new Target(null, entity, max, p.getName(), MatchController.getPlayerAvatar(p)));
            } else if (entity instanceof MagicColor.Color) {
                final MagicColor.Color c = (MagicColor.Color) entity;
                o.targets.add(o.new Target(null, entity, max, "", Forge.getAssets().manaImages().get(c.getShortName())));
            } else {
                o.targets.add(o.new Target(null, entity, max, String.valueOf(entity), null));
            }
        }
        o.title = Forge.getLocalizer().getMessageorUseDefault("lblArenaAssignAmount", "Assign {0}", capitalized(amountLabel));
        o.detail = Forge.getLocalizer().getMessage("lbLAssignAmountForEffect", amountLabel, effectSource == null ? "" : cardName(effectSource));
        o.btnAuto.visible = false;
        o.btnSkip.visible = false;
        o.btnReset.action = () -> {
            o.resetAssigned();
            o.initialAssignAmount();
        };
        o.whenClosed = () -> {
            final Map<Object, Integer> result = new HashMap<>(o.targets.size());
            for (final Target t : o.targets) {
                result.put(t.entity, t.amount);
            }
            done.accept(result);
        };
        o.initialAssignAmount();
        o.openOverlay();
        return true;
    }

    private static String capitalized(final String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String cardName(final CardView card) {
        return card == null || card.getCurrentState() == null ? "" : CardTranslation.getTranslatedName(card.getCurrentState().getName());
    }

    private ArenaDamageOverlay(final boolean combat, final CardView source, final int total) {
        this.combat = combat;
        this.source = source;
        this.total = total;
        setOpenMillis(320);
        btnDone.primary = true;
        btnDone.text = Forge.getLocalizer().getMessage("lblOK");
        btnDone.action = this::finish;
        btnReset.text = Forge.getLocalizer().getMessage("lblReset");
        addPill(btnDone);
        addPill(btnAuto);
        addPill(btnReset);
        addPill(btnSkip);
    }

    @Override
    protected void onClosed() {
        if (whenClosed != null) {
            final Runnable r = whenClosed;
            whenClosed = null;
            r.run();
        }
    }

    // ---- the rules (as VAssignCombatDamage / VAssignGenericAmount) -------------------------------------------------

    private int remaining() {
        int spent = 0;
        for (final Target t : targets) {
            spent += t.amount;
        }
        return total - spent;
    }

    private void resetAssigned() {
        for (final Target t : targets) {
            t.amount = 0;
        }
        btnAuto.enabled = true;
        changed();
    }

    /** What it takes to destroy this target (or to finish a player or planeswalker). */
    private int damageToKill(final Target t) {
        if (!combat) {
            return t.max;
        }
        final CardView card = t.entity == null ? null : (CardView) t.entity;
        int lethal = 0;
        if (card == null) {
            if (defender instanceof PlayerView) {
                final PlayerView p = (PlayerView) defender;
                lethal = infect ? MatchController.instance.getGameView().getPoisonCountersToLose() - p.getCounters(CounterEnumType.POISON) : p.getLife();
            } else if (defender instanceof CardView) {
                final CardView pw = (CardView) defender;
                lethal = parse(pw.getCurrentState().isPlaneswalker() ? pw.getCurrentState().getLoyalty() : pw.getCurrentState().getDefense());
            }
        } else {
            lethal = Math.max(0, card.getLethalDamage());
            if (card.getCurrentState().getType().isPlaneswalker()) {
                lethal = parse(card.getCurrentState().getLoyalty());
            } else if (card.getCurrentState().getType().isBattle()) {
                lethal = parse(card.getCurrentState().getDefense());
            } else if (deathtouch) {
                lethal = Math.min(lethal, 1);
            }
        }
        return lethal;
    }

    private static int parse(final String s) {
        try {
            return Integer.parseInt(s);
        } catch (final RuntimeException e) {
            return 0;
        }
    }

    private boolean canAssignTo(final Target target) {
        for (final Target t : targets) {
            if (t == target) {
                return true;
            }
            if (damageToKill(t) > t.amount) {
                return false;
            }
        }
        return false;
    }

    /** One more (or one fewer) to {@code t}, as the rules allow. */
    private void change(final Target t, final boolean adding) {
        if (combat) {
            assignDamageTo(t, adding);
        } else {
            assignAmountTo(t, adding);
        }
    }

    private void assignDamageTo(final Target t, final boolean isAdding) {
        final boolean toDefender = t.entity == null;
        // trying to assign to what was attacked follows the normal rules; overriding the order skips the active blocker
        if (!divide && (toDefender || !overrideOrder) && isAdding && !canAssignTo(t)) {
            return;
        }
        final int lethal = damageToKill(t);
        final int had = t.amount;
        int add = isAdding ? 1 : -1;
        final int left = remaining();
        if (add > left) {
            add = left;
        }
        // the first blocker may not take less than lethal damage unless the order is overridden
        final boolean first = targets.get(0) == t;
        if (!overrideOrder && first && add + had < lethal) {
            return;
        }
        if (add == 0 || add + had < 0) {
            return;
        }
        addAmount(t, add);
        btnAuto.enabled = allUnassigned();
        checkDamageQueue();
        bumped(t);
        changed();
    }

    private void checkDamageQueue() {
        if (overrideOrder && divide) {
            return;
        }
        // damage that may not go further down the order yet comes off again
        boolean aliveEnemy = false;
        for (final Target t : targets) {
            final int lethal = damageToKill(t);
            if (aliveEnemy && (!overrideOrder || t.entity == null)) {
                t.amount = 0;
            } else {
                aliveEnemy |= t.amount < lethal;
            }
        }
    }

    /** Lethal damage to each in order, the rest (with Auto) to the last. */
    private void initialAssignDamage(final boolean toAll) {
        if (!toAll && overrideOrder) {
            changed();
            return;
        }
        int left = total;
        Target last = null;
        for (final Target t : targets) {
            final int dmg = Math.min(damageToKill(t), left);
            addAmount(t, dmg);
            left -= dmg;
            last = t;
            if (left <= 0 || !toAll) {
                break;
            }
        }
        if (toAll && left > 0 && last != null) {
            addAmount(last, left);
        }
        changed();
    }

    private boolean allUnassigned() {
        for (final Target t : targets) {
            if (t.amount > 0) {
                return false;
            }
        }
        return true;
    }

    private void addAmount(final Target t, int added) {
        final int can = remaining();
        if (can < added) {
            added = can;
        }
        t.amount = Math.max(0, added + t.amount);
    }

    private void assignAmountTo(final Target t, final boolean isAdding) {
        final int assigned = t.amount;
        final int leftFor = Math.max(0, t.max - assigned);
        int add = isAdding ? 1 : -1;
        final int room = Math.min(remaining(), leftFor);
        if (add > room) {
            add = room;
        }
        if (atLeastOne && assigned + add < 1) {
            add = 1 - assigned;
        }
        if (add == 0 || add + assigned < 0) {
            return;
        }
        addAmount(t, add);
        bumped(t);
        changed();
    }

    /** Everything that still fits into {@code t} at once (a double click on it). */
    private void fill(final Target t) {
        if (combat) {
            int guard = total + 1;
            while (remaining() > 0 && guard-- > 0) {
                final int before = t.amount;
                assignDamageTo(t, true);
                if (t.amount == before) {
                    break;
                }
            }
            return;
        }
        final int add = Math.min(remaining(), Math.max(0, t.max - t.amount));
        if (add > 0) {
            addAmount(t, add);
            bumped(t);
            changed();
        }
    }

    private void initialAssignAmount() {
        if (atLeastOne) {
            for (final Target t : targets) {
                addAmount(t, 1);
            }
        }
        changed();
    }

    private void finish() {
        if (finished || (!skip && remaining() > 0)) {
            return;
        }
        finished = true;
        close();
    }

    private void bumped(final Target t) {
        t.bump.set(1f);
        t.bump.to(0f, 260);
    }

    /** The answer may now be given (or not): the line under the title and the buttons follow. */
    private void changed() {
        final int left = remaining();
        btnDone.enabled = left == 0;
        if (combat) {
            boolean allLethal = true;
            for (final Target t : targets) {
                allLethal &= t.amount >= damageToKill(t);
            }
            line = detail + "  ·  " + Forge.getLocalizer().getMessage("lblAvailableDamagePoints") + ": " + left + " / " + total
                    + (allLethal && left > 0 ? "\n" + Forge.getLocalizer().getMessage("lblAssignRemainingText") : "");
        } else {
            line = detail + "  ·  " + Forge.getLocalizer().getMessage("lblAvailableAmount", amountLabel) + ": " + left + " / " + total;
        }
        if (getWidth() > 0) {
            layoutDialog(getWidth(), getHeight());
        }
        ArenaMotion.requestFrame();
    }

    // ---- layout -----------------------------------------------------------------------------------------------------

    @Override
    protected void layoutDialog(final float w, final float h) {
        heading.set(title, line, Forge.getLocalizer().getMessage("lblArenaMakeChoice"));
        ArenaDialogKit.placeButtons(w, h, btnDone, btnAuto, btnSkip, btnReset);
        final boolean hasSource = source != null;
        final int n = targets.size();
        ch = Math.round(h * CARD_H);
        cw = Math.round(ch / FCardPanel.ASPECT_RATIO);
        final float room = ArenaDialogKit.rowRoom(w, h, hasSource);
        float gap = Math.round(w * 0.025f);
        float step = cw + gap;
        if (n > 1 && n * cw + (n - 1) * gap > room) {
            step = Math.max(cw * 0.55f, (room - cw) / (n - 1));
            if (step < cw + gap) {
                // narrower cards rather than cards that cover their neighbours' buttons
                final float fit = (room - (n - 1) * gap) / n;
                if (fit > cw * 0.6f) {
                    cw = Math.round(fit);
                    ch = Math.round(cw * FCardPanel.ASPECT_RATIO);
                    step = cw + gap;
                }
            }
        }
        final float rowW = cw + Math.max(0, n - 1) * step;
        final float left = ArenaDialogKit.rowCentre(w, h, rowW, hasSource) - rowW / 2f;
        rowCy = Math.round(h * ROW_CY);
        final float d = Math.round(h * 0.045f);
        final float plateY = rowCy + ch / 2f + Math.round(h * 0.05f);
        for (int i = 0; i < n; i++) {
            final Target t = targets.get(i);
            t.cx = left + cw / 2f + i * step;
            final float off = Math.max(cw * 0.36f, Math.round(h * 0.055f));
            t.minus.set(t.cx - off - d / 2f, plateY - d / 2f, d, d);
            t.plus.set(t.cx + off - d / 2f, plateY - d / 2f, d, d);
        }
    }

    // ---- drawing ----------------------------------------------------------------------------------------------------

    @Override
    protected void drawScene(final Graphics g, final float w, final float h, final float alpha) {
        heading.draw(g, w, h, alpha);
        if (source != null) {
            ArenaDialogKit.sourceBounds(w, h, SOURCE);
            ArenaDialogKit.source(g, source, SOURCE.x, SOURCE.y, SOURCE.width, SOURCE.height, true, alpha);
        }
    }

    @Override
    protected void drawContent(final Graphics g, final float w, final float h, final float alpha) {
        final int n = targets.size();
        for (int i = 0; i < n; i++) {
            final Target t = targets.get(i);
            final float dealt = entrance.isOver() ? 1f : entrance.inTurn(i, n);
            final float a = alpha * dealt;
            if (a <= 0f) {
                continue;
            }
            final float lift = h * 0.02f * t.hover.get();
            final float cy = rowCy - lift + (1f - dealt) * h * 0.05f;
            final boolean full = t.amount > 0 && t.amount >= damageToKill(t);
            final int kind = t.amount > 0 ? ArenaDialogKit.CHOSEN : ArenaDialogKit.SELECTABLE;
            if (t.card != null) {
                ArenaDialogKit.card(g, t.card, t.cx, cy, cw, ch, 0f, 1f + 0.03f * t.hover.get(), kind, t.hovered ? 1.3f : 1f, false, h, a);
            } else {
                drawPortrait(g, t, t.cx, cy, kind, h, a);
            }
            drawPlate(g, t, full, h, a);
            if (focus == t) {
                final float o = Math.max(4f, Math.round(h * 0.012f));
                g.drawSmoothRoundRect(Math.max(1.5f, ArenaTheme.px(2)), ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, 0.8f * a, S1), t.cx - cw / 2f - o,
                        cy - ch / 2f - o, cw + 2 * o, ch + 2 * o, ArenaDialogKit.cardRadius(cw) + o);
            }
        }
    }

    /** A player, a colour or anything else that is not a card: a card-shaped dark tile with its picture and name. */
    private void drawPortrait(final Graphics g, final Target t, final float cx, final float cy, final int kind, final float h, final float a) {
        final float x = cx - cw / 2f, y = cy - ch / 2f, r = ArenaDialogKit.cardRadius(cw);
        ArenaDialogKit.cardShadow(g, x, y, cw, ch, r, a);
        ArenaDialogKit.cardLight(g, x, y, cw, ch, r, kind, t.hovered ? 1.3f : 1f, false, h, a);
        g.fillSmoothRoundRectGradient(ArenaTheme.alpha(ArenaTheme.NAVY_LIGHT, a, S1), ArenaTheme.alpha(PORTRAIT_BACK, a, S2), true, x, y, cw, ch, r);
        ArenaArt.goldFrame(g, x + 2, y + 2, cw - 4, ch - 4, Math.max(2f, r - 2), Math.max(2f, ArenaTheme.px(3)));
        final float d = cw * 0.7f;
        if (t.image != null) {
            final float old = g.getfloatAlphaComposite();
            if (a < 1f) {
                g.setAlphaComposite(old * a);
            }
            if (t.entity instanceof MagicColor.Color) {
                g.drawImage(t.image, cx - d / 2f, cy - d / 2f, d, d);
            } else {
                g.drawImage(t.image, cx - d / 2f, y + ch * 0.12f, d, d);
            }
            if (a < 1f) {
                g.setAlphaComposite(old);
            }
            if (!(t.entity instanceof MagicColor.Color)) {
                ArenaArt.portraitRing(g, cx, y + ch * 0.12f + d / 2f, d, ArenaTheme.GOLD, 0f);
            }
        }
        if (t.name != null && !t.name.isEmpty()) {
            final FSkinFont f = ArenaDialogKit.fitFont(t.name, h * 0.026f, h * 0.016f, cw * 0.86f);
            final float ty = t.image != null && !(t.entity instanceof MagicColor.Color) ? y + ch * 0.12f + d + ch * 0.04f : cy - f.getLineHeight() / 2f;
            ArenaDialogKit.edged(g, t.name, f, ArenaTheme.alpha(ArenaTheme.WHITE, a, S3), x, ty, cw, f.getLineHeight(), Align.center);
        }
        ArenaDialogKit.cardRimAfter(g, x, y, cw, ch, r, kind, true, false, h, a);
    }

    /** The share in a dark plate with a gold rim between its two round buttons; red once it is lethal. */
    private void drawPlate(final Graphics g, final Target t, final boolean full, final float h, final float a) {
        final float pw = Math.round(h * 0.075f), ph = Math.round(h * 0.052f);
        final float cy = t.minus.y + t.minus.height / 2f;
        final float bump = t.bump.get();
        final float s = 1f + 0.18f * bump;
        final float x = t.cx - pw * s / 2f, y = cy - ph * s / 2f;
        g.fillSmoothRoundRectGradient(ArenaTheme.alpha(PLATE_TOP, a, S1), ArenaTheme.alpha(PLATE_BOTTOM, a, S2), true, x, y, pw * s, ph * s, ph * s / 2f);
        g.drawSmoothRoundRect(Math.max(1.5f, ArenaTheme.px(2)), ArenaTheme.alpha(full ? LETHAL : ArenaTheme.GOLD_BRIGHT, a, S1), x, y, pw * s, ph * s, ph * s / 2f);
        final FSkinFont f = ArenaDialogKit.font(ph * 0.72f);
        ArenaArt.textBold(g, String.valueOf(t.amount), f, ArenaTheme.alpha(ArenaTheme.WHITE, a, S3), x, y, pw * s, ph * s, Align.center);
        // what the share means: lethal for a blocker, the most it may take for a target
        final String note = combat ? (full ? Forge.getLocalizer().getMessage("lblLethal") : null)
                : (t.max != Integer.MAX_VALUE && t.amount >= t.max ? Forge.getLocalizer().getMessage("lblMax") : null);
        if (note != null) {
            final FSkinFont nf = ArenaDialogKit.font(h * 0.02f);
            ArenaDialogKit.edged(g, note, nf, ArenaTheme.alpha(full && combat ? LETHAL : ArenaTheme.GOLD_BRIGHT, a, S3), t.cx - cw, cy + ph / 2f + h * 0.006f,
                    2 * cw, nf.getLineHeight(), Align.center);
        }
        final boolean canTake = t.amount > (atLeastOne ? 1 : 0), canAdd = remaining() > 0 && t.amount < t.max;
        ArenaDialogKit.roundButton(g, t.minus.x + t.minus.width / 2f, cy, t.minus.width, -1, 0, hoverMinus == t, !canTake, a);
        ArenaDialogKit.roundButton(g, t.plus.x + t.plus.width / 2f, cy, t.plus.width, 1, 0, hoverPlus == t, !canAdd, a);
    }

    // ---- input ------------------------------------------------------------------------------------------------------

    private Target hoverMinus, hoverPlus;

    private Target targetAt(final float x, final float y) {
        for (final Target t : targets) {
            if (Math.abs(x - t.cx) <= cw / 2f && Math.abs(y - rowCy) <= ch / 2f + getHeight() * 0.02f) {
                return t;
            }
        }
        return null;
    }

    @Override
    protected void onPointer(final float x, final float y, final boolean onButton) {
        final Target over = onButton ? null : targetAt(x, y);
        for (final Target t : targets) {
            final boolean on = t == over;
            if (t.hovered != on) {
                t.hovered = on;
                t.hover.to(on ? 1f : 0f, on ? 110 : 170);
            }
        }
        hoverMinus = hoverPlus = null;
        if (!onButton) {
            for (final Target t : targets) {
                if (t.minus.contains(x, y)) {
                    hoverMinus = t;
                } else if (t.plus.contains(x, y)) {
                    hoverPlus = t;
                }
            }
        }
        if (over != null) {
            focus = null;
        }
        ArenaMotion.requestFrame();
    }

    @Override
    protected void onTap(final float x, final float y, final int count) {
        for (final Target t : targets) {
            if (t.minus.contains(x, y)) {
                change(t, false);
                return;
            }
            if (t.plus.contains(x, y)) {
                change(t, true);
                return;
            }
        }
        final Target t = targetAt(x, y);
        if (t != null) {
            if (count > 1) {
                fill(t);
            } else {
                change(t, true);
            }
        }
    }

    @Override
    protected void onRightClick(final float x, final float y) {
        // over a target that is not a card: take one back
        final Target t = targetAt(x, y);
        if (t != null) {
            change(t, false);
        }
    }

    @Override
    protected void onScroll(final float x, final float y, final float dx, final float dy) {
        Target t = targetAt(x, y);
        if (t == null) {
            for (final Target u : targets) {
                if (Math.abs(x - u.cx) <= cw / 2f + u.plus.width) {
                    t = u;
                }
            }
        }
        if (t != null && dy != 0) {
            change(t, dy > 0); // the wheel up adds
        }
    }

    @Override
    protected void onArrow(final int dx, final int dy) {
        if (targets.isEmpty()) {
            return;
        }
        if (dx != 0 || focus == null) {
            int at = focus == null ? (dx < 0 ? targets.size() - 1 : 0) : targets.indexOf(focus) + dx;
            at = Math.max(0, Math.min(targets.size() - 1, at));
            focus = targets.get(at);
            ArenaMotion.requestFrame();
            if (dx != 0 || dy == 0) {
                return;
            }
        }
        change(focus, dy < 0); // Up adds, Down takes
    }

    @Override
    protected void onSubmitKey() {
        if (btnDone.enabled) {
            finish();
        }
    }

    @Override
    protected void onEscapeKey() {
        if (btnSkip.visible) {
            btnSkip.fire();
        } else {
            btnReset.fire();
        }
    }

    @Override
    protected CardView cardAt(final float x, final float y) {
        final Target t = targetAt(x, y);
        if (t != null && t.card != null) {
            return t.card;
        }
        if (source != null) {
            ArenaDialogKit.sourceBounds(getWidth(), getHeight(), SOURCE);
            if (SOURCE.contains(x, y)) {
                return source;
            }
        }
        return null;
    }
}
