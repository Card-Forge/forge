package forge.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.adventure.data.ItemData;
import forge.adventure.util.RewardActor;
import forge.assets.FImage;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.deck.ArchetypeDeckGenerator;
import forge.deck.CardThemedDeckGenerator;
import forge.deck.CommanderDeckGenerator;
import forge.deck.DeckProxy;
import forge.game.GameView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.gamemodes.planarconquest.ConquestCommander;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDialog;
import forge.toolbox.FOverlay;
import forge.util.CardRendererUtils;
import forge.util.ImageUtil;
import forge.util.TextUtil;
import forge.util.Utils;
import forge.util.collect.FCollectionView;

import static forge.assets.FSkin.getDefaultSkinFile;

public class CardZoom extends FOverlay {
    private static final float REQ_AMOUNT = Utils.AVG_FINGER_WIDTH;

    private static final CardZoom cardZoom = new CardZoom();
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static List<?> items;
    private static int currentIndex, initialIndex;
    private static CardView currentCard, prevCard, nextCard;
    private static boolean zoomMode = true;
    private static boolean oneCardView = prefs.getPrefBoolean(FPref.UI_SINGLE_CARD_ZOOM);
    private float totalZoomAmount;
    private static ActivateHandler activateHandler;
    private static String currentActivateAction;
    private static Rectangle flipIconBounds;
    private static Rectangle mutateIconBounds;
    private static boolean showAltState;
    private static boolean showBackSide = false;
    private static boolean showMerged = false;
    private static boolean isAdvBack = false;
    private static HashMap<Object, CardView> cardViewsToClearObject;
    private static final ArrayList<CardView> specializedCardsList = new ArrayList<>(8);
    private static float aspectRatioMultiplier = -1f;
    private static String lastEvaluatedString = "";
    // ---- landscape fan / animation ----
    //private static final float FAN_DEGREES = 6f;     // tilt per card step; flip the sign if the fan curls the wrong way
    //private static final float FAN_DROP = 0.04f;      // how far outer cards sink (in card heights); gives the ∩ arc
    //private static final float FAN_TILT_SIGN = -1f;   // if the cards lean inward instead of outward, change to 1f
    private static final float FAN_SPACING = 0.52f;  // first neighbour offset, in card widths
    private static final float SNAP_SPEED = 14f;     // higher = snappier settle
    private static final float ROTATE_SPEED = 12f;
    private static final int FAN_REACH = 2;
    private static final int MAX_FAN = 2 * FAN_REACH + 3;
    private static float scrollPos, targetPos, fanCardWidth = 1f;
    private static boolean dragging, panIgnored, fanActive;
    private static boolean cardRotated;
    private static float rotAnim;                    // 0..1, animated version of cardRotated
    private static final HashMap<Integer, CardView> viewCache = new HashMap<>();
    private static final int[] fanOrder = new int[MAX_FAN];
    private static final int[] hitIdx = new int[MAX_FAN];
    private static final Rectangle[] hitRects = new Rectangle[MAX_FAN];
    private static final Rectangle tmpBounds = new Rectangle();
    private static int hitCount;
    private static final Rectangle leftArrow = new Rectangle(), rightArrow = new Rectangle();
    private static boolean leftArrowVisible, rightArrowVisible;
    private static boolean showRotateBtn;
    //private static final Rectangle rotateHit = new Rectangle();
    //private static final Color BTN_ON = Color.valueOf("#3A7BD5");
    //private static final Color BTN_OFF = Color.valueOf("#303030");
    private static final int ARC_SEGS = 14;
    private static final float[] arcCos = new float[ARC_SEGS + 1], arcSin = new float[ARC_SEGS + 1];
    private static final float[] headX = new float[2], headY = new float[2];
    //private static Texture rotateTex;
    private static final boolean FLIP_IF_AFTERMATH = false;
    // --- Portrait ---
    private static final float PORTRAIT_PEEK_WIDTH = 0.80f;  // centre card width as a fraction of the screen
    private static final float PORTRAIT_PEEK_STEP = 0.62f;   // neighbour offset as a fraction of screen width (~10% sliver)
    private static float fanStep = 1f;                        // pixels between neighbouring cards, set every frame
    // --- buttons ---
    private static TextureAtlas transformAtlas;
    private static boolean transformLoadFailed;
    private static FImage imgFlip, imgMutate, imgRotate, imgRotateMirror;
    private static final float OFFSCREEN = -100000f;
    private static final int BTN_MUTATE = 0, BTN_SPECIALIZE = 1, BTN_ROTATE = 2, BTN_FLIP = 3;   // left-to-right order
    private static final int BTN_COUNT = 4;
    private static final boolean[] btnOn = new boolean[BTN_COUNT];
    private static final float[] btnAlpha = new float[BTN_COUNT];
    private static final float[] btnSlot = new float[BTN_COUNT];
    private static final Rectangle[] btnHit = new Rectangle[BTN_COUNT];

    private static FImage imgSpecialize;
    private static boolean showSpecializeBtn, showSpecialized;
    private static boolean spreadNext;                 // set right before show() to fan the new cards out of a stack
    private static float spreadT = 1f;                 // 0 = stacked on the centre card, 1 = fully fanned
    private static final float SPREAD_SECONDS = 0.45f;

    private static final float OPEN_SECONDS = 0.22f, CLOSE_SECONDS = 0.16f;
    private static float openT = 1f, closeT;
    private static boolean closing, hidePosted;
    private static final float FLIP_SECONDS = 0.34f;
    private static boolean flipping, flipToggled;
    private static float flipT;
    private static Runnable flipAction;

    static {
        for (int i = 0; i < MAX_FAN; i++)
            hitRects[i] = new Rectangle();
        final float start = 40f, sweep = 280f;
        for (int i = 0; i <= ARC_SEGS; i++) {
            double a = Math.toRadians(start + sweep * i / ARC_SEGS);
            arcCos[i] = (float) Math.cos(a);
            arcSin[i] = (float) Math.sin(a);
        }
        double ea = Math.toRadians(start + sweep);
        double travel = Math.atan2(-Math.cos(ea), -Math.sin(ea));
        for (int s = 0; s < 2; s++) {
            double ha = travel + Math.PI + (s == 0 ? -1 : 1) * Math.toRadians(35);
            headX[s] = (float) Math.cos(ha);
            headY[s] = (float) Math.sin(ha);
        }
        for (int i = 0; i < BTN_COUNT; i++) btnHit[i] = new Rectangle(OFFSCREEN, OFFSCREEN, 0, 0);
    }

    public static void show(Object item) {
        show(item, false);
    }

    public static void show(Object item, boolean showbackside) {
        List<Object> items0 = new ArrayList<>();
        items0.add(item);
        showBackSide = showbackside; //reverse the displayed zoomed card for the choice list
        show(items0, 0, null);
    }

    public static void show(FCollectionView<?> items0, int currentIndex0, ActivateHandler activateHandler0) {
        show((List<?>) items0, currentIndex0, activateHandler0);
    }

    public static void show(List<?> items0, int currentIndex0, ActivateHandler activateHandler0) {
        show(items0, currentIndex0, activateHandler0, false);
    }

    public static void show(final List<?> items0, int currentIndex0, ActivateHandler activateHandler0, boolean advBack) {
        items = items0;
        isAdvBack = advBack;
        if (items == null) { return; }
        cardViewsToClearObject = new HashMap<>();
        if (currentIndex0 < 0 || items.size() <= currentIndex0) { return; }
        activateHandler = activateHandler0;
        currentIndex = currentIndex0;
        initialIndex = currentIndex0;
        viewCache.clear();
        scrollPos = targetPos = currentIndex0;
        dragging = panIgnored = false;
        currentCard = getCardView(items.get(currentIndex));
        prevCard = currentIndex > 0 ? getCardView(items.get(currentIndex - 1)) : null;
        nextCard = currentIndex < items.size() - 1 ? getCardView(items.get(currentIndex + 1)) : null;
        onCardChanged();
        spreadT = spreadNext ? 0f : 1f;
        spreadNext = false;
        final boolean wasOpen = cardZoom.isVisible() && !closing;
        closing = false;
        if (!wasOpen) openT = 0f;                    // fresh open: play the open animation
        cardZoom.show();
    }

    public static boolean isOpen() {
        return cardZoom.isVisible();
    }

    public static void hideZoom() {
        if (isAdvBack) {
            Forge.back();
            return;
        }
        if (activateHandler != null)
            activateHandler.setSelectedIndex(currentIndex);
        cardZoom.hide();
    }

    private CardZoom() {

    }

    @Override
    public void setVisible(boolean visible0) {
        if (this.isVisible() == visible0) {
            return;
        }

        super.setVisible(visible0);

        //update selected index when hidden if current index is different than initial index
        if (!visible0 && activateHandler != null && currentIndex != initialIndex) {
            activateHandler.setSelectedIndex(currentIndex);
        }
    }

    private static void loadTransformIcons() {
        if (transformAtlas != null || transformLoadFailed) return;
        try {
            transformAtlas = new TextureAtlas(getDefaultSkinFile("sprite_transform.atlas"));
            for (Texture t : transformAtlas.getTextures())
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            imgFlip = new FTextureRegionImage(transformAtlas.findRegion("flip"));
            imgMutate = new FTextureRegionImage(transformAtlas.findRegion("mutate"));
            TextureRegion sp = transformAtlas.findRegion("specialize");
            imgSpecialize = new FTextureRegionImage(sp);
            TextureRegion rot = transformAtlas.findRegion("rotate");
            imgRotate = new FTextureRegionImage(rot);
            TextureRegion mirrored = new TextureRegion(rot);
            mirrored.flip(true, false);
            imgRotateMirror = new FTextureRegionImage(mirrored);
        } catch (Exception e) {
            transformLoadFailed = true;
        }
    }

    /** Icon size: roughly a finger wide, but never tiny or huge. */
    private static float rotateBtnSize(float messageHeight) {
        return Math.max(messageHeight * 1.2f, Math.min(Utils.AVG_FINGER_WIDTH * 0.9f, messageHeight * 2.6f));
    }

    /** Distance between button centres; the touch areas are smaller than this, so they never overlap. */
    private static float actionStep(float d) {
        return Math.max(d * 1.55f, Utils.AVG_FINGER_WIDTH * 0.95f);
    }

    private static void drawIcon(Graphics g, FImage img, float cx, float cy, float d, float alpha, boolean tint) {
        if (img == null) return;
        Batch b = g.getBatch();
        float oldColor = b.getPackedColor();
        float a = g.getfloatAlphaComposite() * alpha;
        if (tint)
            b.setColor(0.45f, 0.7f, 1f, a);        // blue tint (multiplies the image colours)
        else
            b.setColor(1f, 1f, 1f, a);
        g.drawImage(img, cx - d / 2, cy - d / 2, d, d);
        b.setPackedColor(oldColor);
    }

    private static boolean isEngaged(int i) {
        return switch (i) {
            case BTN_MUTATE -> showMerged;
            case BTN_SPECIALIZE -> showSpecialized;
            case BTN_ROTATE -> cardRotated;
            default -> showAltState || showBackSide;
        };
    }

    private static boolean updateActionRow(float dt) {
        loadTransformIcons();
        btnOn[BTN_MUTATE] = mutateIconBounds != null || showMerged;
        btnOn[BTN_SPECIALIZE] = (showSpecializeBtn || showSpecialized) && imgSpecialize != null;
        btnOn[BTN_ROTATE] = showRotateBtn;
        btnOn[BTN_FLIP] = flipIconBounds != null;

        int n = 0;
        for (boolean on : btnOn) if (on) n++;

        float f = 1f - (float) Math.exp(-16f * dt);
        boolean moving = false;
        int k = 0;
        for (int i = 0; i < BTN_COUNT; i++) {
            float targetAlpha = btnOn[i] ? 1f : 0f;
            if (btnOn[i]) {
                float tx = k++ - (n - 1) / 2f;
                if (btnAlpha[i] < 0.02f) {
                    btnSlot[i] = tx;
                } else if (Math.abs(tx - btnSlot[i]) > 0.002f) {
                    btnSlot[i] += (tx - btnSlot[i]) * f;
                    moving = true;
                } else {
                    btnSlot[i] = tx;
                }
            }
            if (Math.abs(targetAlpha - btnAlpha[i]) > 0.01f) {
                btnAlpha[i] += (targetAlpha - btnAlpha[i]) * f;
                moving = true;
            } else {
                btnAlpha[i] = targetAlpha;
            }
        }
        return moving;
    }

    private static void drawActionRow(Graphics g, float cx, float cy, float d) {
        loadTransformIcons();
        float step = actionStep(d);
        float hit = step * 0.94f;
        float settle = (fanActive ? clamp01(1f - Math.abs(scrollPos - currentIndex) * 3f) : 1f) * spreadT;

        for (int i = 0; i < BTN_COUNT; i++) {
            float a = btnAlpha[i] * settle;
            if (a < 0.03f) {
                btnHit[i].set(OFFSCREEN, OFFSCREEN, 0, 0);
                continue;
            }
            float bx = cx + btnSlot[i] * step;
            boolean engaged = isEngaged(i);
            FImage img = switch (i) {
                case BTN_MUTATE -> imgMutate;
                case BTN_SPECIALIZE -> imgSpecialize;
                case BTN_FLIP -> imgFlip;
                default -> CardRendererUtils.hasAftermath(currentCard) == FLIP_IF_AFTERMATH ? imgRotateMirror : imgRotate;
            };
            drawIcon(g, img, bx, cy, d, a * (engaged ? 0.9f : 0.6f), engaged);
            if (a >= 0.5f)
                btnHit[i].set(bx - hit / 2, cy - hit / 2, hit, hit);
            else
                btnHit[i].set(OFFSCREEN, OFFSCREEN, 0, 0);
        }
    }

    private static void openSpecialized() {
        if (currentCard == null) return;
        specializedCardsList.clear();
        final PaperCard pc = ImageUtil.getPaperCardFromImageKey(currentCard.getCurrentState().getTrackableImageKey());
        if (pc != null) {
            final CardStateName[] states = { CardStateName.SpecializeW, CardStateName.SpecializeU, CardStateName.SpecializeB,
                    CardStateName.SpecializeR, CardStateName.SpecializeG };
            for (CardStateName s : states) {
                Card c = Card.fromPaperCard(pc, null);
                c.setState(s, true);
                specializedCardsList.add(c.getView());
            }
        }
        if (!specializedCardsList.isEmpty()) {
            showSpecialized = true;
            spreadNext = true;                       // fan the five colour versions out of the card
            show(specializedCardsList, 0, null);
        }
    }

    // ---- animated card flip ----
    private static float flipScaleX() {
        return flipping ? Math.max(0.02f, (float) Math.abs(Math.cos(flipT * Math.PI))) : 1f;
    }

    private static void startFlip(Runnable toggle) {
        if (!zoomMode) {                       // text view: no animation (the font would re-wrap every frame)
            toggle.run();
            updateRotateButton();
            return;
        }
        flipping = true;
        flipToggled = false;
        flipT = 0f;
        flipAction = toggle;
        Gdx.graphics.requestRendering();
    }

    private void flipTap() {
        if (flipping || currentCard == null) return;
        if (currentCard.isFaceDown() && currentCard.getBackup() != null) {
            if (currentCard.getBackup().isDoubleFacedCard() || currentCard.getBackup().isFlipCard() || currentCard.getBackup().hasSecondaryState()) {
                show(currentCard.getBackup());
                return;
            }
        }
        startFlip(() -> {
            if (!showBackSide)
                showAltState = !showAltState;
            else
                showBackSide = !showBackSide;
        });
    }

    private void closeZoom() {
        hide();
        if (isAdvBack)
            Forge.back();
    }

    private void tapButton(int i) {
        if (btnOn[i] && btnAlpha[i] >= 0.5f)
            tap(btnHit[i].x + btnHit[i].width / 2, btnHit[i].y + btnHit[i].height / 2, 1);
    }

    private static void incrementCard(int dir) {
        if (dir > 0) {
            if (currentIndex == items.size() - 1) {
                if (isAdvBack)
                    Forge.back();
                return;
            }
            currentIndex++;

            prevCard = currentCard;
            currentCard = nextCard;
            nextCard = currentIndex < items.size() - 1 ? getCardView(items.get(currentIndex + 1)) : null;
        }
        else {
            if (currentIndex == 0) {
                if (isAdvBack)
                    Forge.back();
                return;
            }
            currentIndex--;

            nextCard = currentCard;
            currentCard = prevCard;
            prevCard = currentIndex > 0 ? getCardView(items.get(currentIndex - 1)) : null;
        }
        onCardChanged();
    }

    private static void onCardChanged() {
        mutateIconBounds = null;
        if (activateHandler != null) {
            currentActivateAction = activateHandler.getActivateAction(currentIndex);
        }
        if (MatchController.instance.mayFlip(currentCard)) {
            flipIconBounds = new Rectangle();
        } else {
            flipIconBounds = null;
        }
        if (currentCard != null) {
            if (!currentCard.getMergedCardsCollection().isEmpty())
                mutateIconBounds = new Rectangle();
        }
        showAltState = false;
        showSpecializeBtn = currentCard != null && currentCard.canSpecialize() && currentCard.getCurrentState().getState() == CardStateName.Original;
        cardRotated = false;
        rotAnim = 0f;
        updateRotateButton();
        flipping = false;
        flipT = 0f;
        flipAction = null;
    }

    private static CardView getCardView(Object item) {
        if (item instanceof Entry) {
            item = ((Entry<?, ?>) item).getKey();
        }
        if (item instanceof CardView cw) {
            return cw;
        }
        if (item instanceof DeckProxy deck) {
            if (item instanceof CardThemedDeckGenerator gen) {
                return CardView.getCardForUi(gen.getPaperCard());
            } else if (item instanceof CommanderDeckGenerator gen) {
                return CardView.getCardForUi(gen.getPaperCard());
            } else if (item instanceof ArchetypeDeckGenerator gen) {
                return CardView.getCardForUi(gen.getPaperCard());
            } else {
                return new CardView(-1, null, deck.getName(), deck.getImageKey(false));
            }

        }
        if (item instanceof IPaperCard ipc) {
            return CardView.getCardForUi(ipc);
        }
        if (item instanceof ConquestCommander cc) {
            return CardView.getCardForUi(cc.getCard());
        }
        if (item instanceof InventoryItem ii) {
            CardView cardView = cardViewsToClearObject.get(ii);
            if (cardView != null)
                return cardView;
            cardView = new CardView(-1, null, ii.getDisplayName(), ii.getItemType(), ii);
            cardViewsToClearObject.put(item, cardView);
            return cardView;
        }
        if (item instanceof RewardActor actor) {
            CardView cardView = cardViewsToClearObject.get(actor);
            if (cardView != null)
                return cardView;
            String name = "", description = "";
            switch (actor.getReward().getType()) {
                case Card -> { return CardView.getCardForUi(actor.getReward().getCard()); }
                default -> {
                    ItemData data = actor.getReward().getItem();
                    if (data != null) {
                        name = data.name;
                        description = data.getDescription();
                    } else if (actor.getReward().getDeck() != null) {
                        name = actor.getReward().getDeck().getName();
                    } else {
                        name = actor.getReward().getType().name();
                        description = "Adds " + actor.getReward().getCount() + " " + actor.getReward().getType();
                    }
                    if (data != null && description.isEmpty() && data.questItem)
                        description = "Quest Item";
                    description = TextUtil.fastReplace(description, "[+Shards]", "{M}");
                    cardView = new CardView(-1, null, name, description, actor);
                    cardViewsToClearObject.put(actor, cardView);
                    return cardView;
                }
            }
        }
        return new CardView(-1, null, item.toString(), "", null);
    }

    private static CardView viewAt(int i) {
        if (items == null || i < 0 || i >= items.size()) return null;
        CardView cv = viewCache.get(i);
        if (cv == null) {
            cv = getCardView(items.get(i));
            viewCache.put(i, cv);
        }
        return cv;
    }

    /** Cards whose picture is stored sideways and must be turned to be read. */
    private static boolean canRotate(CardView card) {
        if (card == null) return false;
        CardView.CardStateView s = card.getCurrentState();
        boolean altState = showBackSide ? showBackSide : showAltState;
        // add s.isBattle() here if your battle images are stored sideways
        if (s == null)
            return false;
        if (card.isSplitCard()) {
            return MatchController.instance.mayView(card) && !card.isFaceDown();
        } else {
            return s.isPhenomenon() || s.isPlane() || (s.isBattle() && !altState)
                || (card.getAlternateState() != null && card.getAlternateState().isBattle() && altState);
        }
    }

    private static void updateRotateButton() {
        boolean can = zoomMode && canRotate(currentCard);
        showRotateBtn = can;
        if (!can && cardRotated)
            cardRotated = false;        // card changed into something that can't rotate: swing it back upright
    }

    private static void toggleRotate() {
        if (canRotate(currentCard)) {
            cardRotated = !cardRotated;
            Gdx.graphics.requestRendering();
        }
    }

    private static boolean isSettled() {
        return !fanActive || Math.abs(scrollPos - currentIndex) < 0.02f;
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static void advanceAnimation(boolean landscape) {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        boolean moving = false;

        if (landscape) {
            if (!dragging) {
                float diff = targetPos - scrollPos;
                if (Math.abs(diff) > 0.002f) {
                    scrollPos += diff * (1f - (float) Math.exp(-SNAP_SPEED * dt));
                    moving = true;
                } else {
                    scrollPos = targetPos;
                }
            } else {
                moving = true;
            }
            int nearest = Math.max(0, Math.min(items.size() - 1, Math.round(scrollPos)));
            if (nearest != currentIndex) {
                currentIndex = nearest;
                currentCard = viewAt(nearest);
                prevCard = viewAt(nearest - 1);
                nextCard = viewAt(nearest + 1);
                onCardChanged();
            }
        }

        float rt = cardRotated ? 1f : 0f;
        if (Math.abs(rt - rotAnim) > 0.003f) {
            rotAnim += (rt - rotAnim) * (1f - (float) Math.exp(-ROTATE_SPEED * dt));
            moving = true;
        } else {
            rotAnim = rt;
        }
        if (flipping) {
            flipT += dt / FLIP_SECONDS;
            if (!flipToggled && flipT >= 0.5f) {
                flipToggled = true;
                if (flipAction != null) flipAction.run();
                updateRotateButton();
            }
            if (flipT >= 1f) {
                flipping = false;
                flipT = 0f;
                flipAction = null;
            }
            moving = true;
        }
        if (spreadT < 1f) {
            spreadT = Math.min(1f, spreadT + dt / SPREAD_SECONDS);
            moving = true;
        }
        if (closing) {
            closeT += dt / CLOSE_SECONDS;
            moving = true;
            if (closeT >= 1f && !hidePosted) {
                hidePosted = true;
                Gdx.app.postRunnable(() -> {         // never hide from inside the draw call
                    hidePosted = false;
                    if (closing) cardZoom.finishHide();
                });
            }
        } else if (openT < 1f) {
            openT = Math.min(1f, openT + dt / OPEN_SECONDS);
            moving = true;
        }
        if (updateActionRow(dt)) moving = true;
        if (moving)
            Gdx.graphics.requestRendering();
    }

    private static void drawFace(Graphics g, CardView cv, GameView gv, boolean alt, boolean current,
                                 float x, float y, float w, float h, float screenW, float screenH) {
        if (zoomMode || !current)
            CardImageRenderer.drawZoom(g, cv, gv, alt, x, y, w, h, screenW, screenH, current && !fanActive);
        else
            CardImageRenderer.drawDetails(g, cv, gv, alt, x, y, w, h);
    }

    /** Draws one card centred on (cx, cy). The current card can be rotated and scaled by rotAnim. */
    private static void drawCard(Graphics g, GameView gv, CardView cv, boolean current, float cx, float cy,
                                 float cw, float ch, float tilt, float screenW, float screenH, float maxVisH, Rectangle out) {
        boolean alt = current && (showBackSide || showAltState);
        float rot = current && zoomMode ? rotAnim : 0f;
        if (rot > 0.001f) {
            // a rotated card is ch wide and cw tall: scale it so it fills the free space
            float m = 1f + (Math.min(maxVisH / cw, screenW * 0.98f / ch) - 1f) * rot;
            float rw = cw * m, rh = ch * m;
            float mul = CardRendererUtils.hasAftermath(cv) ? 90f : -90f;
            float ang = mul * rot;
            g.startRotateTransform(cx, cy, ang);
            drawFace(g, cv, gv, alt, current, cx - rw / 2, cy - rh / 2, rw, rh, screenW, screenH);
            g.endTransform();
            out.set(cx - rh / 2, cy - rw / 2, rh, rw);
            return;
        }
        float x = cx - cw / 2, y = cy - ch / 2;
        if (Math.abs(tilt) > 0.05f) {
            g.startRotateTransform(cx, cy, tilt);          // rotate around the card's own centre
            drawFace(g, cv, gv, alt, current, x, y, cw, ch, screenW, screenH);
            g.endTransform();
        } else {
            drawFace(g, cv, gv, alt, current, x, y, cw, ch, screenW, screenH);
        }
        out.set(x, y, cw, ch);
    }

    private static void drawFan(Graphics g, GameView gv, float w, float h, float cy, float cw, float ch, float step, float maxVisH) {
        fanStep = step;
        final int first = Math.max(0, (int) Math.floor(scrollPos) - FAN_REACH);
        final int last = Math.min(items.size() - 1, (int) Math.ceil(scrollPos) + FAN_REACH);

        // order farthest -> nearest so the centre card ends up on top
        int count = 0;
        for (int i = first; i <= last; i++) {
            float di = Math.abs(i - scrollPos);
            int j = count++;
            while (j > 0 && Math.abs(fanOrder[j - 1] - scrollPos) < di) {
                fanOrder[j] = fanOrder[j - 1];
                j--;
            }
            fanOrder[j] = i;
        }

        hitCount = 0;
        final float oldAlpha = g.getfloatAlphaComposite();
        final float sp = 1f - (float) Math.pow(1f - spreadT, 3);
        for (int k = 0; k < count; k++) {
            int idx = fanOrder[k];
            CardView cv = viewAt(idx);
            if (cv == null) continue;

            float d = idx - scrollPos, ad = Math.abs(d);
            boolean current = idx == currentIndex;
            float alpha = clamp01(1f - Math.max(0f, ad - 1.5f));
            if (!current) alpha *= 1f - rotAnim;
            if (!current) alpha *= clamp01(sp * 1.5f);                           // neighbours appear as they leave the stack
            if (alpha <= 0.02f) continue;

            float scale = ((!zoomMode && current) ? 1f : 1f - 0.12f * Math.min(ad, 3f)) * (current ? 1f : 0.9f + 0.1f * sp);
            float dist = (ad <= 1f ? step * ad : step + 0.38f * cw * (ad - 1f)) * sp;
            float cx = w / 2 + Math.signum(d) * dist;
            if (Math.abs(cx - w / 2) - cw * scale / 2 > w / 2) continue;   // completely off screen: don't draw it

            g.setAlphaComposite(oldAlpha * alpha);
            drawCard(g, gv, cv, current, cx, cy, cw * scale * (current ? flipScaleX() : 1f), ch * scale, 0f, w, h, maxVisH, tmpBounds);
            hitIdx[hitCount] = idx;
            hitRects[hitCount].set(tmpBounds);
            hitCount++;
        }
        g.setAlphaComposite(oldAlpha);
    }

    private static int hitTestFan(float x, float y) {
        for (int i = hitCount - 1; i >= 0; i--) {   // nearest first
            if (hitRects[i].contains(x, y)) return hitIdx[i];
        }
        return -1;
    }

    private static void drawScrollArrows(Graphics g, float w, float cy) {
        leftArrowVisible = rightArrowVisible = false;
        if (!fanActive || !Forge.isLandscapeMode() || spreadT < 1f || items == null || items.size() < 2 || rotAnim > 0.01f) return;

        float s = Math.max(FDialog.MSG_HEIGHT * 0.45f, Utils.AVG_FINGER_WIDTH * 0.3f);   // arrow half-height
        float pad = Utils.scale(10);
        float t = Utils.scale(3);                                                          // line thickness
        float hitW = Math.max(s * 2.5f + pad, Utils.AVG_FINGER_WIDTH);
        float hitH = Math.max(s * 4, Utils.AVG_FINGER_WIDTH * 1.5f);
        float oldAlpha = g.getfloatAlphaComposite();
        g.setAlphaComposite(0.85f);

        if (scrollPos > 0.5f) {                    // there is a card to the left
            float x0 = pad;
            g.drawLine(t + 2, Color.BLACK, x0 + s * 0.5f, cy - s, x0, cy);
            g.drawLine(t + 2, Color.BLACK, x0, cy, x0 + s * 0.5f, cy + s);
            g.drawLine(t, Color.WHITE, x0 + s * 0.5f, cy - s, x0, cy);
            g.drawLine(t, Color.WHITE, x0, cy, x0 + s * 0.5f, cy + s);
            leftArrow.set(0, cy - hitH / 2, hitW, hitH);
            leftArrowVisible = true;
        }
        if (scrollPos < items.size() - 1.5f) {     // there is a card to the right
            float x1 = w - pad;
            g.drawLine(t + 2, Color.BLACK, x1 - s * 0.5f, cy - s, x1, cy);
            g.drawLine(t + 2, Color.BLACK, x1, cy, x1 - s * 0.5f, cy + s);
            g.drawLine(t, Color.WHITE, x1 - s * 0.5f, cy - s, x1, cy);
            g.drawLine(t, Color.WHITE, x1, cy, x1 - s * 0.5f, cy + s);
            rightArrow.set(w - hitW, cy - hitH / 2, hitW, hitH);
            rightArrowVisible = true;
        }
        g.setAlphaComposite(oldAlpha);
    }

    private static void resetAltState() {
        showBackSide = false;
        showAltState = false;
        updateRotateButton();
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (closing) return true;
        if (btnHit[BTN_ROTATE].contains(x, y)) {
            toggleRotate();
            return true;
        }
        if (btnHit[BTN_MUTATE].contains(x, y)) {
            if (showMerged) {
                closeZoom();
            } else if (currentCard != null && !currentCard.getMergedCardsCollection().isEmpty()) {
                showMerged = true;
                spreadNext = true;                   // fan the merged cards out of the stack
                show(currentCard.getMergedCardsCollection(), 0, null);
            }
            return true;
        }
        if (btnHit[BTN_SPECIALIZE].contains(x, y)) {
            if (showSpecialized)
                closeZoom();
            else
                openSpecialized();
            return true;
        }
        if (btnHit[BTN_FLIP].contains(x, y)) {
            flipTap();
            return true;
        }
        if (fanActive) {
            if (leftArrowVisible && leftArrow.contains(x, y)) {
                targetPos = Math.max(0, Math.round(targetPos) - 1);
                resetAltState();
                Gdx.graphics.requestRendering();
                return true;
            }
            if (rightArrowVisible && rightArrow.contains(x, y)) {
                targetPos = Math.min(items.size() - 1, Math.round(targetPos) + 1);
                resetAltState();
                Gdx.graphics.requestRendering();
                return true;
            }
            int hit = hitTestFan(x, y);            // tap a neighbouring card to bring it to the centre
            if (hit >= 0 && hit != currentIndex) {
                targetPos = hit;
                resetAltState();
                Gdx.graphics.requestRendering();
                return true;
            }
        }
        closeZoom();
        return true;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        if (closing) return true;
        if (!fanActive || items == null || panIgnored) return false;
        if (!dragging) {
            if (moreVertical) {                // vertical swipes (details / activate) stay with fling()
                panIgnored = true;
                return false;
            }
            dragging = true;
            resetAltState();
        }
        float step = Math.max(1f, fanStep);
        float delta = -deltaX / step;
        float next = scrollPos + delta;
        if (next < 0 || next > items.size() - 1) {   // rubber band at the ends
            delta *= 0.35f;
            next = scrollPos + delta;
        }
        scrollPos = Math.max(-0.5f, Math.min(items.size() - 0.5f, next));
        targetPos = scrollPos;
        return true;
    }

    @Override
    public boolean panStop(float x, float y) {
        panIgnored = false;
        if (!dragging) return false;
        dragging = false;
        targetPos = Math.max(0, Math.min(items.size() - 1, Math.round(scrollPos)));
        return true;
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        if (closing) return true;
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            if (fanActive) {
                int dir = velocityX > 0 ? -1 : 1;
                int base = dir > 0 ? (int) Math.floor(scrollPos + 0.001f) : (int) Math.ceil(scrollPos - 0.001f);
                int t = base + dir;
                if (t < 0 || t >= items.size()) {
                    if (isAdvBack) Forge.back();
                    targetPos = Math.max(0, Math.min(items.size() - 1, Math.round(scrollPos)));
                } else {
                    targetPos = t;
                }
                Gdx.graphics.requestRendering();
            } else {
                incrementCard(velocityX > 0 ? -1 : 1);
            }
            resetAltState();
            return true;
        }
        if (velocityY > 0) {
            zoomMode = !zoomMode;
            cardRotated = false;
            resetAltState();
            return true;
        }
        if (currentActivateAction != null && activateHandler != null) {
            hide();
            activateHandler.activate(currentIndex);
            return true;
        }
        return false;
    }

    private void setOneCardView(boolean oneCardView0) {
        if (oneCardView == oneCardView0 || Forge.isLandscapeMode()) {
            return;
        } //don't allow changing this when in landscape mode

        oneCardView = oneCardView0;
        prefs.setPref(FPref.UI_SINGLE_CARD_ZOOM, oneCardView0);
        prefs.save();
    }

    @Override
    public boolean zoom(float x, float y, float amount) {
        totalZoomAmount += amount;

        if (totalZoomAmount >= REQ_AMOUNT) {
            setOneCardView(true);
            totalZoomAmount = 0;
        } else if (totalZoomAmount <= -REQ_AMOUNT) {
            setOneCardView(false);
            totalZoomAmount = 0;
        }
        return true;
    }

    @Override
    public boolean longPress(float x, float y) {
        setOneCardView(!oneCardView);
        return true;
    }

    @Override
    public void drawOverlay(Graphics g) {
        final GameView gameView = MatchController.instance.getGameView();

        float w = getWidth();
        float h = getHeight();
        float messageHeight = FDialog.MSG_HEIGHT;

        if (aspectRatioMultiplier == -1f || !lastEvaluatedString.equals(Forge.extrawide)) {
            lastEvaluatedString = (Forge.extrawide != null) ? Forge.extrawide : "default";
            switch (lastEvaluatedString) {
                case "wide": aspectRatioMultiplier = 2.5f; break;
                case "extrawide": aspectRatioMultiplier = 2f; break;
                default: aspectRatioMultiplier = 3f; break;
            }
        }

        final float btnD = rotateBtnSize(messageHeight);
        final float pad = Utils.scale(6);
        final float cardCy = h / 2;
        final float btnCy = h - messageHeight - pad - btnD / 2;
        final float maxCardHeight = h - aspectRatioMultiplier * messageHeight;
        final float areaH = maxCardHeight;

        if (!fanActive) {                       // first frame after show()
            scrollPos = targetPos = currentIndex;
            fanActive = true;
        }
        advanceAnimation(true);
        final float ease = closing ? 1f - clamp01(closeT) * clamp01(closeT)
                : 1f - (float) Math.pow(1f - openT, 3);     // ease-out when opening
        final float os = 0.88f + 0.12f * ease;                                  // content scales 88% -> 100%
        final float baseAlpha = g.getfloatAlphaComposite();
        g.setAlphaComposite(baseAlpha * ease);

        // card size and spacing: landscape fan, or portrait peek / full-width pager
        float cardWidth, cardHeight, step;
        if (Forge.isLandscapeMode()) {
            cardHeight = maxCardHeight;
            cardWidth = cardHeight / FCardPanel.ASPECT_RATIO;
            step = cardWidth * FAN_SPACING;
        } else {
            cardWidth = oneCardView ? w : w * PORTRAIT_PEEK_WIDTH;
            cardHeight = FCardPanel.ASPECT_RATIO * cardWidth;
            if (cardHeight > maxCardHeight) {
                cardHeight = maxCardHeight;
                cardWidth = cardHeight / FCardPanel.ASPECT_RATIO;
            }
            step = oneCardView ? w * 1.04f : w * PORTRAIT_PEEK_STEP;
        }
        cardWidth *= os;
        cardHeight *= os;
        step *= os;
        drawFan(g, gameView, w, h, cardCy, cardWidth, cardHeight, step, areaH);

        float y = cardCy - cardHeight / 2;
        float rowCy = Math.min(btnCy, y + cardHeight + pad + btnD / 2);

        drawScrollArrows(g, w, cardCy);

        if (isAdvBack) {
            String message;
            if (items.size() > 1) {
                message = "Swipe Left/Right to navigate Zoom";
                if (currentIndex == 0)
                    message = "Swipe Right to close Zoom";
                else if (currentIndex == items.size() - 1)
                    message = "Swipe Left to close Zoom";
            } else
                message = "Swipe Left/Right to close Zoom";

            g.fillRect(FDialog.getMsgBackColor(), 0, 0, w, messageHeight);
            g.drawText(message, FDialog.MSG_FONT, FDialog.getMsgForeColor(), 0, 0, w, messageHeight, false, Align.center, true);
        } else if (currentActivateAction != null) {
            g.fillRect(FDialog.getMsgBackColor(), 0, 0, w, messageHeight);
            g.drawText(Forge.getLocalizer().getMessage("lblSwipeUpTo").replace("%s", currentActivateAction), FDialog.MSG_FONT, FDialog.getMsgForeColor(), 0, 0, w, messageHeight, false, Align.center, true);
        }
        g.fillRect(FDialog.getMsgBackColor(), 0, h - messageHeight, w, messageHeight);
        g.drawText(zoomMode ? Forge.getLocalizer().getMessage("lblSwipeDownDetailView") : Forge.getLocalizer().getMessage("lblSwipeDownPictureView"), FDialog.MSG_FONT, FDialog.getMsgForeColor(), 0, h - messageHeight, w, messageHeight, false, Align.center, true);

        drawActionRow(g, w / 2, rowCy, btnD);   // buttons last so nothing draws over them
        g.setAlphaComposite(baseAlpha);
        interrupt(false);
    }

    private void drawIconBounds(Graphics g, Rectangle iconBounds, FSkinImage skinImage, float x, float y, float cardWidth, float cardHeight) {
        float imageWidth = cardWidth / 2;
        float imageHeight = imageWidth * skinImage.getHeight() / skinImage.getWidth();
        iconBounds.set(x + (cardWidth - imageWidth) / 2, y + (cardHeight - imageHeight) / 2, imageWidth, imageHeight);
        g.drawImage(skinImage, iconBounds.x, iconBounds.y, iconBounds.width, iconBounds.height);
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    public interface ActivateHandler {
        String getActivateAction(int index);

        void setSelectedIndex(int index);

        void activate(int index);
    }

    public void interrupt(boolean resume) {
        if (MatchController.instance.hasLocalPlayers())
            return;
        if (resume && MatchController.instance.isGamePaused()) {
            MatchController.instance.resumeMatch();
            return;
        }
        if (!MatchController.instance.isGamePaused())
            MatchController.instance.pauseMatch();
    }

    @Override
    public void hide() {
        if (closing) { finishHide(); return; }
        if (isVisible() && fanActive && !isAdvBack) {
            closing = true;
            closeT = 0f;
            Gdx.graphics.requestRendering();
            return;
        }
        finishHide();
    }

    private void finishHide() {
        if (cardViewsToClearObject != null) {
            for (CardView cardView : cardViewsToClearObject.values()) {
                if (cardView != null)
                    cardView.clearObject();
            }
            cardViewsToClearObject.clear();
            cardViewsToClearObject = null;
        }
        viewCache.clear();
        dragging = panIgnored = fanActive = false;
        flipping = false;
        flipAction = null;
        closing = false;
        showBackSide = false;
        showAltState = false;
        showMerged = false;
        showSpecialized = false;
        spreadT = 1f;
        Arrays.fill(btnAlpha, 0f);
        super.hide();
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (closing) { finishHide(); return true; }
        if (isAdvBack) {
            if (keyCode == Input.Keys.ESCAPE || keyCode == Input.Keys.BACK) {
                if (Forge.endKeyInput()) { return true; }

                Forge.back();
                return true;
            }
        }
        if (Forge.hasGamepad()) {
            if (keyCode == Input.Keys.DPAD_LEFT)
                fling(300, 0);
            else if (keyCode == Input.Keys.DPAD_RIGHT)
                fling(-300, 0);
            else if (keyCode == Input.Keys.BUTTON_B)
                hideZoom();
            else if (keyCode == Input.Keys.BUTTON_A)
                fling(0, -300f);
            else if (keyCode == Input.Keys.BUTTON_X) {
                if (btnOn[BTN_MUTATE])
                    tapButton(BTN_MUTATE);
                else
                    tapButton(BTN_FLIP);
            } else if (keyCode == Input.Keys.BUTTON_Y)
                fling(0, 300f);
            return true;
        }
        return super.keyDown(keyCode);
    }
}
