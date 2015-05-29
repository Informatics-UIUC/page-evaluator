package edu.illinois.i3.emop.apps.pageevaluator.hocr;

import com.google.common.base.MoreObjects;

import java.util.Properties;

public class CombinedHOCRToken extends HOCRToken {
    private final HOCRToken _firstToken;
    private final HOCRToken _secondToken;

    public CombinedHOCRToken(HOCRToken firstToken, HOCRToken secondToken) {
        super(
                combineTokenText(firstToken.text(), secondToken.text()),
                combineTokenProperties(firstToken.properties(), secondToken.properties())
        );

        _firstToken = firstToken;
        _secondToken = secondToken;
    }

    public HOCRToken firstToken() { return _firstToken; }
    public HOCRToken secondToken() { return _secondToken; }

    private static String combineTokenText(String text1, String text2) {
        return text1.substring(0, text1.length() - 1) + text2;
    }

    private static Properties combineTokenProperties(Properties p1, Properties p2) {
        Properties props = new Properties();

        for (String key : p1.stringPropertyNames())
            props.put(key + "_1", p1.getProperty(key));

        for (String key : p2.stringPropertyNames())
            props.put(key + "_2", p2.getProperty(key));

        return props;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("text", text())
                .add("properties", properties())
                .add("firstToken", firstToken())
                .add("secondToken", secondToken())
                .toString();
    }
}
