package edu.illinois.i3.emop.apps.pageevaluator;

public class PageQualityIndicators extends KeyValueStore {
    public enum DefaultIndicators {
        TextQuality,
        SpellingQuality,
        OverallQuality
    }

    public void put(DefaultIndicators indicator, Object value) {
        put(indicator.name(), value);
    }

    public String getString(DefaultIndicators indicator) { return getString(indicator.name()); }
    public Integer getInt(DefaultIndicators indicator) { return getInt(indicator.name()); }
    public Float getFloat(DefaultIndicators indicator) { return getFloat(indicator.name()); }
    public Double getDouble(DefaultIndicators indicator) { return getDouble(indicator.name()); }
    public Boolean getBoolean(DefaultIndicators indicator) { return getBoolean(indicator.name()); }

}
