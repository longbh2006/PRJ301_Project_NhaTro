package util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON phẳng một tầng {"key":"value",...} cho API querydr của VNPay.
 * Dự án không có thư viện JSON và API này chỉ dùng chuỗi/số, nên tự viết phần
 * tối thiểu thay vì thêm jar. Gặp mảng hoặc object lồng nhau thì báo lỗi.
 */
public final class FlatJson {

    private final String text;
    private int pos;

    private FlatJson(String text) {
        this.text = text;
    }

    public static String write(Map<String, String> fields) {
        StringBuilder out = new StringBuilder("{");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (out.length() > 1) {
                out.append(',');
            }
            quote(out, entry.getKey());
            out.append(':');
            quote(out, entry.getValue());
        }
        return out.append('}').toString();
    }

    /** Số, true/false được trả về dưới dạng chuỗi; null giữ là null. */
    public static Map<String, String> parse(String json) {
        if (json == null) {
            throw new IllegalArgumentException("Phản hồi JSON rỗng.");
        }
        FlatJson parser = new FlatJson(json);
        Map<String, String> fields = parser.readObject();
        parser.skipSpace();
        if (parser.pos != json.length()) {
            throw new IllegalArgumentException("JSON có dữ liệu thừa sau object.");
        }
        return fields;
    }

    private Map<String, String> readObject() {
        Map<String, String> fields = new LinkedHashMap<>();
        expect('{');
        skipSpace();
        if (peek() == '}') {
            pos++;
            return fields;
        }
        while (true) {
            skipSpace();
            String key = readString();
            skipSpace();
            expect(':');
            skipSpace();
            fields.put(key, readValue());
            skipSpace();
            char next = next();
            if (next == '}') {
                return fields;
            }
            if (next != ',') {
                throw new IllegalArgumentException("JSON thiếu dấu ',' tại vị trí " + (pos - 1));
            }
        }
    }

    private String readValue() {
        char c = peek();
        if (c == '"') {
            return readString();
        }
        if (c == '{' || c == '[') {
            throw new IllegalArgumentException("JSON lồng nhau không được hỗ trợ.");
        }
        int start = pos;
        while (pos < text.length() && ",} \t\r\n".indexOf(text.charAt(pos)) < 0) {
            pos++;
        }
        String literal = text.substring(start, pos);
        if (literal.isEmpty()) {
            throw new IllegalArgumentException("JSON thiếu giá trị tại vị trí " + start);
        }
        return "null".equals(literal) ? null : literal;
    }

    private String readString() {
        expect('"');
        StringBuilder value = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') {
                return value.toString();
            }
            if (c != '\\') {
                value.append(c);
                continue;
            }
            char escaped = next();
            switch (escaped) {
                case '"', '\\', '/' -> value.append(escaped);
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> {
                    if (pos + 4 > text.length()) {
                        throw new IllegalArgumentException("JSON có \\u không đủ 4 ký tự.");
                    }
                    value.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> throw new IllegalArgumentException("JSON có ký tự escape không hợp lệ.");
            }
        }
    }

    private static void quote(StringBuilder out, String value) {
        if (value == null) {
            out.append("null");
            return;
        }
        out.append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    private void skipSpace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= text.length()) {
            throw new IllegalArgumentException("JSON kết thúc bất ngờ.");
        }
        return text.charAt(pos);
    }

    private char next() {
        char c = peek();
        pos++;
        return c;
    }

    private void expect(char expected) {
        if (next() != expected) {
            throw new IllegalArgumentException("JSON thiếu '" + expected + "' tại vị trí " + (pos - 1));
        }
    }
}
