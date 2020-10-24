package snap.geom;
import java.util.Arrays;
import java.util.Objects;

/**
 * A class to manage an array of Segs, an array of points, and an array of Seg-to-Point-Indexes so that it can quickly
 * return all points for a given Seg index.
 */
public class SegPoints extends Shape {

    // The array of segments
    protected Seg  _segs[] = new Seg[8];

    // The segment count
    private int  _scount;

    // The array of points
    protected double  _points[] = new double[16];

    // The number of points
    private int  _pcount;

    // The indexes to start point for each seg (or to ClosePointIndexes for Close seg)
    private int  _segPointIndexes[] = new int[8];

    // Whether path is closed
    private boolean  _closed;

    // The next path (if segs added after close)
    private SegPoints  _nextPath;

    // The total arc length of path
    private double  _arcLength;

    // The arc lengths for each seg
    private double  _arcLengths[];

    // The winding - how a path determines what to fill when segments intersect
    private int  _wind = WIND_EVEN_ODD;

    // Constants for winding
    public static final int WIND_EVEN_ODD = PathIter.WIND_EVEN_ODD;
    public static final int WIND_NON_ZERO = PathIter.WIND_NON_ZERO;

    /**
     * Constructor.
     */
    public SegPoints()  { }

    /**
     * Constructor.
     */
    public SegPoints(Shape aShape)
    {
    }

    /**
     * Returns the winding - how a path determines what to fill when segments intersect.
     */
    public int getWinding()  { return _wind; }

    /**
     * Sets the winding - how a path determines what to fill when segments intersect.
     */
    public void setWinding(int aValue)  { _wind = aValue; }

    /**
     * Returns the number of segments.
     */
    public int getSegCount()  { return _scount; }

    /**
     * Returns the individual segment at given index.
     */
    public Seg getSeg(int anIndex)  { return _segs[anIndex]; }

    /**
     * Adds a segment.
     */
    protected void addSeg(Seg aSeg)
    {
        // If at end of Segs array, extend by 2x
        if (_scount+1>_segs.length) {
            _segs = Arrays.copyOf(_segs, _segs.length * 2);
            _segPointIndexes = Arrays.copyOf(_segPointIndexes, _segPointIndexes.length * 2);
        }

        // Add Seg at end, increment SegCount, notify shapeChanged
        _segs[_scount++] = aSeg;
        shapeChanged();
    }

    /**
     * Returns the number of points.
     */
    public int getPointCount()  { return _pcount; }

    /**
     * Returns individual point at given index.
     */
    public Point getPoint(int anIndex)
    {
        double px = _points[anIndex*2];
        double py = _points[anIndex*2+1];
        return new Point(px, py);
    }

    /**
     * Adds a point.
     */
    protected void addPoint(double x, double y)
    {
        // If at end of Points array, extend by 2x
        if (_pcount*2+1>_points.length)
            _points = Arrays.copyOf(_points, _points.length*2);

        // Add points at end and increment PointCount
        _points[_pcount*2] = x;
        _points[_pcount*2+1] = y;
        _pcount++;
    }

    /**
     * Returns the array of point-indexes for given seg index.
     */
    public int getSegPointIndex(int anIndex)
    {
        return _segPointIndexes[anIndex];
    }

    /**
     * Adds an array of point-indexes.
     */
    protected void addSegPointIndex(int pointIndex)
    {
        _segPointIndexes[_scount-1] = pointIndex;
    }

    /**
     * Returns the points for a given seg index, by copying to given array (should be length of 8).
     */
    public Seg getSegAndPointsForIndex(int anIndex, double theCoords[])
    {
        // Get Seg and Seg PointIndex
        Seg seg = getSeg(anIndex);
        int pointIndex = getSegPointIndex(anIndex);

        // Handle Close special: PointIndex is to index in ClosePointIndexes (an array of each close {start,end} index)
        if (seg==Seg.Close) {
            theCoords[0] = _points[pointIndex];
            theCoords[1] = _points[pointIndex+1];
            theCoords[2] = _points[0];
            theCoords[3] = _points[1];
        }

        // Copy Seg points to given point coord array
        else {
            int pointCount = seg.getCount() + 1;
            for (int i=0; i<pointCount; i++)
                theCoords[i] = _points[pointIndex + i];
        }

        // Return seg
        return seg;
    }

    /**
     * Returns the points for a given seg index, by copying to given array (should be length of 8).
     */
    public Seg getSegEndPointsForIndex(int anIndex, double theCoords[])
    {
        // Get seg and point index
        Seg seg = getSeg(anIndex);
        int pointIndex = getSegPointIndex(anIndex) + 1;

        // Copy end points to given point coord array
        int pointCount = seg.getCount();
        for (int i=0; i<pointCount; i++)
            theCoords[i] = _points[pointIndex + i];

        // Return seg
        return seg;
    }

    /**
     * Moveto.
     */
    public void moveTo(double x, double y)
    {
        // If closed, forward
        if (_closed) {
            getNextPathWithIntentToExtend().moveTo(x, y);
            return;
        }

        // Check for consecutive moveTo
        if (getSegCount()>0 && getSeg(getSegCount()-1)==Seg.MoveTo)
            System.err.println("SegPoints.moveTo: Consecutive MoveTo");

        // Add MoveTo
        addSeg(Seg.MoveTo);

        // Get pointIndex, add point, add pointIndexes for pointIndex
        int pointIndex = getPointCount();
        addPoint(x, y);
        addSegPointIndex(pointIndex);
    }

    /**
     * LineTo.
     */
    public void lineTo(double x, double y)
    {
        // If closed, forward
        if (_closed) {
            getNextPathWithIntentToExtend().lineTo(x, y);
            return;
        }

        // Add LineTo
        addSeg(Seg.LineTo);

        // Get pointIndex to last point (make sure there is a moveTo)
        int pointIndex = getPointCount() - 1;
        if (pointIndex<0) { moveTo(0, 0); pointIndex = 0; }

        // Add point and pointIndex
        addPoint(x, y);
        addSegPointIndex(pointIndex);
    }

    /**
     * QuadTo.
     */
    public void quadTo(double cpx, double cpy, double x, double y)
    {
        // If closed, forward
        if (_closed) {
            getNextPathWithIntentToExtend().quadTo(cpx, cpy, x, y);
            return;
        }

        // Add QuadTo
        addSeg(Seg.QuadTo);

        // Get pointIndex to last point (make sure there is a moveTo)
        int pointIndex = getPointCount() - 1;
        if (pointIndex<0) { moveTo(0, 0); pointIndex = 0; }

        // Add control point, end point
        addPoint(cpx, cpy);
        addPoint(x,y);

        // Add SegPointIndex
        addSegPointIndex(pointIndex);
    }

    /**
     * CubicTo.
     */
    public void curveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y)
    {
        // If closed, forward
        if (_closed) {
            getNextPathWithIntentToExtend().curveTo(cp1x, cp1x, cp2x, cp2y, x, y);
            return;
        }

        // Add CubicTo
        addSeg(Seg.CubicTo);

        // Get pointIndex to last point (make sure there is a moveTo)
        int pointIndex = getPointCount() - 1;
        if (pointIndex<0) { moveTo(0, 0); pointIndex = 0; }

        // Add control points and end point
        addPoint(cp1x, cp1y);
        addPoint(cp2x, cp2y);
        addPoint(x, y);

        // Add SegPointIndex
        addSegPointIndex(pointIndex);
    }

    /**
     * Close.
     */
    public void close()
    {
        // If closed, forward
        if (_closed) {
            getNextPathWithIntentToExtend().close();
            return;
        }

        // Get pointIndex to last point (if no points, just return - don't close empty path)
        int pointIndex = getPointCount() - 1;
        if (pointIndex<0)
            return;

        // Add Close
        addSeg(Seg.Close);
        addSegPointIndex(pointIndex);
        _closed = true;
    }

    /**
     * Returns the next path.
     */
    public SegPoints getNextPath()  { return _nextPath; }

    /**
     * Returns the next path.
     */
    protected SegPoints getNextPathWithIntentToExtend()
    {
        if (_nextPath==null)
            _nextPath = new SegPoints();
        shapeChanged();
        return _nextPath;
    }

    /**
     * Returns the segment at index.
     */
    public Segment getSegment(int anIndex)
    {
        // Get Seg and Seg PointIndex
        Seg seg = getSeg(anIndex);
        int pointIndex = getSegPointIndex(anIndex);

        double p0x = _points[pointIndex];
        double p0y = _points[pointIndex+1];

        // Handle Line
        if (seg==Seg.LineTo) {
            double p1x = _points[pointIndex+2];
            double p1y = _points[pointIndex+3];
            return new Line(p0x, p0y, p1x, p1y);
        }

        // Handle QuadTo
        else if (seg==Seg.QuadTo) {
            double cpx = _points[pointIndex+2];
            double cpy = _points[pointIndex+3];
            double p1x = _points[pointIndex+4];
            double p1y = _points[pointIndex+5];
            return new Quad(p0x, p0y, cpx, cpy, p1x, p1y);
        }

        // Handle CubicTo
        else if (seg==Seg.CubicTo) {
            double cp0x = _points[pointIndex+2];
            double cp0y = _points[pointIndex+3];
            double cp1x = _points[pointIndex+4];
            double cp1y = _points[pointIndex+5];
            double p1x = _points[pointIndex+6];
            double p1y = _points[pointIndex+7];
            return new Cubic(p0x, p0y, cp0x, cp0y, cp1x, cp1y, p1x, p1y);
        }

        // Handle Close special: PointIndex is to index in ClosePointIndexes (an array of each close {start,end} index)
        else if (seg==Seg.Close) {
            double p1x = _points[0];
            double p1y = _points[1];
            return new Line(p0x, p0y, p1x, p1y);
        }

        // No segement
        return null;
    }

    /**
     * Returns the total arc length of path.
     */
    public double getArcLength()
    {
        if (_arcLengths!=null) return _arcLength;
        getArcLengths();
        return _arcLength;
    }

    /**
     * Returns the total arc length of path.
     */
    public double getSegArcLength(int anIndex)
    {
        double arcLens[] = getArcLengths();
        return arcLens[anIndex];
    }

    /**
     * Returns the lengths array.
     */
    public double[] getArcLengths()
    {
        // If already set, just return
        if (_arcLengths!=null) return _arcLengths;

        // Get iter vars
        int segCount = getSegCount();
        double arcLengths[] = new double[segCount];
        double pnts[] = new double[8];
        double arcLength = 0;

        // Iterate over segs and calc lengths
        for (int i=0; i<segCount; i++) {

            Seg seg = getSegAndPointsForIndex(i, pnts);
            double len = 0;

            // Get arcLength for seg
            switch (seg) {
                case MoveTo: break;
                case LineTo:
                    len = Point.getDistance(pnts[0], pnts[1], pnts[2], pnts[3]);
                    break;
                case QuadTo:
                    len = SegmentLengths.getArcLengthQuad(pnts[0], pnts[1], pnts[2], pnts[3], pnts[4], pnts[5]);
                    break;
                case CubicTo:
                    len = SegmentLengths.getArcLengthCubic(pnts[0], pnts[1], pnts[2], pnts[3], pnts[4], pnts[5], pnts[6], pnts[7]);
                    break;
                default: throw new RuntimeException("SegPoints.getArcLengths: Unsuppored seg: " + seg);
            }

            // Update arcLengths
            arcLengths[i] = len;
            arcLength += len;
        }

        // Set/return arcLengths
        _arcLength = arcLength;
        return _arcLengths = arcLengths;
    }

    @Override
    protected void shapeChanged()
    {
        super.shapeChanged();

    }

    /**
     * Standard clone implementation.
     */
    public SegPoints clone()
    {
        SegPoints copy; try { copy = (SegPoints) super.clone(); }
        catch(Exception e) { throw new RuntimeException(e); }
        copy._segs = Arrays.copyOf(_segs, _segs.length);
        copy._segPointIndexes = Arrays.copyOf(_segPointIndexes, _segs.length);
        copy._points = Arrays.copyOf(_points, _points.length);
        if (_nextPath!=null)
            copy._nextPath = _nextPath.clone();
        return copy;
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        // Check identity & class and get other path
        if (anObj==this) return true;
        SegPoints other = anObj instanceof Path ? (SegPoints) anObj : null;
        if (other==null)
            return false;

        // Check ElementCount, WindingRule, Elements and Points
        if (other._scount!=_scount || other._pcount!=_pcount)
            return false;
        if (!Arrays.equals(other._segs, _segs))
            return false;
        if (!Arrays.equals(other._points, _points))
            return false;
        if (!Objects.equals(other._nextPath, _nextPath))
            return false;
        return true; // Return true since all checks passed
    }

    /**
     * Returns a path iterator.
     */
    public PathIter getPathIter(Transform aTrans)
    {
        return new SPPathIter(aTrans);
    }

    /**
     * A PathIter for Path.
     */
    private class SPPathIter extends PathIter {

        // Ivars
        private int  _segCount;
        private int  _sindex;
        private double  _pnts[] = new double[8];
        private PathIter  _nextIter;

        /** Constructor. */
        SPPathIter(Transform aTrans)
        {
            super(aTrans);
            _segCount = getSegCount();
            if (_nextPath!=null)
                _nextIter = _nextPath.getPathIter(aTrans);
        }

        /** Returns whether PathIter has another segment. */
        public boolean hasNext()
        {
            return _sindex < _segCount || (_nextIter!=null && _nextIter.hasNext());
        }

        /** Returns the next segment. */
        public Seg getNext(double coords[])
        {
            if (_sindex < _segCount)
                return getSegEndPointsForIndex(_sindex++, _pnts);
            return _nextIter.getNext(coords);
        }

        /** Returns the winding - how a path determines what to fill when segments intersect. */
        public int getWinding()  { return SegPoints.this.getWinding(); }
    }
}
