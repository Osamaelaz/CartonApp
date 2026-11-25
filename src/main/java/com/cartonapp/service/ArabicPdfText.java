package com.cartonapp.service;

import java.text.Bidi;
import java.util.HashMap;
import java.util.Map;

/**
 * Prepares logical Arabic text for PDF content streams, which draw glyphs strictly left to right
 * without shaping: letters are replaced by their contextual presentation forms (including the
 * lam-alef ligatures and hamza letters), then runs are reordered visually with the Unicode bidi
 * algorithm using a right-to-left paragraph direction. Combining marks stay attached to their base
 * letter and paired brackets are mirrored inside right-to-left runs.
 */
public final class ArabicPdfText {
    /** isolated, final, initial, medial. Right-joining letters only have isolated and final forms. */
    private static final Map<Character, char[]> FORMS = new HashMap<>();
    private static final Map<Character, char[]> LAM_ALEF = new HashMap<>();

    static {
        form('ء', 'ﺀ');
        form('آ', 'ﺁ', 'ﺂ');
        form('أ', 'ﺃ', 'ﺄ');
        form('ؤ', 'ﺅ', 'ﺆ');
        form('إ', 'ﺇ', 'ﺈ');
        form('ئ', 'ﺉ', 'ﺊ', 'ﺋ', 'ﺌ');
        form('ا', 'ﺍ', 'ﺎ');
        form('ب', 'ﺏ', 'ﺐ', 'ﺑ', 'ﺒ');
        form('ة', 'ﺓ', 'ﺔ');
        form('ت', 'ﺕ', 'ﺖ', 'ﺗ', 'ﺘ');
        form('ث', 'ﺙ', 'ﺚ', 'ﺛ', 'ﺜ');
        form('ج', 'ﺝ', 'ﺞ', 'ﺟ', 'ﺠ');
        form('ح', 'ﺡ', 'ﺢ', 'ﺣ', 'ﺤ');
        form('خ', 'ﺥ', 'ﺦ', 'ﺧ', 'ﺨ');
        form('د', 'ﺩ', 'ﺪ');
        form('ذ', 'ﺫ', 'ﺬ');
        form('ر', 'ﺭ', 'ﺮ');
        form('ز', 'ﺯ', 'ﺰ');
        form('س', 'ﺱ', 'ﺲ', 'ﺳ', 'ﺴ');
        form('ش', 'ﺵ', 'ﺶ', 'ﺷ', 'ﺸ');
        form('ص', 'ﺹ', 'ﺺ', 'ﺻ', 'ﺼ');
        form('ض', 'ﺽ', 'ﺾ', 'ﺿ', 'ﻀ');
        form('ط', 'ﻁ', 'ﻂ', 'ﻃ', 'ﻄ');
        form('ظ', 'ﻅ', 'ﻆ', 'ﻇ', 'ﻈ');
        form('ع', 'ﻉ', 'ﻊ', 'ﻋ', 'ﻌ');
        form('غ', 'ﻍ', 'ﻎ', 'ﻏ', 'ﻐ');
        form('ف', 'ﻑ', 'ﻒ', 'ﻓ', 'ﻔ');
        form('ق', 'ﻕ', 'ﻖ', 'ﻗ', 'ﻘ');
        form('ك', 'ﻙ', 'ﻚ', 'ﻛ', 'ﻜ');
        form('ل', 'ﻝ', 'ﻞ', 'ﻟ', 'ﻠ');
        form('م', 'ﻡ', 'ﻢ', 'ﻣ', 'ﻤ');
        form('ن', 'ﻥ', 'ﻦ', 'ﻧ', 'ﻨ');
        form('ه', 'ﻩ', 'ﻪ', 'ﻫ', 'ﻬ');
        form('و', 'ﻭ', 'ﻮ');
        form('ى', 'ﻯ', 'ﻰ');
        form('ي', 'ﻱ', 'ﻲ', 'ﻳ', 'ﻴ');
        LAM_ALEF.put('آ', new char[] {'ﻵ', 'ﻶ'});
        LAM_ALEF.put('أ', new char[] {'ﻷ', 'ﻸ'});
        LAM_ALEF.put('إ', new char[] {'ﻹ', 'ﻺ'});
        LAM_ALEF.put('ا', new char[] {'ﻻ', 'ﻼ'});
    }

    private ArabicPdfText() { }

    private static void form(char letter, char... forms) {
        FORMS.put(letter, forms);
    }

    /** Converts logical text to the visual, shaped string to pass to {@code showText}. */
    public static String visual(String logical) {
        if (logical == null || logical.isEmpty()) return "";
        return reorder(shape(logical));
    }

    static String shape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char[] forms = FORMS.get(c);
            if (forms == null) {
                out.append(c);
                continue;
            }
            int prev = neighbour(text, i, -1);
            int next = neighbour(text, i, +1);
            boolean joinsPrevious = prev >= 0 && joinsToNext(text.charAt(prev)) && forms.length >= 2;
            if (c == 'ل' && next >= 0 && LAM_ALEF.containsKey(text.charAt(next))) {
                char[] ligature = LAM_ALEF.get(text.charAt(next));
                out.append(joinsPrevious ? ligature[1] : ligature[0]);
                for (int k = i + 1; k < next; k++) out.append(text.charAt(k)); // marks between lam and alef
                i = next;
                continue;
            }
            boolean joinsNext = next >= 0 && forms.length == 4 && joinsToPrevious(text.charAt(next));
            int index = joinsPrevious ? (joinsNext ? 3 : 1) : (joinsNext ? 2 : 0);
            out.append(forms[Math.min(index, forms.length - 1)]);
        }
        return out.toString();
    }

    private static int neighbour(String text, int index, int step) {
        for (int i = index + step; i >= 0 && i < text.length(); i += step) {
            if (!isMark(text.charAt(i))) return i;
        }
        return -1;
    }

    private static boolean joinsToNext(char c) {
        if (c == 'ـ') return true; // tatweel
        char[] forms = FORMS.get(c);
        return forms != null && forms.length == 4;
    }

    private static boolean joinsToPrevious(char c) {
        if (c == 'ـ') return true;
        char[] forms = FORMS.get(c);
        return forms != null && forms.length >= 2;
    }

    private static boolean isMark(char c) {
        return (c >= 'ً' && c <= 'ٟ') || c == 'ٰ';
    }

    static String reorder(String text) {
        Bidi bidi = new Bidi(text, Bidi.DIRECTION_RIGHT_TO_LEFT);
        if (bidi.isLeftToRight()) return text;
        int runs = bidi.getRunCount();
        String[] parts = new String[runs];
        byte[] levels = new byte[runs];
        for (int run = 0; run < runs; run++) {
            String part = text.substring(bidi.getRunStart(run), bidi.getRunLimit(run));
            int level = bidi.getRunLevel(run);
            parts[run] = (level & 1) != 0 ? reverseRtlRun(part) : part;
            levels[run] = (byte) level;
        }
        Bidi.reorderVisually(levels, 0, parts, 0, runs);
        return String.join("", parts);
    }

    /** Reverses a right-to-left run, keeping combining marks after their base and mirroring brackets. */
    private static String reverseRtlRun(String run) {
        StringBuilder out = new StringBuilder(run.length());
        int end = run.length();
        while (end > 0) {
            int start = end - 1;
            while (start > 0 && isMark(run.charAt(start))) start--;
            out.append(mirror(run.charAt(start)));
            out.append(run, start + 1, end);
            end = start;
        }
        return out.toString();
    }

    private static char mirror(char c) {
        return switch (c) {
            case '(' -> ')';
            case ')' -> '(';
            case '[' -> ']';
            case ']' -> '[';
            case '{' -> '}';
            case '}' -> '{';
            case '<' -> '>';
            case '>' -> '<';
            case '«' -> '»';
            case '»' -> '«';
            default -> c;
        };
    }
}
