package edu.illinois.i3.emop.apps.pageevaluator.txt;

import com.google.common.base.MoreObjects;
import edu.illinois.i3.emop.apps.pageevaluator.OCRToken;

public class TxtToken implements OCRToken {

    private final String _text;

    public TxtToken(String text) {
        _text = text;
    }

    @Override
    public String text() {
        return _text;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("text", _text)
                .toString();
    }
}
