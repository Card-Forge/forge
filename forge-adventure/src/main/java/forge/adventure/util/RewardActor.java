package forge.adventure.util;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import forge.Forge;
import forge.adventure.scene.RewardScene;
import forge.adventure.scene.Scene;
import forge.assets.FSkin;
import forge.assets.ImageCache;
import forge.gui.GuiBase;
import forge.util.ImageFetcher;

/**
 * Render the rewards as a card on the reward scene.
 *
 */
public class RewardActor extends Actor implements Disposable, ImageFetcher.Callback {
    Tooltip<Image> tooltip;
    Reward reward;

    static TextureRegion backTexture;
    Texture image;
    boolean needsToBeDisposed;
    float flipProcess=0;
    boolean clicked=false;
    boolean flipOnClick;
    private boolean hover;

    static final ImageFetcher fetcher = GuiBase.getInterface().getImageFetcher();
    Image toolTipImage;
    @Override
    public void dispose() {

        if(needsToBeDisposed)
            image.dispose();

    }

    public Reward getReward() {
        return reward;
    }

    @Override
    public void onImageFetched() {
        setCardImage(ImageCache.getImage(reward.getCard().getImageKey(false),false));
    }

    public RewardActor(Reward reward,boolean flippable)
    {

        this.flipOnClick=flippable;
        this.reward=reward;
        if(backTexture==null)
        {
            backTexture= FSkin.getSleeves().get(0);
        }
        switch (reward.type)
        {
            case Card:
                if(ImageCache.imageKeyFileExists(reward.getCard().getImageKey(false)))
                {
                    setCardImage( ImageCache.getImage(reward.getCard().getImageKey(false),false));
                }
                else
                {

                    if (!ImageCache.imageKeyFileExists(reward.getCard().getImageKey(false))) {
                        fetcher.fetchImage(reward.getCard().getImageKey(false), this);
                    }
                }

                break;
            case Gold:
            {

                TextureAtlas atlas= Config.instance().getAtlas("sprites/items.atlas");
                Sprite backSprite=atlas.createSprite("CardBack");
                Pixmap drawingMap=new Pixmap((int)backSprite.getWidth(),(int)backSprite.getHeight(), Pixmap.Format.RGBA8888);

                DrawOnPixmap.draw(drawingMap,backSprite);
                Sprite gold=atlas.createSprite("Gold");
                DrawOnPixmap.draw(drawingMap,(int)((backSprite.getWidth()/2f)-gold.getWidth()/2f),(int)((backSprite.getHeight()/4f)*1f),gold);
                DrawOnPixmap.drawText(drawingMap,String.valueOf(reward.getCount()),0,(int)((backSprite.getHeight()/4f)*2f),backSprite.getWidth());

                image=new Texture(drawingMap);
                drawingMap.dispose();
                needsToBeDisposed=true;
                break;
            }
            case Life:
            {
                TextureAtlas atlas = Config.instance().getAtlas("sprites/items.atlas");
                Sprite backSprite = atlas.createSprite("CardBack");
                Pixmap drawingMap = new Pixmap((int) backSprite.getWidth(), (int) backSprite.getHeight(), Pixmap.Format.RGBA8888);

                DrawOnPixmap.draw(drawingMap, backSprite);
                Sprite gold = atlas.createSprite("Life");
                DrawOnPixmap.draw(drawingMap, (int) ((backSprite.getWidth() / 2f) - gold.getWidth() / 2f), (int) ((backSprite.getHeight() / 4f) * 1f), gold);
                DrawOnPixmap.drawText(drawingMap, String.valueOf(reward.getCount()), 0, (int) ((backSprite.getHeight() / 4f) * 2f), backSprite.getWidth());

                image = new Texture(drawingMap);
                drawingMap.dispose();
                needsToBeDisposed = true;
                break;
            }
        }
        addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            if(flipOnClick)
                flip();
        }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
                hover=true;
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor){
                hover=false;
            }
    });
    }

    private void setCardImage(Texture img) {
        image=img;
        if(Forge.isTextureFilteringEnabled())
        image.setFilter(Texture.TextureFilter.MipMapLinearLinear,Texture.TextureFilter.Linear);
        TextureRegionDrawable drawable=new TextureRegionDrawable(new TextureRegion(image));
        drawable.setMinSize((Scene.GetIntendedHeight()/ RewardScene.CARD_WIDTH_TO_HEIGHT)*0.95f,Scene.GetIntendedHeight()*0.95f);
        toolTipImage=new Image(drawable);
        tooltip=new Tooltip<Image>(toolTipImage);
        tooltip.setInstant(true);
        if(frontSideUp())
            addListener(tooltip);
    }

    private boolean frontSideUp() {
        return (flipProcess >= 0.5f) == flipOnClick;
    }

    public boolean isFlipped() {

        return (clicked&&flipProcess>=1);
    }
    public void flip() {

        if(clicked)
            return;
        clicked=true;
        flipProcess=0;
    }
    @Override
    public  void act(float delta)
    {
        super.act(delta);
        if(clicked)
        {
            if(flipProcess<1)
                flipProcess+=delta;
            else
                flipProcess=1;

            if(tooltip!=null&&frontSideUp()&&!getListeners().contains(tooltip,true))
                addListener(tooltip);
            // flipProcess=(float)Gdx.input.getX()/ (float)Gdx.graphics.getWidth();
        }

    }
    @Override
    public void draw (Batch batch, float parentAlpha) {

        applyTransform(batch, computeTransform(batch.getTransformMatrix().cpy()));

        oldProjectionTransform.set(batch.getProjectionMatrix());
        applyProjectionMatrix(batch);


        if(hover)
            batch.setColor(0.5f,0.5f,0.5f,1);

        if(!frontSideUp())
        {
            batch.draw(backTexture,-getWidth()/2,-getHeight()/2,getWidth(),getHeight());
        }
        else
        {
            drawFrontSide(batch);
        }
        batch.setColor(1,1,1,1);
        resetTransform(batch);
        batch.setProjectionMatrix(oldProjectionTransform);
    }

    private void drawFrontSide(Batch batch) {

        float width;
        float x;
        if(flipOnClick)
        {
            width=-getWidth();
            x=-getWidth()/2+getWidth();
        }
        else
        {
            width=getWidth();
            x=-getWidth()/2;
        }

        if (image != null)
            batch.draw(image,x,-getHeight()/2,width,getHeight());
        else
            batch.draw(ImageCache.defaultImage,x,-getHeight()/2,width,getHeight());
        switch (reward.getType())
        {
            case Card:

                break;
            case Gold:
                break;
            case Item:
                break;
        }
    }

    private void applyProjectionMatrix(Batch batch) {

        final Vector3 direction = new Vector3(0, 0, -1);
        final Vector3 up = new Vector3(0, 1, 0);
        //final Vector3 position = new Vector3( getX()+getWidth()/2 , getY()+getHeight()/2, 0);
        final Vector3 position = new Vector3( Scene.GetIntendedWidth() /2f , Scene.GetIntendedHeight()/2f, 0);

        float fov=67;
        Matrix4 projection=new Matrix4();
        Matrix4 view=new Matrix4();
        float hy= Scene.GetIntendedHeight()/2f;
        float a= (float) ((hy)/Math.sin(MathUtils.degreesToRadians*(fov/2f)));
        float height= (float) Math.sqrt((a*a)-(hy*hy));
        position.z=height*1f;
        float far=height*2f;
        float near=height*0.8f;

        float aspect = (float)Scene.GetIntendedWidth() / (float)Scene.GetIntendedHeight();
        projection.setToProjection(Math.abs(near), Math.abs(far), fov, aspect);
        view.setToLookAt(position, position.cpy().add(direction), up);
        Matrix4.mul(projection.val, view.val);

        batch.setProjectionMatrix(projection);
    }



    private final Matrix4 computedTransform = new Matrix4();
    private final Matrix4 oldTransform = new Matrix4();
    private final Matrix4 oldProjectionTransform = new Matrix4();

    protected void applyTransform (Batch batch, Matrix4 transform)
    {
        oldTransform.set(batch.getTransformMatrix());
        batch.setTransformMatrix(transform);
    }

    /** Restores the batch transform to what it was before {@link #applyTransform(Batch, Matrix4)}. Note this causes the batch to
     * be flushed. */
    protected void resetTransform (Batch batch) {
        batch.setTransformMatrix(oldTransform);
    }
    protected Matrix4 computeTransform (Matrix4 worldTransform) {


        float[] val=worldTransform.getValues();
        //val[Matrix4.M32]=0.0002f;
        worldTransform.set(val);
        float originX = this.getOriginX(), originY = this.getOriginY();
        worldTransform.translate(getX()+getWidth()/2 , getY()+getHeight()/2,0);
        if(clicked)
        {
            worldTransform.rotate(0,1,0,180*flipProcess);
        }
        computedTransform.set(worldTransform);
        return computedTransform;



    }

}
