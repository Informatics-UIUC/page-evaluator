package edu.illinois.i3.emop.apps.pageevaluator.hocr;

import com.google.common.base.MoreObjects;
import edu.illinois.i3.emop.apps.pageevaluator.OCRToken;

import java.util.Properties;

public class HOCRToken implements OCRToken {
    private final String _text;
    private final Properties _tokenProperties;

    public HOCRToken(String text, Properties tokenProperties) {
        _text = text;
        _tokenProperties = tokenProperties;
    }

    public String id() {
        return _tokenProperties.getProperty("id");
    }

    public Properties properties() {
        return _tokenProperties;
    }

    public String text() {
        return _text;
    }

    public boolean isLastTokenOnLine() {
        return Boolean.parseBoolean(_tokenProperties.getProperty("isLastTokenOnLine", "false"));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("text", _text)
                .add("properties", _tokenProperties)
                .toString();
    }
}
