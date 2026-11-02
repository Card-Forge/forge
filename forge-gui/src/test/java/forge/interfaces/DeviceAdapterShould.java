package forge.interfaces;

import org.mockito.Mockito;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class DeviceAdapterShould {

    @Test
    public void testDefaultAdapterIsNotFoldable() {
        IDeviceAdapter adapter = Mockito.mock(IDeviceAdapter.class, Mockito.CALLS_REAL_METHODS);
        AssertJUnit.assertFalse(adapter.isFoldable());
        AssertJUnit.assertFalse(adapter.isFoldableUnfolded());
    }

    @Test
    public void testFoldableAdapterReportsStateCorrectly() {
        IDeviceAdapter adapter = Mockito.mock(IDeviceAdapter.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(adapter.isFoldable()).thenReturn(true);
        Mockito.when(adapter.isFoldableUnfolded()).thenReturn(false);

        AssertJUnit.assertTrue(adapter.isFoldable());
        AssertJUnit.assertFalse(adapter.isFoldableUnfolded());

        Mockito.when(adapter.isFoldableUnfolded()).thenReturn(true);
        AssertJUnit.assertTrue(adapter.isFoldable());
        AssertJUnit.assertTrue(adapter.isFoldableUnfolded());
    }
}
