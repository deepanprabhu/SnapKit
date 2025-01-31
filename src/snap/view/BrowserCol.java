/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.view;

/**
 * A ListView subclass to act as a BrowserView column.
 */
public class BrowserCol <T> extends ListView <T> {
    
    // The Browser
    private BrowserView <T>  _browser;

    // The index of this browser column
    protected int  _index;
    
    /**
     * Creates new BrowserCol for given BrowserView.
     */
    public BrowserCol(BrowserView aBrsr)
    {
        // Set browser
        _browser = aBrsr;

        // Update some attribues
        setRowHeight(_browser.getRowHeight());

        // Configure ScrollView
        ScrollView scroll = getScrollView();
        scroll.setShowHBar(false);
        scroll.setShowVBar(true);
        scroll.setBarSize(12);

        // Configure ListArea to use Browser.configureBrowserCell
        ListArea<T> listArea = getListArea();
        listArea.setCellConfigure(lc -> _browser.configureBrowserCell(this, lc));

        // Add listener for ListArea.MouseRelease to update Browser.SelCol
        listArea.addEventFilter(e -> listAreaMouseReleased(e), MouseRelease);
    }

    /**
     * Returns the browser.
     */
    public BrowserView <T> getBrowser()  { return _browser; }

    /**
     * Returns the column index.
     */
    public int getIndex()  { return _index; }

    /**
     * Called before ListArea.MousePress.
     */
    protected void listAreaMouseReleased(ViewEvent anEvent)
    {
        _browser.setSelColIndex(_index);
        _browser.scrollSelToVisible();
    }

    /**
     * Override to suppress ListArea and fire Browser.
     */
    protected void fireActionEvent(ViewEvent anEvent)
    {
        _browser.fireActionEvent(anEvent);
    }

    /**
     * Override to request size of Browser/VisColCount (Should really be set in BrowserView.setWidth).
     */
    protected double getPrefWidthImpl(double aH)
    {
        // Calculate MinWidth
        Scroller scroller = _browser.getScrollView().getScroller();
        double scrollerW = scroller.getWidth();
        int colCount = Math.min(_browser.getColCount(), _browser.getPrefColCount());
        double minW = Math.floor(scrollerW / colCount);

        // Get normal PrefWidth and return
        double prefW = super.getPrefWidthImpl(aH);
        return Math.max(prefW, minW);
    }
}