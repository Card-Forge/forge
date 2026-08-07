package forge.gamemodes.net.server;

import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.jupnp.support.igd.PortMappingListener;
import org.jupnp.support.model.PortMapping;

/** Compatibility helpers for UPnP Internet gateway port mappings. */
final class UpnpPortMappingSupport {
    private UpnpPortMappingSupport() {
    }

    /** Finds the first compatible WAN connection service on an IGD v1 or v2 device. */
    static Service<?, ?> findConnectionService(final Device<?, ?, ?> device) {
        if (device == null || device.getType() == null
                || !device.getType().implementsVersion(PortMappingListener.IGD_DEVICE_TYPE)) {
            return null;
        }

        final Device<?, ?, ?>[] connectionDevices = device.findDevices(PortMappingListener.CONNECTION_DEVICE_TYPE);
        if (connectionDevices.length == 0) {
            return null;
        }

        final Service<?, ?> ipService = connectionDevices[0].findService(PortMappingListener.IP_SERVICE_TYPE);
        return ipService != null
            ? ipService
            : connectionDevices[0].findService(PortMappingListener.PPP_SERVICE_TYPE);
    }

    /** Creates an add or delete action using the argument types advertised by the router. */
    static ActionInvocation<?> createInvocation(final Service<?, ?> service, final PortMapping mapping, final boolean add) {
        final String actionName = add ? "AddPortMapping" : "DeletePortMapping";
        final ActionInvocation<?> invocation = new ActionInvocation<>(service.getAction(actionName));

        // Strings support both the standard ui2 port type and routers that advertise ui4.
        invocation.setInput("NewExternalPort", mapping.getExternalPort().toString());
        invocation.setInput("NewProtocol", mapping.getProtocol().toString());
        if (mapping.hasRemoteHost()) {
            invocation.setInput("NewRemoteHost", mapping.getRemoteHost());
        }

        if (add) {
            invocation.setInput("NewInternalClient", mapping.getInternalClient());
            invocation.setInput("NewInternalPort", mapping.getInternalPort().toString());
            invocation.setInput("NewLeaseDuration", mapping.getLeaseDurationSeconds().toString());
            invocation.setInput("NewEnabled", mapping.isEnabled());
            if (mapping.hasDescription()) {
                invocation.setInput("NewPortMappingDescription", mapping.getDescription());
            }
        }
        
        return invocation;
    }
}
