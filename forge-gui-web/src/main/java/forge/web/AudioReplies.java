package forge.web;

import com.google.common.primitives.Longs;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static forge.web.WebServer.KEEP_A_DAY;
import static forge.web.WebServer.KEEP_FOREVER;
import static forge.web.WebServer.notFound;
import static forge.web.WebServer.redirect;
import static forge.web.WebServer.respond;
import static forge.web.WebServer.send;

/** Answers the requests for sound effects and music, including the byte ranges a media player asks for. */
final class AudioReplies {
    private static final Pattern BYTE_RANGE = Pattern.compile("bytes=(\\d*)-(\\d*)");
    /** The most bytes that one reply to an audio range request carries: about a minute of music at 128 kbps. */
    private static final int AUDIO_SLICE = 1 << 20;

    private AudioReplies() {
    }

    /** A playlist request gets a redirect to one of its tracks by name. The shuffle then stays on the server, and each track is downloaded once. */
    static void serve(final ChannelHandlerContext ctx, final boolean sound, final String name, final String track,
            final String range, final String ifNoneMatch) throws IOException {
        if (sound) {
            respondAudio(ctx, isFileName(name) ? SoundSystem.instance.getSoundResource(name) : null, range, KEEP_A_DAY, ifNoneMatch);
            return;
        }
        final boolean menu = "menu".equals(name);
        final MusicPlaylist playlist = menu ? MusicPlaylist.MENUS : MusicPlaylist.MATCH;
        if (track == null) {
            final String chosen = playlist.getRandomFilename();
            if (chosen == null) {
                notFound(ctx);
            } else {
                redirect(ctx, "/music?name=" + (menu ? "menu" : "match") + "&track="
                        + URLEncoder.encode(new File(chosen).getName(), StandardCharsets.UTF_8));
            }
            return;
        }
        final File dir = SoundSystem.findMusicDirectory(playlist);
        respondAudio(ctx, dir != null && isFileName(track) ? new File(dir, track) : null, range, KEEP_FOREVER, ifNoneMatch);
    }

    private static boolean isFileName(final String name) {
        return name != null && !name.isEmpty() && !name.contains("/") && !name.contains("\\") && !name.contains("..");
    }

    /** Answers a range one slice at a time, because a media player asks for the rest of a track and often abandons the request. */
    private static void respondAudio(final ChannelHandlerContext ctx, final File file, final String range, final String cacheControl,
            final String ifNoneMatch) throws IOException {
        if (file == null || !file.isFile()) {
            notFound(ctx);
            return;
        }
        final long length = file.length();
        final String etag = "\"" + Long.toHexString(file.lastModified()) + "-" + Long.toHexString(length) + "\"";
        // The browser checks its copy before it asks for a range, so an unchanged file is answered before the range is read
        if (etag.equals(ifNoneMatch)) {
            respond(ctx, HttpResponseStatus.NOT_MODIFIED, new byte[0], "audio/mpeg", null, cacheControl, etag);
            return;
        }
        long from = 0;
        long to = length - 1;
        // A Range header with several ranges, or one this cannot parse, gets the whole file
        final Matcher m = range == null ? null : BYTE_RANGE.matcher(range.trim());
        final boolean partial = m != null && m.matches() && !(m.group(1).isEmpty() && m.group(2).isEmpty());
        if (partial) {
            final Long first = m.group(1).isEmpty() ? null : Longs.tryParse(m.group(1));
            final Long last = m.group(2).isEmpty() ? null : Longs.tryParse(m.group(2));
            if (m.group(1).isEmpty()) {
                from = last == null ? length : Math.max(0, length - last);
            } else {
                from = first == null ? length : first;
                if (last != null) {
                    to = Math.min(to, last);
                }
            }
            to = Math.min(to, from + AUDIO_SLICE - 1);
        }
        final FullHttpResponse resp;
        if (partial && from > to) {
            resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
            resp.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes */" + length);
        } else {
            final byte[] body = new byte[(int) (to - from + 1)];
            try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
                in.seek(from);
                in.readFully(body);
            }
            resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    partial ? HttpResponseStatus.PARTIAL_CONTENT : HttpResponseStatus.OK, Unpooled.wrappedBuffer(body));
            if (partial) {
                resp.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes " + from + "-" + to + "/" + length);
            }
        }
        resp.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, file.getName().toLowerCase(Locale.ROOT).endsWith(".wav") ? "audio/wav" : "audio/mpeg")
                .set(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES)
                .set(HttpHeaderNames.CACHE_CONTROL, cacheControl)
                .set(HttpHeaderNames.ETAG, etag)
                .setInt(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
        send(ctx, resp);
    }
}
