/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.view;
import snap.geom.Insets;
import snap.geom.Path;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.util.*;

/**
 * A class to show a path shape.
 */
public class PathView extends View {

    // The path shape
    private Path  _path;

    /**
     * Constructor.
     */
    public PathView()
    {
        super();
    }

    /**
     * Returns the path.
     */
    public Path getPath()
    {
        if (_path != null) return _path;
        Path path = new Path();
        return _path = path;
    }

    /**
     * Sets the path.
     */
    public void setPath(Path aPath)
    {
        _path = aPath;
        repaint();
    }

    /**
     * Returns the path in shape bounds.
     */
    public Path getPathInBounds()
    {
        return getPath().copyFor(getBoundsLocal());
    }

    /**
     * Replace the polygon's current path with a new path, adjusting the shape's bounds to match the new path.
     */
    public void resetPath(Shape aShape)
    {
        // Get the transform to parent shape coords
        //Transform toParentXF = getLocalToParent();

        // Set the new path and new size
        Path newPath = new Path(aShape);
        setPath(newPath);
        Rect bounds = newPath.getBounds();
        setSizeLocal(bounds.getWidth(), bounds.getHeight());

        // Transform to parent for new x & y
        //Rect boundsInParent = bounds.clone(); toParentXF.transform(boundsInParent);
        //setFrameXY(boundsInParent.getXY());
    }

    /**
     * Override to return path as bounds shape.
     */
    public Shape getBoundsShape()
    {
        return getPath().copyFor(getBoundsLocal());
    }

    /**
     * Calculates the preferred width.
     */
    protected double getPrefWidthImpl(double aH)
    {
        Insets ins = getInsetsAll();
        return ins.left + getPath().getBounds().getMaxX() + ins.right;
    }

    /**
     * Calculates the preferred height.
     */
    protected double getPrefHeightImpl(double aW)
    {
        Insets ins = getInsetsAll();
        return ins.top + getPath().getBounds().getMaxY() + ins.bottom;
    }

    /**
     * XML archival.
     */
    public XMLElement toXML(XMLArchiver anArchiver)
    {
        // Do normal version
        XMLElement e = super.toXML(anArchiver);

        // Archive path
        e.add(_path.toXML(anArchiver));
        return e;
    }

    /**
     * XML unarchival.
     */
    public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Do normal version
        super.fromXML(anArchiver, anElement);

        // Unarchive path
        XMLElement pathXML = anElement.get("path");
        _path = anArchiver.fromXML(pathXML, Path.class, this);
        return this;
    }

}