package forge.animation;

import forge.Graphics;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.sound.AudioClip;

public enum AbilityEffect {
    LIGHTNING("lightning.gif", "lightning.wav");

    private final String gif, wav;
    private GifAnimation animation;
    private AudioClip soundClip;

    AbilityEffect(String gif0, String wav0) {
        gif = gif0;
        wav = wav0;
    }

    public void start() {
        if (animation == null) {
            animation = new GifAnimation(ForgeConstants.EFFECTS_DIR + gif);
        }
        if (soundClip == null) {
            soundClip = AudioClip.createClip(ForgeConstants.EFFECTS_DIR + wav);
        }
        if (soundClip != null)
            soundClip.play(FModel.getPreferences().getPrefInt(ForgePreferences.FPref.UI_VOL_SOUNDS)/100f);
        animation.start();
    }

    public void draw(Graphics g, float x, float y, float w, float h) {
        if (animation != null) {
            animation.draw(g, x, y, w, h);
        }
    }
}
