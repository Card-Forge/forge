/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.download;

import com.esotericsoftware.minlog.Log;
import forge.FThreads;
import forge.GuiBase;
import forge.UiCommand;
import forge.error.BugReporter;
import forge.interfaces.IButton;
import forge.interfaces.IProgressBar;
import forge.interfaces.ITextField;
import forge.util.FileUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("serial")
public abstract class GuiDownloadService implements Runnable {
    public static final Proxy.Type[] TYPES = Proxy.Type.values();

    //Components passed from GUI component displaying download
    private ITextField txtAddress;
    private ITextField txtPort;
    protected IProgressBar progressBar;
    private IButton btnStart;
    private UiCommand cmdClose;
    private Runnable onUpdate;

    private final UiCommand cmdStartDownload = new UiCommand() {
        @Override
        public void run() {
            //invalidate image cache so newly downloaded images will be loaded
            GuiBase.getInterface().clearImageCache();
            FThreads.invokeInBackgroundThread(GuiDownloadService.this);
            btnStart.setEnabled(false);
        }
    };

    // Proxy info
    private int type;

    // Progress variables
    private Map<String, String> files; // local path -> url
    protected boolean cancel;
    private final long[] times = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private int tptr = 0;
    private int skipped = 0;
    private long lTime = System.currentTimeMillis();

    protected GuiDownloadService() {
    }

    public void initialize(ITextField txtAddress0, ITextField txtPort0, IProgressBar progressBar0, IButton btnStart0, UiCommand cmdClose0, final Runnable onReadyToStart, Runnable onUpdate0) {
        txtAddress = txtAddress0;
        txtPort = txtPort0;
        progressBar = progressBar0;
        btnStart = btnStart0;
        cmdClose = cmdClose0;
        onUpdate = onUpdate0;

        String startOverrideDesc = getStartOverrideDesc();
        if (startOverrideDesc == null) {
            // Free up the EDT by assembling card list on a background thread
            FThreads.invokeInBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        files = getNeededFiles();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    FThreads.invokeInEdtLater(new Runnable() {
                        @Override
                        public void run() {
                            if (onReadyToStart != null) {
                                onReadyToStart.run();
                            }
                            readyToStart();
                        }
                    });
                }
            });
        }
        else {
            //handle special case of zip service
            if (onReadyToStart != null) {
                onReadyToStart.run();
            }
            progressBar.setDescription("Click \"Start\" to download and extract " + startOverrideDesc);
            btnStart.setCommand(cmdStartDownload);
            btnStart.setEnabled(true);

            FThreads.invokeInEdtLater(new Runnable() {
                @Override
                public void run() {
                    btnStart.requestFocusInWindow();
                }
            });
        }
    }

    protected String getStartOverrideDesc() {
        return null;
    }

    private void readyToStart() {
        if (files.isEmpty()) {
            progressBar.setDescription("All items have been downloaded.");
            btnStart.setText("OK");
            btnStart.setCommand(cmdClose);
        }
        else {
            progressBar.setMaximum(files.size());
            progressBar.setDescription(files.size() == 1 ? "1 item found." : files.size() + " items found.");
            //for(Entry<String, String> kv : cards.entrySet()) System.out.printf("Will get %s from %s%n", kv.getKey(), kv.getValue());
            btnStart.setCommand(cmdStartDownload);
        }
        btnStart.setEnabled(true);

        FThreads.invokeInEdtLater(new Runnable() {
            @Override
            public void run() {
                btnStart.requestFocusInWindow();
            }
        });
    }

    public void setType(int type0) {
        type = type0;
    }

    public void setCancel(boolean cancel0) {
        cancel = cancel0;
    }

    protected final int getAverageTimePerObject() {
        int numNonzero = 10;

        if (tptr > 9) {
            tptr = 0;
        }

        times[tptr] = System.currentTimeMillis() - lTime;
        lTime = System.currentTimeMillis();

        int tTime = 0;
        for (int i = 0; i < 10; i++) {
            tTime += times[i];
            if (times[i] == 0) {
                numNonzero--;
            }
        }

        tptr++;
        return tTime / Math.max(1, numNonzero);
    }

    private void update(final int count, final File dest) {
        FThreads.invokeInEdtLater(new Runnable() {
            @Override
            public void run() {
                if (onUpdate != null) {
                    onUpdate.run();
                }

                final StringBuilder sb = new StringBuilder();

                final int a = getAverageTimePerObject();

                if (count != files.size()) {
                    sb.append(count).append("/").append(files.size()).append(" - ");

                    long t2Go = (files.size() - count) * a;

                    if (t2Go > 3600000) {
                        sb.append(String.format("%02d:", t2Go / 3600000));
                        t2Go = t2Go % 3600000;
                    }
                    if (t2Go > 60000) {
                        sb.append(String.format("%02d:", t2Go / 60000));
                        t2Go = t2Go % 60000;
                    } else {
                        sb.append("00:");
                    }

                    sb.append(String.format("%02d remaining.", t2Go / 1000));
                }
                else {
                    sb.append(String.format("%d of %d items finished! Skipped " + skipped + " items. Please close!",
                            count, files.size()));
                    finish();
                }

                progressBar.setValue(count);
                progressBar.setDescription(sb.toString());
                System.out.println(count + "/" + files.size() + " - " + dest);
            }
        });
    }

    protected void finish() {
        btnStart.setText("OK");
        btnStart.setCommand(cmdClose);
        btnStart.setEnabled(true);
        btnStart.requestFocusInWindow();
    }

    @Override
    public void run() {
        GuiBase.getInterface().preventSystemSleep(true); //prevent system from going into sleep mode while downloading

        Proxy p = getProxy();

        int bufferLength;
        int iCard = 0;
        byte[] buffer = new byte[1024];

        for (Entry<String, String> kv : files.entrySet()) {
            if (cancel) { break; }

            String url = kv.getValue();
            final File fileDest = new File(kv.getKey());
            final File base = fileDest.getParentFile();

            FileOutputStream fos = null;
            try {
                // test for folder existence
                if (!base.exists() && !base.mkdir()) { // create folder if not found
                    System.out.println("Can't create folder" + base.getAbsolutePath());
                }

                URL imageUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection(p);
                // don't allow redirections here -- they indicate 'file not found' on the server
                conn.setInstanceFollowRedirects(false);
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    conn.disconnect();
                    System.out.println(url);
                    System.out.println("Skipped Download for: " + fileDest.getPath());
                    update(++iCard, fileDest);
                    skipped++;
                    continue;
                }

                fos = new FileOutputStream(fileDest);
                InputStream inputStream = conn.getInputStream();
                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fos.write(buffer, 0, bufferLength);
                }
            }
            catch (final ConnectException ce) {
                System.out.println("Connection refused for url: " + url);
            }
            catch (final MalformedURLException mURLe) {
                System.out.println("Error - possibly missing URL for: " + fileDest.getName());
            }
            catch (final FileNotFoundException fnfe) {
                String formatStr = "Error - the LQ picture %s could not be found on the server. [%s] - %s";
                System.out.println(String.format(formatStr, fileDest.getName(), url, fnfe.getMessage()));
            }
            catch (final Exception ex) {
                Log.error("LQ Pictures", "Error downloading pictures", ex);
            }
            finally {
                if (fos != null) {
                    try {
                        fos.close();
                    }
                    catch (IOException e) {
                        System.out.println("error closing output stream");
                    }
                }
            }

            update(++iCard, fileDest);
        }

        GuiBase.getInterface().preventSystemSleep(false);
    }

    protected Proxy getProxy() {
        if (type == 0) {
            return Proxy.NO_PROXY;
        } else {
            try {
                return new Proxy(TYPES[type], new InetSocketAddress(txtAddress.getText(), Integer.parseInt(txtPort.getText())));
            } catch (final Exception ex) {
                BugReporter.reportException(ex,
                        "Proxy connection could not be established!\nProxy address: %s\nProxy port: %s",
                        txtAddress.getText(), txtPort.getText());
            }
        }
        return null;
    }

    public abstract String getTitle();
    protected abstract Map<String, String> getNeededFiles();

    protected static void addMissingItems(Map<String, String> list, String nameUrlFile, String dir) {
        for (Pair<String, String> nameUrlPair : FileUtil.readNameUrlFile(nameUrlFile)) {
            File f = new File(dir, nameUrlPair.getLeft());
            //System.out.println(f.getAbsolutePath());
            if (!f.exists()) {
                list.put(f.getAbsolutePath(), nameUrlPair.getRight());
            }
        }
    }
}
