/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.util;
import snap.gfx.GFXEnv;
import java.lang.reflect.*;
import java.util.*;

/**
 * Utility methods for Class.
 */
public class ClassUtils {

    // An array of primitive type classes and sister array of it's non-primitive matches
    private static Class[]  _primitives = {
            boolean.class, char.class, byte.class,
            short.class, int.class, long.class,
            float.class, double.class, void.class
    };

    // An array of primitive non-primitive matches
    private static Class[]  _primMappings = {
            Boolean.class, Character.class, Byte.class,
            Short.class, Integer.class, Long.class,
            Float.class, Double.class, Void.class
    };

    /**
     * Returns the given object as instance of given class, if it is.
     */
    public static <T> T getInstance(Object anObj, Class<T> aClass)
    {
        return aClass.isInstance(anObj) ? (T) anObj : null;
    }

    /**
     * Returns a new instance of a given object.
     */
    public static <T> T newInstance(T anObject)
    {
        try { return (T)getClass(anObject).newInstance(); }
        catch(Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns a new instance of a given class.
     */
    public static <T> T newInstance(Class<T> aClass)
    {
        try { return aClass.newInstance(); }
        catch(Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns a clone of given Cloneable object using reflection.
     */
    public static Object cloneCloneable(Cloneable anObj)
    {
        // Get clone method from Obj.Class
        Class cls = anObj.getClass();
        Method meth;
        try { meth = cls.getMethod("clone"); }
        catch(NoSuchMethodException e) { throw new RuntimeException(e); }

        // Invoke clone and return result
        try { return meth.invoke(anObj); }
        catch(Throwable e) { throw new RuntimeException(e); }
    }

    /**
     * Returns the class for an object.
     */
    public static Class getClass(Object anObj)
    {
        // Handle null
        if (anObj == null) return null;

        // Handle Class
        if (anObj instanceof Class)
            return (Class) anObj;

        // Handle normal object class
        return anObj.getClass();
    }

    /**
     * Returns a class for a given name.
     */
    public static Class getClassForName(String aName)
    {
        ClassLoader classLoader = ClassUtils.class.getClassLoader();
        return getClassForName(aName, classLoader);
    }

    /**
     * Returns a class for a given name, using the class loader of the given class.
     */
    public static Class getClassForName(String aName, ClassLoader aClassLoader)
    {
        // Handle arrays, either coded or uncoded (e.g. [I, [D, [LClassName; or  int[], double[] or ClassName[])
        if (aName.startsWith("["))
            return getCodedClass(aName, aClassLoader);
        if (aName.endsWith("[]")) {
            String cname = aName.substring(0, aName.length() - 2);
            Class cls = getClassForName(cname, aClassLoader);
            return cls != null ? Array.newInstance(cls, 0).getClass() : null;
        }

        // Handle primitive classes
        Class pcls = getPrimitiveClass(aName);
        if (pcls != null)
            return pcls;

        // Do normal Class.forName
        try { return Class.forName(aName, false, aClassLoader); }

        // Handle Exceptions
        catch(ClassNotFoundException e) { return null; }
        catch(NoClassDefFoundError t) { System.err.println("ClassUtils.getClass: " + t); return null; }
        catch(Throwable t) { System.err.println("ClassUtils.getClass: " + t); return null; }
    }

    /**
     * Returns a field for a parent class and a name.
     */
    public static Field getFieldForName(Class aClass, String aName)
    {
        // Get non-primitive class
        Class cls = aClass.isPrimitive() ? fromPrimitive(aClass) : aClass;

        // Check declared fields
        Field[] declaredFields = aClass.getDeclaredFields();
        for (Field field : declaredFields)
            if (field.getName().equals(aName))
                return field;

        // Check superclass
        Class superClass = cls.getSuperclass();
        if (superClass != null) {
            Field field = getFieldForName(superClass, aName);
            if (field != null)
                return field;
        }

        // Check interfaces
        for (Class interf : cls.getInterfaces()) {
            Field field = getFieldForName(interf, aName);
            if (field != null)
                return field;
        }

        // Return null since not found
        return null;
    }

    /**
     * Class.getMethod wrapper to isolate call to one place.
     */
    public static Method getMethod(Class aClass, String aName, Class... theClasses)
    {
        // Forward to getMethodOrThrow
        try { return getMethodOrThrow(aClass, aName, theClasses); }

        // Return null for exceptions (suppress)
        catch(NoSuchMethodException e) { return null; }
        catch(SecurityException e) { return null; }
    }

    /**
     * Class.getMethod wrapper to isolate call to one place.
     */
    public static Method getMethodOrThrow(Class aClass, String aName, Class... theClasses) throws NoSuchMethodException
    {
        return GFXEnv.getEnv().getMethod(aClass, aName, theClasses);
    }

    /**
     * Returns a declared inner class for a given class and a name, checking super classes as well.
     */
    public static Class getInnerClassForName(Class aClass, String aName)
    {
        // Make sure class is non-primitive
        Class cls = aClass.isPrimitive() ? fromPrimitive(aClass) : aClass;

        // Check declared inner classes
        Class<?>[] innerClasses = cls.getDeclaredClasses();
        for (Class cls2 : innerClasses)
            if (cls2.getSimpleName().equals(aName))
                return cls2;

        // Check superclass
        Class sclass = cls.getSuperclass();
        if (sclass != null) {
            Class cls2 = getInnerClassForName(sclass, aName);
            if (cls2 != null)
                return cls2;
        }

        // Check interfaces
        for (Class c : cls.getInterfaces()) {
            Class cls2 = getInnerClassForName(c, aName);
            if (cls2 != null)
                return cls2;
        }

        // Return null since class not found
        return null;
    }

    /**
     * Returns whether name is a primitive class name.
     */
    public static boolean isPrimitiveClassName(String aName)
    {
        return getPrimitiveClass(aName) != null;
    }

    /**
     * Returns a primitive class for name.
     */
    public static Class getPrimitiveClass(String aName)
    {
        if (aName.length() > 7 || !Character.isLowerCase(aName.charAt(0)) || aName.indexOf('.') > 0) return null;
        String tp = aName.intern();
        return tp == "boolean" ? boolean.class : tp == "char" ? char.class : tp == "void" ? void.class :
                tp == "byte" ? byte.class : tp == "short" ? short.class : tp == "int" ? int.class :
                        tp == "long" ? long.class : tp == "float" ? float.class : tp == "double" ? double.class : null;
    }

    /**
     * Returns an array class.
     */
    public static Class getCodedClass(String aName, ClassLoader aClassLoader)
    {
        char c = aName.charAt(0);
        switch (c) {
            case 'B': return byte.class;
            case 'C': return char.class;
            case 'D': return double.class;
            case 'F': return float.class;
            case 'I': return int.class;
            case 'J': return long.class;
            case 'S': return short.class;
            case 'Z': return boolean.class;
            case 'V': return void.class;
            case 'L':
                int end = aName.indexOf(';', 1);
                return getClassForName(aName.substring(1, end), aClassLoader);
            case '[':
                Class cls = getCodedClass(aName.substring(1), aClassLoader);
                return cls != null ? Array.newInstance(cls, 0).getClass() : null;
        }
        throw new RuntimeException("ClassUtils.getCodedPrimitiveClass: Not a coded class " + aName);
    }

    /**
     * Returns a class code.
     */
    public static String getClassCoded(Class aClass)
    {
        if (aClass.isArray()) return "[" + getClassCoded(aClass.getComponentType());
        if (aClass == byte.class) return "B";
        if (aClass == char.class) return "C";
        if (aClass == double.class) return "D";
        if (aClass == float.class) return "F";
        if (aClass == int.class) return "I";
        if (aClass == long.class) return "J";
        if (aClass == short.class) return "S";
        if (aClass == boolean.class) return "Z";
        if (aClass == void.class) return "V";
        return "L" + aClass.getName() + ";";
    }

    /**
     * Returns a class code.
     */
    public static String getClassCodedForClassName(String aClassName)
    {
        Class pclass = getPrimitiveClass(aClassName);
        if (pclass != null)
            return getClassCoded(pclass);
        return "L" + aClassName + ";";
    }

    /**
     * Returns whether a given class could be assigned a value from the second given class (accounting for auto-boxing).
     */
    public static boolean isAssignable(Class aClass1, Class aClass2)
    {
        // Handle null
        if (aClass2 == null) return !aClass1.isPrimitive();

        // Handle primitive
        if (aClass1.isPrimitive() || aClass2.isPrimitive())
            return isAssignablePrimitive(aClass1, aClass2);

        // Do normal version
        return aClass1.isAssignableFrom(aClass2);
    }

    /**
     * Returns whether a given primitive class could be assigned a value from the second given class.
     */
    public static boolean isAssignablePrimitive(Class aClass1, Class aClass2)
    {
        // Get primitives
        Class c1 = toPrimitive(aClass1);
        Class c2 = toPrimitive(aClass2);
        if (c1 == Object.class) return true;

        // Handle float, double, Number
        if (c1 == float.class || c1 == double.class || c1 == Number.class)
            return c2 == c1 || c2 == byte.class || c2 == char.class || c2 == short.class || c2 == int.class || c2 == long.class || c2 == float.class;

        // Handle byte, char, short, int long
        if (c1 == byte.class || c1 == char.class || c1 == short.class || c1 == int.class || c1 == long.class)
            return c2 == c1 || c2 == byte.class || c2 == char.class || c2 == short.class || c2 == int.class;

        // Do normal version
        return c1.isAssignableFrom(c2);
    }

    /**
     * Returns whether second batch of classes is assignable to first batch of classes (accounting for auto-boxing).
     */
    protected static boolean isAssignable(Class[] theClasses1, Class[] theClasses2, int aCount)
    {
        if (theClasses1 == null)
            return theClasses2 == null || theClasses2.length == 0;
        if (theClasses2 == null)
            return theClasses1.length == 0;
        for (int i = 0; i < aCount; i++)
            if (theClasses2[i] != null && !isAssignable(theClasses1[i], theClasses2[i]))
                return false;
        return true;
    }

    /**
     * Returns non primitive type for primitive.
     */
    public static Class toPrimitive(Class aClass)
    {
        for (int i = 0; i < _primitives.length; i++)
            if (aClass == _primMappings[i])
                return _primitives[i];
        return aClass;
    }

    /**
     * Returns primitive type for non-primitive.
     */
    public static Class fromPrimitive(Class aClass)
    {
        for (int i = 0; i < _primitives.length; i++)
            if (aClass == _primitives[i])
                return _primMappings[i];
        return aClass;
    }

    /**
     * Returns the common ancestor class for a list of objects.
     */
    public static Class getCommonClass(List aList)
    {
        // Get class for first object, iterate over remaining classes and return common class
        Class cclass = aList.size() > 0 ? getClass(aList.get(0)) : null;
        for (int i = 1, iMax = aList.size(); i < iMax; i++)
            cclass = getCommonClass(cclass, aList.get(i));
        return cclass;
    }

    /**
     * Returns the common ancestor class for two objects.
     */
    public static Class getCommonClass(Object anObj1, Object anObj2)
    {
        // Bail if either object is null
        if (anObj1 == null || anObj2 == null) return null;

        // Get the classes for the objects
        Class c1 = getClass(anObj1);
        Class c2 = getClass(anObj2);

        // If either is assignable from the other, return that class
        if (c1.isAssignableFrom(c2))
            return c1;
        if (c2.isAssignableFrom(c1))
            return c2;

        // Recurse by swapping args and using superclass of second
        return getCommonClass(c2.getSuperclass(), c1);
    }
}