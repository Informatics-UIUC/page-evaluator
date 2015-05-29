package edu.illinois.i3.emop.apps.pageevaluator;

import com.google.common.collect.Maps;
import edu.illinois.i3.emop.apps.pageevaluator.exceptions.UnsupportedLanguageException;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public abstract class NLPToolsFactory {

    private static final Logger log = LoggerFactory.getLogger(NLPToolsFactory.class);

    private static final Map<String, SentenceModel> _sentenceModels = Maps.newHashMap();
    private static final Map<String, TokenizerModel> _tokenizerModels = Maps.newHashMap();
    private static final Map<String, POSModel> _posModels = Maps.newHashMap();

    public enum SimpleTokenizers {
        Simple, Whitespace
    }

    public static SentenceDetector createSentenceDetector(String language) throws UnsupportedLanguageException {
        SentenceModel model = getSentenceModel(language);
        return new SentenceDetectorME(model);
    }

    public static Tokenizer createSimpleTokenizer(SimpleTokenizers tokenizer) {
        switch (tokenizer) {
            case Simple: return SimpleTokenizer.INSTANCE;
            case Whitespace: return WhitespaceTokenizer.INSTANCE;
            default: throw new RuntimeException("Unknown tokenizer: " + tokenizer.name());
        }
    }

    public static Tokenizer createTokenizer(String language) throws UnsupportedLanguageException {
        TokenizerModel model = getTokenModel(language);
        return new TokenizerME(model);
    }

    public static POSTagger createPOSTagger(String language) throws UnsupportedLanguageException {
        POSModel model = getPOSModel(language);
        return new POSTaggerME(model);
    }

    private static SentenceModel getSentenceModel(String language) throws UnsupportedLanguageException {
        synchronized (_sentenceModels) {
            SentenceModel model = _sentenceModels.get(language);
            if (model == null) {
                log.debug("Loading sentence detector model for '{}'...", language);
                String modelResourceFile = String.format("/%s-sent.bin", language);
                try (InputStream modelStream = Main.class.getResourceAsStream(modelResourceFile)) {
                    if (modelStream == null)
                        throw new FileNotFoundException(modelResourceFile);

                    model = new SentenceModel(modelStream);
                }
                catch (IOException e) {
                    throw new UnsupportedLanguageException("Cannot load sentence model for language: " + language, e);
                }

                _sentenceModels.put(language, model);
            }

            return model;
        }
    }

    private static TokenizerModel getTokenModel(String language) throws UnsupportedLanguageException {
        synchronized (_tokenizerModels) {
            TokenizerModel model = _tokenizerModels.get(language);
            if (model == null) {
                log.debug("Loading tokenizer model for '{}'...", language);
                String modelResourceFile = String.format("/%s-token.bin", language);
                try (InputStream modelStream = Main.class.getResourceAsStream(modelResourceFile)) {
                    if (modelStream == null)
                        throw new FileNotFoundException(modelResourceFile);

                    model = new TokenizerModel(modelStream);
                }
                catch (IOException e) {
                    throw new UnsupportedLanguageException("Cannot load tokenizer model for language: " + language, e);
                }

                _tokenizerModels.put(language, model);
            }

            return model;
        }
    }

    private static POSModel getPOSModel(String language) throws UnsupportedLanguageException {
        synchronized (_posModels) {
            POSModel model = _posModels.get(language);
            if (model == null) {
                log.debug("Loading part-of-speech model for '{}'...", language);
                String modelResourceFile = String.format("/%s-pos-maxent.bin", language);
                try (InputStream modelStream = Main.class.getResourceAsStream(modelResourceFile)) {
                    if (modelStream == null)
                        throw new FileNotFoundException(modelResourceFile);

                    model = new POSModel(modelStream);
                }
                catch (IOException e) {
                    throw new UnsupportedLanguageException("Cannot load part-of-speech model for language: " + language, e);
                }

                _posModels.put(language, model);
            }

            return model;
        }
    }
}
