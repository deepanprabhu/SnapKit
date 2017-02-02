/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.view;
import java.util.*;
import snap.util.ArrayUtils;
import static snap.view.ViewEvent.Type.*;

/**
 * A class to help RootView dispatch events to Views.
 */
public class EventDispatcher {
    
    // The RootView
    RootView               _rview;

     // The last mouse press point
     double                _mpx, _mpy;

    // The list of shapes that are currently under the mouse (in "mouse over" state)
    List <View>            _mouseOvers = new ArrayList();
    
    // The top most view under the mouse
    View                   _mouseOverView;
    
    // The view that received the last mouse press
    View                   _mousePressView;
    
    // The view that initiated the current drag/drop state
    View                   _dragSourceView;
    
    // The top most view under the current drag
    View                   _dragOverView;
     
    // A popup window, if one was added to root view during last event
    PopupWindow            _popup;

/**
 * Creates a new EventDispatcher for given RootView.
 */
public EventDispatcher(RootView aRV)  { _rview = aRV; }

/**
 * Returns the popup window, if one was added to root view during last event.
 */
public PopupWindow getPopup()  { return _popup; }

/**
 * Sets the popup window, if one added to this root view during last event.
 */
protected void setPopup(PopupWindow aPopup)  { _popup = aPopup; }

/**
 * Dispatch event.
 */
public void dispatchEvent(ViewEvent anEvent)
{
    // If popup window, forward to it
    if(_popup!=null) {
        if(anEvent.isMouseDrag() || anEvent.isMouseRelease()) {
            _popup.processTriggerEvent(anEvent);
            if(anEvent.isMouseRelease()) { _popup = null; ViewUtils._mouseDown = false; }
            return;
        }
        else if(anEvent.isKeyPress() && anEvent.isEscapeKey())
            _popup.hide();
        if(!_popup.isShowing())
            _popup = null;
    }
    
    // Dispatch Mouse events
    if(anEvent.isMouseEvent() || anEvent.isScroll())
        dispatchMouseEvent(anEvent);
        
    // Dispatch Key events
    else if(anEvent.isKeyEvent())
        dispatchKeyEvent(anEvent);
        
    // Dispatch DragTartget events
    else if(anEvent.isDragEvent())
        dispatchDragTargetEvent(anEvent);
        
    // Dispatch DragSource events
    else if(anEvent.isDragSourceEvent())
        dispatchDragSourceEvent(anEvent);
        
    // All other events just go to the view
    else anEvent.getView().fireEvent(anEvent);
}

/**
 * Dispatch Mouse event.
 */
public void dispatchMouseEvent(ViewEvent anEvent)
{
    // Update ViewEnv.MouseDown
    if(anEvent.isMousePress()) ViewUtils._mouseDown = true;
    else if(anEvent.isMouseRelease()) ViewUtils._mouseDown = false;

    // Get target view (at mouse point, or mouse press, or mouse press point)
    View targ = ViewUtils.getDeepestViewAt(_rview, anEvent.getX(), anEvent.getY());
    if(anEvent.isMouseExit()) targ = null;
    if(anEvent.isMouseDrag() || anEvent.isMouseRelease()) {
        targ = _mousePressView;
        if(targ.getRootView()!=_rview)
            targ = _mousePressView = ViewUtils.getDeepestViewAt(_rview, _mpx, _mpy);
    }
    
    // Get target parents
    View pars[] = getParents(targ);
    
    // Update MouseOvers
    if(anEvent.isMouseMove() || anEvent.isMouseRelease() || anEvent.isMouseEnter() || anEvent.isMouseExit()) {
    
        // Remove old MouseOver views and dispatch appropriate MouseExited events
        for(int i=_mouseOvers.size()-1;i>=0;i--) { View view = _mouseOvers.get(i);
             if(!ArrayUtils.containsId(pars,view)) {
                 _mouseOvers.remove(i); _mouseOverView = i>0? _mouseOvers.get(i-1) : null;
                if(!view.getEventAdapter().isEnabled(MouseExit)) continue;
                 ViewEvent e2 = _rview.getEnv().createEvent(view, anEvent.getEvent(), MouseExit, null);
                 view.fireEvent(e2);
             }
             else break;
        }
        
        // Add new MouseOver views and dispatch appropriate MouseEntered events
        for(int i=_mouseOvers.size();i<pars.length;i++) { View view = pars[i];
            _mouseOvers.add(view); _mouseOverView = view;
            if(!view.getEventAdapter().isEnabled(MouseEnter)) continue;
             ViewEvent e2 = _rview.getEnv().createEvent(view, anEvent.getEvent(), MouseEnter, null);
             view.fireEvent(e2);
        }
        
        // Update CurrentCursor
        if(_mouseOverView!=null && _mouseOverView.getCursor()!=_rview.getCurrentCursor())
            _rview.setCurrentCursor(_mouseOverView.getCursor());
    }
    
    // Handle MousePress: Update MousePressView and mouse pressed point
    else if(anEvent.isMousePress()) {
        _mousePressView = targ; _mpx = anEvent.getX(); _mpy = anEvent.getY();
        for(View n=targ;n!=null;n=n.getParent())
            if(n.isFocusWhenPressed() && (_rview.getFocusedView()==null || !_rview.getFocusedView().isAncestor(n))) {
                n.requestFocus(); break; }
    }
    
    // Iterate down and see if any should filter
    for(View view : pars)
        if(view.getEventAdapter().isEnabled(anEvent.getType())) {
            ViewEvent e2 = anEvent.copyForView(view);
            view.processEventFilters(e2);
            if(e2.isConsumed()) { anEvent.consume(); return; }  }
        
    // Iterate back up and see if any parents should handle
    for(int i=pars.length-1;i>=0;i--) { View view = pars[i];
        if(view.getEventAdapter().isEnabled(anEvent.getType())) {
            ViewEvent e2 = anEvent.copyForView(view);
            view.processEventHandlers(e2);
            if(e2.isConsumed()) { anEvent.consume(); break; }
        }
    }
    
    // If popup window is now present, forward trigger event to it
    if(_popup!=null)
        _popup.processTriggerEvent(anEvent);
}

/**
 * Dispatch key event.
 */
public void dispatchKeyEvent(ViewEvent anEvent)
{
    // Update modifiers
    ViewUtils._altDown = anEvent.isAltDown();
    ViewUtils._cntrDown = anEvent.isControlDown();
    ViewUtils._metaDown = anEvent.isMetaDown();
    ViewUtils._shiftDown = anEvent.isShiftDown();
    ViewUtils._shortcutDown = anEvent.isShortcutDown();
    
    // If key pressed and tab and FocusedView.FocusKeysEnabled, switch focus
    View focusedView = _rview.getFocusedView();
    if(anEvent.isKeyPress() && anEvent.isTabKey() && focusedView!=null && focusedView.isFocusKeysEnabled()) {
        View next = anEvent.isShiftDown()? focusedView.getFocusPrev() : focusedView.getFocusNext();
        if(next!=null) { _rview.requestFocus(next); return; }
    }

    // Get target for event and array of parent
    View targ = focusedView;
    View pars[] = getParents(targ);
    
    // Iterate down and see if any should filter
    for(View view : pars)
        if(view.getEventAdapter().isEnabled(anEvent.getType())) {
            ViewEvent e2 = anEvent.copyForView(view);
            view.processEventFilters(e2);
            if(e2.isConsumed()) { anEvent.consume(); return; }  }

    // Iterate back up and see if any parents should handle
    for(int i=pars.length-1;i>=0;i--) { View view = pars[i];
        if(view.getEventAdapter().isEnabled(anEvent.getType())) {
            ViewEvent event = anEvent.copyForView(view);
            view.processEventHandlers(event);
            if(event.isConsumed()) { anEvent.consume(); return; }
        }
    }
    
    // Send to MenuBar
    if(anEvent.isKeyPress() && anEvent.isShortcutDown() && _rview.getMenuBar()!=null)
        _rview.getMenuBar().processEvent(anEvent);
}

/**
 * Dispatch drag gesture event.
 */
public void dispatchDragSourceEvent(ViewEvent anEvent)
{
    // Handle DragGesture
    if(anEvent.isDragGesture()) {
        for(View view=_mousePressView;view!=null;view=view.getParent())
            if(view.getEventAdapter().isEnabled(DragGesture)) { _dragSourceView = view;
                ViewEvent event = anEvent.copyForView(view);
                view.fireEvent(event); break; }
    }
    
    // Handle DragSource
    else if(_dragSourceView!=null) {
        if(_dragSourceView.getEventAdapter().isEnabled(anEvent.getType())) {
            ViewEvent event = anEvent.copyForView(_dragSourceView);
            _dragSourceView.fireEvent(event);
        }
        if(anEvent.isDragSourceEnd())
            _dragSourceView = null;
    }
}

/**
 * Dispatch drag target event.
 */
public void dispatchDragTargetEvent(ViewEvent anEvent)
{
    // Get target view and parents
    View targ = ViewUtils.getDeepestViewAt(_rview, anEvent.getX(), anEvent.getY());
    View pars[] = getParents(targ);
    
    // Remove old DragOver views and dispatch appropriate MouseExited events
    for(View view=_dragOverView;view!=null;view=view.getParent()) {
         if(!ArrayUtils.containsId(pars,view)) {
             _dragOverView = view.getParent();
             if(!view.getEventAdapter().isEnabled(DragExit)) continue;
             ViewEvent e2 = anEvent.copyForView(view); e2._type = DragExit;
             view.fireEvent(e2);
         }
         else break;
    }
    
    // Add new DragOver views and dispatch appropriate MouseEntered events
    int start = getParentCount(_dragOverView!=null? _dragOverView : _rview);
    for(int i=start;i<pars.length;i++) { View view = pars[i]; _dragOverView = view;
        if(!view.getEventAdapter().isEnabled(DragEnter)) continue;
        ViewEvent e2 = anEvent.copyForView(view); e2._type = DragEnter;
        view.fireEvent(e2);
    }
    
    // Handle DragOver, DragDrop
    if(anEvent.isDragOver() || anEvent.isDragDrop()) {
        for(View view=_dragOverView;view!=null;view=view.getParent()) {
            if(view.getEventAdapter().isEnabled(anEvent.getType())) {
                ViewEvent e2 = anEvent.copyForView(view);
                view.fireEvent(e2); break;
            }
        }
    }
}

/** Returns the number of parents of given view including RootView. */
private int getParentCount(View aView)
{
    if(aView==null) return 0;
    int pc = 1; for(View n=aView;n!=_rview;n=n.getParent()) pc++;
    return pc;
}

/** Returns array of parents of given view up to and including RootView. */
private View[] getParents(View aView)
{
    int pc = getParentCount(aView); View pars[] = new View[pc]; if(pc==0) return pars;
    for(View n=aView;n!=_rview;n=n.getParent()) pars[--pc] = n; pars[0] = _rview;
    return pars;
}

}