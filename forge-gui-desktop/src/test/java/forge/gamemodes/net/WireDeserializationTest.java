package forge.gamemodes.net;

import io.netty.handler.codec.serialization.ClassResolvers;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * F-01, at the stream rather than at the predicate.
 *
 * <p>{@link WireClassFilterTest} pins which names the allowlist accepts. This
 * pins that {@link CObjectInputStream} actually consults it, on every route a
 * class can enter the stream — including the one that does not carry a class
 * name at all.
 */
public class WireDeserializationTest {

    /**
     * Stand-in for a gadget. Deliberately not a nested class of this test: that
     * would be named {@code forge.…} and allowlisted by the very prefix under
     * test.
     */
    private static Serializable disallowedObject() {
        return new java.io.File("not-on-the-wire");
    }

    /** Serializable handler so the proxy below can itself be serialized. */
    private static final class Handler implements InvocationHandler, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            return null;
        }
    }

    private static byte[] encode(final Object graph) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // The real encoder, so the framing matches what a peer would send.
        try (ObjectOutputStream out = new CObjectOutputStream(bytes, false, null, -1, false)) {
            out.writeObject(graph);
        }
        return bytes.toByteArray();
    }

    private static Object decode(final byte[] frame) throws Exception {
        try (CObjectInputStream in = new CObjectInputStream(
                new ByteArrayInputStream(frame), ClassResolvers.cacheDisabled(null), null)) {
            return in.readObject();
        }
    }

    @Test
    public void testAllowedTrafficStillRoundTrips() throws Exception {
        final HashMap<String, Object> graph = new HashMap<>();
        graph.put("list", new ArrayList<>(java.util.Arrays.asList(1, 2, 3)));
        graph.put("text", "hello");

        final Object decoded = decode(encode(graph));
        Assert.assertEquals(decoded, graph, "Allowlisted traffic must still round-trip");
    }

    /**
     * Nested rather than top-level, which strictly subsumes the simpler case: a
     * filter applied only to the first descriptor would pass this and fail only
     * here.
     */
    @Test
    public void testRejectsAClassNestedInsideAllowedCollections() throws Exception {
        final HashMap<String, Object> graph = new HashMap<>();
        graph.put("innocuous", "text");
        graph.put("payload", disallowedObject());

        try {
            decode(encode(graph));
            Assert.fail("A disallowed class nested inside an allowed map was deserialized");
        } catch (final InvalidClassException expected) {
            Assert.assertTrue(expected.getMessage().contains("java.io.File"), expected.getMessage());
        }
    }

    /**
     * The route that carries no class name: a proxy descriptor reaches
     * {@code resolveProxyClass} alone, so a filter on the other two would let
     * it through — and that is where the best-known chains start.
     */
    @Test
    public void testRejectsDynamicProxies() throws Exception {
        final Runnable proxy = (Runnable) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { Runnable.class }, new Handler());

        final byte[] frame = encode(proxy);
        try {
            decode(frame);
            Assert.fail("A dynamic proxy was deserialized from the wire");
        } catch (final InvalidClassException expected) {
            Assert.assertTrue(expected.getMessage().toLowerCase().contains("proxy"),
                    "Rejection should identify the proxy: " + expected.getMessage());
        }
    }

    /**
     * Lengths and depths are data, not names, so the class filter cannot see
     * them. Both bounds share one pattern string, so dropping a term from it is
     * a silent regression — hence checking two. The reference bound is not
     * checked: tripping it legitimately needs a million-object graph.
     */
    @Test
    public void testRejectsResourceExhaustingFrames() throws Exception {
        // Nesting past the depth bound.
        java.util.List<Object> deep = new ArrayList<>();
        java.util.List<Object> cursor = deep;
        for (int i = 0; i < 150; i++) {
            final java.util.List<Object> next = new ArrayList<>();
            cursor.add(next);
            cursor = next;
        }
        try {
            decode(encode(deep));
            Assert.fail("A frame nested 150 deep was accepted");
        } catch (final InvalidClassException expected) {
            // WireStreamLimits refused it.
        }

        final byte[] frame = encode(new int[] { 1, 2, 3, 4 });
        // Rewrite the declared length to 200 million (~762 MB if allocated) and
        // cut the frame short: nothing past the length is ever read.
        final int lengthAt = frame.length - 4 * 4 - 4;
        final int huge = 200_000_000;
        frame[lengthAt] = (byte) (huge >>> 24);
        frame[lengthAt + 1] = (byte) (huge >>> 16);
        frame[lengthAt + 2] = (byte) (huge >>> 8);
        frame[lengthAt + 3] = (byte) huge;

        try {
            decode(frame);
            Assert.fail("A frame declaring int[" + huge + "] was accepted");
        } catch (final InvalidClassException expected) {
            // WireStreamLimits refused the length before anything was allocated.
        } catch (final Throwable other) {
            // Anything else — EOF from reading elements, or OutOfMemoryError —
            // means the length was accepted and the array already allocated.
            Assert.fail("The declared array was allocated (" + other.getClass().getSimpleName()
                    + "): the wire stream limits are not in place");
        }
    }
}
