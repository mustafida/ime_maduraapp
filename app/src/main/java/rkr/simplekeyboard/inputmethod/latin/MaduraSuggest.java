package rkr.simplekeyboard.inputmethod.latin;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * MaduraSuggest - Smart AI Engine for Madurese Keyboard.
 * Uses a hybrid approach:
 * 1. Frequency-based sorting (Unigram HMM)
 * 2. Prefix matching
 * 3. Fuzzy matching (Levenshtein) for typo correction.
 */
public class MaduraSuggest {
    private final Map<String, Integer> mDictionary = new HashMap<>();

    public MaduraSuggest(Context context) {
        loadDictionaryFromAssets(context);
    }

    private void loadDictionaryFromAssets(Context context) {
        try {
            // Load base words
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("basewords.txt"), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.strip();
                    if (!word.isEmpty()) mDictionary.put(word, 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase()
            .replace("â", "a")
            .replace("ê", "e")
            .replace("ô", "o")
            .replace("ḍ", "d")
            .replace("ṭ", "t")
            .replace("ḷ", "l");
    }

    public List<String> getSuggestions(String input) {
        if (input == null || input.isEmpty()) return new ArrayList<>();

        List<WordScore> scoredWords = new ArrayList<>();
        String lowerInput = input.toLowerCase();
        String normalizedInput = normalize(input);

        for (String word : mDictionary.keySet()) {
            String lowerWord = word.toLowerCase();
            String normalizedWord = normalize(word);
            int frequency = mDictionary.getOrDefault(word, 1);
            int specialCount = word.length() - normalize(word).length();

            if (normalizedWord.equals(normalizedInput)) {
                // This is the SAME word (with or without accents)
                // We give a big bonus for special characters (â, ê, etc.)
                // so that aêng wins over aeng
                scoredWords.add(new WordScore(word, 2000 + frequency + (specialCount * 500)));
            } else if (lowerWord.startsWith(lowerInput)) {
                scoredWords.add(new WordScore(word, 1000 + frequency));
            } else if (calculateLevenshteinDistance(normalizedInput, normalizedWord) <= 1) {
                scoredWords.add(new WordScore(word, 500 + frequency));
            }
        }

        Collections.sort(scoredWords, (a, b) -> b.score - a.score);

        List<String> results = new ArrayList<>();
        for (int i = 0; i < Math.min(scoredWords.size(), 3); i++) {
            results.add(scoredWords.get(i).word);
        }
        return results;
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        if (Math.abs(s1.length() - s2.length()) > 2) return 99;
        int[] prev = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) prev[j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            int[] curr = new int[s2.length() + 1];
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int d1 = prev[j] + 1;
                int d2 = curr[j - 1] + 1;
                int d3 = prev[j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1);
                curr[j] = Math.min(Math.min(d1, d2), d3);
            }
            prev = curr;
        }
        return prev[s2.length()];
    }

    public List<String> getNextWordSuggestions(String prevWord) {
        if (prevWord == null || prevWord.isEmpty()) return new ArrayList<>();
        
        List<String> results = new ArrayList<>();
        String lowerPrev = prevWord.toLowerCase();

        // 1. SMART CONTEXT: Common Madura transitions
        Map<String, String[]> commonBigrams = new HashMap<>();
        commonBigrams.put("mator", new String[]{"sakalangkong", "sakelangkong"});
        commonBigrams.put("ḍek", new String[]{"remmah", "emma", "attas"});
        commonBigrams.put("bâ’na", new String[]{"ampon", "gi’", "sapah"});
        commonBigrams.put("engghi", new String[]{"bhunten", "lerres", "ampon"});
        commonBigrams.put("bhâsâ", new String[]{"madhurâ", "alôs", "engghi-enten"});

        if (commonBigrams.containsKey(lowerPrev)) {
            for (String s : commonBigrams.get(lowerPrev)) {
                results.add(s);
            }
        }

        // 2. DYNAMIC FALLBACK: Pick 3 random words from popular list to avoid repetition
        if (results.size() < 3) {
            String[] popularWords = {"engghi", "ampon", "bâdâ", "ngakan", "mareh", "ka’dhinto", "panèka", "samangkèn", "mator", "sakalangkong"};
            List<String> list = new ArrayList<>();
            for (String w : popularWords) {
                if (!w.equalsIgnoreCase(prevWord) && !results.contains(w)) {
                    list.add(w);
                }
            }
            Collections.shuffle(list);
            for (int i = 0; i < list.size() && results.size() < 3; i++) {
                results.add(list.get(i));
            }
        }
        return results;
    }

    private static class WordScore {
        String word;
        int score;
        WordScore(String word, int score) {
            this.word = word;
            this.score = score;
        }
    }
}
