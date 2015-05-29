package edu.illinois.i3.emop.apps.pageevaluator;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import edu.illinois.i3.emop.apps.pageevaluator.exceptions.PageParserException;
import edu.illinois.i3.emop.apps.pageevaluator.exceptions.UnsupportedLanguageException;
import edu.illinois.i3.emop.apps.pageevaluator.hocr.HOCRPage;
import edu.illinois.i3.emop.apps.pageevaluator.txt.TxtPage;
import edu.illinois.i3.spellcheck.engine.SpellDictionary;
import edu.illinois.i3.spellcheck.engine.SpellDictionaryHashMap;
import opennlp.tools.tokenize.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Reader;

import static edu.illinois.i3.emop.apps.pageevaluator.NLPToolsFactory.SimpleTokenizers.*;
import static edu.illinois.i3.emop.apps.pageevaluator.PageQualityIndicators.DefaultIndicators.*;
import static edu.illinois.i3.emop.apps.pageevaluator.PageStatsCalculator.DefaultStats.*;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public enum DocumentFormat {
        TXT, HOCR
    }

    public static void main(String[] args) {
        try {
            // Extract command line arguments
            JSAPResult cmdLine = parseArguments(args);
            DocumentFormat format = DocumentFormat.valueOf(cmdLine.getString("format").toUpperCase());
            File pageOcrFile = cmdLine.getFile("pageOcrFile");
            boolean quiet = cmdLine.getBoolean("quiet");
            File dictionaryFile = cmdLine.getFile("dictionary");
            String tokenizerType = cmdLine.getString("tokenizer");
            String language = cmdLine.getString("language");

            if (!quiet)
                log.info("Processing {}: {}", format, pageOcrFile);

            // Read the page
            OCRPage<? extends OCRToken> page;
            try (Reader pageReader = Files.newReader(pageOcrFile, Charsets.UTF_8)) {
                Tokenizer tokenizer = createTokenizer(tokenizerType, language);
                page = readPage(pageReader, pageOcrFile.getName(), format, tokenizer);
            }

            // Compute page stats
            PageStatsCalculator.OCRPageStats pageStats = PageStatsCalculator.calculateStatistics(page);

            // Load the dictionary and spell check the page tokens
            if (dictionaryFile != null) {
                try (Reader dictReader = Files.newReader(dictionaryFile, Charsets.UTF_8)) {
                    final SpellDictionary dictionary = new SpellDictionaryHashMap(dictReader);
                    Iterable<? extends OCRToken> correctTokens = FluentIterable.from(page.tokens())
                            .filter(new Predicate<OCRToken>() {
                                @Override
                                public boolean apply(OCRToken ocrToken) {
                                    String tokenText = ocrToken.text();
                                    String cleanText = PageStatsCalculator.cleanToken(tokenText);
                                    boolean isCorrect = dictionary.isCorrect(cleanText);
                                    return isCorrect;
                                }
                            });
                    int numCorrectTokens = Iterables.size(correctTokens);

                    pageStats.put("numCorrectTokens", numCorrectTokens);
                }
            }

            PageQualityIndicators pageQuality = computePageQualityIndicators(pageStats);
            if (pageQuality != null) {
                System.out.println(String.format("%s\t%.2f\t%.2f", pageOcrFile.getName(),
                        pageQuality.getDouble(TextQuality), pageQuality.getDouble(SpellingQuality)));
            }

        }
        catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static PageQualityIndicators computePageQualityIndicators(PageStatsCalculator.OCRPageStats pageStats) {
        double tokenCount = pageStats.getDouble(TokenCount);

        if (tokenCount == 0)
            return null;

        PageQualityIndicators pageQuality = new PageQualityIndicators();
        double textTokenCount = tokenCount - pageStats.getInt(PunctTokenCount) - pageStats.getInt(NumberObjectsTokenCount);
        double textQuality = pageStats.getDouble(CleanAllAlphaNoRepTokenCount) / textTokenCount;
        pageQuality.put(TextQuality, textQuality);

        Double numCorrectTokens = pageStats.getDouble("numCorrectTokens");
        if (numCorrectTokens != null) {
            double spellingQuality = numCorrectTokens / textTokenCount;
            pageQuality.put(SpellingQuality, spellingQuality);
        }

        return pageQuality;
    }

    private static Tokenizer createTokenizer(String tokenizerType, String language) throws UnsupportedLanguageException {
        if (tokenizerType.equalsIgnoreCase("simple"))
            return NLPToolsFactory.createSimpleTokenizer(Simple);

        else

        if (tokenizerType.equalsIgnoreCase("whitespace"))
            return NLPToolsFactory.createSimpleTokenizer(Whitespace);

        else
            return NLPToolsFactory.createTokenizer(language);
    }

    private static OCRPage<? extends OCRToken> readPage(Reader pageReader, String id, DocumentFormat format,
                                                        Tokenizer tokenizer) throws PageParserException {

        OCRPage<? extends OCRToken> ocrPage;

        switch (format) {
            case HOCR:
                ocrPage = HOCRPage.parse(pageReader);
                break;

            case TXT:
                ocrPage = TxtPage.parse(pageReader, id, tokenizer);
                break;

            default:
                throw new RuntimeException("Unsupported format: " + format);
        }

        return ocrPage;
    }

    private static Parameter[] getApplicationParameters() {
        Parameter format = new FlaggedOption("format")
                .setStringParser(EnumeratedStringParser.getParser("txt;hocr"))
                .setDefault("hocr")
                .setShortFlag('f')
                .setHelp("Specifies the format of the page OCR file");

        Parameter dictionary = new FlaggedOption("dictionary")
                .setStringParser(
                        FileStringParser.getParser()
                            .setMustBeFile(true)
                            .setMustExist(true)
                )
                .setShortFlag('d')
                .setHelp("Specifies the dictionary to use for spell checking of tokens");

        Parameter quiet = new Switch("quiet")
                .setShortFlag('q')
                .setDefault("false")
                .setHelp("Enables quiet mode - only page scores are printed, separated by a comma");

        Parameter tokenizer = new FlaggedOption("tokenizer")
                .setStringParser(EnumeratedStringParser.getParser("simple;whitespace;model"))
                .setDefault("model")
                .setShortFlag('t')
                .setHelp("Specifies the OpenNLP tokenizer to use; " +
                        "see http://opennlp.apache.org/documentation/manual/opennlp.html#tools.tokenizer.introduction");

        Parameter language = new FlaggedOption("language")
                .setStringParser(EnumeratedStringParser.getParser("da;de;en;es;nl;pt;se"))
                .setDefault("en")
                .setShortFlag('l')
                .setHelp("Specifies the language to use when constructing the model-based tokenizer (if requested)");

        Parameter pageOcrFile = new UnflaggedOption("pageOcrFile")
                .setStringParser(
                        FileStringParser.getParser()
                                .setMustBeFile(true)
                                .setMustExist(true))
                .setRequired(true)
                .setHelp("The page OCR file");

        return new Parameter[] { format, dictionary, quiet, tokenizer, language, pageOcrFile };
    }

    private static String getApplicationHelp() {
        return "Compute a score that estimates the correctability of an OCR'd page";
    }

    private static JSAPResult parseArguments(String[] args) throws JSAPException {
        SimpleJSAP jsap = new SimpleJSAP("PageEvaluator", getApplicationHelp(), getApplicationParameters());
        JSAPResult result = jsap.parse(args);

        if (jsap.messagePrinted())
            System.exit(1);

        return result;
    }
}
