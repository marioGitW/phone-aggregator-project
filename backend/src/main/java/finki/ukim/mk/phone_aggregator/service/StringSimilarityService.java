package finki.ukim.mk.phone_aggregator.service;

import org.springframework.stereotype.Service;

/**
 * Generic Jaro-Winkler string similarity. Used by {@link PhoneModelMatchingService} to
 * fuzzy-match an incoming offer's model key against existing PhoneModels of the same
 * brand when storage/RAM couldn't be extracted (so no exact natural-key match is possible).
 */
@Service
public class StringSimilarityService {

    private static final int MAX_PREFIX_LENGTH = 4;

    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isBlank() || s2.isBlank()) {
            return 0.0d;
        }

        return jaroWinklerSimilarity(s1, s2);
    }

    private double jaroWinklerSimilarity(String s1, String s2) {
        double jaro = jaroSimilarity(s1, s2);
        int prefixLength = commonPrefixLength(s1, s2);
        return jaro + (prefixLength * 0.1 * (1.0 - jaro));
    }

    private double jaroSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0d;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 == 0 || len2 == 0) {
            return 0.0d;
        }

        int matchDistance = Math.max(len1, len2) / 2 - 1;
        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);

            for (int j = start; j < end; j++) {
                if (s2Matches[j]) {
                    continue;
                }
                if (s1.charAt(i) != s2.charAt(j)) {
                    continue;
                }

                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0d;
        }

        double transpositions = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) {
                continue;
            }
            while (!s2Matches[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }

        transpositions /= 2.0d;

        return ((matches / (double) len1)
                + (matches / (double) len2)
                + ((matches - transpositions) / matches)) / 3.0d;
    }

    private int commonPrefixLength(String s1, String s2) {
        int length = Math.min(Math.min(s1.length(), s2.length()), MAX_PREFIX_LENGTH);
        int prefix = 0;

        for (int i = 0; i < length; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                break;
            }
            prefix++;
        }

        return prefix;
    }
}
