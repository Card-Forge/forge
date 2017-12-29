package forge.app;

import android.app.Activity;
import android.os.Bundle;

public class Exiter extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        finish();

        //ensure process fully killed
        System.exit(0);
    }
}
