/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.gfx3d;
import snap.gfx.*;

/**
 * This class represents a 3D shape to be rendered in a G3DView.
 */
public abstract class Shape3D {

    // Shape fill
    private Color  _color;
    
    // Shape stroke
    private Stroke  _stroke = Stroke.Stroke1;
    
    // Shape Stroke color
    private Color  _strokeColor;
    
    // Shape opacity
    private double  _opacity = 1;

    // The path bounding box
    private Box3D  _boundsBox;

    /**
     * Constructor.
     */
    public Shape3D()  { }

    /**
     * Returns the color of shape.
     */
    public Color getColor()  { return _color; }

    /**
     * Sets the color of shape.
     */
    public void setColor(Color aColor)  { _color = aColor; }

    /**
     * Returns the stroke of shape.
     */
    public Stroke getStroke()  { return _stroke; }

    /**
     * Sets the stroke of shape.
     */
    public void setStroke(Stroke aStroke)
    {
        _stroke = aStroke;
    }

    /**
     * Returns the stroke color of shape.
     */
    public void setStroke(Color aColor, double aWidth)
    {
        setStrokeColor(aColor);
        setStroke(Stroke.getStrokeRound(aWidth));
    }

    /**
     * Returns the stroke color of shape.
     */
    public Color getStrokeColor()  { return _strokeColor; }

    /**
     * Sets the stroke color of shape.
     */
    public void setStrokeColor(Color aColor)
    {
        _strokeColor = aColor;
    }

    /**
     * Returns the opacity of shape.
     */
    public double getOpacity()  { return _opacity; }

    /**
     * Sets the opacity of shape.
     */
    public void setOpacity(double aValue)
    {
        _opacity = aValue;
    }

    /**
     * Returns the bounds box.
     */
    public Box3D getBoundsBox()
    {
        if (_boundsBox != null) return _boundsBox;
        Box3D boundsBox = createBoundsBox();
        return _boundsBox = boundsBox;
    }

    /**
     * Creates the bounds box.
     */
    protected abstract Box3D createBoundsBox();

    /**
     * Returns the max X for the path.
     */
    public double getMinX()  { return getBoundsBox().getMinX(); }

    /**
     * Returns the max Y for the path.
     */
    public double getMinY()  { return getBoundsBox().getMinY(); }

    /**
     * Returns the max Z for the path.
     */
    public double getMinZ()  { return getBoundsBox().getMinZ(); }

    /**
     * Returns the max X for the path.
     */
    public double getMaxX()  { return getBoundsBox().getMaxX(); }

    /**
     * Returns the max Y for the path.
     */
    public double getMaxY()  { return getBoundsBox().getMaxY(); }

    /**
     * Returns the max Z for the path.
     */
    public double getMaxZ()  { return getBoundsBox().getMaxZ(); }

    /**
     * Returns the array of Path3D that can render this shape.
     */
    public abstract Path3D[] getPath3Ds();

    /**
     * Clears cached values when shape changes.
     */
    protected void clearCachedValues()
    {
        _boundsBox = null;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        String className = getClass().getSimpleName();
        String propsStr = toStringProps();
        return className + " { " + propsStr + " }";
    }

    /**
     * Standard toStringProps implementation.
     */
    public String toStringProps()
    {
        Box3D boundsBox = getBoundsBox();
        Point3D minXYZ = boundsBox.getMinXYZ();
        Point3D maxXYZ = boundsBox.getMaxXYZ();
        return "MinXYZ=" + minXYZ + ", MaxXYZ=" + maxXYZ;
    }
}