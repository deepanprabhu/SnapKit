/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.gfx3d;
import snap.util.StringUtils;

/**
 *  This class represents a 3D point.
 */
public class Point3D implements Cloneable {
    
    // X Y Z components
    public double x, y, z;
    
    /**
     * Constructor.
     */
    public Point3D()  { }

    /**
     * Constructor for the x, y, z components.
     */
    public Point3D(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Sets the point from components.
     */
    public void setPoint(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Sets the point from components.
     */
    public void setPoint(Point3D aPoint)
    {
        setPoint(aPoint.x, aPoint.y, aPoint.z);
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        Point3D p = (Point3D) anObj;
        return p == this || (p.x == x && p.y == y && p.z == z);
    }

    /**
     * Standard clone implementation.
     */
    public Point3D clone()
    {
        return new Point3D(x,y,z);
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return "Point3D [" + StringUtils.toString(x) + " " + StringUtils.toString(y) + " " + StringUtils.toString(z) + "]";
    }
}