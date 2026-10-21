package forge.sound;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.SourceDataLine;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.sound.SoftwareMixer.Voice;

@Test(groups = {"UnitTest"})
public class SoftwareMixerTest {
    private static Field field(String name) throws Exception {
        Field field = SoftwareMixer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object call(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = SoftwareMixer.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static void fill(List<Voice> voices, int[] samples, byte[] output) throws Exception {
        call("fill", new Class<?>[] {List.class, int[].class, byte[].class}, voices, samples, output);
    }

    public void spacesRepeatsOnTheSampleClock() throws Exception {
        @SuppressWarnings("unchecked")
        List<Voice> voices = (List<Voice>) field("voices").get(null);
        // Keep the real mixer asleep while inspecting its queue and rendering into memory.
        synchronized (voices) {
            long cursor = field("frameCursor").getLong(null);
            try {
                byte[] impulse = {1, 0, 1, 0};
                for (int i = 0; i < 3; i++) {
                    SoftwareMixer.play(impulse, 1f, false, 70);
                }
                List<Integer> starts = new ArrayList<>();
                int[] samples = new int[1024];
                byte[] output = new byte[2048];
                for (int block = 0; block < 15; block++) {
                    fill(voices, samples, output);
                    for (int frame = 0; frame < 512; frame++) {
                        if (samples[frame * 2] != 0) {
                            starts.add(block * 512 + frame);
                        }
                    }
                    field("frameCursor").setLong(null, cursor + (block + 1) * 512L);
                }
                Assert.assertEquals(starts, List.of(0, 3087, 6174));
                field("frameCursor").setLong(null, cursor + SoftwareMixer.framesForMillis(1000));
                Voice afterIdle = SoftwareMixer.play(impulse, 1f, false, 70);
                fill(List.of(afterIdle), samples, output);
                Assert.assertEquals(samples[0], 1);
            } finally {
                call("dropVoices", new Class<?>[0]);
                field("frameCursor").setLong(null, cursor);
            }
        }
    }

    public void cancelledRepeatsDoNotDelayNewPlayback() throws Exception {
        Object voices = field("voices").get(null);
        synchronized (voices) {
            try {
                byte[] pcm = {1, 0, 1, 0};
                List<Voice> queued = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    queued.add(SoftwareMixer.play(pcm, 1f, false, 70));
                }
                // Keep the first voice: replacing the cancelled tail should occupy its next slot.
                for (int i = 1; i < queued.size(); i++) {
                    queued.get(i).stop();
                }
                Voice replacement = SoftwareMixer.play(pcm, 1f, false, 70);
                Field silentFrames = Voice.class.getDeclaredField("silentFrames");
                silentFrames.setAccessible(true);
                Assert.assertEquals(silentFrames.getInt(replacement), SoftwareMixer.framesForMillis(70));

                queued.get(0).stop();
                replacement.stop();
                Voice restarted = SoftwareMixer.play(pcm, 1f, false, 70);
                Assert.assertEquals(silentFrames.getInt(restarted), 0);
                int[] samples = new int[2];
                fill(List.of(restarted), samples, new byte[4]);
                Assert.assertEquals(samples, new int[] {1, 1});
            } finally {
                call("dropVoices", new Class<?>[0]);
            }
        }
    }

    public void releasesFailedOutputEvenWhenStopThrows() throws Exception {
        Object voices = field("voices").get(null);
        synchronized (voices) {
            long retry = field("lineUnavailableUntil").getLong(null);
            List<String> operations = new ArrayList<>();
            SourceDataLine output = (SourceDataLine) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[] {SourceDataLine.class}, (proxy, method, args) -> {
                        operations.add(method.getName());
                        if (method.getName().equals("stop")) {
                            throw new IllegalStateException("Output disconnected");
                        }
                        return null;
                    });
            try {
                Voice pending = SoftwareMixer.play(new byte[] {1, 0, 1, 0}, 1f, false, 70);
                field("line").set(null, output);
                call("handleLineFailure", new Class<?>[] {Exception.class}, new IllegalStateException("Write failed"));
                Assert.assertFalse(pending.isPlaying());
                Assert.assertNull(field("line").get(null));
                Assert.assertEquals(operations, List.of("stop", "flush", "close"));
                Assert.assertTrue(field("lineUnavailableUntil").getLong(null) > System.currentTimeMillis());
            } finally {
                call("dropVoices", new Class<?>[0]);
                field("line").set(null, null);
                field("lineUnavailableUntil").setLong(null, retry);
            }
        }
    }

}
