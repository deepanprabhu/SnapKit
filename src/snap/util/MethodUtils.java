package snap.util;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class holds special methods relating to Methods.
 */
public class MethodUtils extends ClassUtils {

    /**
     * Returns all methods for given class and subclasses that start with given prefix.
     */
    public static Method[] getMethodsForPrefix(Class aClass, String aPrefix)
    {
        List meths = new ArrayList();
        getMethodsForPrefix(aClass, aPrefix.toLowerCase(), meths, true);
        return (Method[]) meths.toArray(new Method[0]);
    }

    /**
     * Returns the method for given class, name and parameter types.
     */
    private static void getMethodsForPrefix(Class aClass, String aPrefix, List theMethods, boolean doPrivate)
    {
        Class cls = aClass.isPrimitive() ? fromPrimitive(aClass) : aClass;
        Method methods[] = cls.getDeclaredMethods();
        for (Method meth : methods) {
            if (meth.isSynthetic()) continue;
            if (meth.getName().toLowerCase().startsWith(aPrefix) && (doPrivate || !Modifier.isPrivate(meth.getModifiers())))
                theMethods.add(meth);
        }

        // If interface, recurse for extended interfaces
        if (cls.isInterface()) {
            for (Class cl : cls.getInterfaces())
                getMethodsForPrefix(cl, aPrefix, theMethods, false);
        }

        // Otherwise, recurse for extended superclass
        else if ((cls = cls.getSuperclass()) != null)
            getMethodsForPrefix(cls, aPrefix, theMethods, false);
    }

    /**
     * Returns the method for given class and name that best matches given parameter types.
     */
    public static Method getMethodBest(Class aClass, String aName, Class... theClasses)
    {
        // Get methods with compatible name/args (just return if null, no args or singleton)
        Method methods[] = getMethods(aClass, aName, theClasses, null);
        if (methods == null)
            return null;
        if (theClasses.length == 0 || methods.length == 1)
            return methods[0];

        // Rate compatible methods and return the most compatible
        Method method = null;
        int rating = 0;
        for (Method meth : methods) {
            int rtg = getRating(meth.getParameterTypes(), theClasses, meth.isVarArgs());
            if (rtg > rating) {
                method = meth;
                rating = rtg;
            }
        }
        return method;
    }

    /**
     * Returns the method for given class, name and parameter types.
     */
    private static Method[] getMethods(Class aClass, String aName, Class theClasses[], Method theResult[])
    {
        // Make sure class is non-primitive
        Class cls = aClass.isPrimitive() ? fromPrimitive(aClass) : aClass;

        // Check declared methods
        Method methods[] = getDeclaredMethods(cls, aName, theClasses, theResult);

        // If interface, check extended interfaces
        if (cls.isInterface()) {
            for (Class cl : cls.getInterfaces())
                methods = getMethods(cl, aName, theClasses, methods);
        }

        // Otherwise, check superclass
        else if ((cls = cls.getSuperclass()) != null)
            methods = getMethods(cls, aName, theClasses, methods);

        // Return methods
        return methods;
    }

    /**
     * Returns compatible declared methods for a given class, name and parameter types array.
     */
    private static Method[] getDeclaredMethods(Class aClass, String aName, Class theClasses[], Method theResult[])
    {
        // Get class methods and intern name
        Method methods[] = aClass.getDeclaredMethods();
        String name = aName.intern();

        // Iterate over methods and if compatible, add to results
        for (Method meth : methods) {
            if (meth.isSynthetic()) continue;
            if (meth.getName() == name && isCompatible(meth.getParameterTypes(), theClasses, meth.isVarArgs())) {
                theResult = theResult != null ? Arrays.copyOf(theResult, theResult.length + 1) : new Method[1];
                theResult[theResult.length - 1] = meth;
            }
        }
        return theResult;
    }

    /**
     * Returns the method for given class, name and parameter types.
     */
    public static Constructor getConstructor(Class aClass, Class theClasses[])
    {
        // Get methods that match name/args (just return if null, no args or singleton)
        if (aClass.isInterface()) return null;
        Constructor methods[] = getConstructors(aClass, theClasses, null);
        if (methods == null) return null;
        if (theClasses.length == 0 || methods.length == 1) return methods[0];

        // Rate compatible constructors and return the most compatible
        Constructor method = null;
        int rating = 0;
        for (Constructor meth : methods) {
            int rtg = getRating(meth.getParameterTypes(), theClasses, meth.isVarArgs());
            if (rtg > rating) {
                method = meth;
                rating = rtg;
            }
        }
        return method;
    }

    /**
     * Returns the method for given class, name and parameter types.
     */
    private static Constructor[] getConstructors(Class aClass, Class theClasses[], Constructor theResult[])
    {
        Class cls = aClass.isPrimitive() ? fromPrimitive(aClass) : aClass;
        Constructor[] methods = getDeclaredConstructors(cls, theClasses, theResult);
        if ((cls = cls.getSuperclass()) != null)
            methods = getConstructors(cls, theClasses, methods);
        return methods;
    }

    /**
     * Returns the declared method for a given class, name and parameter types array.
     */
    private static Constructor[] getDeclaredConstructors(Class aClass, Class theClasses[], Constructor theResult[])
    {
        // Iterate over declared constructors and if compatible, add to results
        Constructor[] methods = aClass.getDeclaredConstructors();
        for (Constructor meth : methods)
            if (isCompatible(meth.getParameterTypes(), theClasses, meth.isVarArgs())) {
                theResult = theResult != null ? Arrays.copyOf(theResult, theResult.length + 1) : new Constructor[1];
                theResult[theResult.length - 1] = meth;
            }
        return theResult;
    }

    /**
     * Returns whether arg classes are compatible.
     */
    protected static boolean isCompatible(Class params[], Class theClasses[], boolean isVarArgs)
    {
        // Handle Var args
        if (isVarArgs) {

            // If standard args don't match return false
            if (theClasses.length < params.length - 1 || !isAssignable(params, theClasses, params.length - 1))
                return false;

            // Get VarArgClass
            Class varArgArrayClass = params[params.length - 1];
            Class varArgClass = varArgArrayClass.getComponentType();

            // If only one arg and it is of array class, return true
            Class argClass = theClasses.length == params.length ? theClasses[params.length - 1] : null;
            if (argClass != null && argClass.isArray() && varArgArrayClass.isAssignableFrom(argClass))
                return true;

            // If any var args don't match, return false
            for (int i = params.length - 1; i < theClasses.length; i++)
                if (theClasses[i] != null && !isAssignable(varArgClass, theClasses[i]))
                    return false;
            return true;
        }

        // Handle normal method
        return params.length == theClasses.length && isAssignable(params, theClasses, params.length);
    }

    /**
     * Returns a rating of a method for given possible arg classes.
     */
    protected static int getRating(Class theParamTypes[], Class theClasses[], boolean isVarArgs)
    {
        // Iterate over classes and add score based on matching classes
        // This is a punt - need to groc the docs on this: https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html
        Class[] classes = theParamTypes;
        int clen = classes.length;
        if (isVarArgs) clen--;
        int rating = 0;
        for (int i = 0, iMax = clen; i < iMax; i++) {
            Class cls1 = classes[i];
            Class cls2 = theClasses[i];
            if (cls1 == cls2) rating += 1000;
            else if (cls2 != null && cls1.isAssignableFrom(cls2))
                rating += 100;
            else if (isAssignable(cls1, cls2))
                rating += 10;
        }

        // If varargs, check remaining args
        if (isVarArgs) {

            // Get VarArgClass
            Class varArgArrayClass = theParamTypes[clen];
            Class varArgClass = varArgArrayClass.getComponentType();

            // If only one arg and it is of array class, return true
            Class argClass = theClasses.length == theParamTypes.length ? theClasses[theParamTypes.length - 1] : null;
            if (argClass != null && argClass.isArray() && varArgArrayClass.isAssignableFrom(argClass))
                rating += 1000;

                // If any var args don't match, return false
            else for (int i = clen; i < theClasses.length; i++)
                if (theClasses[i] != null && !isAssignable(varArgClass, theClasses[i]))
                    rating += 1000;
        }

        // Return rating
        return rating;
    }
}
