package snap.util;
import java.util.*;

/**
 * A base class for anything that wants to work with props.
 */
public class PropObject implements PropChange.DoChange {

    // The PropSheet to hold actual property values
    private PropSheet  _propSheet;

    // PropertyChangeSupport
    protected PropChangeSupport  _pcs = PropChangeSupport.EMPTY;

    // A map to hold prop keys for unique classes
    private static Map<Class<? extends PropObject>, String[]>  _classProps = new HashMap<>();

    /**
     * Returns the PropSheet.
     */
    public PropSheet getPropSheet()
    {
        // If already set, just return
        if (_propSheet != null) return _propSheet;

        // Create PropSheet, set and return
        PropSheet propSheet = new PropSheet(this);
        return _propSheet = propSheet;
    }

    /**
     * Returns the parent PropObject (if available).
     */
    public PropObject getPropParent()  { return null; }

    /**
     * Initialize PropDefaults. Override to provide custom defaults.
     */
    protected void initPropDefaults(PropDefaults aPropDefaults)
    {
        // super.initPropDefaults(aPropDefaults);
        // aPropDefaults.setPropDefault(Something_Prop, DEFAULT_SOMETHING_VALUE);
    }

    /**
     * Returns the value for given prop name.
     */
    public Object getPropValue(String aPropName)
    {
        // switch (aPropName) {
        //     case Something_Prop: return getSomething();
        //     default: return super.getPropValue(aPropName);
        // }

        return null;
    }

    /**
     * Sets the value for given prop name.
     */
    public void setPropValue(String aPropName, Object aValue)
    {
        // switch (aPropName) {
        //     case Something_Prop: setSomething(aValue); break;
        //     default: super.setPropValue(aPropName, aValue);
        // }
    }

    /**
     * Returns a property value.
     */
    // public Object getSomething()
    // {
    //     PropSheet propSheet = getPropSheet();
    //     return propSheet.getPropValue(Something_Prop);
    // }

    /**
     * Returns a property value.
     */
    // public void setSomething(Object aValue)
    // {
    //     PropSheet propSheet = getPropSheet();
    //     return propSheet.setPropValue(Something_Prop, aValue);
    // }

    /**
     * Returns whether give prop is set to default.
     */
    public boolean isPropDefault(String aPropName)
    {
        Object propValue = getPropValue(aPropName);
        Object propDefault = getPropDefault(aPropName);
        return Objects.equals(propValue, propDefault);
    }

    /**
     * Returns the value for given key.
     */
    public Object getPropDefault(String aPropName)
    {
        return null;
    }

    /**
     * Returns prop default as int.
     */
    public final boolean getPropDefaultBool(String aPropName)
    {
        Object val = getPropDefault(aPropName);
        return SnapUtils.boolValue(val);
    }

    /**
     * Returns prop default as int.
     */
    public final int getPropDefaultInt(String aPropName)
    {
        Object val = getPropDefault(aPropName);
        return SnapUtils.intValue(val);
    }

    /**
     * Returns prop default as double.
     */
    public final double getPropDefaultDouble(String aPropName)
    {
        Object val = getPropDefault(aPropName);
        return SnapUtils.doubleValue(val);
    }

    /**
     * Returns prop default as String.
     */
    public final String getPropDefaultString(String aPropName)
    {
        Object val = getPropDefault(aPropName);
        return SnapUtils.stringValue(val);
    }

    /**
     * Add listener.
     */
    public void addPropChangeListener(PropChangeListener aPCL)
    {
        if (_pcs==PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
        _pcs.addPropChangeListener(aPCL);
    }

    /**
     * Add listener.
     */
    public void addPropChangeListener(PropChangeListener aPCL, String ... theProps)
    {
        if (_pcs==PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
        for (String prop : theProps)
            _pcs.addPropChangeListener(aPCL, prop);
    }

    /**
     * Remove listener.
     */
    public void removePropChangeListener(PropChangeListener aPCL)
    {
        _pcs.removePropChangeListener(aPCL);
    }

    /**
     * Remove listener.
     */
    public void removePropChangeListener(PropChangeListener aPCL, String ... theProps)
    {
        for (String prop : theProps)
            _pcs.removePropChangeListener(aPCL, prop);
    }

    /**
     * Fires a property change for given property name, old value, new value and index.
     */
    protected final void firePropChange(String aProp, Object oldVal, Object newVal)
    {
        if (_pcs == PropChangeSupport.EMPTY) return; // if (!_pcs.hasListener(aProp)) return;
        PropChange propChange = new PropChange(this, aProp, oldVal, newVal);
        firePropChange(propChange);
    }

    /**
     * Fires a property change for given property name, old value, new value and index.
     */
    protected final void firePropChange(String aProp, Object oldVal, Object newVal, int anIndex)
    {
        if (_pcs == PropChangeSupport.EMPTY) return; // if (!_pcs.hasListener(aProp)) return;
        PropChange propChange = new PropChange(this, aProp, oldVal, newVal, anIndex);
        firePropChange(propChange);
    }

    /**
     * Fires a given property change.
     */
    protected void firePropChange(PropChange aPC)
    {
        _pcs.firePropChange(aPC);
    }

    /**
     * Add DeepChange listener.
     */
    public void addDeepChangeListener(DeepChangeListener aDCL)
    {
        if (_pcs==PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
        _pcs.addDeepChangeListener(aDCL);
    }

    /**
     * Remove DeepChange listener.
     */
    public void removeDeepChangeListener(DeepChangeListener aPCL)
    {
        _pcs.removeDeepChangeListener(aPCL);
    }

    /**
     * PropChange.DoChange method.
     */
    public void processPropChange(PropChange aPC, Object oldVal, Object newVal)
    {
        setPropValue(aPC.getPropName(), newVal);
    }

    /**
     * Returns the prop keys.
     */
    public String[] getPropKeysAll()
    {
        Class cls = getClass();
        return getPropKeysAllForClass(cls);
    }

    /**
     * Returns the prop keys.
     */
    protected String[] getPropKeysLocal()
    {
        return new String[0];
    }

    /**
     * Standard clone implementation.
     */
    @Override
    protected PropObject clone() throws CloneNotSupportedException
    {
        PropObject clone = (PropObject) super.clone();
        clone._pcs = PropChangeSupport.EMPTY;
        return clone;
    }

    /**
     * Returns the prop keys.
     */
    public static String[] getPropKeysAllForClass(Class<? extends PropObject> aClass)
    {
        // Get props from cache and just return if found
        String props[] = _classProps.get(aClass);
        if (props != null)
            return props;

        // Create list and add super props to it
        List<String> propsList = new ArrayList<>();
        Class superClass = aClass.getSuperclass();
        String[] superProps = PropObject.class.isAssignableFrom(superClass) ? getPropKeysAllForClass(superClass) : null;
        if (superProps != null)
            Collections.addAll(propsList, superProps);

        // Add props for class
        try {
            PropObject object = aClass.newInstance();
            String[] classProps = object.getPropKeysLocal();
            Collections.addAll(propsList, classProps);
        }
        catch (Exception e) { throw new RuntimeException("ChartPart.getPropKeysAllForClass failed: " + aClass); }

        // Add props array to Class map and return
        props = propsList.toArray(new String[0]);
        _classProps.put(aClass, props);
        return props;
    }
}
