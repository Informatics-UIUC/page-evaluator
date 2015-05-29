package edu.illinois.i3.emop.apps.pageevaluator;

import com.google.common.collect.ImmutableList;

public interface OCRPage<T extends OCRToken> {

    String pageId();
    ImmutableList<T> tokens();

}
