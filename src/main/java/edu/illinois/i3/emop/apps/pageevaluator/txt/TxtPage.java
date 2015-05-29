package edu.illinois.i3.emop.apps.pageevaluator.txt;


import com.google.common.base.Function;
import com.google.common.collect.*;
import com.google.common.io.CharStreams;
import edu.illinois.i3.emop.apps.pageevaluator.OCRPage;
import edu.illinois.i3.emop.apps.pageevaluator.exceptions.PageParserException;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.illinois.i3.emop.apps.pageevaluator.ParseOptions.COMBINE_HYPHENATED_EOL_TOKENS;
import static edu.illinois.i3.emop.apps.pageevaluator.ParseOptions.FUSE_TOKENIZED_CONTRACTIONS;

public class TxtPage implements OCRPage<TxtToken> {
    private static final Logger log = LoggerFactory.getLogger(TxtPage.class);
    private static final Pattern HYPHEN_WORD_MATCHER = Pattern.compile("(?m)(\\S*\\p{L})-\\n(\\p{L}\\S*)\\s*");
    private static final Set<String> CONTRACTIONS = ImmutableSet.of("'ll", "'s", "n't", "'ve", "'m", "'d", "'re");

    private final String _pageId;
    private final ImmutableList<TxtToken> _tokens;

    private TxtPage(String pageId, ImmutableList<TxtToken> tokens) {
        _pageId = pageId;
        _tokens = tokens;
    }

    public static TxtPage parse(Reader pageReader, String pageId, Tokenizer tokenizer) throws PageParserException {
        int pageOptions = COMBINE_HYPHENATED_EOL_TOKENS;
        if (tokenizer instanceof TokenizerME)
            pageOptions |= FUSE_TOKENIZED_CONTRACTIONS;

        return parse(pageReader, pageId, tokenizer, pageOptions);
    }

    public static TxtPage parse(Reader pageReader, String pageId, Tokenizer tokenizer, int parseOptions)
            throws PageParserException {

        if ((parseOptions & FUSE_TOKENIZED_CONTRACTIONS) > 0 && !(tokenizer instanceof TokenizerME))
            log.warn("Can only fuse tokenized contractions when using TokenizerME. You're using " +
                    tokenizer.getClass().getSimpleName());

        BufferedReader reader = (pageReader instanceof BufferedReader) ?
            (BufferedReader) pageReader : new BufferedReader(pageReader);

        String text;
        try {
            text = CharStreams.toString(reader);
        }
        catch (IOException e) {
            log.error("Txt parser error", e);
            throw new PageParserException(e);
        }

        if ((parseOptions & COMBINE_HYPHENATED_EOL_TOKENS) > 0) {
            // combine hyphenated words at end-of-line
            Matcher matcher = HYPHEN_WORD_MATCHER.matcher(text);
            text = matcher.replaceAll("$1$2\n");
        }

        FluentIterable<String> tokens = FluentIterable.of(tokenizer.tokenize(text));
        if ((parseOptions & FUSE_TOKENIZED_CONTRACTIONS) > 0 && (tokenizer instanceof TokenizerME))
            tokens = fuseTokenizedContractions(tokens.iterator());

        ImmutableList<TxtToken> tokenList = tokens.transform(
                new Function<String, TxtToken>() {
                    @Override
                    public TxtToken apply(String tokenText) {
                        return new TxtToken(tokenText);
                    }
                }).toList();

        return new TxtPage(pageId, tokenList);
    }

    private static FluentIterable<String> fuseTokenizedContractions(Iterator<String> tokenIterator) {
        List<String> fusedTokens = new LinkedList<>();

        String nextToken = null;
        while (nextToken != null || tokenIterator.hasNext()) {
            String token = (nextToken != null) ? nextToken : tokenIterator.next();
            nextToken = (tokenIterator.hasNext()) ? tokenIterator.next() : null;

            if (nextToken != null && CONTRACTIONS.contains(nextToken.toLowerCase())) {
                token += nextToken;
                nextToken = null;
            }

            fusedTokens.add(token);
        }

        return FluentIterable.from(fusedTokens);
    }

    public String pageId() {
        return _pageId;
    }

    @Override
    public ImmutableList<TxtToken> tokens() {
        return _tokens;
    }

}
