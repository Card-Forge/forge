package forge.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static Object parse(final String text) throws IOException {
        final Parser parser = new Parser(text);
        final Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.index != parser.text.length()) {
            throw new IOException("Trailing JSON data.");
        }
        return value;
    }

    public static String escape(final String value) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
            case '\\' -> sb.append("\\\\");
            case '"' -> sb.append("\\\"");
            case '\b' -> sb.append("\\b");
            case '\f' -> sb.append("\\f");
            case '\n' -> sb.append("\\n");
            case '\r' -> sb.append("\\r");
            case '\t' -> sb.append("\\t");
            default -> {
                if (c < 0x20) {
                    sb.append(String.format("\\u%04x", (int)c));
                }
                else {
                    sb.append(c);
                }
            }
            }
        }
        return sb.toString();
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(final String text) {
            this.text = text;
        }

        private Object readValue() throws IOException {
            skipWhitespace();
            if (index >= text.length()) {
                throw new IOException("Unexpected end of JSON.");
            }
            return switch (text.charAt(index)) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
            };
        }

        private Map<String, Object> readObject() throws IOException {
            index++;
            final Map<String, Object> result = new HashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return result;
            }
            while (true) {
                final String key = readString();
                skipWhitespace();
                expect(':');
                result.put(key, readValue());
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return result;
                }
                expect(',');
                skipWhitespace();
            }
        }

        private List<Object> readArray() throws IOException {
            index++;
            final List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return result;
            }
            while (true) {
                result.add(readValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private String readString() throws IOException {
            expect('"');
            final StringBuilder sb = new StringBuilder();
            while (index < text.length()) {
                final char c = text.charAt(index++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (index >= text.length()) {
                    throw new IOException("Bad JSON escape.");
                }
                final char escaped = text.charAt(index++);
                switch (escaped) {
                case '"', '\\', '/' -> sb.append(escaped);
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (index + 4 > text.length()) {
                        throw new IOException("Bad JSON unicode escape.");
                    }
                    try {
                        sb.append((char)Integer.parseInt(text.substring(index, index + 4), 16));
                    }
                    catch (final NumberFormatException e) {
                        throw new IOException("Bad JSON unicode escape.", e);
                    }
                    index += 4;
                }
                default -> throw new IOException("Bad JSON escape.");
                }
            }
            throw new IOException("Unterminated JSON string.");
        }

        private Object readNumber() throws IOException {
            final int start = index;
            while (index < text.length() && "-+0123456789.eE".indexOf(text.charAt(index)) >= 0) {
                index++;
            }
            final String number = text.substring(start, index);
            if (number.isEmpty()) {
                throw new IOException("Expected JSON value.");
            }
            try {
                return number.contains(".") || number.contains("e") || number.contains("E")
                        ? Double.parseDouble(number)
                        : Long.parseLong(number);
            }
            catch (final NumberFormatException e) {
                throw new IOException("Bad JSON number.", e);
            }
        }

        private Object readLiteral(final String literal, final Object value) throws IOException {
            if (!text.startsWith(literal, index)) {
                throw new IOException("Bad JSON literal.");
            }
            index += literal.length();
            return value;
        }

        private void expect(final char c) throws IOException {
            if (index >= text.length() || text.charAt(index) != c) {
                throw new IOException("Expected '" + c + "'.");
            }
            index++;
        }

        private boolean peek(final char c) {
            return index < text.length() && text.charAt(index) == c;
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }
}
