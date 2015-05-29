package edu.illinois.i3.emop.apps.pageevaluator;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static edu.illinois.i3.emop.apps.pageevaluator.PageStatsCalculator.DefaultStats.*;

public class PageStatsCalculator {
    public enum DefaultStats {
        TokenCount,
        Ge4RepeatedCharsTokenCount,
        NumberObjectsTokenCount,
        PunctTokenCount,
        LenGt1NonAlphaTokenCount,
        CleanOneNonAlphaNoRepTokenCount,
        CleanTwoNonAlphaNoRepTokenCount,
        CleanThreeOrMoreNonAlphaTokenCount,
        CleanAllAlphaNoRepTokenCount,
        CleanShortWordCount,
        SingleLetterCount
    }

    protected static final int MAX_LEADING_PUNCT_TO_REMOVE = 1;
    protected static final int MAX_TRAILING_PUNCT_TO_REMOVE = 2;
    protected static final int CLEAN_TOKEN_LEN_THRESHOLD = 3;

    protected static final Pattern NonAlphaPattern = Pattern.compile("\\P{L}", Pattern.CANON_EQ);
    protected static final Pattern PunctPattern = Pattern.compile("^\\p{Punct}$");
    protected static final Pattern NumberBasedObjectPattern = Pattern.compile("^\\p{Sc}?[\\.,/\\-]?(\\p{N}+[\\.,/%\\-]?)+\\p{Sc}?$");
    protected static final Pattern OneAlphaPattern = Pattern.compile("^\\p{L}$", Pattern.CANON_EQ);
    protected static final Pattern Repeated4orMoreCharsPattern = Pattern.compile("(\\P{N})\\1{3,}", Pattern.CANON_EQ);

    // Note: correctable profile = tokens which, after cleaning, contain at most 2 non-alpha characters and at least 1 alpha character,
    //       have a length of at least 3, and do not contain 4 or more repeated characters in a run

    // Note: cleaning = removal of max MAX_LEADING_PUNCT_TO_REMOVE + MAX_TRAILING_PUNCT_TO_REMOVE total punctuation characters from the beginning and end of a token
    //       a token can be cleaned only if, after cleaning, the remaining substring has a length >= CLEAN_TOKEN_LEN_THRESHOLD

    public static OCRPageStats calculateStatistics(OCRPage<? extends OCRToken> page) {
                                                    // number of...
        int tokenCount = 0;                         // tokens on page
        int ge4RepeatedCharsTokenCount = 0;         // tokens containing 4 or more repeated characters (not numbers) in a run
        int numberObjectsTokenCount = 0;            // tokens that could represent numbers, dates, amounts of money, identifiers..etc. (are number based)
        int punctTokenCount = 0;                    // tokens that are made up of exactly 1 punctuation character (non-alphanum)
        int lenGt1NonAlphaTokenCount = 0;           // tokens of length > 1 that contain exclusively non-alpha characters (but are not made up entirely of numbers) (can be thought of as "garbage" tokens)
        int cleanOneNonAlphaNoRepTokenCount = 0;    // tokens which, after cleaning, are of length at least 3, contain exactly 1 non-alpha character and at least 1 alpha, and no 4 or more repeated characters in a run
        int cleanTwoNonAlphaNoRepTokenCount = 0;    // tokens which, after cleaning, are of length at least 3, contain exactly 2 non-alpha character and at least 1 alpha, and no 4 or more repeated characters in a run
        int cleanThreeOrMoreNonAlphaTokenCount = 0; // tokens which, after cleaning, are of length at least 3, contain > 2 non-alpha character and at least 1 alpha (for correction purposes they can also be thought of as "garbage")
        int cleanAllAlphaNoRepTokenCount = 0;       // tokens which, after cleaning, contain exclusively alpha characters and no 4 or more repeated characters in a run
        int cleanShortWordCount = 0;                // tokens which, after cleaning, have length < 3 and are supposed to be words (i.e. no numbers, no single punctuation, no single letters)
        int singleLetterCount = 0;                  // tokens made up of exactly 1 alpha character

        Iterator<? extends OCRToken> tokenIterator = page.tokens().iterator();
        while (tokenIterator.hasNext()) {
            OCRToken token = tokenIterator.next();
            String tokenText = token.text();

            tokenCount++;

            String normTokenText = tokenText.toLowerCase();
            String cleanTokenText = cleanToken(normTokenText);

            // tokenText      = the default, not-normalized, token (trimmed)
            // normTokenText  = the normalized (lowercased) tokenText
            // cleanTokenText = the normTokenText with MAX_LEADING_PUNCT_REMOVE punctuation removed, and MAX_TRAILING_PUNCT_REMOVE punctuation removed
            //                  (can be 'null' if, after cleaning, the remaining substring has a length < CLEAN_TOKEN_LEN_THRESHOLD)

            Integer tokenLength = tokenText.length();
            Integer cleanTokenLength = cleanTokenText.length();

            Matcher punctMatcher = PunctPattern.matcher(tokenText);
            if (punctMatcher.matches()) {
                punctTokenCount++;
                continue;
            }

            Matcher numberMatcher = NumberBasedObjectPattern.matcher(tokenText);
            if (numberMatcher.matches()) {
                numberObjectsTokenCount++;
                continue;
            }

            Matcher singleAlphaMatcher = OneAlphaPattern.matcher(tokenText);
            if (singleAlphaMatcher.matches()) {
                singleLetterCount++;
                continue;
            }

            Matcher ge4RepeatedCharsMatcher = Repeated4orMoreCharsPattern.matcher(normTokenText);
            if (ge4RepeatedCharsMatcher.find()) {
                ge4RepeatedCharsTokenCount++;
                continue;
            }

            // compute the number of non-alpha characters in the cleaned token (if it contains no more than 3 repeated characters in a run)
            Matcher nonAlphaMatcher = NonAlphaPattern.matcher(cleanTokenText);
            int nonAlphaCount = 0;
            while (nonAlphaMatcher.find())
                nonAlphaCount++;

            if (nonAlphaCount == cleanTokenLength) {
                lenGt1NonAlphaTokenCount++;
                continue;
            }

            // a token can be cleaned only if, after cleaning, the remaining substring has a length >= 3
            if (cleanTokenLength < CLEAN_TOKEN_LEN_THRESHOLD) {
                cleanShortWordCount++;
                continue;
            }

            switch (nonAlphaCount) {
                case 0:
                    cleanAllAlphaNoRepTokenCount++;
                    break;

                case 1:
                    cleanOneNonAlphaNoRepTokenCount++;
                    break;

                case 2:
                    cleanTwoNonAlphaNoRepTokenCount++;
                    break;

                default:
                    cleanThreeOrMoreNonAlphaTokenCount++;
                    break;
            }
        }

        OCRPageStats pageStats = new OCRPageStats();
        pageStats.put(TokenCount, tokenCount);
        pageStats.put(Ge4RepeatedCharsTokenCount, ge4RepeatedCharsTokenCount);
        pageStats.put(NumberObjectsTokenCount, numberObjectsTokenCount);
        pageStats.put(PunctTokenCount, punctTokenCount);
        pageStats.put(LenGt1NonAlphaTokenCount, lenGt1NonAlphaTokenCount);
        pageStats.put(CleanOneNonAlphaNoRepTokenCount, cleanOneNonAlphaNoRepTokenCount);
        pageStats.put(CleanTwoNonAlphaNoRepTokenCount, cleanTwoNonAlphaNoRepTokenCount);
        pageStats.put(CleanThreeOrMoreNonAlphaTokenCount, cleanThreeOrMoreNonAlphaTokenCount);
        pageStats.put(CleanAllAlphaNoRepTokenCount, cleanAllAlphaNoRepTokenCount);
        pageStats.put(CleanShortWordCount, cleanShortWordCount);
        pageStats.put(SingleLetterCount, singleLetterCount);

        return pageStats;
    }

    protected static String cleanToken(String token) {
        return token.replaceFirst("^\\p{Punct}{0," + MAX_LEADING_PUNCT_TO_REMOVE + "}", "")
                .replaceFirst("\\p{Punct}{0," + MAX_TRAILING_PUNCT_TO_REMOVE + "}$", "");
    }


    public static class OCRPageStats extends KeyValueStore {

        private OCRPageStats() { }

        public void put(DefaultStats stat, Object value) {
            put(stat.name(), value);
        }
        public String getString(DefaultStats stat) { return getString(stat.name()); }
        public Integer getInt(DefaultStats stat) { return getInt(stat.name()); }
        public Float getFloat(DefaultStats stat) { return getFloat(stat.name()); }
        public Double getDouble(DefaultStats stat) { return getDouble(stat.name()); }
        public Boolean getBoolean(DefaultStats stat) { return getBoolean(stat.name()); }

    }
}
