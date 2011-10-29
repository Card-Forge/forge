package forge;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>
 * HttpUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class HttpUtil {

    private static final String BOUNDARY = "--7d021a37605f0";

    /**
     * <p>
     * upload.
     * </p>
     * 
     * @param sURL
     *            a {@link java.lang.String} object.
     * @param file
     *            a {@link java.lang.String} object.
     */
    public final void upload(final String sURL, final String file) {
        URL url = null;
        try {
            url = new URL(sURL);
        } catch (MalformedURLException e) {
            return;
        }

        HttpURLConnection theUrlConnection = null;
        try {
            theUrlConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            return;
        }
        theUrlConnection.setDoOutput(true);
        theUrlConnection.setDoInput(true);
        theUrlConnection.setUseCaches(false);
        theUrlConnection.setChunkedStreamingMode(1024);

        theUrlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

        DataOutputStream httpOut = null;
        try {
            httpOut = new DataOutputStream(theUrlConnection.getOutputStream());
        } catch (IOException e1) {
            return;
        }

        File f = new File(file);
        String str = "--" + BOUNDARY + "\r\n" + "Content-Disposition: form-data;name=\"data\"; filename=\""
                + f.getName() + "\"\r\n" + "Content-Type: text/plain\r\n\r\n";

        try {
            httpOut.write(str.getBytes());
        } catch (IOException e) {
            return;
        }

        FileInputStream uploadFileReader = null;
        try {
            uploadFileReader = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            return;
        }
        int numBytesToRead = 1024;
        int availableBytesToRead;
        try {
            while ((availableBytesToRead = uploadFileReader.available()) > 0) {
                byte[] bufferBytesRead;
                bufferBytesRead = availableBytesToRead >= numBytesToRead ? new byte[numBytesToRead]
                        : new byte[availableBytesToRead];
                uploadFileReader.read(bufferBytesRead);
                httpOut.write(bufferBytesRead);
                httpOut.flush();
            }
        } catch (IOException e) {
            return;
        }
        try {
            httpOut.write(("--" + BOUNDARY + "--\r\n").getBytes());
        } catch (IOException e) {
            return;
        }

        try {
            httpOut.flush();
        } catch (IOException e) {
            return;
        }
        try {
            httpOut.close();
        } catch (IOException e) {
            return;
        }

        // read & parse the response
        InputStream is = null;
        try {
            is = theUrlConnection.getInputStream();
        } catch (IOException e) {
            return;
        }
        StringBuilder response = new StringBuilder();
        byte[] respBuffer = new byte[8192];
        try {
            while (is.read(respBuffer) >= 0) {
                response.append(new String(respBuffer).trim());
            }
        } catch (IOException e) {
            return;
        }
        try {
            is.close();
        } catch (IOException e) {
            return;
        }
        if (Constant.Runtime.DEV_MODE[0]) {
            System.out.println(response.toString());
        }
    }

    /**
     * <p>
     * getURL.
     * </p>
     * 
     * @param sURL
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public final String getURL(final String sURL) {
        URL url = null;
        try {
            url = new URL(sURL);
        } catch (MalformedURLException e) {
            return "error 1";
        }
        InputStream is = null;
        try {
            is = url.openStream();
        } catch (IOException e) {
            return "error 2";
        }
        int ptr = 0;
        StringBuffer buffer = new StringBuffer();
        try {
            while ((ptr = is.read()) != -1) {
                buffer.append((char) ptr);
            }
        } catch (IOException e) {
            return "error 3";
        }

        return buffer.toString();
    }
}
