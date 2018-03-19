/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.view;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import snap.gfx.*;
import snap.util.*;

/**
 * A View to manage a list of items (ListArea) in a ScrollView.
 * 
 * To display custom text in list, simply call list.setItemTextFunction(itm -> itm.getName());
 * 
 * To custom configure list cell, simply call list.setCellConfigure(cell -> cell.setImage(img));
 */
public class ListView <T> extends ParentView implements View.Selectable <T> {
    
    // The ListArea (real ListView functionality without scroll)
    ListArea <T>          _listArea;
    
    // The ScrollView
    ScrollView            _scroll;
    
    // The Preferred number of rows
    int                   _prefRowCount = -1;
    
    // The maximum number of rows
    int                   _maxRowCount = -1;

    // Constants for properties
    public static final String CellPadding_Prop = "CellPadding";
    public static final String ItemKey_Prop = "ItemKey";

/**
 * Creates a new ListView.
 */
public ListView()
{
    // Create/configure ListArea
    _listArea = createListArea();
    _listArea.addEventHandler(e -> fireActionEvent(), Action);
    _listArea.addPropChangeListener(pce -> listAreaPropChange(pce));
    _listArea.setCellConfigure(lc -> configureCell(lc));
    
    // Create/configure ScrollView
    _scroll = createScrollView();
    _scroll.setContent(_listArea);
    addChild(_scroll);
    
    // Configure this ListView
    enableEvents(Action);
}

/**
 * Returns the ListArea.
 */
public ListArea <T> getListArea()  { return _listArea; }

/**
 * Creates the ListArea.
 */
protected ListArea createListArea()  { return new ListArea(); }

/**
 * Returns the ScrollView.
 */
public ScrollView getScrollView()  { return _scroll; }

/**
 * Creates the ScrollView.
 */
protected ScrollView createScrollView()  { return new ScrollView(); }

/**
 * Returns the preferred number of rows.
 */
public int getPrefRowCount()  { return _prefRowCount; }

/**
 * Sets the preferred number of rows.
 */
public void setPrefRowCount(int aValue)  { _prefRowCount = aValue; relayoutParent(); }

/**
 * Returns the maximum number of rows.
 */
public int getMaxRowCount()  { return _maxRowCount; }

/**
 * Sets the maximum number of rows.
 */
public void setMaxRowCount(int aValue)  { _maxRowCount = aValue; relayoutParent(); }

/**
 * Returns the number of items.
 */
public int getItemCount()  { return _listArea.getItemCount(); }

/**
 * Returns the individual item at index.
 */
public T getItem(int anIndex)  { return _listArea.getItem(anIndex); }

/**
 * Returns the items.
 */
public List <T> getItems()  { return _listArea.getItems(); }

/**
 * Sets the items.
 */
public void setItems(List <T> theItems)  { _listArea.setItems(theItems); }

/**
 * Sets the items.
 */
public void setItems(T ... theItems)  { setItems(theItems!=null? Arrays.asList(theItems) : null); }

/**
 * Returns the selected index.
 */
public int getSelectedIndex()  { return _listArea.getSelectedIndex(); }

/**
 * Sets the selected index.
 */
public void setSelectedIndex(int anIndex)  { _listArea.setSelectedIndex(anIndex); }

/**
 * Returns the selected item.
 */
public T getSelectedItem()  { return _listArea.getSelectedItem(); }

/**
 * Sets the selected index.
 */
public void setSelectedItem(T anItem)  { _listArea.setSelectedItem(anItem); }

/**
 * Selects up in the list.
 */
public void selectUp()  { _listArea.selectUp(); }

/**
 * Selects up in the list.
 */
public void selectDown()  { _listArea.selectDown(); }

/**
 * Returns the row height.
 */
public double getRowHeight()  { return _listArea.getRowHeight(); }

/**
 * Sets the row height.
 */
public void setRowHeight(double aValue)  { _listArea.setRowHeight(aValue); }

/**
 * Returns the row at given Y location.
 */
public int getRowAt(double aY)  { return _listArea.getRowAt(aY); }

/**
 * Returns function for deteriming text for an item.
 */
public Function <T,String> getItemTextFunction()  { return _listArea.getItemTextFunction(); }

/**
 * Sets function for deteriming text for an item.
 */
public void setItemTextFunction(Function <T,String> aFunc)  { _listArea.setItemTextFunction(aFunc); }

/**
 * Returns method to configure list cells.
 */
public Consumer<ListCell<T>> getCellConfigure()  { return _listArea.getCellConfigure(); }

/**
 * Sets method to configure list cells.
 */
public void setCellConfigure(Consumer<ListCell<T>> aCC)  { _listArea.setCellConfigure(aCC); }

/**
 * Returns the ItemKey (a simple alternate way to set ListArea item text using KeyChain).
 */
public String getItemKey()  { return _listArea.getItemKey(); }

/**
 * Sets the ItemKey (a simple alternate way to set ListArea item text using KeyChain).
 */
public void setItemKey(String aKey)  { _listArea.setItemKey(aKey); }

/**
 * Returns the paint for alternating cells.
 */
public Paint getAltPaint()  { return _listArea.getAltPaint(); }

/**
 * Sets the paint for alternating cells.
 */
public void setAltPaint(Paint aPaint)  { _listArea.setAltPaint(aPaint); }

/**
 * Returns whether to fire action on mouse release instead of press.
 */
public boolean isFireActionOnRelease()  { return _listArea.isFireActionOnRelease(); }

/**
 * Sets whether to fire action on mouse release instead of press.
 */
public void setFireActionOnRelease(boolean aValue)  { _listArea.setFireActionOnRelease(aValue); }

/**
 * Returns whether list shows visual cue for item under the mouse.
 */
public boolean isTargeting()  { return _listArea.isTargeting(); }

/**
 * Sets whether list shows visual cue for item under the mouse.
 */
public void setTargeting(boolean aValue)  { _listArea.setTargeting(aValue); }

/**
 * Called to update items in list that have changed.
 */
public void updateItems(T ... theItems)  { _listArea.updateItems(theItems); }

/**
 * Returns the cell at given index.
 */
public ListCell <T> getCell(int anIndex)  { return _listArea.getCell(anIndex); }

/**
 * Returns the preferred width.
 */
protected double getPrefWidthImpl(double aH)  { return BoxView.getPrefWidth(this, _scroll, aH); }

/**
 * Returns the preferred height.
 */
protected double getPrefHeightImpl(double aW)
{
    // If PrefRowCount set, return PrefRowCount*RowHeight
    if(getPrefRowCount()>0)
        return getPrefRowCount()*getRowHeight() + getInsetsAll().getHeight();
    
    // Return pref height of Scroll
    return BoxView.getPrefHeight(this, _scroll, aW);
}

/**
 * Returns the maximum height.
 */
public double getMaxHeight()
{
    // If MaxRowCount set, return MaxRowCount*RowHeight
    if(getMaxRowCount()>0)
        return getMaxRowCount()*getRowHeight() + getInsetsAll().getHeight();
    
    // Return normal version
    return super.getMaxHeight();
}

/**
 * Override to layout ScrollView.
 */
protected void layoutImpl()  { BoxView.layout(this, _scroll, null, true, true); }

/**
 * Returns text for item.
 */
public String getText(T anItem)  { return _listArea.getText(anItem); }

/**
 * Override to return text for currently selected item.
 */
public String getText()  { return _listArea.getText(); }

/**
 * Override to forward to ListArea.
 */
public void setText(String aString)  { _listArea.setText(aString); }

/**
 * Called to configure a cell.
 */
protected void configureCell(ListCell <T> aCell)  { }

/**
 * Process events.
 */
protected void processEvent(ViewEvent anEvent)
{
    // Handle KeyPress
    if(anEvent.isKeyPress()) {
        int kcode = anEvent.getKeyCode();
        switch(kcode) {
            case KeyCode.UP: selectUp(); anEvent.consume(); break;
            case KeyCode.DOWN: selectDown(); anEvent.consume(); break;
            case KeyCode.ENTER: _listArea.fireActionEvent(); anEvent.consume(); break;
        }
    }
}

/**
 * Returns a mapped property name.
 */
public String getValuePropName()  { return getBinding("SelectedIndex")!=null? "SelectedIndex" : "SelectedItem"; }

/**
 * Catches property changes from ListArea and redispatches for this ListView.
 */
void listAreaPropChange(PropChange aPC)
{
    if(aPC.getPropertyName()==SelectedIndex_Prop)
        firePropChange(SelectedIndex_Prop, aPC.getOldValue(), aPC.getNewValue());
}

/**
 * Override to forward to ListArea.
 */
public void setFocusWhenPressed(boolean aValue)
{
    super.setFocusWhenPressed(aValue);
    _listArea.setFocusWhenPressed(aValue);
}

/**
 * XML archival.
 */
public XMLElement toXMLView(XMLArchiver anArchiver)
{
    // Archive basic view attributes
    XMLElement e = super.toXMLView(anArchiver);
    
    // Archive ItemKey
    if(getItemKey()!=null) e.add(ItemKey_Prop, getItemKey());
    
    // Return element
    return e;
}

/**
 * XML unarchival.
 */
public void fromXMLView(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic view attributes
    super.fromXMLView(anArchiver, anElement);
    
    // Unarchive ItemKey
    if(anElement.hasAttribute(ItemKey_Prop)) setItemKey(anElement.getAttributeValue(ItemKey_Prop));
}

}