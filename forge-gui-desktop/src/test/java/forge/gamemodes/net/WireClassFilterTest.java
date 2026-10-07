package forge.gamemodes.net;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * F-01: the multiplayer wire must name only classes the protocol carries. Both
 * directions matter — measured traffic has to keep working, and the shapes an
 * attacker reaches for must not.
 */
public class WireClassFilterTest {

    /** Every entry was observed in instrumented wire traffic. */
    private static final String[] ALLOWED = {
            "forge.game.card.CardView",
            "forge.gamemodes.net.event.LoginEvent",
            "forge.trackable.TrackableProperty",
            "forge.item.PaperCard$PaperCardFlags",
            "java.util.ArrayList", "java.util.HashMap", "java.util.EnumMap",
            "java.util.EnumSet$SerializationProxy",
            "java.util.Collections$UnmodifiableSet",
            "java.util.concurrent.ConcurrentHashMap$Segment",
            "java.util.concurrent.locks.ReentrantLock$NonfairSync",
            "java.lang.Integer", "java.lang.Number", "java.lang.Enum",
            "java.lang.String$CaseInsensitiveComparator",
            "com.google.common.collect.HashMultimap",
            "com.google.common.collect.ImmutableSet$SerializedForm",
            // Array forms, including nested and multi-dimensional.
            "[B", "[I", "[Ljava.lang.Object;",
            "[Ljava.util.concurrent.ConcurrentHashMap$Segment;",
            // The filter's one deliberate soft spot: getChoices ships a lambda.
            "java.lang.invoke.SerializedLambda",
    };

    private static final String[] REJECTED = {
            // The classic gadget catalogue.
            "org.apache.commons.collections.functors.InvokerTransformer",
            "org.apache.commons.collections4.functors.InvokerTransformer",
            "org.apache.commons.beanutils.BeanComparator",
            "org.codehaus.groovy.runtime.ConvertedClosure",
            "com.sun.rowset.JdbcRowSetImpl",
            "org.springframework.beans.factory.ObjectFactory",
            "com.mchange.v2.c3p0.PoolBackedDataSource",
            "bsh.Interpreter",
            "com.thoughtworks.xstream.XStream",
            // java.lang is admitted by simple name, so deeper packages stay out —
            // java.lang.reflect.Proxy starts the InvocationHandler chains.
            "java.lang.reflect.Proxy",
            "java.lang.reflect.annotation.AnnotationInvocationHandler",
            "java.lang.Runtime", "java.lang.ProcessBuilder",
            "javax.management.BadAttributeValueExpException",
            "javax.naming.InitialContext",
            "sun.reflect.annotation.AnnotationInvocationHandler",
            "java.rmi.registry.Registry", "java.net.URL",
            // An array of a forbidden class matches no prefix in its raw form,
            // so it must be judged by element type rather than waved through.
            "[Lorg.apache.commons.collections.Transformer;",
            "[[Ljavax.management.BadAttributeValueExpException;",
            "[Ljava.lang.reflect.Proxy;",
            // Prefixes must match on the package boundary, not as substrings.
            "evil.forge.Gadget", "notjava.util.ArrayList", "forgery.Gadget",
            null, "",
    };

    @Test
    public void testAllowsMeasuredWireTraffic() {
        for (final String name : ALLOWED) {
            Assert.assertTrue(WireClassFilter.isAllowed(name), name + " was measured on the wire");
        }
    }

    @Test
    public void testRejectsEverythingElse() {
        for (final String name : REJECTED) {
            Assert.assertFalse(WireClassFilter.isAllowed(name), name + " must be rejected");
        }
    }
}
