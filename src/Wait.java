import forge.error.ErrorViewer;


public class Wait {
    public static void delay() {
        try {
            Thread.sleep(1000);
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Wait : delay() " + ex);
        }
    }
}
