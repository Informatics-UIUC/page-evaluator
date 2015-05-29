package edu.illinois.i3.emop.apps.pageevaluator.hocr;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import edu.illinois.i3.emop.apps.pageevaluator.OCRPage;
import edu.illinois.i3.emop.apps.pageevaluator.exceptions.PageParserException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import static edu.illinois.i3.emop.apps.pageevaluator.ParseOptions.COMBINE_HYPHENATED_EOL_TOKENS;

public class HOCRPage implements OCRPage<HOCRToken> {

    // Page metadata
    private final String _pageId;
    private final String _ocrEngine;
    private final Set<String> _ocrCapabilities;
    private final ImmutableList<HOCRToken> _tokens;

    private HOCRPage(String pageId, ImmutableList<HOCRToken> tokens, String ocrEngine, Set<String> ocrCapabilities) {
        _pageId = pageId;
        _tokens = tokens;
        _ocrEngine = ocrEngine;
        _ocrCapabilities = ocrCapabilities;
    }

    public static HOCRPage parse(Reader pageReader) throws PageParserException {
        return parse(pageReader, COMBINE_HYPHENATED_EOL_TOKENS);
    }

    public static HOCRPage parse(Reader pageReader, int parseOptions) throws PageParserException {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputSource inputSource = new InputSource(pageReader);
            Document document = documentBuilder.parse(inputSource);
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

            String ocrEngine = (String) xpath.evaluate("/html/head/meta[@name='ocr-system']/@content", document, XPathConstants.STRING);
            Set<String> ocrCapabilities = Sets.newHashSet();
            String capabilities = (String) xpath.evaluate("/html/head/meta[@name='ocr-capabilities']/@content", document, XPathConstants.STRING);
            ocrCapabilities.addAll(Arrays.asList(capabilities.split(" ")));

            NodeList pagesXml = (NodeList) xpath.evaluate("//*[@class='ocr_page']", document, XPathConstants.NODESET);
            Element pageXml = (Element) pagesXml.item(0);  // we only consider the first page
            String pageId = pageXml.getAttribute("id");

            Iterator<HOCRToken> tokenIterator = new HOCRTokenIterator(pageXml).iterator();
            ImmutableList<HOCRToken> tokens;

            if ((parseOptions & COMBINE_HYPHENATED_EOL_TOKENS) > 0) {
                // combine hyphenated words at end-of-line
                ImmutableList.Builder<HOCRToken> builder = ImmutableList.builder();
                while (tokenIterator.hasNext()) {
                    HOCRToken token = tokenIterator.next();
                    String tokenText = token.text();

                    // join end of line hyphenated words
                    if (token.isLastTokenOnLine() && tokenText.endsWith("-") && tokenIterator.hasNext()) {
                        HOCRToken nextToken = tokenIterator.next();
                        token = new CombinedHOCRToken(token, nextToken);
                    }

                    if (token.text().isEmpty())
                        continue;

                    builder.add(token);
                }

                tokens  = builder.build();
            } else
                tokens = ImmutableList.copyOf(tokenIterator);

            return new HOCRPage(pageId, tokens, ocrEngine, ocrCapabilities);
        }
        catch (Exception e) {
            throw new PageParserException(e);
        }
    }

    public String pageId() {
        return _pageId;
    }

    public String ocrEngine() {
        return _ocrEngine;
    }

    public Set<String> ocrCapabilities() {
        return _ocrCapabilities;
    }

    @Override
    public ImmutableList<HOCRToken> tokens() {
        return _tokens;
    }
}
