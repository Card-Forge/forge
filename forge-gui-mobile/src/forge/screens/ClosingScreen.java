package forge.screens;

import com.badlogic.gdx.graphics.Color;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.sound.SoundSystem;
import forge.toolbox.FContainer;
import forge.toolbox.FOverlay;

public class ClosingScreen extends FContainer {
    private BGAnimation bgAnimation;
    private StaticAnimation staticAnimation;
    private boolean restart = false;
    private boolean drawStatic = false;

    public ClosingScreen(boolean restart0) {
        bgAnimation = new BGAnimation();
        staticAnimation = new StaticAnimation();
        restart = restart0;
    }

    @Override
    protected void doLayout(float width, float height) {

    }

    private class StaticAnimation extends ForgeAnimation {
        float DURATION = 0.8f;
        private float progress = 0;

        public void drawBackgroud(Graphics g) {
            float percentage = progress / DURATION;
            float oldAlpha = g.getfloatAlphaComposite();
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            try {
                //fade out volume
                SoundSystem.instance.fadeModifier(1-percentage);
            } catch (Exception e) {
                e.printStackTrace();
            }
            g.fillRect(Color.BLACK, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
            g.setAlphaComposite(1-percentage);
            g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
            g.setAlphaComposite(oldAlpha);
            float xmod = Forge.getScreenHeight() > 2000 ? 1.5f : 1f;
            if (FSkin.hdLogo != null) {
                g.drawImage(FSkin.hdLogo, Forge.getScreenWidth()/2 - (FSkin.hdLogo.getWidth()*xmod)/2, Forge.getScreenHeight()/2 - (FSkin.hdLogo.getHeight()*xmod)/2, FSkin.hdLogo.getWidth()*xmod, FSkin.hdLogo.getHeight()*xmod);
            } else {
                g.drawImage(FSkinImage.LOGO,Forge.getScreenWidth()/2 - (FSkinImage.LOGO.getWidth()*xmod)/2, Forge.getScreenHeight()/2 - (FSkinImage.LOGO.getHeight()*xmod)/1.5f, FSkinImage.LOGO.getWidth()*xmod, FSkinImage.LOGO.getHeight()*xmod);
            }
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            if (restart)
                Forge.getDeviceAdapter().restart();
            else
                Forge.getDeviceAdapter().exit();

        }
    }
    private class BGAnimation extends ForgeAnimation {
        float DURATION = 0.6f;
        private float progress = 0;

        public void drawBackground(Graphics g) {
            float percentage = progress / DURATION;
            float oldAlpha = g.getfloatAlphaComposite();
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            g.fillRect(Color.BLACK, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
            g.setAlphaComposite(percentage);
            g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
            g.setAlphaComposite(oldAlpha);
            float xmod = Forge.getScreenHeight() > 2000 ? 1.5f : 1f;
            xmod *= 21-(20*percentage);
            if (FSkin.hdLogo != null) {
                g.drawImage(FSkin.hdLogo, Forge.getScreenWidth()/2 - (FSkin.hdLogo.getWidth()*xmod)/2, Forge.getScreenHeight()/2 - (FSkin.hdLogo.getHeight()*xmod)/2, FSkin.hdLogo.getWidth()*xmod, FSkin.hdLogo.getHeight()*xmod);
            } else {
                g.drawImage(FSkinImage.LOGO,Forge.getScreenWidth()/2 - (FSkinImage.LOGO.getWidth()*xmod)/2, Forge.getScreenHeight()/2 - (FSkinImage.LOGO.getHeight()*xmod)/1.5f, FSkinImage.LOGO.getWidth()*xmod, FSkinImage.LOGO.getHeight()*xmod);
            }
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            drawStatic = true;
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        //fix overlay showing on closing screen animation
        FOverlay.hideAll();
        if (drawStatic) {
            staticAnimation.start();
            staticAnimation.drawBackgroud(g);
            return;
        }
        bgAnimation.start();
        bgAnimation.drawBackground(g);
    }
}
