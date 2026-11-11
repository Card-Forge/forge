package forge.gamemodes.replay;

import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.event.HeartbeatEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The recording file format: what {@link ReplayRecorder} writes must come back through {@link ReplayReader} and
 * decode with the same codec a netplay client uses, and a file cut short must still play up to its last whole frame.
 * These need no running game; the game-side wiring is exercised by playing a match.
 */
public class ReplayRecordingShould {
    private File file;

    @BeforeMethod
    public void createFile() throws IOException {
        file = File.createTempFile("replay-test", ReplayStore.EXTENSION);
    }

    @AfterMethod
    public void deleteFile() {
        file.delete();
    }

    private void record(final int frames) throws IOException {
        final ReplayRecorder recorder = new ReplayRecorder(file, "Alice vs Bob");
        for (int i = 0; i < frames; i++) {
            recorder.send(new HeartbeatEvent());
        }
        recorder.close();
        assertThat(recorder.hasFailed()).isFalse();
    }

    private static List<byte[]> readAll(final File f) throws IOException {
        final List<byte[]> payloads = new ArrayList<>();
        try (ReplayReader reader = ReplayReader.open(f)) {
            while (reader.next()) {
                payloads.add(reader.getPayload());
            }
        }
        return payloads;
    }

    @Test
    public void keepTheTitleAndEveryFrameInOrder() throws IOException {
        record(3);

        try (ReplayReader reader = ReplayReader.open(file)) {
            assertThat(reader.getTitle()).isEqualTo("Alice vs Bob");
            int count = 0;
            int lastTime = -1;
            while (reader.next()) {
                assertThat(reader.getTimeMs()).isGreaterThanOrEqualTo(lastTime);
                lastTime = reader.getTimeMs();
                count++;
            }
            assertThat(count).isEqualTo(3);
        }
    }

    @Test
    public void decodeWithTheNetplayDecoderAsTheOriginalMessages() throws IOException {
        record(2);

        final EmbeddedChannel channel = new EmbeddedChannel(
                new CompatibleObjectDecoder(16 * 1024 * 1024, ClassResolvers.cacheDisabled(null)));
        for (final byte[] payload : readAll(file)) {
            // the file keeps frames without the length prefix the wire adds
            final ByteBuf frame = Unpooled.buffer(payload.length + 4);
            frame.writeInt(payload.length).writeBytes(payload);
            channel.writeInbound(frame);
        }

        assertThat((Object) channel.readInbound()).isInstanceOf(HeartbeatEvent.class);
        assertThat((Object) channel.readInbound()).isInstanceOf(HeartbeatEvent.class);
        assertThat((Object) channel.readInbound()).isNull();
        channel.finishAndReleaseAll();
    }

    @Test
    public void playUpToTheLastWholeFrameWhenTheFileIsCutShort() throws IOException {
        record(3);
        final byte[] bytes = Files.readAllBytes(file.toPath());
        Files.write(file.toPath(), Arrays.copyOf(bytes, bytes.length - 3)); // the app died mid-write

        assertThat(readAll(file)).hasSize(2);
    }

    @Test
    public void refuseAFileThatIsNotAReplay() throws IOException {
        Files.write(file.toPath(), "definitely not a replay".getBytes());

        assertThatThrownBy(() -> ReplayReader.open(file)).isInstanceOf(IOException.class);
    }

    @Test
    public void notWaitForAnswersBecauseNobodyIsListening() throws IOException {
        final ReplayRecorder recorder = new ReplayRecorder(file, "title");

        assertThat(recorder.sendAndWait(null)).isNull();
        recorder.close();
    }
}
