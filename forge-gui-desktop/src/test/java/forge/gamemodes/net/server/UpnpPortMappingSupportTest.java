package forge.gamemodes.net.server;

import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.BooleanDatatype;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.StringDatatype;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.UnsignedIntegerFourBytesDatatype;
import org.jupnp.model.types.UnsignedIntegerTwoBytes;
import org.jupnp.model.types.UnsignedIntegerTwoBytesDatatype;
import org.jupnp.support.igd.PortMappingListener;
import org.jupnp.support.model.PortMapping;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class UpnpPortMappingSupportTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void acceptsIgdVersionTwo() {
        final Device gateway = Mockito.mock(Device.class);
        final Device connection = Mockito.mock(Device.class);
        final Service service = Mockito.mock(Service.class);
        Mockito.when(gateway.getType()).thenReturn(new UDADeviceType("InternetGatewayDevice", 2));
        Mockito.when(gateway.findDevices(PortMappingListener.CONNECTION_DEVICE_TYPE)).thenReturn(new Device[] {connection});
        Mockito.when(connection.findService(PortMappingListener.IP_SERVICE_TYPE)).thenReturn(service);

        Assert.assertSame(UpnpPortMappingSupport.findConnectionService(gateway), service);
    }

    @Test
    public void createsAddInvocationForUi4PortArguments() {
        final ActionInvocation<?> invocation = UpnpPortMappingSupport.createInvocation(service("AddPortMapping", new UnsignedIntegerFourBytesDatatype()), mapping(), true);

        Assert.assertTrue(invocation.getInput("NewExternalPort").getValue() instanceof UnsignedIntegerFourBytes);
        Assert.assertTrue(invocation.getInput("NewInternalPort").getValue() instanceof UnsignedIntegerFourBytes);
    }

    @Test
    public void createsDeleteInvocationForStandardUi2PortArgument() {
        final ActionInvocation<?> invocation = UpnpPortMappingSupport.createInvocation(
                service("DeletePortMapping", new UnsignedIntegerTwoBytesDatatype()), mapping(), false);

        Assert.assertTrue(invocation.getInput("NewExternalPort").getValue()
                instanceof UnsignedIntegerTwoBytes);
    }

    private PortMapping mapping() {
        return new PortMapping(36743, "192.168.1.99", PortMapping.Protocol.TCP, "Forge");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Service<?, ?> service(final String actionName, final Datatype<?> portDatatype) {
        final Service service = Mockito.mock(Service.class);
        final Action action = Mockito.mock(Action.class);
        final Map<String, Datatype<?>> datatypes = Map.of(
        "NewExternalPort", portDatatype,
        "NewProtocol", new StringDatatype(),
        "NewInternalClient", new StringDatatype(),
        "NewInternalPort", portDatatype,
        "NewLeaseDuration", new UnsignedIntegerFourBytesDatatype(),
        "NewEnabled", new BooleanDatatype(),
        "NewPortMappingDescription", new StringDatatype()
    );

        Mockito.when(service.getAction(actionName)).thenReturn(action);
        Mockito.when(action.getInputArgument(Mockito.anyString())).thenAnswer(call -> {
            final String name = call.getArgument(0);
            final ActionArgument argument = Mockito.mock(ActionArgument.class);
            Mockito.when(argument.getName()).thenReturn(name);
            Mockito.when(argument.getDatatype()).thenReturn(datatypes.get(name));
            return argument;
        });
        return service;
    }
}
