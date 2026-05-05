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
    private static class WordEntry {
        final String original;
        final String lower;
        final String normalized;
        final String phonetic;
        final int frequency;
        final int specialCount;

        WordEntry(String original, String lower, String normalized, String phonetic, int frequency, int specialCount) {
            this.original = original;
            this.lower = lower;
            this.normalized = normalized;
            this.phonetic = phonetic;
            this.frequency = frequency;
            this.specialCount = specialCount;
        }
    }

    private final List<WordEntry> mWordEntries = new ArrayList<>();

    public MaduraSuggest(Context context) {
        loadDictionaryFromAssets(context);
    }

    private void loadDictionaryFromAssets(Context context) {
        try {
            // Load base words
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("basewords.txt"), "UTF-8"))) {
                String line;
                Map<String, Integer> tempDict = new HashMap<>();
                while ((line = reader.readLine()) != null) {
                    // Split by whitespace and punctuation, keeping special characters like â, ê, ô, ḍ, ṭ, ḷ, '
                    String[] words = line.split("[^\\p{L}âêôḍṭḷ']+");
                    for (String word : words) {
                        word = word.trim();
                        if (!word.isEmpty()) {
                            tempDict.put(word, tempDict.getOrDefault(word, 0) + 1);
                        }
                    }
                }
                
                // Pre-calculate and cache word properties
                for (Map.Entry<String, Integer> entry : tempDict.entrySet()) {
                    addWordToEntries(entry.getKey(), entry.getValue());
                }

                // Add common Madurese particles that might be missing
                String[] commonParticles = {"la", "ella", "roh", "sè", "è", "bân", "dâri", "dâ", "ma", "be", "ghi'"};
                for (String p : commonParticles) {
                    if (!tempDict.containsKey(p)) {
                        addWordToEntries(p, 100); // Give them a decent frequency
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addWordToEntries(String word, int frequency) {
        mWordEntries.add(new WordEntry(
            word,
            word.toLowerCase(),
            normalize(word),
            phoneticNormalize(word),
            frequency,
            word.length() - normalize(word).length()
        ));
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

    /**
     * phoneticNormalize - Collapses similar-sounding consonants and vowels
     * into groups to handle modern Madura slang/simplification.
     * Groups: (bh,b,p), (dh,d,ḍ,ṭ,t), (gh,g,k), (jh,j,c)
     */
    private String phoneticNormalize(String s) {
        if (s == null) return "";
        String normalized = s.toLowerCase()
            // Nasals and digraphs
            .replace("ng", "N").replace("ny", "Y")
            // Consonants (Voiced Aspirated -> Voiceless Slang)
            .replace("bh", "B").replace("b", "B").replace("p", "B")
            .replace("dh", "D").replace("ḍ", "D").replace("ṭ", "D").replace("d", "D").replace("t", "D")
            .replace("gh", "G").replace("g", "G").replace("k", "G")
            .replace("jh", "J").replace("j", "J").replace("c", "J")
            .replace("ḷ", "L").replace("l", "L")
            // Vowels (Slang often swaps â/e/a)
            .replace("â", "V").replace("ê", "V").replace("e", "V").replace("a", "V")
            .replace("ô", "O").replace("o", "O")
            .replace("u", "U").replace("i", "I");

        // Collapse duplicate characters (e.g., "NN" -> "N", "BB" -> "B", "VV" -> "V")
        // This handles slang that simplifies double consonants like "bhengngis" -> "pengis"
        StringBuilder collapsed = new StringBuilder();
        if (normalized.length() > 0) {
            collapsed.append(normalized.charAt(0));
            for (int i = 1; i < normalized.length(); i++) {
                if (normalized.charAt(i) != normalized.charAt(i - 1)) {
                    collapsed.append(normalized.charAt(i));
                }
            }
        }
        return collapsed.toString();
    }

    public List<String> getSuggestions(String input) {
        if (input == null || input.isEmpty()) return new ArrayList<>();

        List<WordScore> scoredWords = new ArrayList<>();
        String lowerInput = input.toLowerCase();
        String normalizedInput = normalize(input);
        String phoneticInput = phoneticNormalize(input);

        for (WordEntry entry : mWordEntries) {
            if (entry.normalized.equals(normalizedInput)) {
                // Exact match (including special characters bonus)
                scoredWords.add(new WordScore(entry.original, 5000 + entry.frequency + (entry.specialCount * 500)));
            } else if (entry.phonetic.equals(phoneticInput)) {
                // SLANG MATCH: e.g., "ocen" matches "ojhân"
                scoredWords.add(new WordScore(entry.original, 2500 + entry.frequency));
            } else if (entry.lower.startsWith(lowerInput)) {
                // Prefix match
                int score = 1000 + entry.frequency;
                // Penalty for long words if input is very short (prevents roh -> rohani)
                if (input.length() <= 3 && entry.original.length() > input.length() + 2) {
                    score -= 800; 
                }
                scoredWords.add(new WordScore(entry.original, score));
            } else if (input.length() > 3 && calculateLevenshteinDistance(normalizedInput, entry.normalized) <= 1) {
                // Fuzzy match (only for words longer than 3 chars to avoid chaos)
                scoredWords.add(new WordScore(entry.original, 500 + entry.frequency));
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

    /**
     * getAutoCorrection - Similar to getSuggestions but only returns
     * a result if it's a high-confidence match (Exact or Phonetic/Slang).
     */
    public String getAutoCorrection(String input) {
        if (input == null || input.isEmpty()) return null;

        String lowerInput = input.toLowerCase();
        String normalizedInput = normalize(input);
        String phoneticInput = phoneticNormalize(input);

        WordEntry bestEntry = null;
        int bestScore = -1;

        for (WordEntry entry : mWordEntries) {
            int score = -1;
            if (entry.normalized.equals(normalizedInput)) {
                score = 5000 + entry.frequency + (entry.specialCount * 500);
            } else if (entry.phonetic.equals(phoneticInput)) {
                score = 2500 + entry.frequency;
            }

            if (score > bestScore) {
                bestScore = score;
                bestEntry = entry;
            }
        }

        if (bestEntry != null && !bestEntry.original.equalsIgnoreCase(input)) {
            return bestEntry.original;
        }
        return null;
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
