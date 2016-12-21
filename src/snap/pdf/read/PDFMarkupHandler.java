/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.pdf.read;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import snap.gfx.*;
import snap.pdf.PDFException;

/**
 * The PDFPainter implements methods for displaying or processing page marking operations on a pdf page.
 * 
 * The PDFFile object is used to open a pdf. It creates a PDFReader, which understands the general syntax for pulling
 * all PDF objects out of file. As the parser reads the file, it can create objects for dealing with specific pdf
 * objects, like streams or pages.  It also creates graphic objects, as needed, like fonts or paths.  The default
 * implementation just returns the basic awt objects, but users can supply various Factory objects if they want to
 * create custom subclasses, or if they want more control over how the awt objects are created.
 * See the FontFactory interface for more details.
 *
 * The actual page marking operations are parsed by the PDFPageParser.
 * 
 * A note about Mac text antialiasing problems. When you draw text on a Mac into a buffer with a transparent
 * background, the text looks ugly when composited to screen. If text is drawn onto an opaque backrgound, it's fine.
 *    
 * This is due to Mac's Cooltype (or whatever they call it) antialiasing. Cooltype antialising works by using
 * individual r, g, & b samples as separate 'subpixels' to antialias.  This gives them effectively 3x the horizontal
 * resolution while antialiasing.
 * 
 * The normal (non-cooltype) method for antialising with alpha is to draw the pixel the original color, and set the
 * alpha to match the pixel coverage. When that image is later composited, the alpha will cause the correct blending
 * with whatever color pixel the image is being composited on top of.
 * 
 * With cooltype antialiasing, however, this same strategy can't be used.  That's because there's only a single alpha
 * channel for each pixel. The coolfont antialiasing depends on the fact that individual r, g, & b samples will have
 * different coverage. It can't do the alpha antialiasing since setting a pixel's alpha would apply to all 3 subpixels.
 *    
 * This explains why they would have to turn off cooltype when drawing to alpha,  although it doesn't really explain
 * why it would look so ugly.  Seems like they could have used a good non-cooltype antialiasing and the text would
 * still look decent, if not super-cool.
 */
public class PDFMarkupHandler {
  
    // The painter
    Painter             _pntr;
    
    // The graphics
    Graphics2D          _gfx;
    
    // The bounds rect
    Rect                _destRect;
    
    // The transform to flip coordinates from Painter (origin is top-left) to PDF (origin is bottom-left)
    AffineTransform     _flipXForm;
    
    // The start clip
    Shape               _initialClip;
    
    // The image, if painting to image
    Image               _image;

/**
 * Create new PDFMarkupHandler.
 */
public PDFMarkupHandler(Painter aPntr, Rect aRect)
{
    _pntr = aPntr; _destRect = aRect;
    _gfx = aPntr!=null? aPntr.getNative(Graphics2D.class) : null;
}

/**
 * Returns the painter.
 */
public Painter getPainter()  { return _pntr; }

/**
 * Returns the image, if painter was created for image.
 */
public Image getImage()  { return _image; }
  
/**
 * Set the bounds of the page.  This will be called before any marking operations.
 */
public void beginPage(double width, double height)
{
    // If no painter, create image and painter
    if(_pntr==null) {
        _image = Image.get((int)Math.ceil(width), (int)Math.ceil(height), true);
        _pntr = _image.getPainter();
        _pntr.setAntialiasing(true); //_pntr.setAntialiasingText(true);
        _pntr.setImageQuality(1); //_pntr.setFractionalMetrics(true);
        _gfx = _pntr.getNative(Graphics2D.class);
    }

    // If no destination rect has been set, draw unscaled & untranslated
    if(_destRect==null) _destRect = new Rect(0, 0, width, height);
    
    // Save away the initial user clip
    _initialClip = _gfx.getClip();
    
    // Sets the clip for the destination to the page size
    _pntr.clip(_destRect);
    
    // The PDF space has (0,0) at the top, awt has it at the bottom
    _flipXForm = _gfx.getTransform();
    _flipXForm.concatenate(new AffineTransform(_destRect.width/width, 0, 0, -_destRect.height/height,
        _destRect.x, _destRect.getMaxY()));
}

/**
 * Restore painter to the state it was in before we started.
 */
public void endPage() { _gfx.setClip(_initialClip); }

/**
 * reset the clip
 */
public void clipChanged(PDFGState g)
{
    // apply original clip, if any. A null clip in the gstate resets the clip to whatever it was originally
    if(_initialClip!=null || g.clip==null)
        _gfx.setClip(_initialClip);
    
     // Clip is defined in page space, so apply only the page->awtspace transform
     if (g.clip != null) {
        AffineTransform old = establishTransform(null);
        if(_initialClip == null) _gfx.setClip(g.clip);
        else _gfx.clip(g.clip);
        _gfx.setTransform(old);
    }
}

/**
 * Stroke the current path with the current miter limit, color, etc.
 */
public void strokePath(PDFGState aGS, GeneralPath aShape)
{
    AffineTransform old = establishTransform(aGS);
    if(aGS.scomposite != null) _gfx.setComposite(aGS.scomposite);
    _gfx.setColor(aGS.scolor); _gfx.setStroke(aGS.lineStroke); _gfx.draw(aShape);
    _gfx.setTransform(old);
}

/**
 * Fill the current path using the fill params in the gstate
 */
public void fillPath(PDFGState aGS, GeneralPath aShape)
{
    AffineTransform old = establishTransform(aGS);
    if(aGS.composite != null) _gfx.setComposite(aGS.composite);
    _gfx.setPaint(aGS.color); _gfx.fill(aShape);
    _gfx.setTransform(old);
}    

/**
 * Draw an image.
 */
public void drawImage(PDFGState aGS, java.awt.Image anImg, AffineTransform ixform) 
{
    AffineTransform old = establishTransform(aGS);
    if (aGS.composite != null)
        _gfx.setComposite(aGS.composite);
    
    // special case for huge images
    if (anImg instanceof PDFTiledImage.TiledImageProxy)
        drawTiledImage((PDFTiledImage.TiledImageProxy)anImg, ixform);
    
    // normal image case - If the image drawing throws an exception, try the workaround
    _gfx.drawImage(anImg, ixform, null); // If fails with ImagingOpException, see RM14 sun_bug_4723021_workaround
    
    // restore transform
    _gfx.setTransform(old);
}

public void drawTiledImage(PDFTiledImage.TiledImageProxy aIP, AffineTransform ixform)
{
    // save old antialiasing value and turn it off
    boolean oldVal = _pntr.setAntialiasing(false);
    
    _gfx.transform(ixform);
    try { aIP.drawImage(_gfx); }
    catch (OutOfMemoryError e) { System.gc(); throw new PDFException("Out of memory error ",e); }
    
    _pntr.setAntialiasing(oldVal);
}

/**
 * Establish transform.
 */
AffineTransform establishTransform(PDFGState aGS)
{
    AffineTransform old = _gfx.getTransform();
    _gfx.setTransform(_flipXForm);
    if(aGS!=null) _gfx.transform(aGS.trans);
    return old;
}

/**
 * Draw some text at the current text position.  
 * The glyphVector will have been created by the parser using the current font and its character encodings.
 */
public void showText(PDFGState aGS, GlyphVector v)
{
    AffineTransform old = establishTransform(aGS);
    
    // TODO: eventually need check the font render mode in the gstate
    if (aGS.composite != null)
        _gfx.setComposite(aGS.composite);
    
    _gfx.setPaint(aGS.color);
    _gfx.drawGlyphVector(v,0,0);
    _gfx.setTransform(old);
}

/**
 * Return graphics.
 */
public Graphics2D getGraphics()  { return _gfx; }

/**
 * Returns pdf image.
 */
public BufferedImage getBufferedImage()  { return (BufferedImage)_image.getNative(); }

/**
 * Return an awt FontRenderContext object used to render the fonts.
 */
public FontRenderContext getFontRenderContext()  { return _gfx.getFontRenderContext(); }

/**
 * Returns a PDFMarkupHandler for given painter.
 */
public static PDFMarkupHandler get()  { return new PDFMarkupHandler(null, null); }

/**
 * Returns a PDFMarkupHandler for given painter.
 */
public static PDFMarkupHandler get(Painter aPntr, Rect aRect)
{
    return aPntr.getNative() instanceof Graphics2D? new PDFMarkupHandler(aPntr, aRect) : null;
}

}