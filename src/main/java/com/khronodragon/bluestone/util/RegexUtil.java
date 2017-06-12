package com.khronodragon.bluestone.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RegexUtil {
    public static Iterable<MatchResult> iterMatches(final Pattern p, final CharSequence input) {
        return () -> new Iterator<MatchResult>() {
            // Use a matcher internally.
            final Matcher matcher = p.matcher(input);
            // Keep a match around that supports any interleaving of hasNext/next calls.
            MatchResult pending;

            public boolean hasNext() {
                // Lazily fill pending, and avoid calling find() multiple times if the
                // clients call hasNext() repeatedly before sampling via next().
                if (pending == null && matcher.find()) {
                    pending = matcher.toMatchResult();
                }
                return pending != null;
            }

            public MatchResult next() {
                // Fill pending if necessary (as when clients call next() without
                // checking hasNext()), throw if not possible.
                if (!hasNext()) { throw new NoSuchElementException(); }
                // Consume pending so next call to hasNext() does a find().
                MatchResult next = pending;
                pending = null;
                return next;
            }

            public void remove() { throw new UnsupportedOperationException(); }
        };
    }

    public static Stream<MatchResult> matchStream(final Pattern pat, final CharSequence input) {
        return StreamSupport.stream(iterMatches(pat, input).spliterator(), false);
    }
}
