/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.text;

import snap.geom.HPos;

/**
 * This class represents a line of text in a Text.
 */
public abstract class BaseTextLine implements CharSequence, Cloneable {

    // The BaseText that contains this line
    protected BaseText  _text;

    // The StringBuffer that holds line chars
    protected StringBuffer  _sb = new StringBuffer();

    // The char index of the start of this line in text
    protected int  _start;

    // The run for this line
    protected BaseTextRun[]  _runs = EMPTY_RUNS;

    // The line style
    protected TextLineStyle  _lineStyle;

    // The index of this line in text
    protected int  _index;

    // The width of this line
    protected double _width = -1;

    // Constants
    private static final BaseTextRun[] EMPTY_RUNS = new BaseTextRun[0];

    /**
     * Constructor.
     */
    public BaseTextLine(BaseText aBaseText)
    {
        _text = aBaseText;
        _lineStyle = _text.getDefaultLineStyle();
        //addRun(createRun(), 0);
    }

    /**
     * Returns the RichText.
     */
    public BaseText getText()  { return _text; }

    /**
     * Returns the length of this text line.
     */
    public int length()  { return _sb.length(); }

    /**
     * Returns the char value at the specified index.
     */
    public char charAt(int anIndex)  { return _sb.charAt(anIndex); }

    /**
     * Returns a new char sequence that is a subsequence of this sequence.
     */
    public CharSequence subSequence(int aStart, int anEnd)  { return _sb.subSequence(aStart, anEnd); }

    /**
     * Returns the index of given string in line.
     */
    public int indexOf(String aStr, int aStart)  { return _sb.indexOf(aStr, aStart); }

    /**
     * Returns the string for the line.
     */
    public String getString()  { return _sb.toString(); }

    /**
     * Returns the start char index of this line in text.
     */
    public int getStart()  { return _start; }

    /**
     * Returns the end char index of this line in text.
     */
    public int getEnd()  { return _start + length(); }

    /**
     * Returns the index of this line in text.
     */
    public int getIndex()  { return _index; }

    /**
     * Returns the number of runs for this line.
     */
    public int getRunCount()  { return _runs.length; }

    /**
     * Returns the individual run at given index.
     */
    public BaseTextRun getRun(int anIndex)  { return _runs[anIndex]; }

    /**
     * Returns the line runs.
     */
    public BaseTextRun[] getRuns()  { return _runs; }

    /**
     * Returns the line style.
     */
    public TextLineStyle getLineStyle()  { return _lineStyle; }

    /**
     * Sets the line style.
     */
    public void setLineStyle(TextLineStyle aLineStyle)
    {
        _lineStyle = aLineStyle;
    }

    /**
     * Returns the width of line.
     */
    public double getWidth()
    {
        // If already set, just return
        if (_width >= 0) return _width;

        // Get, set, return
        double width = getWidthImpl();
        return _width = width;
    }

    /**
     * Returns the width of line.
     */
    protected abstract double getWidthImpl();

    /**
     * Returns the head run for the line.
     */
    public BaseTextRun getRunForCharIndex(int anIndex)
    {
        // Iterate over runs and return run containing char index
        for (BaseTextRun run : _runs)
            if (anIndex < run.getEnd())
                return run;

        // If char index at line end, return last run
        if (anIndex == length())
            return getRunLast();

        // Complain
        throw new IndexOutOfBoundsException("Index " + anIndex + " beyond " + length());
    }

    /**
     * Returns the last run.
     */
    public BaseTextRun getRunLast()
    {
        int runCount = getRunCount();
        return runCount > 0 ? getRun(runCount - 1) : null;
    }

    /**
     * Returns the next line, if available.
     */
    public BaseTextLine getNext()
    {
        return _text != null && _index + 1 < _text.getLineCount() ? _text.getLine(_index + 1) : null;
    }

    /**
     * Returns the alignment associated with this line.
     */
    public HPos getAlignX()  { return _lineStyle.getAlign(); }

    /**
     * Sets the alignment associated with this line.
     */
    public void setAlignX(HPos anAlign)
    {
        TextLineStyle lineStyle = getLineStyle().copyFor(anAlign);
        setLineStyle(lineStyle);
    }

    /**
     * Returns the last char.
     */
    public char getLastChar()
    {
        int len = length();
        return len > 0 ? charAt(len - 1) : 0;
    }

    /**
     * Returns whether line ends with space.
     */
    public boolean isLastCharWhiteSpace()
    {
        char c = getLastChar();
        return c == ' ' || c == '\t';
    }

    /**
     * Returns whether run ends with newline.
     */
    public boolean isLastCharNewline()
    {
        char c = getLastChar();
        return c == '\r' || c == '\n';
    }

    /**
     * Updates length due to change in given run.
     */
    protected void updateRuns(int aRunIndex)
    {
        // Get BaseRun and Length at end of BaseRun
        BaseTextRun baseRun = aRunIndex >= 0 ? getRun(aRunIndex) : null;
        int length = baseRun != null ? baseRun.getEnd() : 0;

        // Iterate over runs beyond BaseRun and update Index, Start and Length
        for (int i = aRunIndex + 1, iMax = getRunCount(); i < iMax; i++) {
            BaseTextRun run = getRun(i);
            run._index = i;
            run._start = length;
            length += run.length();
        }
    }

    /**
     * Updates text.
     */
    protected void updateText()
    {
        if (_text != null)
            _text.updateLines(getIndex());
        _width = -1;
    }

    /**
     * Standard clone implementation.
     */
    public BaseTextLine clone()
    {
        // Do normal version
        BaseTextLine clone;
        try { clone = (BaseTextLine) super.clone(); }
        catch (Exception e) { throw new RuntimeException(e); }

        // Clone StringBuffer, Runs
        clone._sb = new StringBuffer(_sb);
        clone._runs = _runs.clone();
        for (int i = 0; i < _runs.length; i++) {
            BaseTextRun runClone = clone._runs[i] = _runs[i].clone();
            runClone._textLine = clone;
        }

        // Return
        return clone;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        String className = getClass().getSimpleName();
        String propStrings = toStringProps();
        return className + " { " + propStrings + " }";
    }

    /**
     * Standard toStringProps implementation.
     */
    public String toStringProps()
    {
        StringBuilder sb = new StringBuilder();

        // Add Start, End, Length, Index, String
        sb.append("Start=").append(getStart());
        sb.append(", End=").append(getEnd());
        sb.append(", Length=").append(length());
        sb.append(", Index=").append(getIndex());

        // Append String
        String string = getString().replace("\n", "\\n");
        sb.append(", String=").append(string);

        // Return
        return sb.toString();
    }
}