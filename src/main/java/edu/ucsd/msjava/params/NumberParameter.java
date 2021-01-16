package edu.ucsd.msjava.params;

import org.apache.commons.lang3.StringUtils;

public abstract class NumberParameter<T extends Number> extends Parameter {
    protected T value;

    protected T minValue;        // default: inclusive
    protected T maxValue;        // default: exclusive
    protected boolean isMinInclusive = true;
    protected boolean isMaxInclusive = false;

    public NumberParameter(String key, String name, String description) {
        super(key, name, description);
    }

    public NumberParameter<T> defaultValue(T defaultValue) {
        value = defaultValue;
        super.setOptional();
        return this;
    }

    public NumberParameter<T> minValue(T minValue) {
        this.minValue = minValue;
        return this;
    }

    public NumberParameter<T> maxValue(T maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    public NumberParameter<T> setMinExclusive() {
        this.isMinInclusive = false;
        return this;
    }

    public NumberParameter<T> setMaxInclusive() {
        this.isMaxInclusive = true;
        return this;
    }

    protected String getValidRange() {
        return (isMinInclusive ? "[" : "(") + minValue + "," + maxValue + (isMaxInclusive ? "]" : ")");
    }

    @Override
    public abstract String parse(String value);

    @Override
    public String getValueAsString() {
        return String.valueOf(value);
    }

    /**
     * Remove the specified characters from the end of the value
     * @param value
     * @param stripChars
     * @return
     */
    public String trimTrailingChars(String value, String stripChars)
    {
        return StringUtils.stripEnd(value, stripChars);
    }

    public T getValue() {
        return value;
    }
}
