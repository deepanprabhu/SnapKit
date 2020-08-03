package snap.util;
import java.lang.reflect.Array;
import java.util.*;

/**
 * A list implementation that includes support for a selection and a synchronized sorted list.
 */
public class PickList <E> extends AbstractList <E> implements Cloneable {
    
    // The real list
    private List <E>  _list = new ArrayList<>();

    // Whether list supports multiple selection
    private boolean  _multiSel;

    // The selection
    private ListSel  _sel = new ListSel(-1, -1);

    // The PropChangeSupport
    protected PropChangeSupport  _pcs = PropChangeSupport.EMPTY;

    // Constants for properties
    public static final String Item_Prop = "Item";
    public static final String Sel_Prop = "Sel";
    public static final String MultiSel_Prop = "MultiSel";

    /**
     * Return list size.
     */
    public int size()  { return _list.size(); }

    /**
     * Return list item at index.
     */
    public E get(int anIndex)  { return _list.get(anIndex); }

    /**
     * Add list item.
     */
    public void add(int anIndex, E anItem)
    {
        _list.add(anIndex, anItem);
        firePropChange(Item_Prop, null, anItem, anIndex);
    }

    /**
     * Remove list item.
     */
    public E remove(int anIndex)
    {
        E item = _list.remove(anIndex);
        firePropChange(Item_Prop, item, null, anIndex);
        return item;
    }

    /**
     * Sets all items.
     */
    public void setAll(Collection <? extends E> aCol)
    {
        E sitems[] = (E[]) getSelItems();
        clear();
        if (aCol!=null) addAll(aCol);
        setSelItems(sitems);
    }

    /**
     * Clears the list.
     */
    public void clear()  { _list.clear(); }

    /**
     * Returns whether list allows multiple selections.
     */
    public boolean isMultiSel()  { return _multiSel; }

    /**
     * Sets whether list allows multiple selections.
     */
    public void setMultiSel(boolean aValue)
    {
        if (aValue==isMultiSel()) return;
        firePropChange(MultiSel_Prop, _multiSel, _multiSel = aValue, -1);
    }

    /**
     * Returns the ListSel.
     */
    public ListSel getSel()  { return _sel; }

    /**
     * Sets the ListSel.
     */
    public void setSel(ListSel aSel)
    {
        // Trim to size
        ListSel sel = aSel!=null ? aSel.copyForMaxSize(size()) : ListSel.EMPTY;

        // If already set, just return
        if (SnapUtils.equals(sel, _sel)) return;

        // Set new Sel and fire prop change
        firePropChange(Sel_Prop, _sel, _sel = sel, -1);
    }

    /**
     * Returns whether selection is empty.
     */
    public boolean isSelEmpty()  { return _sel.isEmpty(); }

    /**
     * Returns the selected index.
     */
    public int getSelIndex()
    {
        return !_sel.isEmpty() ? getSelIndexes()[0] : -1;
    }

    /**
     * Sets the selected index.
     */
    public void setSelIndex(int anIndex)
    {
        setSel(new ListSel(anIndex, anIndex));
    }

    /**
     * Returns the selected indices.
     */
    public int[] getSelIndexes()  { return _sel.getIndexes(); }

    /**
     * Sets the selected index.
     */
    public void setSelIndexes(int ... theIndexes)
    {
        ListSel sel = ListSel.getSelForIndexArray(theIndexes);
        setSel(sel);
    }

    /**
     * Adds a selected index.
     */
    public void addSelIndex(int anIndex)
    {
        // If SingleSel, just set and return
        if (!isMultiSel()) {
            setSelIndex(anIndex);
            return;
        }

        // If already selected, just return
        if (isSelIndex(anIndex)) return;

        // Create ListSel for index and set
        ListSel sel = getSel().copyForMetaAdd(anIndex, anIndex);
        setSel(sel);
    }

    /**
     * Removes a selected index.
     */
    public void removeSelIndex(int anIndex)
    {
        // If SingleSel, just set and return
        if (!isMultiSel()) {
            if (anIndex==getSelIndex())
                clearSel();
            return;
        }

        // If not selected, just return
        if (!isSelIndex(anIndex)) return;

        // Create ListSel for index and set
        ListSel sel = getSel().copyForMetaAdd(anIndex, anIndex);
        setSel(sel);
    }

    /**
     * Clears the selection.
     */
    public void clearSel()
    {
        setSel(ListSel.EMPTY);
    }

    /**
     * Returns whether given index is selected index.
     */
    public boolean isSelIndex(int anIndex)  { return _sel.isSel(anIndex); }

    /**
     * Returns the selected item.
     */
    public E getSelItem()
    {
        int ind = getSelIndex();
        return ind>=0 && ind<size() ? get(ind) : null;
    }

    /**
     * Sets the selected index.
     */
    public void setSelItem(E anItem)
    {
        int index = indexOf(anItem);
        setSelIndex(index);
    }

    /**
     * Returns the selected item.
     */
    public Object[] getSelItems()  { return getSelItems(Object.class); }

    /**
     * Returns the selected item.
     */
    public <T> T[] getSelItems(Class <T> aClass)
    {
        int selInds[] = getSelIndexes();
        T[] items = (T[]) Array.newInstance(aClass, selInds.length);
        for (int i=0; i<selInds.length; i++)
            items[i] = (T) get(selInds[i]);
        return items;
    }

    /**
     * Adds a selected item.
     */
    public void addSelItem(E anItem)
    {
        int ind = indexOf(anItem);
        if (ind>=0)
            addSelIndex(ind);
    }

    /**
     * Sets the selected index.
     */
    public void setSelItems(E ... theItems)
    {
        for (E item : theItems)
            addSelItem(item);
    }

    /**
     * Selects up in the list.
     */
    public void selectUp()
    {
        if (getSelIndex()>0)
            setSelIndex(getSelIndex()-1);
    }

    /**
     * Selects up in the list.
     */
    public void selectDown()
    {
        if (getSelIndex()<size()-1)
            setSelIndex(getSelIndex()+1);
    }

    /**
     * Returns the list items as a single string with items separated by newlines.
     */
    public String getItemsString()
    {
        return ListUtils.joinStrings(this, "\n");
    }

    /**
     * Sets the list items as a single string with items separated by newlines.
     */
    public void setItemsString(String aString)
    {
        String items[] = aString!=null ? aString.split("\n") : new String[0];
        for (int i=0; i<items.length; i++)
            items[i] = items[i].trim();
        clear();
        Collections.addAll(this, (E)items);
    }

    /**
     * Add listener.
     */
    public void addPropChangeListener(PropChangeListener aLsnr)
    {
        if (_pcs==PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
        _pcs.addPropChangeListener(aLsnr);
    }

    /**
     * Remove listener.
     */
    public void removePropChangeListener(PropChangeListener aLsnr)  { _pcs.removePropChangeListener(aLsnr); }

    /**
     * Fires a property change for given property name, old value, new value and index.
     */
    protected void firePropChange(String aProp, Object oldVal, Object newVal, int anIndex)
    {
        if (!_pcs.hasListener(aProp)) return;
        firePropChange(new PropChange(this, aProp, oldVal, newVal, anIndex));
    }

    /**
     * Fires a given property change.
     */
    protected void firePropChange(PropChange aPCE)  {  _pcs.firePropChange(aPCE); }

    /**
     * Standard clone implementation.
     */
    public Object clone()
    {
        PickList clone;
        try { clone = (PickList)super.clone(); }
        catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
        clone._list = new ArrayList(_list);
        clone._pcs = PropChangeSupport.EMPTY;  // Clear listeners and return clone
        return clone;
    }
}