package forge.adventure.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
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
import forge.Forge;
import forge.Graphics;
import forge.ImageKeys;
import forge.adventure.data.ItemData;
import forge.adventure.scene.RewardScene;
import forge.adventure.scene.Scene;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
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
    Tooltip<Image> tooltip;
    HoldTooltip holdTooltip;
    Reward reward;
    ShaderProgram shaderGrayscale = Forge.getGraphics().getShaderGrayscale();

    final int preview_w = 488; //Width and height for generated images.
    final int preview_h = 680;

    static TextureRegion backTexture;
    Texture image, T;
    Graphics graphics;
    Texture generatedTooltip = null; //Storage for a generated tooltip. To dispose of on exit.
    boolean needsToBeDisposed;
    float flipProcess = 0;
    boolean clicked = false;
    boolean sold = false;
    boolean flipOnClick;
    private boolean hover;
    boolean loaded = true;
    TextureParameter parameter;

    public static int renderedCount = 0; //Counter for cards that require rendering a preview.
    static final ImageFetcher fetcher = GuiBase.getInterface().getImageFetcher();
    Image toolTipImage;

    @Override
    public void dispose() {
        if (needsToBeDisposed) {
            needsToBeDisposed = false;
            image.dispose();
            if (generatedTooltip != null)
                generatedTooltip.dispose();
        }
        if (T != null)
            T.dispose();
    }

    public Reward getReward() {
        return reward;
    }
    @Override
    public void onImageFetched() {
        String imageKey = reward.getCard().getImageKey(false);
        PaperCard card = ImageUtil.getPaperCardFromImageKey(imageKey);
        imageKey = card.getCardImageKey();
        if (StringUtils.isBlank(imageKey))
            return;
        File imageFile = ImageKeys.getImageFile(imageKey);
        if (imageFile == null)
            return;
        if (!Forge.getAssets().manager().contains(imageFile.getPath())) {
            Forge.getAssets().manager().load(imageFile.getPath(), Texture.class, Forge.getAssets().getTextureFilter());
            Forge.getAssets().manager().finishLoadingAsset(imageFile.getPath());
        }
        Texture replacement = Forge.getAssets().manager().get(imageFile.getPath(), Texture.class, false);
        if (replacement == null)
            return;
        image = replacement;
        loaded = true;
        TextureRegionDrawable drawable = new TextureRegionDrawable(ImageCache.croppedBorderImage(image));
        if(Forge.isLandscapeMode())
            drawable.setMinSize((Scene.getIntendedHeight() / RewardScene.CARD_WIDTH_TO_HEIGHT) * 0.95f, Scene.getIntendedHeight() * 0.95f);
        else
            drawable.setMinSize(Scene.getIntendedWidth()  * 0.95f, Scene.getIntendedWidth()* RewardScene.CARD_WIDTH_TO_HEIGHT * 0.95f);
        if (toolTipImage != null) {
            if (toolTipImage.getDrawable() instanceof TextureRegionDrawable) {
                ((TextureRegionDrawable) toolTipImage.getDrawable()).getRegion().getTexture().dispose();
            }
        }
        toolTipImage.remove();
        toolTipImage = new Image(drawable);
        if (GuiBase.isAndroid()) {
            if (holdTooltip.tooltip_image.getDrawable() instanceof TextureRegionDrawable) {
                ((TextureRegionDrawable) holdTooltip.tooltip_image.getDrawable()).getRegion().getTexture().dispose();
            }
            holdTooltip.tooltip_actor.clear();
            holdTooltip.tooltip_actor.add(toolTipImage);
        } else {
            tooltip.setActor(toolTipImage);
        }
        if (T != null)
            T.dispose();
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
                    setCardImage(ImageCache.getImage(reward.getCard().getImageKey(false), false));
                } else {
                    if (!ImageCache.imageKeyFileExists(reward.getCard().getImageKey(false))) {
                        //Cannot find an image file, set up a rendered card until (if) a file is downloaded.
                        if(renderedCount++ == 0) {
                            //The first time we find a card that has no art, render one out of view to fully initialize CardImageRenderer.
                            getGraphics().begin(preview_w, preview_h);
                            CardImageRenderer.drawCardImage(getGraphics(), CardView.getCardForUi(reward.getCard()), false, -(preview_w + 20), 0, preview_w, preview_h, CardRenderer.CardStackPosition.Top, Forge.enableUIMask.equals("Art"), true);
                            getGraphics().end();
                        }
                        T = renderPlaceholder(getGraphics(), reward.getCard()); //Now we can render the card.
                        setCardImage(T);
                        loaded = false;
                        fetcher.fetchImage(reward.getCard().getImageKey(false), this);
                    }
                }
                break;
            }
            case Item: {
                TextureAtlas atlas = Config.instance().getAtlas(ITEMS_ATLAS);
                Sprite backSprite = atlas.createSprite("CardBack");
                Pixmap drawingMap = new Pixmap((int) backSprite.getWidth(), (int) backSprite.getHeight(), Pixmap.Format.RGBA8888);

                DrawOnPixmap.draw(drawingMap, backSprite);
                Sprite item = reward.getItem().sprite();

                DrawOnPixmap.draw(drawingMap, (int) ((backSprite.getWidth() / 2f) - item.getWidth() / 2f), (int) ((backSprite.getHeight() / 4f) * 1.7f), item);
                //DrawOnPixmap.drawText(drawingMap, String.valueOf(reward.getItem().name), 0, (int) ((backSprite.getHeight() / 8f) * 1f), backSprite.getWidth(), false);

                setItemTooltips(item);
                image=new Texture(drawingMap);
                drawingMap.dispose();
                needsToBeDisposed = true;
                break;
            }
            case Gold: {
                TextureAtlas atlas = Config.instance().getAtlas(ITEMS_ATLAS);
                Sprite backSprite = atlas.createSprite("CardBack");
                Pixmap drawingMap = new Pixmap((int) backSprite.getWidth(), (int) backSprite.getHeight(), Pixmap.Format.RGBA8888);

                DrawOnPixmap.draw(drawingMap, backSprite);
                Sprite gold = atlas.createSprite("Gold");
                DrawOnPixmap.draw(drawingMap, (int) ((backSprite.getWidth() / 2f) - gold.getWidth() / 2f), (int) ((backSprite.getHeight() / 4f) * 1f), gold);
                DrawOnPixmap.drawText(drawingMap, String.valueOf(reward.getCount()), 0, (int) ((backSprite.getHeight() / 4f) * 2f)-1, backSprite.getWidth(), true,Color.WHITE);

                image=new Texture(drawingMap);
                drawingMap.dispose();
                needsToBeDisposed = true;
                break;
            }
            case Life: {
                TextureAtlas atlas = Config.instance().getAtlas(ITEMS_ATLAS);
                Sprite backSprite = atlas.createSprite("CardBack");
                Pixmap drawingMap = new Pixmap((int) backSprite.getWidth(), (int) backSprite.getHeight(), Pixmap.Format.RGBA8888);

                DrawOnPixmap.draw(drawingMap, backSprite);
                Sprite gold = atlas.createSprite("Life");
                DrawOnPixmap.draw(drawingMap, (int) ((backSprite.getWidth() / 2f) - gold.getWidth() / 2f), (int) ((backSprite.getHeight() / 4f) * 1f), gold);
                DrawOnPixmap.drawText(drawingMap, String.valueOf(reward.getCount()), 0, (int) ((backSprite.getHeight() / 4f) * 2f)-1, backSprite.getWidth(), true,Color.WHITE);

                image = new Texture(drawingMap);
                drawingMap.dispose();
                needsToBeDisposed = true;
                break;
            }
        }
        if (GuiBase.isAndroid()) {
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (flipOnClick)
                        flip();
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

    private void setCardImage(Texture img) {
        if (img == null)
            return;
        image = img;
        if (Forge.isTextureFilteringEnabled())
            image.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        TextureRegionDrawable drawable = new TextureRegionDrawable(ImageCache.croppedBorderImage(image));
        if(Forge.isLandscapeMode())
            drawable.setMinSize((Scene.getIntendedHeight() / RewardScene.CARD_WIDTH_TO_HEIGHT) * 0.95f, Scene.getIntendedHeight() * 0.95f);
        else
            drawable.setMinSize(Scene.getIntendedWidth()  * 0.95f, Scene.getIntendedWidth()* RewardScene.CARD_WIDTH_TO_HEIGHT * 0.95f);
        if (toolTipImage == null)
            toolTipImage = new Image(drawable);
        if (GuiBase.isAndroid()) {
            if (holdTooltip == null)
                holdTooltip = new HoldTooltip(toolTipImage);
            if (frontSideUp())
                addListener(holdTooltip);
        } else {
            if (tooltip == null)
                tooltip = new Tooltip<Image>(toolTipImage);
            tooltip.setInstant(true);
            if (frontSideUp())
                addListener(tooltip);
        }
    }

    private Texture renderPlaceholder(Graphics g, PaperCard card){ //Use CardImageRenderer to output a Texture.
        Matrix4 m  = new Matrix4();
        FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGB888, preview_w, preview_h, false);
        frameBuffer.begin();
        m.setToOrtho2D(0,preview_h, preview_w, -preview_h); //So it renders flipped directly.

        g.begin(preview_w, preview_h);
        g.setProjectionMatrix(m);
        g.startClip();
        CardImageRenderer.drawCardImage(g, CardView.getCardForUi(card), false, 0, 0, preview_w, preview_h, CardRenderer.CardStackPosition.Top, Forge.enableUIMask.equals("Art"), true);
        g.end();
        g.endClip();
        //Rendering ends here. Create a new Pixmap to Texture with mipmaps, otherwise will render as full black.
        Texture result = new Texture(Pixmap.createFromFrameBuffer(0, 0, preview_w, preview_h), Forge.isTextureFilteringEnabled());
        frameBuffer.end();
        g.dispose();
        frameBuffer.dispose();
        return result;
    }

    private void setItemTooltips(Sprite icon) {
        if (generatedTooltip == null) {
            float icon_w = 64f; float icon_h = 64f; //Sizes for the embedded icon. Could be made smaller on smaller resolutions.
            Matrix4 m  = new Matrix4();
            ItemData item = getReward().getItem();
            FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, preview_w, preview_h, false);
            frameBuffer.begin();
            m.setToOrtho2D(0,preview_h, preview_w, -preview_h); //So it renders flipped directly.
            getGraphics().begin(preview_w, preview_h);
            getGraphics().setProjectionMatrix(m);
            getGraphics().startClip();
            //Draw item description panel.
            getGraphics().fillRect(new Color(0f, 0f, 0f, 0.96f), 0, 0, preview_w, preview_h); //Translucent background.
            getGraphics().drawRectLines(2, Color.WHITE, 0, 0, preview_w, preview_h); //Add a border.
            getGraphics().drawImage(icon, 2, 2, icon_w, icon_h); //Draw the item's icon.
            getGraphics().drawText(item.name, FSkinFont.get(24), Color.WHITE, icon_w + 2, 2, preview_w - (icon_w + 2), icon_h, false, 1, true); //Item name.
            getGraphics().drawRectLines(1, Color.WHITE, 6, icon_h + 2, preview_w - 12, preview_h - (icon_h + 6)); //Description border.
            getGraphics().drawText(item.getDescription(), FSkinFont.get(18), Color.WHITE, 10, icon_h + 8, preview_w - 10, preview_h - 4, true, Align.left, false); //Description.
            getGraphics().end();
            getGraphics().endClip();
            generatedTooltip = new Texture(Pixmap.createFromFrameBuffer(0, 0, preview_w, preview_h), Forge.isTextureFilteringEnabled());
            frameBuffer.end();
            getGraphics().dispose();
            frameBuffer.dispose();
        }

        //Rendering code ends here.

        TextureRegionDrawable drawable = new TextureRegionDrawable(generatedTooltip);
        if(Forge.isLandscapeMode())
            drawable.setMinSize((Scene.getIntendedHeight() / RewardScene.CARD_WIDTH_TO_HEIGHT) * 0.95f, Scene.getIntendedHeight() * 0.95f);
        else
            drawable.setMinSize(Scene.getIntendedWidth()  * 0.95f, Scene.getIntendedWidth()* RewardScene.CARD_WIDTH_TO_HEIGHT * 0.95f);

        if (toolTipImage == null)
            toolTipImage = new Image(drawable);

        if (frontSideUp()) {
            if (GuiBase.isAndroid()) {
                if (holdTooltip == null)
                    holdTooltip = new HoldTooltip(toolTipImage);
                addListener(holdTooltip);
            } else {
                if (tooltip == null) {
                    tooltip = new Tooltip<>(toolTipImage);
                    tooltip.setInstant(true);
                }
                addListener(tooltip);
            }
        }
    }

    private boolean frontSideUp() {
        return (flipProcess >= 0.5f) == flipOnClick;
    }

    public boolean isFlipped() {
        return (clicked && flipProcess >= 1);
    }
    public void clearHoldToolTip() {
        if (holdTooltip != null) {
            try {
                hover = false;
                holdTooltip.tooltip_actor.clear();
                holdTooltip.tooltip_actor.remove();
            } catch (Exception e){}
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

            if (GuiBase.isAndroid()) {
                if (holdTooltip != null && frontSideUp() && !getListeners().contains(holdTooltip, true)) {
                    addListener(holdTooltip);
                }
            } else {
                if (tooltip != null && frontSideUp() && !getListeners().contains(tooltip, true)) {
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


        if (hover)
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
                    T = renderPlaceholder(getGraphics(), reward.getCard());
                drawCard(batch, T, x, width);
            }
        } else if (image != null) {
            batch.draw(image, x, -getHeight() / 2, width, getHeight());
        }
    }
    private void drawCard(Batch batch, Texture image, float x, float width) {
        if (image != null) {
            if (!sold)
                batch.draw(ImageCache.croppedBorderImage(image), x, -getHeight() / 2, width, getHeight());
            else {
                batch.end();
                shaderGrayscale.bind();
                shaderGrayscale.setUniformf("u_grayness", 1f);
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

    class HoldTooltip extends ActorGestureListener {
        Image tooltip_image;
        Table tooltip_actor;
        float height;
        //Vector2 tmp = new Vector2();

        public HoldTooltip(Image tooltip_image) {
            this.tooltip_image = tooltip_image;
            tooltip_actor = new Table();
            tooltip_actor.add(this.tooltip_image);
            tooltip_actor.align(Align.center);
            tooltip_actor.setSize(this.tooltip_image.getPrefWidth(), this.tooltip_image.getPrefHeight());
            this.height = tooltip_actor.getHeight();

            getGestureDetector().setLongPressSeconds(0.1f);
        }

        @Override
        public boolean longPress(Actor actor, float x, float y) {
            //Vector2 point = actor.localToStageCoordinates(tmp.set(x, y));
            tooltip_actor.setX(actor.getRight());
            if (tooltip_actor.getX() + tooltip_actor.getWidth() > Scene.getIntendedWidth())
                tooltip_actor.setX(Math.max(0,actor.getX() - tooltip_actor.getWidth()));
            tooltip_actor.setY(Scene.getIntendedHeight() / 2 - tooltip_actor.getHeight() / 2);
            //tooltip_actor.setX(480/2 - tooltip_actor.getWidth()/2); //480 hud width
            //tooltip_actor.setY(270/2-tooltip_actor.getHeight()/2); //270 hud height
            actor.getStage().addActor(tooltip_actor);
            return super.longPress(actor, x, y);
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            tooltip_actor.remove();
            super.touchUp(event, x, y, pointer, button);
        }
    }
}
