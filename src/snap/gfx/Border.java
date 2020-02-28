/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.gfx;
import snap.geom.Insets;
import snap.geom.Shape;
import snap.util.*;

/**
 * A class to represent a painted stroke.
 */
public abstract class Border implements Cloneable, XMLArchiver.Archivable {
    
    // Cached version of insets
    private Insets _insets = null;
    
    // Whether to paint above view
    private boolean _paintAbove;

    // Constants for properties
    public static final String Insets_Prop = "Insets";
    public static final String PaintAbove_Prop = "PaintAbove";

    /**
     * Returns the insets.
     */
    public Insets getInsets()
    {
        if (_insets!=null) return _insets;
        return _insets = createInsets();
    }

    /**
     * Creates the insets.
     */
    protected Insets createInsets()  { return Insets.EMPTY; }

    /**
     * Returns the basic color of the border.
     */
    public Color getColor()  { return Color.BLACK; }

    /**
     * Returns the basic width of the border.
     */
    public double getWidth()  { return 1; }

    /**
     * Returns the name for border.
     */
    public String getName()  { return getClass().getSimpleName(); }

    /**
     * Returns whether the border paints above view.
     */
    public boolean isPaintAbove()  { return _paintAbove; }

    /**
     * Paint border.
     */
    public void paint(Painter aPntr, Shape aShape)  { }

    /**
     * Copies border for given color.
     */
    public Border copyForColor(Color aColor)  { return this; }

    /**
     * Copies border for given stroke width.
     */
    public Border copyForWidth(double aWidth)  { return this; }

    /**
     * Copies border for given insets.
     */
    public Border copyForInsets(Insets theIns)
    {
        Border copy = clone();
        setPropValue(Insets_Prop, theIns);
        return copy;
    }

    /**
     * Returns a border with given insets.
     */
    public Border copyFor(String aPropName, Object aValue)
    {
        Border copy = clone();
        copy.setPropValue(aPropName, aValue);
        return copy;
    }

    /**
     * Returns a value for a key.
     */
    public Object getPropValue(String aPropName)
    {
        switch (aPropName) {
            case Insets_Prop: return _insets;
            case PaintAbove_Prop: return _paintAbove;
            default: throw new RuntimeException("Border.getPropValue: Unknown key: " + aPropName);
        }
    }

    /**
     * Sets a value for a key.
     */
    protected void setPropValue(String aPropName, Object aValue)
    {
        switch (aPropName) {
            case Insets_Prop: _insets = (Insets)aValue; break;
            case PaintAbove_Prop: _paintAbove = (Boolean)aValue; break;
            default: throw new RuntimeException("Border.setPropValue: Unknown key: " + aPropName);
        }
    }

    /**
     * Standard clone implementation - only used interally (by copyFor methods).
     */
    protected Border clone()
    {
        try { return (Border)super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        // Check identity and get other
        if(anObj==this) return true;
        Border other = anObj instanceof Border? (Border)anObj : null; if(other==null) return false;

        // Check Color, Width
        if(!other.getColor().equals(getColor())) return false;
        if(other.getWidth()!=getWidth()) return false;

        // Return true since all checks passed
        return true;
    }

    /**
     * Creates an empty border for inset.
     */
    public static Borders.EmptyBorder createEmptyBorder(double w)
    {
        return new Borders.EmptyBorder(w,w,w,w);
    }

    /**
     * Creates an empty border.
     */
    public static Borders.EmptyBorder createEmptyBorder(double tp, double rt, double bm, double lt)
    {
        return new Borders.EmptyBorder(tp,rt,bm,lt);
    }

    /**
     * Creates an empty border.
     */
    public static Borders.LineBorder createLineBorder(Color aColor, double aWidth)
    {
        return new Borders.LineBorder(aColor, aWidth);
    }

    /**
     * Creates a compound border.
     */
    public static Border createCompoundBorder(Border aB1, Border aB2)
    {
        return new Borders.CompoundBorder(aB1, aB2);
    }

    /**
     * Creates a compound border.
     */
    public static Borders.BevelBorder createLoweredBevelBorder()
    {
        return new Borders.BevelBorder(Borders.BevelBorder.LOWERED);
    }

    /**
     * XML unarchival.
     */
    public static Border fromXMLBorder(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Get type
        String type = anElement.getAttributeValue("type", "");

        // Create instance based on type
        Border border;
        if(type.equals("line")) border = new Borders.LineBorder();
        else if(type.equals("bevel")) border = new Borders.BevelBorder();
        else if(type.equals("etched")) border = new Borders.EtchBorder();
        else if(type.equals("empty")) border = new Borders.EmptyBorder();
        else border = new Borders.NullBorder();

        // Unarchive border and return
        border.fromXML(anArchiver, anElement);
        return border;
    }
}