/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.gfx3d;
import snap.geom.Path;
import snap.geom.PathIter;
import snap.geom.Polygon;
import snap.geom.Seg;
import java.util.*;

/**
 * This class represents a path in 3D space.
 */
public class Path3D extends Shape3D implements Cloneable {
    
    // The list of elements in this path
    private List<Seg>  _elements = new ArrayList<>();
    
    // The list of point3Ds in this path
    private List<Point3D>  _points = new ArrayList<>();
    
    // A list of Path3Ds to be drawn in front of this Path3D
    private List<Path3D>  _layers;

    // The path normal vector
    private Vector3D  _normal;
    
    // Cached array of this Path3D to efficiently satisfy getPath3Ds() method
    private Path3D[]  _path3Ds = { this };

    // The VertexBuffer holding triangles of Path3D
    private VertexBuffer  _trianglesVB;
    
    // Cached pointers for iterating efficiently over the path
    private int  _nextElementIndex = -100;
    private int  _nextPointIndex = -100;

    // Constants for path construction element types
    public static final Seg MOVE_TO = Seg.MoveTo;
    public static final Seg LINE_TO = Seg.LineTo;
    public static final Seg QUAD_TO = Seg.QuadTo;
    public static final Seg CURVE_TO = Seg.CubicTo;
    public static final Seg CLOSE = Seg.Close;

    /**
     * Constructor.
     */
    public Path3D()
    {
        super();
    }

    /**
     * Constructor for a 2D path with a depth.
     */
    public Path3D(Path aPath, double aDepth)
    {
        addPath(aPath, aDepth);
    }

    /**
     * Returns the number of elements in the path3d.
     */
    public int getElementCount()  { return _elements.size(); }

    /**
     * Returns the element type at the given index.
     */
    public Seg getElement(int anIndex)  { return _elements.get(anIndex); }

    /**
     * Returns the number of points in the path3d.
     */
    public int getPointCount()  { return _points.size(); }

    /**
     * Returns the point3d at the given index.
     */
    public Point3D getPoint(int anIndex)  { return _points.get(anIndex); }

    /**
     * Returns the element at the given index.
     */
    public Seg getElement(int anIndex, Point3D[] pts)
    {
        // Get element type (if no points, just return type)
        Seg type = getElement(anIndex);
        if (pts == null)
            return type;

        // If given index isn't equal to "next index" optimizer, reset next index ivar
        if (anIndex != _nextElementIndex) {
            _nextPointIndex = 0;
            for (int i=0; i<anIndex; i++) {
                Seg t = _elements.get(i);
                _nextPointIndex += t==MOVE_TO || t==LINE_TO ? 1 : t==QUAD_TO ? 2 : t==CURVE_TO ? 3 : 0;
            }
        }

        // Handle element types
        switch (type) {
            case MoveTo:
            case LineTo:
                pts[0] = getPoint(_nextPointIndex++);
                break;
            case QuadTo:
                pts[0] = getPoint(_nextPointIndex++);
                pts[1] = getPoint(_nextPointIndex++);
                break;
            case CubicTo:
                pts[0] = getPoint(_nextPointIndex++);
                pts[1] = getPoint(_nextPointIndex++);
                pts[2] = getPoint(_nextPointIndex++);
                break;
            case Close: break;
        }

        // Update next element pointer and return
        _nextElementIndex = anIndex+1;
        return type;
    }

    /**
     * Adds a moveto to the path3d with the given 3D coords.
     */
    public void moveTo(double x, double y, double z)
    {
        _elements.add(MOVE_TO);
        _points.add(new Point3D(x, y, z));
        clearCachedValues();
    }

    /**
     * Adds a line to the path3d with the given 3D coords.
     */
    public void lineTo(double x, double y, double z)
    {
        _elements.add(LINE_TO);
        _points.add(new Point3D(x, y, z));
        clearCachedValues();
    }

    /**
     * Adds a quad to to the path3d with the given 3D control point and coords.
     */
    public void quadTo(double cpx, double cpy, double cpz, double x, double y, double z)
    {
        _elements.add(QUAD_TO);
        _points.add(new Point3D(cpx, cpy, cpz));
        _points.add(new Point3D(x, y, z));
        clearCachedValues();
    }

    /**
     * Adds a curve-to to the path3d with the given 3d coords.
     */
    public void curveTo(double cp1x,double cp1y,double cp1z,double cp2x,double cp2y,double cp2z,double x,double y,double z)
    {
        _elements.add(CURVE_TO);
        _points.add(new Point3D(cp1x, cp1y, cp1z));
        _points.add(new Point3D(cp2x, cp2y, cp2z));
        _points.add(new Point3D(x, y, z));
        clearCachedValues();
    }

    /**
     * Adds a close element to the path3d.
     */
    public void close()
    {
        _elements.add(CLOSE);
    }

    /**
     * Adds a 2D path to the path3D at the given depth.
     */
    public void addPath(Path aPath, double aDepth)
    {
        // Iterate over elements in given path
        PathIter piter = aPath.getPathIter(null);
        double[] pts = new double[6];
        for (int i = 0; piter.hasNext(); i++) {
            switch (piter.getNext(pts)) {
                case MoveTo:
                    if (i+1 < aPath.getSegCount() && aPath.getSeg(i+1) != Seg.MoveTo)
                        moveTo(pts[0], pts[1], aDepth);
                    break;
                case LineTo:
                    lineTo(pts[0], pts[1], aDepth);
                    break;
                case QuadTo:
                    quadTo(pts[0], pts[1], aDepth, pts[2], pts[3], aDepth);
                    break;
                case CubicTo:
                    curveTo(pts[0], pts[1], aDepth, pts[2], pts[3], aDepth, pts[4], pts[5], aDepth);
                    break;
                case Close: close(); break;
            }
        }
    }

    /**
     * Returns the center point of the path.
     */
    public Point3D getCenter()
    {
        Box3D boundsBox = getBoundsBox();
        return boundsBox.getCenter();
    }

    /**
     * Returns the normal of the path3d. Right hand rule for clockwise/counter-clockwise defined polygons.
     */
    public Vector3D getNormal()
    {
        // If already set, just return
        if (_normal != null) return _normal;

        // Calculate least-square-fit normal. Works for either convex or concave polygons.
        // Reference is Newell's Method for Computing the Plane Equation of a Polygon.
        //   Graphics Gems III, David Kirk (Ed.), AP Professional, 1992.
        Vector3D normal = new Vector3D(0, 0, 0);
        for (int i=0, pc=getPointCount(); i<pc; i++) {
            Point3D cur = getPoint(i);
            Point3D next = getPoint((i+1) % pc);
            normal.x += (cur.y - next.y) * (cur.z + next.z);
            normal.y += (cur.z - next.z) * (cur.x + next.x);
            normal.z += (cur.x - next.x) * (cur.y + next.y);
        }

        // Normalize the result and swap sign so it matches right hand rule
        normal.normalize();
        normal.negate();
        return _normal = normal;
    }

    /**
     * Returns whether path is surface.
     */
    public boolean isSurface()
    {
        Vector3D normal = getNormal();
        return !Double.isNaN(normal.x);
    }

    /**
     * Reverses the path3d.
     */
    public void reverse()
    {
        reverse(0, null, null);
    }

    /**
     * Reverse method worker method.
     */
    private void reverse(int element, Point3D lastPoint, Point3D lastMoveTo)
    {
        // Simply return if element is beyond bounds
        if (element == getElementCount()) {
            _elements.clear(); _points.clear(); clearCachedValues();
            return;
        }

        // Get info for this element
        Point3D[] pts = new Point3D[3];
        Point3D lp = null;
        Point3D lmt = lastMoveTo;
        Seg type = getElement(element, pts);
        switch (type) {
            case MoveTo: lmt = pts[0];
            case LineTo: lp = pts[0]; break;
            case QuadTo: lp = pts[1]; break;
            case CubicTo: lp = pts[2]; break;
            case Close: lp = lastMoveTo;
        }

        // Recursively add following elements before this one
        Seg nextType = element+1 < getElementCount() ? getElement(element+1,null) : null;
        reverse(element+1, lp, lmt);

        // Add reverse element to path for current element
        switch (type) {
            case MoveTo:
                if (nextType != MOVE_TO)
                    close();
                break;
            case LineTo:
                if (!lastPoint.equals(lastMoveTo))
                    lineTo(lastPoint.x, lastPoint.y, lastPoint.z);
                break;
            case QuadTo:
                quadTo(pts[0].x, pts[0].y, pts[0].z, lastPoint.x, lastPoint.y, lastPoint.z);
                break;
            case CubicTo:
                curveTo(pts[1].x, pts[1].y, pts[1].z, pts[0].x, pts[0].y, pts[0].z, lastPoint.x, lastPoint.y, lastPoint.z);
                break;
            case Close:
                moveTo(lastMoveTo.x, lastMoveTo.y, lastMoveTo.z);
                lineTo(lastPoint.x, lastPoint.y, lastPoint.z);
                break;
        }
    }

    /**
     * Transforms the path by the given transform3d.
     */
    public void transform(Transform3D xform)
    {
        for (int i=0, iMax=getPointCount(); i<iMax; i++)
            getPoint(i).transform(xform);
        clearCachedValues();
    }

    /**
     * Transforms the path so the normal is aligned with the given vector.
     */
    public void align(Vector3D aVector)
    {
        // Get transform to vector (just return if IDENTITY)
        Transform3D xfm = getTransformToAlignToVector(aVector);
        if (xfm == Transform3D.IDENTITY)
            return;

        // Transform this Path3D
        transform(xfm);
    }

    /**
     * Returns the transform to make this Path3D align with given vector.
     */
    public Transform3D getTransformToAlignToVector(Vector3D aVector)
    {
        // Get angle between Path3D.Normal and given vector
        Vector3D norm = getNormal();
        double angle = norm.getAngleBetween(aVector);
        if (angle == 0 || angle == 180) // THIS IS WRONG - NO 180!!!
            return Transform3D.IDENTITY;

        // Get axis about which to rotate the path (its the cross product of Path3D.Normal and given vectors)
        Vector3D rotAxis = norm.getCrossProduct(aVector);

        // Create the rotation matrix
        Transform3D rotMatrix = new Transform3D();
        rotMatrix.rotateAboutAxis(rotAxis, angle);

        // The point of rotation is located at the shape's center
        Point3D rotOrigin = getCenter();

        Transform3D xform = new Transform3D();
        xform.translate(-rotOrigin.x, -rotOrigin.y, -rotOrigin.z);
        xform.multiply(rotMatrix);
        xform.translate(rotOrigin.x, rotOrigin.y, rotOrigin.z);
        return xform;
    }

    /**
     * Returns a path for the path3d.
     */
    public Path getPath()
    {
        // Create new path
        Path path = new Path();
        Point3D[] pts = new Point3D[3];

        // Iterate over this path3d and add segments as 2D
        for (int i = 0, iMax = getElementCount(); i < iMax; i++) {
            Seg type = getElement(i, pts);
            switch (type) {
                case MoveTo: path.moveTo(pts[0].x, pts[0].y); break;
                case LineTo: path.lineTo(pts[0].x, pts[0].y); break;
                case QuadTo: path.quadTo(pts[0].x, pts[0].y, pts[1].x, pts[1].y); break;
                case CubicTo: path.curveTo(pts[0].x, pts[0].y, pts[1].x, pts[1].y, pts[2].x, pts[2].y); break;
                case Close: path.close();
            }
        }

        // Draw surface normals - handy for debugging
        //Point3D c = getCenter(); Vector3D n = getNormal(); path.moveTo(c.x,c.y); path.lineTo(c.x+n.x*20,c.y+.y*20);
        return path;
    }

    /**
     * Returns the triangle paths.
     */
    public VertexBuffer getTrianglesVB()
    {
        // If already set, just return
        if (_trianglesVB != null) return _trianglesVB;

        // Create, set, return
        VertexBuffer triVB = createTrianglesVB();
        return _trianglesVB = triVB;
    }

    /**
     * Creates the triangle paths.
     */
    protected VertexBuffer createTrianglesVB()
    {
        // Create VertexBuffer
        VertexBuffer vbuf = new VertexBuffer();

        // If no normal, just return empty
        Vector3D pathNormal = getNormal();
        if (Double.isNaN(pathNormal.x))
            return vbuf;

        // Get copy facing Z
        Vector3D zFacing = new Vector3D(0, 0, 1);
        Transform3D xfmToZ = getTransformToAlignToVector(zFacing);
        Transform3D xfmFromZ = xfmToZ.invert();
        Path3D copy = copyForTransform(xfmToZ);
        double zVal = copy.getMinZ();

        // Get Path2D, break into triangles
        Path path2D = copy.getPath();
        Polygon[] triangles = Polygon.getConvexPolys(path2D, 3);

        // Create loop variables
        Point3D p0 = new Point3D(0, 0, 0);
        Point3D p1 = new Point3D(0, 0, 0);
        Point3D p2 = new Point3D(0, 0, 0);
        Point3D[] points = { p0, p1, p2 };
        Vector3D pointsNormal = new Vector3D(0, 0, 0);

        // Get Path3Ds
        for (int i = 0, iMax = triangles.length; i < iMax; i++) {

            // Get triangle points
            Polygon triangle = triangles[i];
            p0.x = triangle.getX(0);
            p0.y = triangle.getY(0);
            p1.x = triangle.getX(1);
            p1.y = triangle.getY(1);
            p2.x = triangle.getX(2);
            p2.y = triangle.getY(2);
            p0.z = p1.z = p2.z = zVal;

            // Transform points back and add to VertexBuffer
            p0.transform(xfmFromZ);
            p1.transform(xfmFromZ);
            p2.transform(xfmFromZ);

            // If points normal facing backwards, reverse points (swap p0 and p2)
            Vector3D.getNormalForPoints(pointsNormal, points);
            if (pointsNormal.equals(pathNormal)) {
                double px = p0.x, py = p0.y, pz = p0.z;
                p0.x = p2.x; p0.y = p2.y; p0.z = p2.z;
                p2.x = px; p2.y = py; p2.z = pz;
            }

            // Add points to VertexBuffer
            vbuf.addValues3(p0.x, p0.y, p0.z);
            vbuf.addValues3(p1.x, p1.y, p1.z);
            vbuf.addValues3(p2.x, p2.y, p2.z);
        }

        // Return Triangle VertexBuffer
        return vbuf;
    }

    /**
     * Returns the bounds box.
     */
    @Override
    protected Box3D createBoundsBox()
    {
        // Create and init bounds box
        Box3D boundsBox = new Box3D();
        boundsBox.setMinXYZ(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        boundsBox.setMaxXYZ(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
        for (int i = 0, iMax = getPointCount(); i < iMax; i++) {
            Point3D pt = getPoint(i);
            boundsBox.addXYZ(pt.x, pt.y, pt.z);
        }

        // Return
        return boundsBox;
    }

    /**
     * Returns layers to be drawn in front of this path.
     */
    public List <Path3D> getLayers()  { return _layers!=null ? _layers : Collections.EMPTY_LIST; }

    /**
     * Adds a path to be drawn immediately in front of this path.
     */
    public void addLayer(Path3D aPath)
    {
        if (_layers == null) _layers = new ArrayList<>();
        _layers.add(aPath);
    }

    /**
     * Clears cached values when path changes.
     */
    @Override
    protected void clearCachedValues()
    {
        super.clearCachedValues();
        _normal = null;
        _trianglesVB = null;
    }

    /**
     * Compares ZMin for this path and given path.
     */
    public int compareZMin(Path3D path2)
    {
        double z0 = getMinZ();
        double z1 = path2.getMinZ();
        return z0 < z1 ? Sort3D.ORDER_BACK_TO_FRONT : z1 < z0 ? Sort3D.ORDER_FRONT_TO_BACK : 0;
    }

    /**
     * Returns whether this path is in front (FRONT_TO_BACK) or aPath in front (BACK_TO_FRONT).
     * Returns ORDER_SAME if the two paths are coplanar, or INDETERMINATE if they intersect.
     */
    public int comparePlane(Path3D aPath)
    {
        double d1 = 0;
        for (int i = 0, iMax = aPath.getPointCount(); i < iMax; i++) {
            Point3D pnt = aPath.getPoint(i);
            double d2 = getDistance(pnt);
            if (d1 == 0)
                d1 = d2;
            if (d2 != 0 && d1 * d2 < 0)
                return Sort3D.ORDER_INEDETERMINATE;
        }

        // If all points are above aPath's plane, return BACK_TO_FRONT (receiver in front), otherwise ORDER_DESCEND
        return d1 > 0 ? Sort3D.ORDER_BACK_TO_FRONT : d1 < 0 ? Sort3D.ORDER_FRONT_TO_BACK : Sort3D.ORDER_SAME;
    }

    /**
     * Returns the distance from a point to the plane of this polygon.
     */
    public double getDistance(Point3D aPoint)
    {
        Vector3D normal = getNormal();
        Point3D p0 = getPoint(0);
        double d = -normal.x * p0.x - normal.y * p0.y - normal.z * p0.z;
        double dist = normal.x * aPoint.x + normal.y * aPoint.y + normal.z * aPoint.z + d;
        return Math.abs(dist) < .01 ? 0 : dist;
    }

    /**
     * Copies path for given transform.
     */
    public Path3D copyForTransform(Transform3D aTrans)
    {
        Path3D copy = clone();
        copy.transform(aTrans);
        if (_layers != null)
            for (Path3D layer : copy._layers)
                layer.transform(aTrans);
        return copy;
    }

    /**
     * Standard clone implementation.
     */
    public Path3D clone()
    {
        // Normal clone
        Path3D clone;
        try { clone = (Path3D) super.clone(); }
        catch(Exception e) { throw new RuntimeException(e); }

        // Reset _path3ds
        clone._path3Ds = new Path3D[] { clone };

        // Copy elements
        clone._elements = new ArrayList<>(_elements);
        clone._points = new ArrayList<>(_points.size());
        for (Point3D pnt : _points)
            clone._points.add(pnt.clone());
        if (_layers != null) {
            clone._layers = new ArrayList<>(_layers.size());
            for (Path3D path3D : _layers)
                clone._layers.add(path3D.clone());
        }

        // Return clone
        return clone;
    }

    /**
     * Returns the array of Path3D that can render this shape.
     */
    public Path3D[] getPath3Ds()  { return _path3Ds; }

    /**
     * Standard toStringProps implementation.
     */
    public String toStringProps()
    {
        String boundsProps = getBoundsBox().toStringProps();
        return "PointCount=" + getPointCount() + ", " + boundsProps;
    }
}