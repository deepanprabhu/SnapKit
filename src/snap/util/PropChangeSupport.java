/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.util;

/**
 * A class to easily add propery change support to a class (and DeepChange support).
 */
public class PropChangeSupport {
    
    // The source
    Object              _src;
    
    // The PropChangeListener
    PropChangeListener  _pcl;
    
    // The DeepChangeListener
    DeepChangeListener  _dcl;
    
    // An empty PropChangeSupport
    public static final PropChangeSupport EMPTY = new PropChangeSupport("");
    
/**
 * Creates a new PropChangeSupport.
 */
public PropChangeSupport(Object aSrc)  { _src = aSrc; }

/**
 * Returns whether there are listeners.
 */
public boolean hasListeners(String aProp)
{
    if(_pcl==null) return false;
    if(aProp==null) return true;
    if(_pcl instanceof NamedPCL)
        return ((NamedPCL)_pcl).prop.equals(aProp);
    if(_pcl instanceof SplitPCL)
        return ((SplitPCL)_pcl).hasListeners(aProp);
    return true;
}

/**
 * Adds a PropChangeListener.
 */
public void addPropChangeListener(PropChangeListener aLsnr)
{
    if(_pcl==null) _pcl = aLsnr;
    else if(_pcl instanceof SplitPCL) ((SplitPCL)_pcl).add(aLsnr);
    else _pcl = new SplitPCL(_pcl, aLsnr);
}

/**
 * Removes a PropChangeListener.
 */
public void removePropChangeListener(PropChangeListener aLsnr)
{
    if(_pcl==aLsnr) _pcl = null;
    else if(_pcl instanceof SplitPCL) _pcl = ((SplitPCL)_pcl).remove(aLsnr);
}

/**
 * Adds a PropChangeListener.
 */
public void addPropChangeListener(PropChangeListener aLsnr, String aProp)
{
    addPropChangeListener(new NamedPCL(aLsnr, aProp));
}

/**
 * Removes a PropChangeListener.
 */
public void removePropChangeListener(PropChangeListener aLsnr, String aProp)
{
    if(_pcl instanceof NamedPCL) { NamedPCL npcl = (NamedPCL)_pcl;
        if(npcl.pcl==aLsnr && npcl.prop.equals(aProp)) _pcl = null; }
    else if(_pcl instanceof SplitPCL) _pcl = ((SplitPCL)_pcl).remove(aLsnr, aProp);
}

/**
 * Fires a property change.
 */
public void firePropChange(String aProp, Object oldVal, Object newVal)
{
    if(hasListeners(aProp))
        _pcl.propertyChange(new PropChange(_src, aProp, oldVal, newVal));
}

/** Fires an indexed property change. */
public void firePropChange(String aProp, Object oldVal, Object newVal, int anIndex)
{
    if(hasListeners(aProp))
        _pcl.propertyChange(new PropChange(_src, aProp, oldVal, newVal, anIndex));
}

/**
 * Sends the property change.
 */
public void firePropChange(PropChange anEvent)
{
    if(hasListeners(anEvent.getPropertyName()))
        _pcl.propertyChange(anEvent);
}

/**
 * Returns whether there are deep listeners.
 */
public boolean hasDeepListeners()  { return _dcl!=null; }

/**
 * Adds a DeepChangeListener.
 */
public void addDeepChangeListener(DeepChangeListener aLsnr)
{
    if(_dcl==null) _dcl = aLsnr;
    else if(_dcl instanceof SplitDCL) ((SplitDCL)_dcl).add(aLsnr);
    else _dcl = new SplitDCL(_dcl, aLsnr);
}

/**
 * Removes a DeepChangeListener.
 */
public void removeDeepChangeListener(DeepChangeListener aLsnr)
{
    if(_dcl==aLsnr) _dcl = null;
    else if(_dcl instanceof SplitDCL) _dcl = ((SplitDCL)_dcl).remove(aLsnr);
}

/**
 * Sends the deep change.
 */
public void fireDeepChange(PropChangeListener aSrc, PropChange anEvent)
{
    if(hasDeepListeners())
        _dcl.deepChange(aSrc, anEvent);
}

/**
 * A PropChangeListener implementation to restrict a PCL to a given prop name.
 */
private static class NamedPCL implements PropChangeListener {
    PropChangeListener pcl; String prop;
    public NamedPCL(PropChangeListener aPCL, String aProp)  { pcl = aPCL; prop = aProp; }
    public void propertyChange(PropChange aPC)  { if(aPC.getPropertyName().equals(prop)) pcl.propertyChange(aPC); }
}

/**
 * A PropChangeListener implementation to multiplex property change call.
 */
private static class SplitPCL implements PropChangeListener {
    
    // The two PCLs
    PropChangeListener _pc1, _pc2;
    
    /** Create new PCLSplitter. */
    public SplitPCL(PropChangeListener aPC1, PropChangeListener aPC2)  { _pc1 = aPC1; _pc2 = aPC2; }
    
    /** Returns whether either PCL hasListeners for prop. */
    public boolean hasListeners(String aProp)
    {
        if(_pc1 instanceof NamedPCL && ((NamedPCL)_pc1).prop.equals(aProp)) return true;
        if(_pc1 instanceof SplitPCL && ((SplitPCL)_pc1).hasListeners(aProp)) return true;
        if(_pc2 instanceof NamedPCL) return ((NamedPCL)_pc2).prop.equals(aProp);
        if(_pc2 instanceof SplitPCL) return ((SplitPCL)_pc2).hasListeners(aProp);
        return true;
    }
    
    /** Send PropertyChange. */
    public void propertyChange(PropChange aPC)  { _pc1.propertyChange(aPC); _pc2.propertyChange(aPC); }
    
    /** Add. */
    public void add(PropChangeListener aPCL)
    {
        if(_pc2 instanceof SplitPCL) ((SplitPCL)_pc2).add(aPCL);
        else _pc2 = new SplitPCL(_pc2, aPCL);
    }
    
    /** Remove. */
    public PropChangeListener remove(PropChangeListener aPCL)
    {
        if(_pc1==aPCL) return _pc2; if(_pc2==aPCL) return _pc1;
        if(_pc1 instanceof SplitPCL) _pc1 = ((SplitPCL)_pc1).remove(aPCL);
        if(_pc2 instanceof SplitPCL) _pc2 = ((SplitPCL)_pc2).remove(aPCL);
        return this;
    }
    
    /** Remove. */
    public PropChangeListener remove(PropChangeListener aPCL, String aProp)
    {
        if(_pc1 instanceof NamedPCL) { NamedPCL npcl = (NamedPCL)_pc1;
            if(npcl.pcl==aPCL && npcl.prop.equals(aProp)) return _pc2; }
        if(_pc2 instanceof NamedPCL) { NamedPCL npcl = (NamedPCL)_pc2;
            if(npcl.pcl==aPCL && npcl.prop.equals(aProp)) return _pc1; }
        if(_pc1 instanceof SplitPCL) _pc1 = ((SplitPCL)_pc1).remove(aPCL);
        if(_pc2 instanceof SplitPCL) _pc2 = ((SplitPCL)_pc2).remove(aPCL);
        return this;
    }
}

/**
 * A DeepChangeListener implementation to multiplex property change call.
 */
private static class SplitDCL implements DeepChangeListener {
    
    // The two PCLs
    DeepChangeListener _pc1, _pc2;
    
    /** Create new SplitDCL. */
    public SplitDCL(DeepChangeListener aPC1, DeepChangeListener aPC2)  { _pc1 = aPC1; _pc2 = aPC2; }
    
    /** Send DeepChange. */
    public void deepChange(PropChangeListener aSrc, PropChange aPC)
    {
        _pc1.deepChange(aSrc, aPC); _pc2.deepChange(aSrc, aPC);
    }
    
    /** Add. */
    public void add(DeepChangeListener aPCL)
    {
        if(_pc2 instanceof SplitDCL) ((SplitDCL)_pc2).add(aPCL);
        else _pc2 = new SplitDCL(_pc2, aPCL);
    }
    
    /** Remove. */
    public DeepChangeListener remove(DeepChangeListener aPCL)
    {
        if(_pc1==aPCL) return _pc2; if(_pc2==aPCL) return _pc1;
        if(_pc1 instanceof SplitDCL) _pc1 = ((SplitDCL)_pc1).remove(aPCL);
        if(_pc2 instanceof SplitDCL) _pc2 = ((SplitDCL)_pc2).remove(aPCL);
        return this;
    }
}

}