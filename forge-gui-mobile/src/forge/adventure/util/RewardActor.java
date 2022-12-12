package forge.adventure.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.Graphics;
import forge.ImageKeys;
import forge.adventure.data.ItemData;
import forge.adventure.scene.Scene;
import forge.assets.FSkin;
import forge.assets.ImageCache;
import forge.card.CardImageRenderer;
import forge.card.CardRenderer;
import forge.game.card.CardView;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.util.ImageFetcher;
import forge.util.ImageUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

import static forge.adventure.util.Paths.ITEMS_ATLAS;

/**
 * Render the rewards as a card on the reward scene.
 */
public class RewardActor extends Actor implements Disposable, ImageFetcher.Callback {
    ImageToolTip tooltip;
    HoldTooltip holdTooltip;
    Reward reward;
    ShaderProgram shaderGrayscale = Forge.getGraphics().getShaderGrayscale();
    ShaderProgram shaderRoundRect = Forge.getGraphics().getShaderRoundedRect();

    final int preview_w = 488; //Width and height for generated images.
    final int preview_h = 680;

    static TextureRegion backTexture;
    Texture image, T, Talt;
    Graphics graphics;
    Texture generatedTooltip = null; //Storage for a generated tooltip. To dispose of on exit.
    boolean needsToBeDisposed;
    float flipProcess = 0;
    boolean clicked = false;
    boolean sold = false;
    boolean flipOnClick;
    private boolean hover;
    boolean loaded = true;
    boolean alternate = false, shown = false;

    public static int renderedCount = 0; //Counter for cards that require rendering a preview.
    static final ImageFetcher fetcher = GuiBase.getInterface().getImageFetcher();
    RewardImage toolTipImage;

    @Override
    public void dispose() {
        if (needsToBeDisposed) {
            needsToBeDisposed = false;
            if (!Reward.Type.Card.equals(reward.type))
                image.dispose(); //clear only generated images and let assetmanager handle the disposal of actual card texture
            if (generatedTooltip != null)
                generatedTooltip.dispose();
        }
        if (T != null)
            T.dispose();
        if (Talt != null)
            Talt.dispose();
    }

    public boolean toolTipIsVisible() {
        if (holdTooltip != null)
            return holdTooltip.tooltip_actor.getStage() != null;
        return false;
    }

    public Reward getReward() {
        return reward;
    }

    @Override
    public void onImageFetched() {
        ImageCache.clear();
        String imageKey = reward.getCard().getImageKey(false);
        PaperCard card = ImageUtil.getPaperCardFromImageKey(imageKey);
        imageKey = card.getCardImageKey();
        int count = 0;
        if (StringUtils.isBlank(imageKey))
            return;
        File imageFile = ImageKeys.getImageFile(imageKey);
        if (imageFile == null)
            return;
        if (!Forge.getAssets().manager().contains(imageFile.getPath())) {
            Forge.getAssets().manager().load(imageFile.getPath(), Texture.class, Forge.getAssets().getTextureFilter());
            Forge.getAssets().manager().finishLoadingAsset(imageFile.getPath());
            count += 1;
        }
        Texture replacement = Forge.getAssets().manager().get(imageFile.getPath(), Texture.class, false);
        if (replacement == null)
            return;
        image = replacement;
        loaded = true;
        if (toolTipImage != null) {
            if (toolTipImage.getDrawable() instanceof TextureRegionDrawable) {
                ((TextureRegionDrawable) toolTipImage.getDrawable()).getRegion().getTexture().dispose();
            }
            toolTipImage.remove();
            toolTipImage = new RewardImage(processDrawable(image));
            if (GuiBase.isAndroid() || Forge.hasGamepad()) {
                if (holdTooltip.tooltip_image.getDrawable() instanceof TextureRegionDrawable) {
                    ((TextureRegionDrawable) holdTooltip.tooltip_image.getDrawable()).getRegion().getTexture().dispose();
                }
                holdTooltip.tooltip_actor.clear();
                holdTooltip.tooltip_actor.add(toolTipImage);
            } else {
                tooltip.setActor(toolTipImage);
            }
        }
        if (T != null)
            T.dispose();
        if (alternate && Talt != null)
            Talt.dispose();
        ImageCache.updateSynqCount(imageFile, count);
        Gdx.graphics.requestRendering();
    }

    public RewardActor(Reward reward, boolean flippable) {
        this.flipOnClick = flippable;
        this.reward = reward;
        if (backTexture == null) {
            backTexture = FSkin.getSleeves().get(0);
        }
        switch (reward.type) {
            case Card: {
                if (ImageCache.imageKeyFileExists(reward.getCard().getImageKey(false))) {
                    int count = 0;
                    PaperCard card = ImageUtil.getPaperCardFromImageKey(reward.getCard().getImageKey(false));
                    File frontFace = ImageKeys.getImageFile(card.getCardImageKey());
                    if (frontFace != null) {
                        try {
                            if (!Forge.getAssets().manager().contains(frontFace.getPath())) {
                                Forge.getAssets().manager().load(frontFace.getPath(), Texture.class, Forge.getAssets().getTextureFilter());
                                Forge.getAssets().manager().finishLoadingAsset(frontFace.getPath());
                                count += 1;
                            }
                            Texture front = Forge.getAssets().manager().get(frontFace.getPath(), Texture.class, false);
                            if (front != null) {
                                setCardImage(front);
                            } else {
                                loaded = false;
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load image: " + frontFace.getPath());
                            loaded = false;
                        }
                    } else {
                        loaded = false;
                    }
                    ImageCache.updateSynqCount(frontFace, count);
                    //preload card back for performance
                    if (reward.getCard().hasBackFace() && ImageCache.imageKeyFileExists(reward.getCard().getImageKey(true))) {
                        PaperCard cardBack = ImageUtil.getPaperCardFromImageKey(reward.getCard().getImageKey(true));
                        File backFace = ImageKeys.getImageFile(cardBack.getCardAltImageKey());
                        if (backFace != null) {
                            try {
                                if (!Forge.getAssets().manager().contains(backFace.getPath())) {
                                    Forge.getAssets().manager().load(backFace.getPath(), Texture.class, Forge.getAssets().getTextureFilter());
                                    Forge.getAssets().manager().finishLoadingAsset(backFace.getPath());
                                    ImageCache.updateSynqCount(backFace, 1);
                                }
                            } catch (Exception e) {
                                System.err.println("Failed to load image: " + backFace.getPath());
                            }
                        }
                    }
                } else {
                    String imagePath = ImageUtil.getImageRelativePath(reward.getCard(), "", true, false);
                    File lookup = ImageKeys.hasSetLookup(imagePath) ? ImageKeys.setLookUpFile(imagePath, imagePath + "border") : null;
                    int count = 0;
                    if (lookup != null) {
                        try {
                            if (!Forge.getAssets().manager().contains(lookup.getPath())) {
                                Forge.getAssets().manager().load(lookup.getPath(), Texture.class, Forge.getAssets().getTextureFilter());
                                Forge.getAssets().manager().finishLoadingAsset(lookup.getPath());
                                count += 1;
                            }
                            Texture replacement = Forge.getAssets().manager().get(lookup.getPath(), Texture.class, false);
                            if (replacement != null) {
                                setCardImage(replacement);
                            } else {
                                loaded = false;
                            }
                            ImageCache.updateSynqCount(lookup, count);
                        } catch (Exception e) {
                            System.err.println("Failed to load image: " + lookup.getPath());
                            loaded = false;
                        }
                    } else if (!ImageCache.imageKeyFileExists(reward.getCard().getImageKey(false))) {
                        //Cannot find an image file, set up a rendered card until (if) a file is downloaded.
                        T = renderPlaceholder(new Graphics(), reward.getCard(), false); //Now we can render the card.
                        setCardImage(T);
                        loaded = false;
                        fetcher.fetchImage(reward.getCard().getImageKey(false), this);
                        if (reward.getCard().hasBackFace()) {
                            if (!ImageCache.imageKeyFileExists(reward.getCard().getImageKey(true))) {
                                fetcher.fetchImage(reward.getCard().getImageKey(true), this);
                            }
                        }
                    }
                }
                break;
            }
            case Item: {
                TextureAtlas atlas = Config.instance().getAtlas(ITEMS_ATLAS);
                Sprite backSprite = atlas.createSprite("CardBack");
                Pixmap drawingMap = new Pixmap((int) backSprite.getWidth(), (int) backSprite.getHeight(), Pixmap.Format.RGBA8888);

                DrawOnPixmap.draw(drawingMap, backSprite);
                if (reward.getItem() == null) {
                    needsToBeDisposed = true;
                    image = new Texture(drawingMap);
                    break;
                }
                Sprite item = reward.getItem().sprite();

                DrawOnPixmap.draw(drawingMap, (int) ((backSprite.getWidth() / 2f) - item.getWidth() / 2f), (int) ((backSprite.getHeight() / 4f) * 1.7f), item);
                //DrawOnPixmap.drawText(drawingMap, String.valueOf(reward.getItem().name), 0, (int) ((backSprite.getHeight() / 8f) * 1f), backSprite.getWidth(), false);

                setItemTooltips(item, backSprite);
                image = new Texture(drawingMap);
                drawingMap.dispose();
                needsToBeDisposed = true;
                break;
            }
            case Life:
            case Mana:
            case Gold: {
                TextureAtlas atlas = Config.instance().getAtlas(ITEMS_ATLAS);
                Sprite backSprite = atlas.createSprite("CardBack");
                Pixmap drawingMap = new Pixmap((int) backSprite.getWidth(), (int) backSprite.getHeight(), Pixmap.Format.RGBA8888);

                DrawOnPixmap.draw(drawingMap, backSprite);
                Sprite item = atlas.createSprite(reward.type.toString());
                DrawOnPixmap.draw(drawingMap, (int) ((backSprite.getWidth() / 2f) - item.getWidth() / 2f), (int) ((backSprite.getHeight() / 4f) * 1f), item);
                DrawOnPixmap.drawText(drawingMap, String.valueOf(reward.getCount()), 0, (int) ((backSprite.getHeight() / 4f) * 2f) - 1, backSprite.getWidth(), true, Color.WHITE);

                setItemTooltips(item, backSprite);
                image = new Texture(drawingMap);
                drawingMap.dispose();
                needsToBeDisposed = true;
                break;
            }
        }
        if (GuiBase.isAndroid() || Forge.hasGamepad()) {
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (flipOnClick)
                        flip();
                }

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    hover = true;
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    hover = false;
                }

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    hover = true;
                    return super.touchDown(event, x, y, pointer, button);
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    hover = false;
                    super.touchUp(event, x, y, pointer, button);
                }
            });
        } else {
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (flipOnClick)
                        flip();
                    if (frontSideUp())
                        alternate = !alternate;
                    switchTooltip();
                }

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    hover = true;
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    hover = false;
                }
            });
        }
    }

    private void switchTooltip() {
        if (!Reward.Type.Card.equals(reward.type))
            return;
        if (!reward.getCard().hasBackFace())
            return;
        Texture alt = ImageCache.getImage(reward.getCard().getImageKey(true), false);
        if (GuiBase.isAndroid() || Forge.hasGamepad()) {
            if (alternate) {
                if (alt != null) {
                    holdTooltip.tooltip_actor.clear();
                    holdTooltip.tooltip_actor.add(new RewardImage(processDrawable(alt)));
                } else {
                    if (Talt == null)
                        Talt = renderPlaceholder(new Graphics(), reward.getCard(), true);
                    holdTooltip.tooltip_actor.clear();
                    holdTooltip.tooltip_actor.add(new RewardImage(processDrawable(Talt)));
                }
            } else {
                if (toolTipImage != null) {
                    holdTooltip.tooltip_actor.clear();
                    holdTooltip.tooltip_actor.add(toolTipImage);
                }
            }
        } else {
            if (hover) {
                if (alternate) {
                    if (alt != null) {
                        tooltip.setActor(new RewardImage(processDrawable(alt)));
                    } else {
                        if (Talt == null)
                            Talt = renderPlaceholder(new Graphics(), reward.getCard(), true);
                        tooltip.setActor(new RewardImage(processDrawable(Talt)));
                    }
                } else {
                    if (toolTipImage != null)
                        tooltip.setActor(toolTipImage);
                }
            }
        }
    }

    private TextureRegionDrawable processDrawable(Texture texture) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(ImageCache.croppedBorderImage(texture));
        float origW = texture.getWidth();
        float origH = texture.getHeight();
        float boundW = Scene.getIntendedWidth() * 0.95f;
        float boundH = Scene.getIntendedHeight() * 0.95f;
        float newW = origW;
        float newH = origH;
        if (origW > boundW) {
            newW = boundW;
            newH = (newW * origH) / origW;
        }
        if (newH > boundH) {
            newH = boundH;
            newW = (newH * origW) / origH;
        }
        float AR = 480f / 270f;
        int x = Forge.getDeviceAdapter().getRealScreenSize(false).getLeft();
        int y = Forge.getDeviceAdapter().getRealScreenSize(false).getRight();
        int realX = Forge.getDeviceAdapter().getRealScreenSize(true).getLeft();
        int realY = Forge.getDeviceAdapter().getRealScreenSize(true).getRight();
        if (realX > x) {
            x *= 1.1f;
        } else if (realY > y) {
            y *= 1.1f;
        }
        float fW = x > y ? x : y;
        float fH = x > y ? y : x;
        float mul = fW / fH < AR ? AR / (fW / fH) : (fW / fH) / AR;
        Float custom = Forge.isLandscapeMode() ? Config.instance().getSettingData().cardTooltipAdjLandscape : Config.instance().getSettingData().cardTooltipAdj;
        if (custom != null && custom != 1f) {
            mul *= custom;
        } else {
            if (fW / fH >= 2f) {//tall display
                mul = (fW / fH) - ((fW / fH) / AR);
                if ((fW / fH) >= 2.1f && (fW / fH) < 2.2f)
                    mul *= 0.9f;
                else if ((fW / fH) > 2.2f) //ultrawide 21:9 Galaxy Fold, Huawei X2, Xperia 1
                    mul *= 0.8f;
            }
        }
        if (Forge.isLandscapeMode())
            drawable.setMinSize(newW * mul, newH);
        else
            drawable.setMinSize(newW, newH * mul);
        return drawable;
    }

    private void setCardImage(Texture img) {
        if (img == null)
            return;
        image = img;
        if (Forge.isTextureFilteringEnabled())
            image.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        if (toolTipImage == null)
            toolTipImage = new RewardImage(processDrawable(image));
        if (GuiBase.isAndroid() || Forge.hasGamepad()) {
            if (holdTooltip == null)
                holdTooltip = new HoldTooltip(toolTipImage);
            addListener(holdTooltip);
        } else {
            if (tooltip == null)
                tooltip = new ImageToolTip(toolTipImage);
            tooltip.setInstant(true);
            addListener(tooltip);
        }
    }

    public void showTooltip() {
        if (holdTooltip != null) {
            holdTooltip.show();
        }
    }

    public void hideTooltip() {
        if (holdTooltip != null) {
            holdTooltip.hide();
        }
    }

    private Texture renderPlaceholder(Graphics g, PaperCard card, boolean alternate) { //Use CardImageRenderer to output a Texture.
        if (renderedCount++ == 0) {
            //The first time we find a card that has no art, render one out of view to fully initialize CardImageRenderer.
            g.begin(preview_w, preview_h);
            CardImageRenderer.drawCardImage(g, CardView.getCardForUi(reward.getCard()), false, -(preview_w + 20), 0, preview_w, preview_h, CardRenderer.CardStackPosition.Top, Forge.allowCardBG, true);
            g.end();
        }
        Matrix4 m = new Matrix4();
        FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGB888, preview_w, preview_h, false);
        frameBuffer.begin();
        m.setToOrtho2D(0, preview_h, preview_w, -preview_h); //So it renders flipped directly.

        g.begin(preview_w, preview_h);
        g.setProjectionMatrix(m);
        g.startClip();
        CardImageRenderer.drawCardImage(g, CardView.getCardForUi(card), alternate, 0, 0, preview_w, preview_h, CardRenderer.CardStackPosition.Top, Forge.allowCardBG, true);
        g.end();
        g.endClip();
        //Rendering ends here. Create a new Pixmap to Texture with mipmaps, otherwise will render as full black.
        Texture result = new Texture(Pixmap.createFromFrameBuffer(0, 0, preview_w, preview_h), Forge.isTextureFilteringEnabled());
        frameBuffer.end();
        g.dispose();
        frameBuffer.dispose();
        return result;
    }

    private void setItemTooltips(Sprite icon, Sprite backSprite) {
        if (generatedTooltip == null) {
            Matrix4 m = new Matrix4();
            GlyphLayout layout = new GlyphLayout();
            ItemData item = getReward().getItem();
            FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, preview_w, preview_h, false);
            frameBuffer.begin();
            try {
                m.setToOrtho2D(0, preview_h, preview_w, -preview_h); //So it renders flipped directly.
                getGraphics().begin(preview_w, preview_h);
                getGraphics().setProjectionMatrix(m);
                getGraphics().startClip();
                getGraphics().drawImage(backSprite, 0, 0, preview_w, preview_h);
                getGraphics().drawImage(icon, preview_w / 2 - 75, 160, 160, 160);
                BitmapFont font = Controls.getBitmapFont("default", 4 / (preview_h / preview_w));
                layout.setText(font, item != null ? item.name : getReward().type.name(), Color.WHITE, preview_w - 64, Align.center, true);
                getGraphics().drawText(font, layout, 32, preview_h - 70);
                font = Controls.getBitmapFont("default", 3.5f / (preview_h / preview_w));
                layout.setText(font, item != null ? item.getDescription() : "Adds " +
                        String.valueOf(getReward().getCount()) + " " + getReward().type, Color.WHITE, preview_w - 128, item != null ? Align.left : Align.center, true);
                getGraphics().drawText(font, layout, 64, preview_h / 2.5f);
                getGraphics().end();
                getGraphics().endClip();
                generatedTooltip = new Texture(Pixmap.createFromFrameBuffer(0, 0, preview_w, preview_h), Forge.isTextureFilteringEnabled());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                frameBuffer.end();
                getGraphics().dispose();
                frameBuffer.dispose();
                Controls.getBitmapFont("default");
            }
        }

        //Rendering code ends here.

        if (toolTipImage == null)
            toolTipImage = new RewardImage(processDrawable(generatedTooltip));

        if (GuiBase.isAndroid() || Forge.hasGamepad()) {
            if (holdTooltip == null)
                holdTooltip = new HoldTooltip(toolTipImage);
            addListener(holdTooltip);
        } else {
            if (tooltip == null) {
                tooltip = new ImageToolTip(toolTipImage);
                tooltip.setInstant(true);
            }
            addListener(tooltip);
        }
    }

    private boolean frontSideUp() {
        return (flipProcess >= 0.5f) == flipOnClick;
    }

    public boolean isFlipped() {
        return (clicked && flipProcess >= 1);
    }

    public void removeTooltip() {
        if (tooltip != null) {
            tooltip.getActor().remove();
        }
    }

    public void clearHoldToolTip() {
        if (holdTooltip != null) {
            try {
                hover = false;
                holdTooltip.tooltip_actor.clear();
                holdTooltip.tooltip_actor.remove();
            } catch (Exception e) {
            }
        }
    }

    public void flip() {
        if (clicked)
            return;
        clicked = true;
        flipProcess = 0;
    }

    public void sold() {
        //todo add new card to be sold???
        if (sold)
            return;
        sold = true;
        getColor().a = 0.5f;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (clicked) {
            if (flipProcess < 1)
                flipProcess += delta * 1.5;
            else
                flipProcess = 1;

            if (GuiBase.isAndroid() || Forge.hasGamepad()) {
                if (holdTooltip != null && !getListeners().contains(holdTooltip, true)) {
                    addListener(holdTooltip);
                }
            } else {
                if (tooltip != null && !getListeners().contains(tooltip, true)) {
                    addListener(tooltip);
                }
            }
            // flipProcess=(float)Gdx.input.getX()/ (float)Gdx.graphics.getWidth();
        }

    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        applyTransform(batch, computeTransform(batch.getTransformMatrix().cpy()));

        oldProjectionTransform.set(batch.getProjectionMatrix());
        applyProjectionMatrix(batch);


        if (hover | hasKeyboardFocus())
            batch.setColor(0.5f, 0.5f, 0.5f, 1);


        if (!frontSideUp()) {
            if (flipOnClick) {
                batch.draw(backTexture, -getWidth() / 2, -getHeight() / 2, getWidth(), getHeight());
            } else {
                batch.draw(backTexture, getWidth() / 2, -getHeight() / 2, -getWidth(), getHeight());
            }
        } else {
            drawFrontSide(batch);
        }
        batch.setColor(1, 1, 1, 1);
        resetTransform(batch);
        batch.setProjectionMatrix(oldProjectionTransform);
    }

    private void drawFrontSide(Batch batch) {
        float width;
        float x;
        if (flipOnClick) {
            width = -getWidth();
            x = -getWidth() / 2 + getWidth();
        } else {
            width = getWidth();
            x = -getWidth() / 2;
        }
        if (Reward.Type.Card.equals(reward.getType())) {
            if (image != null) {
                drawCard(batch, image, x, width);
            } else if (!loaded) {
                if (T == null)
                    T = renderPlaceholder(getGraphics(), reward.getCard(), false);
                drawCard(batch, T, x, width);
            }
        } else if (image != null) {
            batch.draw(image, x, -getHeight() / 2, width, getHeight());
        }
    }

    private void drawCard(Batch batch, Texture image, float x, float width) {
        if (image != null) {
            if (image.toString().contains(".fullborder.") && Forge.enableUIMask.equals("Full")) {
                batch.end();
                shaderRoundRect.bind();
                shaderRoundRect.setUniformf("u_resolution", image.getWidth(), image.getHeight());
                shaderRoundRect.setUniformf("edge_radius", (image.getHeight() / image.getWidth()) * 20);
                shaderRoundRect.setUniformf("u_gray", sold ? 1f : 0f);
                batch.setShader(shaderRoundRect);
                batch.begin();
                //draw rounded
                batch.draw(image, x, -getHeight() / 2, width, getHeight());
                //reset
                batch.end();
                batch.setShader(null);
                batch.begin();
            } else {
                if (!sold)
                    batch.draw(ImageCache.croppedBorderImage(image), x, -getHeight() / 2, width, getHeight());
                else {
                    batch.end();
                    shaderGrayscale.bind();
                    shaderGrayscale.setUniformf("u_grayness", 1f);
                    shaderGrayscale.setUniformf("u_bias", 0.7f);
                    batch.setShader(shaderGrayscale);
                    batch.begin();
                    //draw gray
                    batch.draw(ImageCache.croppedBorderImage(image), x, -getHeight() / 2, width, getHeight());
                    //reset
                    batch.end();
                    batch.setShader(null);
                    batch.begin();
                }
            }
        }
    }

    private Graphics getGraphics() {
        if (graphics == null)
            graphics = new Graphics();
        return graphics;
    }

    private void applyProjectionMatrix(Batch batch) {
        final Vector3 direction = new Vector3(0, 0, -1);
        final Vector3 up = new Vector3(0, 1, 0);
        //final Vector3 position = new Vector3( getX()+getWidth()/2 , getY()+getHeight()/2, 0);
        final Vector3 position = new Vector3(Scene.getIntendedWidth() / 2f, Scene.getIntendedHeight() / 2f, 0);

        float fov = 67;
        Matrix4 projection = new Matrix4();
        Matrix4 view = new Matrix4();
        float hy = Scene.getIntendedHeight() / 2f;
        float a = (float) ((hy) / Math.sin(MathUtils.degreesToRadians * (fov / 2f)));
        float height = (float) Math.sqrt((a * a) - (hy * hy));
        position.z = height * 1f;
        float far = height * 2f;
        float near = height * 0.8f;

        float aspect = (float) Scene.getIntendedWidth() / (float) Scene.getIntendedHeight();
        projection.setToProjection(Math.abs(near), Math.abs(far), fov, aspect);
        view.setToLookAt(position, position.cpy().add(direction), up);
        Matrix4.mul(projection.val, view.val);

        batch.setProjectionMatrix(projection);
    }


    private final Matrix4 computedTransform = new Matrix4();
    private final Matrix4 oldTransform = new Matrix4();
    private final Matrix4 oldProjectionTransform = new Matrix4();

    protected void applyTransform(Batch batch, Matrix4 transform) {
        oldTransform.set(batch.getTransformMatrix());
        batch.setTransformMatrix(transform);
    }

    /**
     * Restores the batch transform to what it was before {@link #applyTransform(Batch, Matrix4)}. Note this causes the batch to
     * be flushed.
     */
    protected void resetTransform(Batch batch) {
        batch.setTransformMatrix(oldTransform);
    }

    protected Matrix4 computeTransform(Matrix4 worldTransform) {
        float[] val = worldTransform.getValues();
        //val[Matrix4.M32]=0.0002f;
        worldTransform.set(val);
        float originX = this.getOriginX(), originY = this.getOriginY();
        worldTransform.translate(getX() + getWidth() / 2, getY() + getHeight() / 2, 0);
        if (clicked) {
            worldTransform.rotate(0, 1, 0, 180 * flipProcess);
        }
        computedTransform.set(worldTransform);
        return computedTransform;
    }

    class ImageToolTip extends Tooltip<Image> {
        public ImageToolTip(Image contents) {
            super(contents);
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (!frontSideUp())
                return;
            super.enter(event, x, y, pointer, fromActor);
        }
    }

    class HoldTooltip extends ActorGestureListener {
        RewardImage tooltip_image;
        Table tooltip_actor;
        float height;
        TextraButton switchButton;
        //Vector2 tmp = new Vector2();

        public HoldTooltip(RewardImage tooltip_image) {
            this.tooltip_image = tooltip_image;
            tooltip_actor = new Table();
            tooltip_actor.add(this.tooltip_image);
            tooltip_actor.align(Align.center);
            tooltip_actor.setSize(this.tooltip_image.getPrefWidth(), this.tooltip_image.getPrefHeight());
            tooltip_actor.setX(Scene.getIntendedWidth() / 2 - tooltip_actor.getWidth() / 2);
            tooltip_actor.setY(Scene.getIntendedHeight() / 2 - tooltip_actor.getHeight() / 2);
            this.height = tooltip_actor.getHeight();
            switchButton = Controls.newTextButton("Flip");
            switchButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    alternate = !alternate;
                    switchTooltip();
                    super.clicked(event, x, y);
                }
            });
            getGestureDetector().setLongPressSeconds(0.1f);
        }

        @Override
        public boolean longPress(Actor actor, float x, float y) {
            if (!frontSideUp())
                return false;
            TextraButton done = actor.getStage().getRoot().findActor("done");
            if (done != null && Reward.Type.Card.equals(reward.type)) {
                switchButton.setBounds(done.getX(), done.getY(), done.getWidth(), done.getHeight());
                if (reward.getCard().hasBackFace())
                    actor.getStage().addActor(switchButton);
            }

            actor.getStage().addActor(tooltip_actor);
            return super.longPress(actor, x, y);
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            tooltip_actor.remove();
            switchButton.remove();
            super.touchUp(event, x, y, pointer, button);
        }

        @Override
        public void tap(InputEvent event, float x, float y, int count, int button) {
            if (count > 1) {
                alternate = !alternate;
                switchTooltip();
            }
            super.tap(event, x, y, count, button);
        }

        public void show() {
            if (!frontSideUp())
                return;
            tooltip_actor.setX(Scene.getIntendedWidth() / 2 - tooltip_actor.getWidth() / 2);
            tooltip_actor.setY(Scene.getIntendedHeight() / 2 - tooltip_actor.getHeight() / 2);
            getStage().addActor(tooltip_actor);
            shown = true;
        }

        public void hide() {
            tooltip_actor.remove();
            switchButton.remove();
            shown = false;
        }
    }

    class RewardImage extends Image {
        public RewardImage(TextureRegionDrawable processDrawable) {
            setDrawable(processDrawable);
            setScaling(Scaling.stretch);
            setAlign(Align.center);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            try {
                if (getDrawable() instanceof TextureRegionDrawable) {
                    Texture t = ((TextureRegionDrawable) getDrawable()).getRegion().getTexture();
                    if (t != null) {
                        float x = GuiBase.isAndroid() || Forge.hasGamepad() ? Scene.getIntendedWidth() / 2 - holdTooltip.tooltip_actor.getWidth() / 2 : tooltip.getActor().getImageX();
                        float y = GuiBase.isAndroid() || Forge.hasGamepad() ? Scene.getIntendedHeight() / 2 - holdTooltip.tooltip_actor.getHeight() / 2 : tooltip.getActor().getImageY();
                        float w = GuiBase.isAndroid() || Forge.hasGamepad() ? holdTooltip.tooltip_actor.getPrefWidth() : tooltip.getActor().getPrefWidth();
                        float h = GuiBase.isAndroid() || Forge.hasGamepad() ? holdTooltip.tooltip_actor.getPrefHeight() : tooltip.getActor().getPrefHeight();
                        if (t.toString().contains(".fullborder.") && Forge.enableUIMask.equals("Full")) {
                            batch.end();
                            shaderRoundRect.bind();
                            shaderRoundRect.setUniformf("u_resolution", t.getWidth(), t.getHeight());
                            shaderRoundRect.setUniformf("edge_radius", (t.getHeight() / t.getWidth()) * ImageCache.getRadius(t));
                            shaderRoundRect.setUniformf("u_gray", sold ? 0.8f : 0f);
                            batch.setShader(shaderRoundRect);
                            batch.begin();
                            //draw rounded
                            batch.draw(t, x, y, w, h);
                            //reset
                            batch.end();
                            batch.setShader(null);
                            batch.begin();
                        } else {
                            batch.end();
                            shaderGrayscale.bind();
                            shaderGrayscale.setUniformf("u_grayness", sold ? 1f : 0f);
                            shaderGrayscale.setUniformf("u_bias", sold ? 0.8f : 1f);
                            batch.setShader(shaderGrayscale);
                            batch.begin();
                            //draw gray
                            batch.draw(t, x, y, w, h);
                            //reset
                            batch.end();
                            batch.setShader(null);
                            batch.begin();
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.draw(batch, parentAlpha);
        }
    }
}
