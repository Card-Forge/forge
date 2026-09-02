package forge.util;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import forge.Forge;

public class ShaderUtil implements Disposable {
    public static ShaderUtil instance;
    private ShaderProgram shaderOutline, shaderGrayscale, shaderWarp, shaderUnderwater, shaderNightDay, shaderPixelate,
        shaderRipple, shaderPixelateWarp, shaderChromaticAberration, shaderHueShift, shaderRoundedRect, shaderRoundedRect2,
        shaderNoiseFade, shaderPortal;
    private ShaderUtil() {
        ShaderProgram.pedantic = false;
    }
    public static ShaderUtil getInstance() {
        return instance == null ? instance = new ShaderUtil() : instance;
    }
    public ShaderProgram getShaderOutline() {
        if (shaderOutline == null)
            shaderOutline = new ShaderProgram(Shaders.outlineVert, Shaders.outlineFrag);
        return shaderOutline;
    }

    public ShaderProgram getShaderGrayscale() {
        if (shaderGrayscale == null)
            shaderGrayscale = new ShaderProgram(Shaders.grayscaleVert, Shaders.grayscaleFrag);
        return shaderGrayscale;
    }

    public ShaderProgram getShaderRoundedRect() {
        if (shaderRoundedRect == null)
            shaderRoundedRect = new ShaderProgram(Shaders.vertPixelateShader, Shaders.fragRoundedRect);
        return shaderRoundedRect;
    }

    public ShaderProgram getShaderWarp() {
        if (shaderWarp == null)
            shaderWarp = new ShaderProgram(Shaders.grayscaleVert, Shaders.warpFrag);
        return shaderWarp;
    }
    public ShaderProgram getShaderRipple() {
        if (shaderRipple == null)
            shaderRipple = new ShaderProgram(Shaders.vertPixelateShader, Shaders.fragRipple);
        return shaderRipple;
    }

    public ShaderProgram getShaderRoundedRect2() {
        if (shaderRoundedRect2 == null)
            shaderRoundedRect2 = new ShaderProgram(Shaders.vertPixelateShader, Shaders.fragRoundedRect2);
        return shaderRoundedRect2;
    }

    public ShaderProgram getShaderNoiseFade() {
        if (shaderNoiseFade == null)
            shaderNoiseFade = new ShaderProgram(Shaders.vertPixelateShader, Shaders.fragNoiseFade);
        return shaderNoiseFade;
    }

    public ShaderProgram getShaderPortal() {
        if (shaderPortal == null)
            shaderPortal = new ShaderProgram(Shaders.vertPixelateShader, Shaders.fragPortal);
        return shaderPortal;
    }

    public ShaderProgram getShaderHueShift() {
        if (shaderHueShift == null)
            shaderHueShift = new ShaderProgram(Shaders.vertPixelateShader, Shaders.fragHueShift);
        return shaderHueShift;
    }

    public ShaderProgram getShaderChromaticAberration() {
        if (shaderChromaticAberration == null)
            shaderChromaticAberration = new ShaderProgram(Shaders.vertPixelateShader, Shaders.fragChromaticAbberation);
        return shaderChromaticAberration;
    }

    public ShaderProgram getShaderPixelate() {
        if (shaderPixelate == null)
            shaderPixelate = new ShaderProgram(Shaders.vertPixelateShader, Shaders.fragPixelateShader);
        return shaderPixelate;
    }

    public ShaderProgram getShaderPixelateWarp() {
        if (shaderPixelateWarp == null)
            shaderPixelateWarp = new ShaderProgram(Shaders.vertPixelateShader, Shaders.fragPixelateShaderWarp);
        return shaderPixelateWarp;
    }

    public ShaderProgram getShaderUnderwater() {
        if (shaderUnderwater == null)
            shaderUnderwater = new ShaderProgram(Shaders.grayscaleVert, Shaders.underwaterFrag);
        return shaderUnderwater;
    }

    public ShaderProgram getShaderNightDay() {
        if (shaderNightDay == null)
            shaderNightDay = new ShaderProgram(Shaders.vertexShaderDayNight, Shaders.fragmentShaderDayNight);
        return shaderNightDay;
    }

    @Override
    public void dispose() {
        Forge.safeDispose(shaderOutline, shaderGrayscale, shaderWarp, shaderUnderwater, shaderNightDay, shaderPixelate,
            shaderRipple, shaderPixelateWarp, shaderChromaticAberration, shaderHueShift, shaderRoundedRect,
            shaderRoundedRect2, shaderNoiseFade, shaderPortal);
    }
}
