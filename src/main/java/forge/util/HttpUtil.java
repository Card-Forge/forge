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
package forge.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import forge.Constant.Preferences;

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
        } catch (final MalformedURLException e) {
            return;
        }

        HttpURLConnection theUrlConnection = null;
        try {
            theUrlConnection = (HttpURLConnection) url.openConnection();
        } catch (final IOException e) {
            return;
        }
        theUrlConnection.setDoOutput(true);
        theUrlConnection.setDoInput(true);
        theUrlConnection.setUseCaches(false);
        theUrlConnection.setChunkedStreamingMode(1024);

        theUrlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + HttpUtil.BOUNDARY);

        DataOutputStream httpOut = null;
        try {
            httpOut = new DataOutputStream(theUrlConnection.getOutputStream());
        } catch (final IOException e1) {
            return;
        }

        final File f = new File(file);
        final String str = "--" + HttpUtil.BOUNDARY + "\r\n"
                + "Content-Disposition: form-data;name=\"data\"; filename=\"" + f.getName() + "\"\r\n"
                + "Content-Type: text/plain\r\n\r\n";

        try {
            httpOut.write(str.getBytes());
        } catch (final IOException e) {
            return;
        }

        FileInputStream uploadFileReader = null;
        try {
            uploadFileReader = new FileInputStream(f);
        } catch (final FileNotFoundException e) {
            return;
        }
        final int numBytesToRead = 1024;
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
        } catch (final IOException e) {
            return;
        }
        try {
            httpOut.write(("--" + HttpUtil.BOUNDARY + "--\r\n").getBytes());
        } catch (final IOException e) {
            return;
        }

        try {
            httpOut.flush();
        } catch (final IOException e) {
            return;
        }
        try {
            httpOut.close();
        } catch (final IOException e) {
            return;
        }

        // read & parse the response
        InputStream is = null;
        try {
            is = theUrlConnection.getInputStream();
        } catch (final IOException e) {
            return;
        }
        final StringBuilder response = new StringBuilder();
        final byte[] respBuffer = new byte[8192];
        try {
            while (is.read(respBuffer) >= 0) {
                response.append(new String(respBuffer).trim());
            }
        } catch (final IOException e) {
            return;
        }
        try {
            is.close();
        } catch (final IOException e) {
            return;
        }
        if (Preferences.DEV_MODE) {
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
        } catch (final MalformedURLException e) {
            return "error 1";
        }
        InputStream is = null;
        try {
            is = url.openStream();
        } catch (final IOException e) {
            return "error 2";
        }
        int ptr = 0;
        final StringBuffer buffer = new StringBuffer();
        try {
            while ((ptr = is.read()) != -1) {
                buffer.append((char) ptr);
            }
        } catch (final IOException e) {
            return "error 3";
        }

        return buffer.toString();
    }
}
