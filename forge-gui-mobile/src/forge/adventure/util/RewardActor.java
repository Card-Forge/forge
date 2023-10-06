package forge.adventure.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.Graphics;
import forge.ImageKeys;
import forge.StaticData;
import forge.adventure.data.ItemData;
import forge.adventure.scene.RewardScene;
import forge.adventure.scene.Scene;
import forge.adventure.scene.UIScene;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.assets.ImageCache;
import forge.card.CardImageRenderer;
import forge.card.CardRenderer;
import forge.game.card.CardView;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.ImageFetcher;
import forge.util.ImageUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

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

    TextureRegion backTexture;
    Texture image, T, Talt;
    Graphics graphics;
    Texture generatedTooltip = null; //Storage for a generated tooltip. To dispose of on exit.
    boolean needsToBeDisposed;
    float flipProcess = 0;
    boolean clicked = false;
    boolean sold = false;
    boolean flipOnClick;
    private boolean hover, hasbackface;
    boolean loaded = true;
    boolean alternate = false, shown = false;
    boolean isRewardShop, showOverlay;
    TextraLabel overlayLabel;

    public int renderedCount = 0; //Counter for cards that require rendering a preview.
    static final ImageFetcher fetcher = GuiBase.getInterface().getImageFetcher();
    RewardImage toolTipImage;
    String description = "";

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
        if (imageFile == null || !imageFile.exists())
            return;
        Texture replacement = Forge.getAssets().manager().get(imageFile.getPath(), Texture.class, false);
        if (replacement == null) {
            try {
                Forge.getAssets().manager().load(imageFile.getPath(), Texture.class, Forge.getAssets().getTextureFilter());
                Forge.getAssets().manager().finishLoadingAsset(imageFile.getPath());
                replacement = Forge.getAssets().manager().get(imageFile.getPath(), Texture.class, false);
            } catch (Exception e) {
                //e.printStackTrace();
                return;
            }
        }
        if (replacement == null)
            return;
        count += 1;
        image = replacement;
        loaded = true;
        if (toolTipImage != null) {
            if (toolTipImage.getDrawable() instanceof TextureRegionDrawable) {
                ((TextureRegionDrawable) toolTipImage.getDrawable()).getRegion().getTexture().dispose();
            }
            toolTipImage.remove();
            toolTipImage = new RewardImage(processDrawable(image));
            if (GuiBase.isAndroid() || Forge.hasGamepad()) {
                if (holdTooltip != null) {
                    if (shown) {
                        holdTooltip.getTouchDownTarget().fire(RewardScene.eventTouchUp());
                        Gdx.input.setInputProcessor(null);
                    }
                    if (holdTooltip.getImage() != null && holdTooltip.getImage().getDrawable() instanceof TextureRegionDrawable) {
                        try { // if texture is null either it's not initialized or already disposed
                            ((TextureRegionDrawable) holdTooltip.getImage().getDrawable()).getRegion().getTexture().dispose();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    holdTooltip.hide();
                    holdTooltip.tooltip_actor = new ComplexTooltip(toolTipImage);
                }
            } else {
                tooltip.setActor(new ComplexTooltip(toolTipImage));
            }
        }
        if (T != null)
            T.dispose();
        if (alternate && Talt != null)
            Talt.dispose();
        ImageCache.updateSynqCount(imageFile, count);
        if (Forge.getCurrentScene() instanceof RewardScene)
            RewardScene.instance().reactivateInputs();
        else if (Forge.getCurrentScene() instanceof UIScene) {
            (Forge.getCurrentScene()).updateInput();
        }
        Gdx.graphics.requestRendering();
    }

    public RewardActor(Reward reward, boolean flippable, RewardScene.Type type, boolean showOverlay) {
        this.flipOnClick = flippable;
        this.reward = reward;
        this.isRewardShop = RewardScene.Type.Shop.equals(type);
        this.showOverlay = showOverlay;
        if (backTexture == null) {
            backTexture = FSkin.getSleeves().get(0);
        }
        switch (reward.type) {
            case Card: {
                hasbackface = reward.getCard().hasBackFace();
                if (ImageCache.imageKeyFileExists(reward.getCard().getImageKey(false)) && !Forge.enableUIMask.equals("Art")) {
                    int count = 0;
                    PaperCard card = ImageUtil.getPaperCardFromImageKey(reward.getCard().getImageKey(false));
                    File frontFace = ImageKeys.getImageFile(card.getCardImageKey());
                    if (frontFace != null) {
                        try {
                            Texture front = Forge.getAssets().manager().get(frontFace.getPath(), Texture.class, false);
                            if (front == null) {
                                Forge.getAssets().manager().load(frontFace.getPath(), Texture.class, Forge.getAssets().getTextureFilter());
                                Forge.getAssets().manager().finishLoadingAsset(frontFace.getPath());
                                front = Forge.getAssets().manager().get(frontFace.getPath(), Texture.class, false);
                            }
                            if (front != null) {
                                count += 1;
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
                    if (hasbackface) {
                        if (ImageCache.imageKeyFileExists(reward.getCard().getImageKey(true))) {
                            PaperCard cardBack = ImageUtil.getPaperCardFromImageKey(reward.getCard().getImageKey(true));
                            File backFace = ImageKeys.getImageFile(cardBack.getCardAltImageKey());
                            if (backFace != null) {
                                try {
                                    Texture back = Forge.getAssets().manager().get(backFace.getPath(), Texture.class, false);
                                    if (back == null) {
                                        Forge.getAssets().manager().load(backFace.getPath(), Texture.class, Forge.getAssets().getTextureFilter());
                                        Forge.getAssets().manager().finishLoadingAsset(backFace.getPath());
                                        back = Forge.getAssets().manager().get(backFace.getPath(), Texture.class, false);
                                    }
                                    if (back != null) {
                                        ImageCache.updateSynqCount(backFace, 1);
                                        generateBackFace(reward, back);
                                    } else {
                                        generateBackFace(reward, getRenderedBackface(reward));
                                    }
                                } catch (Exception e) {
                                    System.err.println("Failed to load image: " + backFace.getPath());
                                }
                            }
                        } else {
                            generateBackFace(reward, getRenderedBackface(reward));
                        }
                    }
                } else {
                    String imagePath = ImageUtil.getImageRelativePath(reward.getCard(), "", true, false);
                    File lookup = ImageKeys.hasSetLookup(imagePath) ? ImageKeys.setLookUpFile(imagePath, imagePath + "border") : null;
                    int count = 0;
                    if (lookup != null && !Forge.enableUIMask.equals("Art")) {
                        try {
                            Texture replacement = Forge.getAssets().manager().get(lookup.getPath(), Texture.class, false);
                            if (replacement == null) {
                                Forge.getAssets().manager().load(lookup.getPath(), Texture.class, Forge.getAssets().getTextureFilter());
                                Forge.getAssets().manager().finishLoadingAsset(lookup.getPath());
                                replacement = Forge.getAssets().manager().get(lookup.getPath(), Texture.class, false);
                            }
                            if (replacement != null) {
                                count += 1;
                                setCardImage(replacement);
                            } else {
                                loaded = false;
                            }
                            ImageCache.updateSynqCount(lookup, count);
                        } catch (Exception e) {
                            System.err.println("Failed to load image: " + lookup.getPath());
                            loaded = false;
                        }
                    } else {
                        //Cannot find an image file, set up a rendered card until (if) a file is downloaded.
                        String imageRelativePath = ImageUtil.getImageRelativePath(reward.getCard(), "", true, false);
                        if (imageRelativePath != null) {
                            File file = ImageKeys.getImageFile(imagePath);
                            try {
                                if (file != null) {
                                    Texture check = Forge.getAssets().manager().get(file.getPath(), Texture.class, false);
                                    if (check == null) {
                                        Forge.getAssets().manager().load(file.getPath(), Texture.class, Forge.getAssets().getTextureFilter());
                                        Forge.getAssets().manager().finishLoadingAsset(file.getPath());
                                    }
                                    ImageCache.updateSynqCount(file, 1);
                                }
                            } catch (Exception e) {
                            }
                        }
                        T = renderPlaceholder(new Graphics(), reward.getCard(), false); //Now we can render the card.
                        setCardImage(T);
                        loaded = false;
                        if (!ImageCache.imageKeyFileExists(reward.getCard().getImageKey(false)))
                            fetcher.fetchImage(reward.getCard().getImageKey(false), this);
                        if (hasbackface) {
                            if (!ImageCache.imageKeyFileExists(reward.getCard().getImageKey(true))) {
                                fetcher.fetchImage(reward.getCard().getImageKey(true), null);
                            }
                        }
                    }
                }
                break;
            }
            case Item: {
                Sprite backSprite = Config.instance().getItemSprite("CardBack");
                if (reward.getItem() == null) {
                    needsToBeDisposed = true;
                    processSprite(backSprite, null, null, 0, 0, false);
                    break;
                }
                Sprite item = reward.getItem().sprite();
                setItemTooltips(item, backSprite, false);
                boolean isQuestItemLoot = RewardScene.Type.Loot.equals(type) && reward.getItem().questItem;
                processSprite(backSprite, item, isQuestItemLoot ? Controls.newTextraLabel("[%200]" + reward.getItem().name) : null, 0, isQuestItemLoot ? -10 : 0, false);
                needsToBeDisposed = true;
                break;
            }
            case CardPack: {
                Sprite backSprite = Config.instance().getItemSprite("CardBack");
                if (reward.getDeck() == null) {
                    needsToBeDisposed = true;
                    processSprite(backSprite, null, null, 0, 0, false);
                    break;
                }


                String imageKey = "";
                String editionCode = "";
                try {
                    editionCode = reward.getDeck().getComment();
                    int artIndex = 1;
                    if (SealedProduct.specialSets.contains(editionCode) || editionCode.equals("?")) {
                        imageKey = "b:" + getName().substring(0, getName().indexOf("Booster Pack") - 1);
                    } else {
                        int maxIdx = StaticData.instance().getEditions().get(editionCode).getCntBoosterPictures();
                        artIndex = Aggregates.randomInt(1, 2);//MyRandom.getRandom().nextInt(maxIdx) + 1;
                        imageKey = ImageKeys.BOOSTER_PREFIX + editionCode + ((1 >= maxIdx) ? "" : ("_" + artIndex));
                    }
                } catch (Exception e) {
                    //Comment did not contain the edition code, this is not a basic booster pack
                }
                boolean isBooster = false;
                Sprite item;
                Texture t = ImageCache.getImage(imageKey, false, true);
                if (t != null) {
                    item = new Sprite(new TextureRegion(t));
                    isBooster = true;
                } else {
                    item = Config.instance().getItemSprite("Deck");
                }

                setItemTooltips(item, backSprite, isBooster);
                if (isBooster)
                    processSprite(backSprite, item, Controls.newTextraLabel("[%200]" + editionCode + " Booster"), 0, -10, isBooster);
                else
                    processSprite(backSprite, item, Controls.newTextraLabel("[%200]Event Reward Pack"), 0, -10, isBooster);
                needsToBeDisposed = true;
                break;
            }
            case Life:
            case Shards:
            case Gold: {
                Sprite backSprite = Config.instance().getItemSprite("CardBack");
                Sprite item = Config.instance().getItemSprite(reward.type.toString());
                setItemTooltips(item, backSprite, false);
                processSprite(backSprite, item, isRewardShop ? null :
                        Controls.newTextraLabel("[%200]" + reward.getCount() + " " + reward.type), 0, isRewardShop ? 0 : -10, false);
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

    private Texture getRenderedBackface(Reward r) {
        if (Talt == null)
            Talt = renderPlaceholder(new Graphics(), r.getCard(), true);
        return Talt;
    }

    private void generateBackFace(Reward r, Texture t) {
        try {
            if (holdTooltip != null) {
                if (holdTooltip.tooltip_actor.getChildren().size <= 2) {
                    holdTooltip.tooltip_actor.altcImage = new RewardImage(processDrawable(t));
                    holdTooltip.tooltip_actor.addActorAt(2, holdTooltip.tooltip_actor.altcImage);
                    holdTooltip.tooltip_actor.swapActor(holdTooltip.tooltip_actor.altcImage, holdTooltip.tooltip_actor.cImage);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load alternate image: " + r.getCard());
        }
    }

    private void switchTooltip() {
        if (!Reward.Type.Card.equals(reward.type))
            return;
        if (!reward.getCard().hasBackFace())
            return;
        if (GuiBase.isAndroid() || Forge.hasGamepad()) {
            if (holdTooltip.tooltip_actor.altcImage != null) {
                holdTooltip.tooltip_actor.swapActor(holdTooltip.tooltip_actor.cImage, holdTooltip.tooltip_actor.altcImage);
            }
        } else {
            Texture alt = ImageCache.getImage(reward.getCard().getImageKey(true), false);
            if (hover) {
                if (alternate) {
                    if (alt != null) {
                        tooltip.setActor(new ComplexTooltip(new RewardImage(processDrawable(alt))));
                    } else {
                        if (Talt == null)
                            Talt = renderPlaceholder(new Graphics(), reward.getCard(), true);
                        tooltip.setActor(new ComplexTooltip(new RewardImage(processDrawable(Talt))));
                    }
                } else {
                    if (toolTipImage != null)
                        tooltip.setActor(new ComplexTooltip(toolTipImage));
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
        float fW = Math.max(x, y);
        float fH = Math.min(x, y);
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
                holdTooltip = new HoldTooltip(new ComplexTooltip(toolTipImage));
            addListener(holdTooltip);
        } else {
            if (tooltip == null)
                tooltip = new ImageToolTip(new ComplexTooltip(toolTipImage));
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
        if (renderedCount < 1) {
            renderedCount++;
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
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, preview_w, preview_h);
        Texture result = new Texture(pixmap, Forge.isTextureFilteringEnabled());
        frameBuffer.end();
        g.dispose();
        frameBuffer.dispose();
        pixmap.dispose();
        return result;
    }

    private void processSprite(Sprite sprite, Sprite item, TextraLabel itemText, int modX, int modY, boolean isBooster) {
        int pw = 192;
        int ph = 256;
        FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGB888, pw, ph, false);
        SpriteBatch batch = new SpriteBatch();

        frameBuffer.begin();

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Matrix4 matrix = new Matrix4();
        matrix.setToOrtho2D(0, ph, pw, -ph);
        batch.setProjectionMatrix(matrix);

        batch.begin();
        batch.draw(sprite, 0, 0, pw, ph);
        if (item != null) {
            if (!isBooster) {
                float iw = item.getWidth() * 4;
                float ih = item.getHeight() * 4;
                batch.draw(item, pw / 2f - iw / 2f, (ph / 2f - ih / 2f), iw, ih);
            } else
                batch.draw(item, pw / 4f, ph / 4f, pw / 2f, ph / 2f);
        }
        if (itemText != null) {
            itemText.setWrap(true);
            itemText.setAlignment(1);
            itemText.setWidth(pw);
            itemText.setHeight(ph);
            itemText.setX(itemText.getX() + (modX * 4));
            itemText.setY(itemText.getY() + (modY * 8));
            itemText.draw(batch, 1);
        }
        batch.end();
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, pw, ph);
        image = new Texture(pixmap);
        frameBuffer.end();
        batch.dispose();
        pixmap.dispose();
        frameBuffer.dispose();
    }

    private void setItemTooltips(Sprite icon, Sprite backSprite, boolean isBooster) {
        int align = Align.left;
        if (generatedTooltip == null) {
            Matrix4 m = new Matrix4();
            GlyphLayout layout = new GlyphLayout();
            ItemData item = getReward().getItem();
            boolean itemExists = item != null;
            FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, preview_w, preview_h, false);
            frameBuffer.begin();
            try {
                m.setToOrtho2D(0, preview_h, preview_w, -preview_h); //So it renders flipped directly.
                getGraphics().begin(preview_w, preview_h);
                getGraphics().setProjectionMatrix(m);
                getGraphics().startClip();
                getGraphics().drawImage(backSprite, 0, 0, preview_w, preview_h);
                if (!isBooster)
                    getGraphics().drawImage(icon, preview_w / 2f - 75, 160, 160, 160);
                else
                    getGraphics().drawImage(icon, 0, 0, preview_w, preview_h);
                float div = (float) preview_h / preview_w;
                BitmapFont font = Controls.getBitmapFont("default", 4 / div);
                layout.setText(font, itemExists ? item.name : getReward().type.name(), Color.WHITE, preview_w - 64, Align.center, true);
                getGraphics().drawText(font, layout, 32, preview_h - 70);
                align = itemExists ? Align.topLeft : Align.top;
                if (itemExists) {
                    description = item.getDescription();
                    layout.reset();
                } else if (getReward().getDeck() != null) {
                    description = getReward().getDeck().getName();
                } else {
                    description = "Adds " + getReward().getCount() + " " + getReward().type;
                }
                if (itemExists && description.isEmpty() && item.questItem)
                    description = "Quest Item";
                getGraphics().end();
                getGraphics().endClip();
                Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, preview_w, preview_h);
                generatedTooltip = new Texture(pixmap, Forge.isTextureFilteringEnabled());
                pixmap.dispose();
            } catch (Exception e) {
                //e.printStackTrace();
            } finally {
                frameBuffer.end();
                getGraphics().dispose();
                frameBuffer.dispose();
                //reset bitmapfont to default
                Controls.getBitmapFont("default");
            }
        }

        //Rendering code ends here.

        if (toolTipImage == null)
            toolTipImage = new RewardImage(processDrawable(generatedTooltip));

        if (GuiBase.isAndroid() || Forge.hasGamepad()) {
            if (holdTooltip == null)
                holdTooltip = new HoldTooltip(new ComplexTooltip(toolTipImage, align));
            addListener(holdTooltip);
        } else {
            if (tooltip == null) {
                tooltip = new ImageToolTip(new ComplexTooltip(toolTipImage, align));
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
            if (tooltip.getActor() != null)
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
        SoundSystem.instance.play(SoundEffectType.FlipCard, false);
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
                flipProcess += delta * 2.4;
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

        if (showOverlay && Config.instance().getSettingData().showShopOverlay) {
            if (overlayLabel == null) {
                setOverlayLabel();
            }
            if (overlayLabel != null) {
                overlayLabel.draw(batch, parentAlpha);
            }
        }
    }

    private void setOverlayLabel() {
        String display = "";
        int alignment = Align.top;
        String labelStyle = "background";
        if (reward == null)
            return;
        Reward.Type rewardType = reward.getType();
        switch (rewardType) {
            case Card:
                display = reward.getCard() != null ? CardTranslation.getTranslatedName(reward.getCard().getName()) : "";
                //alignment = Align.topLeft;
                labelStyle = "dialog";
                break;
            case Life:
            case Gold:
            case Shards:
                display = reward.type.toString();
                break;
            case Item:
                display =  reward.getItem() != null ? reward.getItem().name : "";
                break;
            case CardPack:
                display = reward.getDeck() != null ? "Card Pack (" + reward.getDeck().getComment() + ")" : "";
                break;
            default:
                break;
        }
        overlayLabel = Controls.newRewardLabel("[%98]" + display);
        overlayLabel.setWidth(this.getWidth());
        overlayLabel.setWrap(true);
        overlayLabel.setAlignment(alignment);
        overlayLabel.style = (Controls.getSkin().get(labelStyle, Label.LabelStyle.class));
        //compute layout
        overlayLabel.layout();
        //get the layout values and apply
        overlayLabel.setHeight(overlayLabel.layout.getHeight());
        overlayLabel.setPosition(this.getX(), (this.getY(Align.top) - overlayLabel.layout.getHeight()));
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
                    T = renderPlaceholder(new Graphics(), reward.getCard(), false);
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
                shaderRoundRect.setUniformf("edge_radius", (float) (image.getHeight() / image.getWidth()) * 20);
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
            if (hasbackface) {
                TextureRegion icon = FSkinImage.ADV_FLIPICON.getTextureRegion();
                float scale = getHeight() / 4f;
                batch.draw(icon, getOriginX() - scale / 2f, getOriginY() - scale / 2f, scale, scale);
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

    class ComplexTooltip extends Group {
        private TextraLabel cLabel;
        private Image cImage, altcImage;
        private float inset, width, x, y;
        private int ARP;

        public ComplexTooltip(Image i) {
            this(i, Align.left);
        }

        public ComplexTooltip(Image i, int align) {
            cImage = i;
            setSize(cImage.getPrefWidth(), cImage.getPrefHeight());
            setPosition(0, 0, Align.center);
            inset = cImage.getPrefWidth() * 0.13f;
            width = cImage.getPrefWidth() - inset * 2;
            x = cImage.getX() + inset;
            y = cImage.getPrefHeight() / 2.3f;
            ARP = Forge.isLandscapeMode() ? 100 : 150;
            cLabel = new TextraLabel("[%" + ARP + "]" + description, Controls.getSkin(), Controls.getTextraFont());
            cLabel.setAlignment(align);
            cLabel.setWrap(true);
            cLabel.setWidth(width);
            cLabel.setX(x);
            cLabel.setY(y);
            addActorAt(0, cImage);
            addActorAt(1, cLabel);
        }

        public Image getStoredImage() {
            return cImage;
        }

        public TextraLabel getStoredLabel() {
            return cLabel;
        }
    }

    class ImageToolTip extends Tooltip<ComplexTooltip> {
        public ImageToolTip(ComplexTooltip contents) {
            super(contents);
        }

        public Image getImage() {
            return getActor().getStoredImage();
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (!frontSideUp())
                return;
            super.enter(event, x, y, pointer, fromActor);
        }
    }

    class HoldTooltip extends ActorGestureListener {
        private ComplexTooltip tooltip_actor;
        private TextraButton switchButton;

        public HoldTooltip(ComplexTooltip complexTooltip) {
            tooltip_actor = complexTooltip;
            switchButton = Controls.newTextButton("[+Flip]");
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

        public Image getImage() {
            return tooltip_actor.getStoredImage();
        }

        @Override
        public boolean longPress(Actor actor, float x, float y) {
            if (!frontSideUp())
                return false;
            show();
            return super.longPress(actor, x, y);
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            hide();
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
            tooltip_actor.setBounds(tooltip_actor.cImage.getX(), tooltip_actor.cImage.getY(), tooltip_actor.cImage.getPrefWidth(), tooltip_actor.cImage.getPrefHeight());
            tooltip_actor.cLabel.setX(Scene.getIntendedWidth() / 2f - tooltip_actor.width / 2);
            tooltip_actor.cLabel.setY(Scene.getIntendedHeight() / 2f - tooltip_actor.inset);
            getStage().addActor(tooltip_actor);
            TextraButton done = getStage().getRoot().findActor("done");
            if (done != null && Reward.Type.Card.equals(reward.type)) {
                switchButton.setBounds(done.getX(), done.getY(), done.getWidth(), done.getHeight());
                if (reward.getCard().hasBackFace())
                    getStage().addActor(switchButton);
            }
            shown = true;
        }

        public void hide() {
            if (tooltip_actor != null)
                tooltip_actor.remove();
            if (switchButton != null)
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
                    TextureRegion tr = ((TextureRegionDrawable) getDrawable()).getRegion();
                    Texture t = tr.getTexture();
                    if (t != null) {
                        float x = GuiBase.isAndroid() || Forge.hasGamepad() ? Scene.getIntendedWidth() / 2f - holdTooltip.tooltip_actor.getWidth() / 2f : tooltip.getActor().getStoredImage().getImageX();
                        float y = GuiBase.isAndroid() || Forge.hasGamepad() ? Scene.getIntendedHeight() / 2f - holdTooltip.tooltip_actor.getHeight() / 2f : tooltip.getActor().getStoredImage().getImageY();
                        float w = GuiBase.isAndroid() || Forge.hasGamepad() ? holdTooltip.tooltip_actor.getStoredImage().getPrefWidth() : tooltip.getActor().getStoredImage().getPrefWidth();
                        float h = GuiBase.isAndroid() || Forge.hasGamepad() ? holdTooltip.tooltip_actor.getStoredImage().getPrefHeight() : tooltip.getActor().getStoredImage().getPrefHeight();
                        if (t.toString().contains(".fullborder.") && Forge.enableUIMask.equals("Full")) {
                            batch.end();
                            shaderRoundRect.bind();
                            shaderRoundRect.setUniformf("u_resolution", t.getWidth(), t.getHeight());
                            shaderRoundRect.setUniformf("edge_radius", ((float) (t.getHeight() / t.getWidth())) * ImageCache.getRadius(t));
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
                            batch.draw(tr, x, y, w, h);
                            //reset
                            batch.end();
                            batch.setShader(null);
                            batch.begin();
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
            super.draw(batch, parentAlpha);
        }
    }
}
