/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.props;
import java.text.DecimalFormat;

/**
 * This class holds utility methods for Props.
 */
public class PropUtils {

    // A formatter to format double without exponent
    private static DecimalFormat _doubleFmt = new DecimalFormat("0.#########");

    /**
     * Returns whether given object is PropNode array.
     */
    public static boolean isPropNodeArray(Object anObj)
    {
        // If not array return false
        Class<?> objClass = anObj.getClass();
        if (!objClass.isArray()) return false;

        // If array component is PropNode, return true
        Object[] array = (Object[]) anObj;
        Class<?> compClass = objClass.getComponentType();
        if (PropNode.class.isAssignableFrom(compClass))
            return true;

        // If first array item PropNode, return true
        Object comp0 = array.length > 0 ? array[0] : null;
        if (comp0 instanceof PropNode)
            return true;

        // Return false
        return false;
    }

    /**
     * Returns whether given object is empty PropNode or array.
     */
    public static boolean isEmptyNodeOrArray(Object anObj)
    {
        // Handle null?
        if (anObj == null)
            return false;

        // Handle PropNode with PropValues size 0
        if (anObj instanceof PropNode && ((PropNode) anObj).getPropNames().size() == 0)
            return true;

        // Handle array with length 0
        if (anObj.getClass().isArray()) {
            Class compClass = anObj.getClass().getComponentType();
            if (Object.class.isAssignableFrom(compClass))
                return ((Object[]) anObj).length == 0;
            else if (float.class == compClass)
                return ((float[]) anObj).length == 0;
            else if (double.class == compClass)
                return ((double[]) anObj).length == 0;
            else if (int.class == compClass)
                return ((int[]) anObj).length == 0;
            else System.err.println("PropArchiver.isEmptyObject: Unknown comp class: " + compClass);
        }

        // Return false since no PropNode or array with no values
        return false;
    }

    /**
     * Return string for double array.
     */
    public static String getStringForDoubleArray(double[] theValues)
    {
        // If empty, return empty array string
        if (theValues.length == 0) return "[ ]";

        // Create string with open bracket and first val
        StringBuilder sb = new StringBuilder("[ ");
        sb.append(_doubleFmt.format(theValues[0]));

        // Iterate over remaining vals and add separator plus val for each
        for (int i = 1; i < theValues.length; i++)
            sb.append(", ").append(_doubleFmt.format(theValues[i]));

        // Return string with close bracket
        return sb.append(" ]").toString();
    }

}
