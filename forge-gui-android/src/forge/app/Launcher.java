package forge.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import forge.gui.GuiBase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Launcher extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent main = new Intent(Launcher.this, Main.class);
        main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(main);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                if ("text/plain".equals(type)) {
                    Uri textUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (textUri != null) {
                        try {
                            InputStream in = getContentResolver().openInputStream(textUri);
                            BufferedReader r = new BufferedReader(new InputStreamReader(in));
                            StringBuilder total = new StringBuilder();
                            for (String line; (line = r.readLine()) != null; ) {
                                total.append(line).append('\n');
                            }
                            GuiBase.getInterface().copyToClipboard(total.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            GuiBase.getInterface().copyToClipboard(intent.getStringExtra(Intent.EXTRA_TEXT));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 1500);
        }
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                if ("text/plain".equals(type)) {
                    Uri textUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (textUri != null) {
                        try {
                            InputStream in = getContentResolver().openInputStream(textUri);
                            BufferedReader r = new BufferedReader(new InputStreamReader(in));
                            StringBuilder total = new StringBuilder();
                            for (String line; (line = r.readLine()) != null; ) {
                                total.append(line).append('\n');
                            }
                            GuiBase.getInterface().copyToClipboard(total.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            GuiBase.getInterface().copyToClipboard(intent.getStringExtra(Intent.EXTRA_TEXT));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 1500);
        }
    }
}
