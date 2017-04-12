/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.view;
import snap.util.*;

/**
 * A View subclass that lays out children using auto sizing settings.
 */
public class SpringView extends ChildView {

    // The HBox layout
    ViewLayout.SpringLayout  _layout = new ViewLayout.SpringLayout(this);
    
/**
 * Override to add layout info.
 */
public void addChild(View aChild, int anIndex)
{
    super.addChild(aChild, anIndex);
    _layout.addSpringInfo(aChild);
}

/**
 * Override to remove layout info.
 */
public View removeChild(int anIndex)
{
    View child = super.removeChild(anIndex);
    _layout.removeSpringInfo(child);
    return child;
}

/**
 * Resets spring info for children.
 */
public void resetSpringInfo()
{
    for(View v : getChildren()) {
        _layout.removeSpringInfo(v);
        _layout.addSpringInfo(v);
    }
}

/**
 * Layout children.
 */
protected void layoutImpl()  { _layout.layoutChildren(); }

/**
 * XML Archival.
 */
public void fromXMLView(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic view attributes
    super.fromXMLView(anArchiver, anElement);
    setPrefSize(getWidth(), getHeight());
}

}