package forge.ios;

import org.robovm.apple.foundation.NSObject;
import org.robovm.objc.ObjCRuntime;
import org.robovm.objc.annotation.Method;
import org.robovm.objc.annotation.NativeClass;
import org.robovm.objc.annotation.Property;
import org.robovm.rt.bro.annotation.Library;

/**
 * Minimal RoboVM binding to ObjectAL's OpenALManager (compiled into libObjectAL.a).
 *
 * Used to permanently suspend the OpenAL effects engine. Sound effects now play
 * through libGDX Music (AVAudioPlayer) on iOS (see forge.sound.AudioClip), so OpenAL
 * is unused - but libGDX auto-initializes it, and an ACTIVE OpenAL 3D-mixer render
 * cycle beats against the AVAudioPlayer music/effects (mediaserverd) clock, producing
 * a slow periodic music crackle. OALSuspendManager.manuallySuspended alcSuspendContext's
 * the effects context, halting that render cycle so everything is one clock domain.
 * OpenAL-only: AVAudioPlayer music/effects are a separate subsystem and keep playing.
 *
 * The libGDX OALSimpleAudio binding exposes no pause/suspend method, so this binds the
 * underlying ObjC class directly. NOT force-linked in robovm.xml: it is referenced
 * directly from Main.didBecomeActive, so RoboVM reachability keeps it - and adding it
 * to forceLinkClasses would change the config hash, invalidating the whole AOT cache.
 * Verified selectors in libObjectAL.a:
 *   +[OpenALManager sharedInstance]
 *   -[OpenALManager manuallySuspended] / -[OpenALManager setManuallySuspended:]
 */
@Library(Library.INTERNAL)
@NativeClass
public final class OpenALManager extends NSObject {

    static {
        ObjCRuntime.bind(OpenALManager.class);
    }

    @Method
    public native static OpenALManager sharedInstance();

    @Property(selector = "manuallySuspended")
    public native boolean isManuallySuspended();

    @Property(selector = "setManuallySuspended:")
    public native void setManuallySuspended(boolean suspended);
}
