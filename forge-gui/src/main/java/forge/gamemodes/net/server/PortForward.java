package forge.gamemodes.net.server;

import forge.gui.GuiBase;
import forge.util.IHasForgeLog;
import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.model.meta.Device;
import org.jupnp.registry.Registry;
import org.jupnp.support.igd.PortMappingListener;
import org.jupnp.support.model.PortMapping;
import org.jupnp.util.SpecificationViolationReporter;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/** Asks the router over UPnP to forward a TCP port to this machine. close() removes the forwarding. */
public final class PortForward implements IHasForgeLog {
    /** How long to wait for a router to confirm. After that the forwarding counts as failed. */
    private static final long ANSWER_MILLIS = 5000;

    private final int port;
    private UpnpService service;

    public PortForward(final int port) {
        this.port = port;
    }

    /** Starts the request without waiting for the answer. result is called once: true if a router accepted the forwarding, false if none did. */
    public synchronized void open(final Consumer<Boolean> result) {
        close();
        try {
            final PortMapping mapping = new PortMapping(port, FServerManager.getLocalAddress(), PortMapping.Protocol.TCP, "Forge");
            // Gateways routinely break the UPnP spec in ways jupnp tolerates; don't log each one
            SpecificationViolationReporter.disableReporting();
            service = new UpnpServiceImpl(GuiBase.getInterface().getUpnpPlatformService());
            service.startup();
            final Listener listener = new Listener(mapping, result);
            service.getRegistry().addListener(listener);
            service.getControlPoint().search();
            new Timer("upnp-timeout", true).schedule(new TimerTask() {
                @Override
                public void run() {
                    if (listener.finish(false)) {
                        netLog.warn("UPnP: no gateway confirmed a mapping for port {} within {} seconds", port, ANSWER_MILLIS / 1000);
                    }
                }
            }, ANSWER_MILLIS);
        } catch (LinkageError | Exception e) {
            // jupnp's classes do not load on iOS/MobiVM. Catching the LinkageError lets hosting continue there without UPnP
            netLog.error(e, "UPnP mapping unavailable");
            result.accept(false);
        }
    }

    public synchronized void close() {
        if (service == null) {
            return;
        }
        try {
            // Shutting down the service also removes its port mappings from the router
            service.shutdown();
        } catch (Exception | AssertionError e) {
            // The JDK wraps a failed multicast leave in AssertionError, which jupnp doesn't catch
            netLog.debug("UPnP shutdown incomplete: {}", e.toString());
        }
        service = null;
    }

    // The superclass maps the port synchronously inside deviceAdded, so the outcome is known when super.deviceAdded returns
    private static final class Listener extends PortMappingListener {
        private final Consumer<Boolean> result;
        private boolean finished;

        Listener(final PortMapping mapping, final Consumer<Boolean> result) {
            super(mapping);
            this.result = result;
        }

        @Override
        public synchronized void deviceAdded(final Registry registry, final Device device) {
            super.deviceAdded(registry, device);
            if (!activePortMappings.isEmpty()) {
                finish(true);
            }
        }

        @Override
        protected void handleFailureMessage(final String message) {
            super.handleFailureMessage(message);
            finish(false);
        }

        /** Reports the outcome once. Returns true if this call was the one that reported it. */
        synchronized boolean finish(final boolean accepted) {
            if (finished) {
                return false;
            }
            finished = true;
            result.accept(accepted);
            return true;
        }
    }
}
