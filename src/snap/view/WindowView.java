/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.view;
import java.lang.reflect.Array;
import java.util.*;
import snap.gfx.*;
import snap.util.*;
import snap.web.WebURL;

/**
 * A class to manage a Window.
 */
public class WindowView extends ParentView {
    
    // The panel's window title
    String                    _title;
    
    // The type
    String                    _type = TYPE_MAIN;
    
    // The root view
    RootView                  _rview;
    
    // Whether the panel's window is always on top
    boolean                   _alwaysOnTop;
    
    // The document file
    WebURL                    _docURL;
    
    // The window image
    Image                     _image;
    
    // Whether window hides on deativate
    boolean                   _hideOnDeactivate;
    
    // Whether window is modal
    boolean                   _modal = false;
    
    // Whether window is resizable
    boolean                   _resizable = true;
    
    // Whether window is sized to maximum screen size
    boolean                   _maximized;
    
    // The bounds for maximized window
    Rect                      _maxBounds, _unmaxBounds;
    
    // The Frame save name
    String                    _saveName;
    
    // Save frame size
    boolean                   _saveSize;
    
    // The View that referenced on last show
    View                      _clientView;
    
    // A list of all open windows
    static List <WindowView>  _openWins = new ArrayList();
    
    // Constants for Type
    public static final String TYPE_MAIN = "MAIN";
    public static final String TYPE_UTILITY = "UTILITY";
    public static final String TYPE_PLAIN = "PLAIN";
    
    // Constants for style
    public static enum Style { Small }
    
    // Constants for properties
    public static final String AlwaysOnTop_Prop = "AlwaysOnTop";
    public static final String Image_Prop = "Image";
    public static final String Maximized_Prop = "Maximized";
    public static final String Resizable_Prop = "Resizable";
    public static final String Title_Prop = "Title";

/**
 * Returns the title of the window.
 */
public String getTitle()  { return _title; }

/**
 * Sets the title of the window.
 */
public void setTitle(String aValue)
{
    if(SnapUtils.equals(aValue,_title)) return;
    firePropChange(Title_Prop, _title, _title=aValue);
}

/**
 * Returns the window type.
 */
public String getType()  { return _type; }

/**
 * Sets the window type.
 */
public void setType(String aType)  { _type = aType; }

/**
 * Returns whether the window is resizable.
 */
public boolean isResizable()  { return _resizable; }

/**
 * Sets whether the window is resizable (default to true).
 */
public void setResizable(boolean aValue)
{
    if(aValue==_resizable) return;
    firePropChange(Resizable_Prop, _resizable, _resizable=aValue);
}

/**
 * Returns whether the window is sized to maximum screen size.
 */
public boolean isMaximized()  { return _maximized; }

/**
 * Sets whether the window is sized to maximum screen size.
 */
public void setMaximized(boolean aValue)
{
    // If already set, just return
    if(aValue==_maximized) return;
    _maximized = aValue;
    
    // If Maximizing
    if(aValue) {
        _unmaxBounds = getBounds();
        Rect maxBounds = getMaximizedBounds();
        setBounds(maxBounds);
    }
    
    // If Un-Maximizing, reset bounds
    else {
        if(_unmaxBounds!=null && !_unmaxBounds.isEmpty())
            setBounds(_unmaxBounds);
        else {
            Size psize = getPrefSize();
            Rect screenRect = ViewEnv.getEnv().getScreenBoundsInset();
            Rect centeredBounds = screenRect.getRectCenteredInside(psize.width, psize.height);
        }
    }
    
    // Get screen size
    firePropChange(Maximized_Prop, !_maximized, _maximized);
}

/**
 * Returns the window max size.
 */
public Rect getMaximizedBounds()
{
    return _maxBounds!=null? _maxBounds : ViewEnv.getEnv().getScreenBoundsInset();
}

/**
 * Sets the bounds rect to use when window is maximized (uses ViewEnv ScreenBoundsInset by default).
 */
public void setMaximizedBounds(Rect aRect)  { _maxBounds = aRect; }

/**
 * Returns the save name.
 */
public String getSaveName()  { return _saveName; }

/**
 * Sets the save name.
 */
public void setSaveName(String aName)  { _saveName = aName; }

/**
 * Returns whether to save size
 */
public boolean getSaveSize()  { return _saveSize; }

/**
 * Sets whether to save size.
 */
public void setSaveSize(boolean aValue)  { _saveSize = aValue; }

/**
 * Save frame.
 */
public void saveFrame()
{
    int x = (int)getX(), y = (int)getY();
    int w = (int)getWidth(), h = (int)getHeight();
    StringBuffer sb = new StringBuffer().append(x).append(' ').append(y);
    if(_saveSize) sb.append(' ').append(w).append(' ').append(h);
    Prefs.get().set(_saveName + "Loc", sb.toString());
}

/**
 * Returns the root view.
 */
public RootView getRootView()  { return _rview; }

/**
 * Sets the root view.
 */
protected void setRootView(RootView aRV)
{
    _rview = aRV;
    setOwner(aRV.getOwner());
    addChild(aRV);
}

/**
 * Returns the content associated with this window.
 */
public View getContent()  { return getRootView().getContent(); }

/**
 * Sets the content associated with this window.
 */
public void setContent(View aView)  { getRootView().setContent(aView); }

/**
 * Returns whether the window is always on top.
 */
public boolean isAlwaysOnTop()  { return _alwaysOnTop; }

/**
 * Sets whether the window is always on top.
 */
public void setAlwaysOnTop(boolean aValue)
{
    if(aValue==_alwaysOnTop) return;
    firePropChange(AlwaysOnTop_Prop, _alwaysOnTop, _alwaysOnTop = aValue);
}

/**
 * Returns the document file URL for the window title bar proxy icon.
 */
public WebURL getDocURL()  { return _docURL; }

/**
 * Returns the document file URL for the window title bar proxy icon.
 */
public void setDocURL(WebURL aURL)
{
    if(SnapUtils.equals(aURL, _docURL)) return; _docURL = aURL;
    getHelper().setDocURL(aURL);
}

/**
 * Returns the icon image for the window.
 */
public Image getImage()  { return _image; }

/**
 * Sets the icon image for the window.
 */
public void setImage(Image anImage)  { firePropChange(Image_Prop, _image, _image = anImage); }
    
/**
 * Returns whether the window will hide on deactivate.
 */
public boolean isHideOnDeactivate()  { return _hideOnDeactivate; }

/**
 * Sets whether the window will hide on deacativate.
 */
public void setHideOnDeactivate(boolean aValue)  { _hideOnDeactivate = aValue; }

/**
 * Returns the modal mode of the window.
 */
public boolean isModal()  { return _modal; }

/**
 * Sets the modal mode of the window (defaults to false).
 */
public void setModal(boolean aValue)  { _modal = aValue; }

/**
 * Initializes the native window.
 */
protected void initNativeWindow()
{
    getHelper().initWindow();
    pack();
}

/** Initializes the native window once. */
void initNativeWindowOnce()  { if(!_initWin) { _initWin = true; initNativeWindow(); } } boolean _initWin;

/**
 * Shows window in center of given view.
 */
public void showCentered(View aView)
{
    Point pnt = getScreenLocation(aView, Pos.CENTER, 0, 0);
    show(null, pnt.x, pnt.y);
}

/**
 * Show the window relative to given node.
 */
public void show(View aView, double aX, double aY)
{
    // Set ClientView
    _clientView = aView;
    
    // Make window is initialized
    initNativeWindowOnce();
    
    // If aView provided, convert point
    if(aView!=null) {
        Point pt = aView.localToParent(aX, aY, null); aX = pt.x; aY = pt.y; }
        
    // If FrameSaveName provided, set Location from defaults and register to store future window moves
    if(getSaveName()!=null) {
        String locString = Prefs.get().get(getSaveName() + "Loc");
        if(locString!=null) {
            String strings[] = locString.split(" ");
            aX = StringUtils.intValue(strings[0]);
            aY = StringUtils.intValue(strings[1]);
            int w = getSaveSize() && strings.length>2? StringUtils.intValue(strings[2]) : 0;
            int h = getSaveSize() && strings.length>3? StringUtils.intValue(strings[3]) : 0;
            if(w>0 && h>0) setSize(w,h);
        }
    }
    
    // Set window location
    setXY(aX, aY);
    
    // Have helper show window
    show();
}

/**
 * Shows window in center of screen.
 */
public void show()
{
    getHelper().show();
}

/**
 * Hide the window.
 */
public void hide()
{
    if(getRootView().getPopup()!=null)
        getRootView().getPopup().hide();
    if(isShowing())
        getHelper().hide();
}

/**
 * Returns the view associated with the last show() call.
 */
public View getClientView()  { return _clientView; }

/**
 * Packs the window.
 */
public void pack()  { setSize(getBestSize()); }

/**
 * Order window to front.
 */
public void toFront()  { getHelper().toFront(); }

/**
 * Returns the screen location for given node, position and offsets.
 */
public Point getScreenLocation(View aView, Pos aPos, double aDX, double aDY)
{
    // Set ClientView
    _clientView = aView;
    
    // Make window is initialized
    initNativeWindowOnce();
    
    // Get rect for given node and point for given offsets
    Rect rect = aView!=null? aView.getBoundsLocal().copyFor(aView.getLocalToParent(null)).getBounds() :
        getEnv().getScreenBoundsInset();
    double x = aDX, y = aDY;
    
    // Modify x for given HPos
    switch(aPos.getHPos()) {
        case LEFT: x += rect.x; break;
        case CENTER: x += Math.round(rect.x + (rect.getWidth()-getWidth())/2); break;
        case RIGHT: x += rect.getMaxX() - getWidth(); break;
    }
    
    // Modify y for given VPos
    switch(aPos.getVPos()) {
        case TOP: y += rect.y; break;
        case CENTER: y += Math.round(rect.y + (rect.getHeight()-getHeight())/2); break;
        case BOTTOM: y += rect.getMaxY() - getHeight(); break;
    }
    
    // Return point
    return new Point(x,y);
}

/**
 * Override to do layout immediately.
 */
public void relayout()  { layout(); }

/**
 * Override to return showing, since it is eqivalent for window.
 */
public boolean isVisible()  { return isShowing(); }

/**
 * Override to call show/hide.
 */
public void setVisible(boolean aValue)  { if(aValue) showCentered(null); else hide(); }

/**
 * Override to add/remove window to/from global windows list.
 */
public void setShowing(boolean aVal)
{
    if(aVal==isShowing()) return; super.setShowing(aVal);
    if(aVal) ListUtils.moveToFront(_openWins, this);
    else _openWins.remove(this);
}

/**
 * Override to move window to front of all windows list.
 */
protected void setFocused(boolean aValue)
{
    if(aValue==isFocused()) return; super.setFocused(aValue);
    if(aValue) ListUtils.moveToFront(_openWins, this);
}

/**
 * Returns the preferred width.
 */
protected double getPrefWidthImpl(double aH)  { return BoxView.getPrefWidth(this, getRootView(), aH); }

/**
 * Returns the preferred height.
 */
protected double getPrefHeightImpl(double aW)  { return BoxView.getPrefHeight(this, getRootView(), aW); }

/**
 * Layout children.
 */
protected void layoutImpl()  { BoxView.layout(this, getRootView(), null, true, true); }

/**
 * Returns an array of all open windows.
 */
public static WindowView[] getOpenWindows()  { return _openWins.toArray(new WindowView[_openWins.size()]); }

/**
 * Returns an array of all open windows.
 */
public static <T extends ViewOwner> T getOpenWindowOwner(Class <T> aClass)
{
    for(WindowView wnode : _openWins) {
        RootView rview = wnode.getRootView();
        View content = rview.getContent();
        ViewOwner ownr = content.getOwner();
        if(ownr!=null && (aClass==null || aClass.isAssignableFrom(ownr.getClass())))
            return (T)ownr;
    }
    return null;
}

/**
 * Returns an array of all open windows.
 */
public static <T extends ViewOwner> T[] getOpenWindowOwners(Class <T> aClass)
{
    List <T> ownrs = new ArrayList();
    for(WindowView wnode : _openWins) {
        RootView rview = wnode.getRootView();
        View content = rview.getContent();
        ViewOwner ownr = content.getOwner();
        if(ownr!=null && (aClass==null || aClass.isAssignableFrom(ownr.getClass())))
            ownrs.add((T)ownr);
    }
    return ownrs.toArray((T[])Array.newInstance(aClass, ownrs.size()));
}

}