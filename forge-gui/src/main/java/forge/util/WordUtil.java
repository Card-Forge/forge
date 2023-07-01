package forge.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class WordUtil {
    public static String capitalize(String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        final char[] buffer = str.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (Character.isWhitespace(ch)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }
        }
        return new String(buffer);
    }

    private final static Pattern patternToWrapOn = Pattern.compile(" ");
    public static String wordWrapAsHTML(String str) {
        String result = null;
        int wrapLength = 40;
        String newLineStr = "<br>";
        if (str != null) {
            final int inputLineLength = str.length();
            int offset = 0;
            final StringBuilder wrappedLine = new StringBuilder(inputLineLength + 32);
            while (offset < inputLineLength) {
                int spaceToWrapAt = -1;
                Matcher matcher = patternToWrapOn.matcher(str.substring(offset, Math.min(offset + wrapLength + 1, inputLineLength)));
                if (matcher.find()) {
                    if (matcher.start() == 0) {
                        offset += matcher.end();
                        continue;
                    }
                    spaceToWrapAt = matcher.start() + offset;
                }

                // only last line without leading spaces is left
                if (inputLineLength - offset <= wrapLength) {
                    break;
                }

                while (matcher.find()) {
                    spaceToWrapAt = matcher.start() + offset;
                }

                if (spaceToWrapAt >= offset) {
                    // normal case
                    wrappedLine.append(str, offset, spaceToWrapAt);
                    wrappedLine.append(newLineStr);
                    offset = spaceToWrapAt + 1;

                } else {
                    // do not wrap really long word, just extend beyond limit
                    matcher = patternToWrapOn.matcher(str.substring(offset + wrapLength));
                    if (matcher.find()) {
                        spaceToWrapAt = matcher.start() + offset + wrapLength;
                    }

                    if (spaceToWrapAt >= 0) {
                        wrappedLine.append(str, offset, spaceToWrapAt);
                        wrappedLine.append(newLineStr);
                        offset = spaceToWrapAt + 1;
                    } else {
                        wrappedLine.append(str, offset, str.length());
                        offset = inputLineLength;
                    }
                }
            }// Whatever is left in line is short enough to just pass through
            wrappedLine.append(str, offset, str.length());
            result = wrappedLine.toString();
        }

        return "<html>" + result + "</html>";
    }

    private WordUtil() {}
}
