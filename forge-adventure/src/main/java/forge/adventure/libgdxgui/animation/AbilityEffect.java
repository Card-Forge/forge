package forge.adventure.libgdxgui.animation;

import forge.adventure.libgdxgui.Graphics;
import forge.adventure.libgdxgui.sound.AudioClip;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

public enum AbilityEffect {
    LIGHTNING("lightning.gif", "lightning.wav");

    private final String gif, wav;
    private forge.adventure.libgdxgui.animation.GifAnimation animation;
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
        soundClip.play(FModel.getPreferences().getPrefInt(ForgePreferences.FPref.UI_VOL_SOUNDS)/100f);
        animation.start();
    }

    public void draw(Graphics g, float x, float y, float w, float h) {
        if (animation != null) {
            animation.draw(g, x, y, w, h);
        }
    }
}
