/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.gfx3d;
import snap.geom.*;
import snap.gfx.Color;
import java.util.Arrays;

/**
 * This class represents a polygon surface in 3D space.
 */
public class Poly3D extends FacetShape implements Cloneable {

    // The float array to hold actual vertex point components
    private float[]  _pointArray = new float[24];

    // The number of components in vertex points array
    private int  _pointArrayLen = 0;

    // The float array to hold actual vertex color components
    private float[]  _colorArray = new float[0];

    // The number of components in vertex colors array
    private int  _colorArrayLen = 0;

    // The float array to hold vertex texture coords
    private float[]  _texCoordArray = new float[0];

    // The number of entries in vertex texture coords array
    private int  _texCoordArrayLen = 0;

    // Constants
    public static final int POINT_COMP_COUNT = 3;
    public static final int COLOR_COMP_COUNT = 4;
    public static final int TEX_COORD_COMP_COUNT = 2;

    /**
     * Constructor.
     */
    public Poly3D()
    {
        super();
    }

    /**
     * Constructor.
     */
    public Poly3D(Shape aShape, double aDepth)
    {
        super();
        addShapePath(aShape, aDepth);
    }

    /**
     * Returns the vertex points components array.
     */
    public float[] getPointArray()
    {
        trim();
        return _pointArray;
    }

    /**
     * Returns the number of vertex points in array.
     */
    @Override
    public int getPointCount()  { return _pointArrayLen / POINT_COMP_COUNT; }

    /**
     * Returns the Point3D at index.
     */
    @Override
    public Point3D getPoint(int anIndex)
    {
        return getPoint(new Point3D(), anIndex);
    }

    /**
     * Returns the Point3D at index.
     */
    public Point3D getPoint(Point3D aPoint, int anIndex)
    {
        int index = anIndex * POINT_COMP_COUNT;
        aPoint.x = _pointArray[index + 0];
        aPoint.y = _pointArray[index + 1];
        aPoint.z = _pointArray[index + 2];
        return aPoint;
    }

    /**
     * Adds value triplet to array.
     */
    public void addPoint(double aVal1, double aVal2, double aVal3)
    {
        // Expand Vertex components array if needed
        if (_pointArrayLen + 3 > _pointArray.length)
            _pointArray = Arrays.copyOf(_pointArray, Math.max(_pointArray.length * 2, 24));

        // Add values
        _pointArray[_pointArrayLen++] = (float) aVal1;
        _pointArray[_pointArrayLen++] = (float) aVal2;
        _pointArray[_pointArrayLen++] = (float) aVal3;
    }

    /**
     * Returns the vertex colorscomponents array.
     */
    public float[] getColorArray()
    {
        trim();
        return _colorArray;
    }

    /**
     * Adds a color to vertex color components array.
     */
    public void addColor(Color aColor)
    {
        // Expand color components array if needed
        if (_colorArrayLen + COLOR_COMP_COUNT > _colorArray.length)
            _colorArray = Arrays.copyOf(_colorArray, Math.max(_colorArray.length * 2, 24));

        // Add values
        _colorArray[_colorArrayLen++] = (float) aColor.getRed();
        _colorArray[_colorArrayLen++] = (float) aColor.getGreen();
        _colorArray[_colorArrayLen++] = (float) aColor.getBlue();
        if (COLOR_COMP_COUNT > 3)
            _colorArray[_colorArrayLen++] = (float) aColor.getAlpha();
    }

    /**
     * Returns whether texture coords array is set.
     */
    public boolean isTexCoordArraySet()  { return _texCoordArray != null && _texCoordArray.length > 0; }

    /**
     * Returns the vertex texture coords array.
     */
    public float[] getTexCoordArray()
    {
        trim();
        return _texCoordArray;
    }

    /**
     * Adds a texture coord to vertex texture coords array.
     */
    public void addTexCoord(double aU, double aV)
    {
        // Expand color components array if needed
        if (_texCoordArrayLen + TEX_COORD_COMP_COUNT > _texCoordArray.length)
            _texCoordArray = Arrays.copyOf(_texCoordArray, Math.max(_texCoordArray.length * 2, 24));

        // Add values
        _texCoordArray[_texCoordArrayLen++] = (float) aU;
        _texCoordArray[_texCoordArrayLen++] = (float) aV;
    }

    /**
     * Returns the normal of the path3d. Right hand rule for clockwise/counter-clockwise defined polygons.
     */
    @Override
    protected Vector3D createNormal()
    {
        return Vector3D.getNormalForPoints3fv(new Vector3D(), _pointArray, getPointCount());
    }

    /**
     * Returns the 2D shape for the path3d (should only be called when path is facing Z).
     */
    @Override
    public Shape getShape2D()
    {
        return new Shape() {
            public PathIter getPathIter(Transform aTransform)
            {
                return new PointArrayPathIter2D(_pointArray, _pointArrayLen, aTransform);
            }
        };
    }

    /**
     * Creates a VertexArray of path triangles.
     */
    @Override
    protected VertexArray createTriangleArray()
    {
        // Create/configure VertexArray
        VertexArray triangleArray = new VertexArray();
        triangleArray.setColor(getColor());
        triangleArray.setDoubleSided(isDoubleSided());

        // If no normal, just return empty
        Vector3D pathNormal = getNormal();
        if (Double.isNaN(pathNormal.x))
            return triangleArray;

        // Get/set polygon points in VertexArray
        float[] pointArray = getPointArray();
        triangleArray.setPointArray(pointArray);

        // Add colors
        //for (Color color : _colors)
        //    vertexArray.addColor(color);

        // If Texture/TexCoordArray is set, configure in VertexArray
        Texture texture = getTexture();
        if (texture != null && isTexCoordArraySet()) {
            triangleArray.setTexture(texture);
            float[] texCoordArray = getTexCoordArray();
            triangleArray.setTexCoordArray(texCoordArray);
        }

        // Get/set point index array for triangle points in VertexArray
        int[] indexArrayForTrianglePoints = getIndexArrayForTrianglePoints();
        triangleArray.setIndexArray(indexArrayForTrianglePoints);

        // Handle Stroke: Create/add stroke VertexArray
        if (getStrokeColor() != null) {
            VertexArray strokeVA = getStrokeTriangleArray();
            triangleArray.setLast(strokeVA);
        }

        // Handle Painter: Create/add painterVertexArray
        Painter3D painter3D = getPainter();
        if (painter3D != null) {
            VertexArray painterVA = getPainterTriangleArray();
            triangleArray.setLast(painterVA);
        }

        // Return
        return triangleArray;
    }

    /**
     * Returns the array of indexes to points for triangles from polygon points.
     */
    protected int[] getIndexArrayForTrianglePoints()
    {
        // Get copy of path facing Z
        Matrix3D localToFacingZ = getTransformToAlignToVector(0, 0, 1);
        Poly3D polyFacingZ = copyForMatrix(localToFacingZ);

        // Get Poly2D, break into triangles
        Shape poly2D = polyFacingZ.getShape2D();
        PointArrayIndex trianglePointArray = PointArrayIndex.newTrianglePointArrayForShape(poly2D);

        // Get/set point index array in VertexArray
        int[] indexArray = trianglePointArray.getIndexArray();
        return indexArray;
    }

    /**
     * Returns a VertexArray for path stroke.
     */
    protected VertexArray getStrokeTriangleArray()
    {
        // Get info
        Vector3D facetNormal = getNormal();
        Color strokeColor = getStrokeColor();
        double strokeWidth = getStroke() != null ? getStroke().getWidth() : 1;

        // Create/configure VertexArray
        VertexArray triangleArray = new VertexArray();
        triangleArray.setColor(strokeColor != null ? strokeColor : Color.BLACK);

        // Path3D iteration vars
        int pointCount = getPointCount();
        float[] pointArray = getPointArray();
        Point3D p0 = new Point3D(pointArray[0], pointArray[1], pointArray[2]);
        Point3D p1 = new Point3D();

        // Iterate over points and add line stroke
        for (int i = 0; i <= pointCount; i++) {
            int i3 = i % pointCount * 3;
            p1.x = pointArray[i3];
            p1.y = pointArray[i3 + 1];
            p1.z = pointArray[i3 + 2];
            VertexArrayUtils.addLineStrokePoints(triangleArray, p0, p1, facetNormal, strokeWidth);
            p0.setPoint(p1);
        }

        // Return
        return triangleArray;
    }

    /**
     * Transforms the path by the given transform matrix.
     */
    public void transform(Matrix3D xform)
    {
        float[] pointArray = getPointArray();
        int pointCount = getPointCount();
        xform.transformXYZArray(pointArray, pointCount);
        clearCachedValues();
    }

    /**
     * Copies path for given transform matrix.
     */
    @Override
    public Poly3D copyForMatrix(Matrix3D aTrans)
    {
        Poly3D copy = clone();
        copy.transform(aTrans);
        return copy;
    }

    /**
     * Reverses the path3d.
     */
    public void reverse()
    {
        // Reverse PointArray
        float[] pointArray = getPointArray();
        int pointCount = getPointCount();
        reverseArray(pointArray, pointCount, POINT_COMP_COUNT);

        // Reverse ColorArray
        float[] colorArray = getColorArray();
        int colorCount = _colorArrayLen / COLOR_COMP_COUNT;
        reverseArray(colorArray, colorCount, COLOR_COMP_COUNT);

        // Clear cached values
        clearCachedValues();
    }

    /**
     * Trims arrays.
     */
    public void trim()
    {
        // Trim PointArray
        if (_pointArray.length != _pointArrayLen)
            _pointArray = Arrays.copyOf(_pointArray, _pointArrayLen);

        // Trim ColorArray
        if (_colorArray.length != _colorArrayLen)
            _colorArray = Arrays.copyOf(_colorArray, _colorArrayLen);

        // Trim TexCoordArray
        if (_texCoordArray.length != _texCoordArrayLen)
            _texCoordArray = Arrays.copyOf(_texCoordArray, _texCoordArrayLen);
    }

    /**
     * Adds a Shape path to this path3D at given depth.
     */
    public void addShapePath(Shape aPath, double aDepth)
    {
        // Iterate over elements in given path
        PathIter pathIter = aPath.getPathIter(null);
        double[] pts = new double[6];

        // Iterate over elements in given path
        while (pathIter.hasNext()) {

            // Get/handle Seg
            Seg seg = pathIter.getNext(pts);
            switch (seg) {

                // Handle MoveTo
                case MoveTo:
                    if (getPointCount() > 0)
                        System.err.println("Poly3D.addShapePath: Poly3D cannot have multiple moveTos");
                    addPoint(pts[0], pts[1], aDepth);
                    break;

                // Handle LineTo
                case LineTo:
                    addPoint(pts[0], pts[1], aDepth);
                    break;

                // Handle QuadTo
                case QuadTo:
                    System.err.println("Poly3D.addShapePath: Poly3D cannot add non-flat path (QuadTo)");
                    break;

                // Handle CubicTo
                case CubicTo:
                    System.err.println("Poly3D.addShapePath: Poly3D cannot add non-flat path (CubicTo)");
                    break;

                // Handle Close
                case Close: break;
            }
        }
    }

    /**
     * Returns the bounds.
     */
    @Override
    protected Bounds3D createBounds3D()
    {
        // Create and init bounds
        Bounds3D bounds = new Bounds3D();
        bounds.setMinXYZ(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        bounds.setMaxXYZ(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

        // Iterate over points
        float[] pointArray = getPointArray();
        int arrayCount = getPointCount() * POINT_COMP_COUNT;
        for (int i = 0; i < arrayCount; i += POINT_COMP_COUNT) {
            float x = pointArray[i + 0];
            float y = pointArray[i + 1];
            float z = pointArray[i + 2];
            bounds.addXYZ(x, y, z);
        }

        // Return
        return bounds;
    }

    /**
     * Standard clone implementation.
     */
    @Override
    public Poly3D clone()
    {
        // Trim
        trim();

        // Do normal version
        Poly3D clone  = (Poly3D) super.clone();

        // Clone arrays
        clone._pointArray = _pointArray.clone();
        clone._colorArray = _colorArray.clone();
        clone._texCoordArray = _texCoordArray.clone();

        // Return clone
        return clone;
    }

    /**
     * Standard toStringProps implementation.
     */
    @Override
    public String toStringProps()
    {
        String superProps = super.toStringProps();
        return superProps + ", PointCount=" + getPointCount();
    }

    /**
     * Reverses a float array with given number of records of given component count.
     */
    private static void reverseArray(float[] theArray, int recordCount, int componentCount)
    {
        // Get number of values in array
        int arrayCount = recordCount * componentCount;
        int halfCount = recordCount / 2;

        // Iterate over array records
        for (int recIndex = 0; recIndex < halfCount; recIndex++) {

            // Get array index for current record and opposite array index for swap record
            int arrayIndex = recIndex * componentCount;
            int oppositeIndex = arrayCount - arrayIndex - componentCount;

            // Iterate over components and swap
            for (int compIndex = 0; compIndex < componentCount; compIndex++) {
                float temp = theArray[arrayIndex + compIndex];
                theArray[arrayIndex + compIndex] = theArray[oppositeIndex + compIndex];
                theArray[oppositeIndex + compIndex] = temp;
            }
        }
    }

    /**
     * PathIter for PointArray.
     */
    public static class PointArrayPathIter2D extends PathIter {

        // Ivars
        private float[]  _pointArray;
        private int  _pointCount;
        private int  _pointIndex;

        /**
         * Constructor.
         */
        public PointArrayPathIter2D(float[] aPointArray, int aPointArrayLength, Transform aTransform)
        {
            super(aTransform);
            _pointArray = aPointArray;
            _pointCount = aPointArrayLength / POINT_COMP_COUNT;
            if (_pointCount == 0)
                _pointIndex = 999;
        }

        /**
         * Returns whether there are more segments.
         */
        public boolean hasNext() { return _pointIndex <= _pointCount; }

        /**
         * Returns the coordinates and type of the current path segment in the iteration.
         */
        public Seg getNext(double[] coords)
        {
            // Return a lineTo for each segment
            if (_pointIndex < _pointCount) {

                // Get X/Y for PointIndex
                int pointArrayIndex = _pointIndex * POINT_COMP_COUNT;
                float x = _pointArray[pointArrayIndex + 0];
                float y = _pointArray[pointArrayIndex + 1];
                _pointIndex++;

                // Handle first segment
                if (_pointIndex == 1)
                    return moveTo(x, y, coords);

                // Handle successive segments
                return lineTo(x, y, coords);
            }

            // Close
            if (_pointIndex == _pointCount) {
                _pointIndex++;
                return close();
            }

            // Handle the impossible
            throw new RuntimeException("Poly3D.PointArrayPathIter2D: Index beyond bounds " + _pointIndex + " " + _pointCount);
        }
    }
}