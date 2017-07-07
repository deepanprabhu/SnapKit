/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.pdf.read;
import java.awt.Color;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.color.*;
import java.util.*;
import snap.gfx.*;
import snap.pdf.*;

/**
 * This class parses the page marking operators for a specific page number (it gets the
 * contents for that page from n PDFReader.)  It uses the various factory objects for
 * graphic object creation and a MarkupHandler to do the actual drawing.
 *
 *  Currently unsupported:
 *       - Ignores hyperlinks (annotations)
 *       - Type 1 & Type 3 fonts
 *       - Transparency blend modes other than /Normal
 *       ...
 */
public class PDFPageParser {
    
    // The PDF file that owns this parser
    PDFFile              _pfile;
    
    // The page index of the page being parsed
    PDFPage              _page;
    
    // The bounds of the area being parsed
    Rect                 _bounds;
    
    // The tokens of the page being parsed
    List <PageToken>     _tokens;
    
    // The current token index
    int                  _index;
    
    // The current text object (gets created once and reused)
    PDFTextObject        _textObj;
    
    // The current painter
    PDFMarkupHandler     _pntr;
    
    // Current PageBytes
    byte                 _pageBytes[];
    
    // Current path
    GeneralPath path = null, future_clip = null;
    
    // The number of operands available for the current operator
    int numops = 0;
    
    // save away the factory callback handler objects
    int compatibility_sections = 0;
    
    //
    PDFGState gs;
   
/**
 * Creates a new page parser for a given PDF file and a page index.
 */
public PDFPageParser(PDFFile aPdfFile, int aPageIndex, PDFMarkupHandler aPntr)
{
    // Cache given pdf file and page index
    _pfile = aPdfFile;
    _page = _pfile.getPage(aPageIndex);
    _pntr = aPntr;
    
    // Get Media box and crop box and set bounds of the area we're drawing into (interseciont of media and crop)
    Rect media = _page.getMediaBox();
    Rect crop = _page.getCropBox();
    _bounds = media.getIntersectRect(crop);
    
    // Set Painter and initialize gstate to bounds. TODO need to set transform for pages with "/Rotate" key
    _pntr.getGState().trans.setToTranslation(-_bounds.getX(),-_bounds.getY());
    //Transform  t = getGState().trans; t.setToTranslation(-_bounds.getX(),-_bounds.getY());
    //t.rotate(-Math.PI/2); t.translate(0,_bounds.getWidth());
    //TODO also need to make sure PDFPage returns right rect (ImageShape initialized from file) 
}

/**
 * Returns the page.
 */
public PDFPage getPage() { return _page; }

/**
 * Returns the token at the given index.
 */
private PageToken getToken(int index) { return _tokens.get(index); }

/**
 * Main entry point. Runs the lexer on the pdf content and passes the list of tokens to the parser. By separating out
 * a routine that operates on the list of tokens, we can implement Forms & patterns by recursively calling the parse
 * routine with a subset of the token list.
 */
public void parse()
{
    // Get page contents stream and stream bytes (decompressed/decoded)
    PDFStream pstream = getPage().getPageContentsStream(); if(pstream==null) return;
    byte pbytes[] = pstream.decodeStream();
    
    // Create top-level list of tokens and run the lexer to fill the list
    List pageTokens = PageToken.getTokens(pbytes);
 
    // Start the markup handler
    _pntr.beginPage(_bounds.getWidth(), _bounds.getHeight());
    
    // Initialize a text object
    _textObj = new PDFTextObject(_pntr.getFontRenderContext());
    
    // Parse the tokens
    parse(pageTokens, pbytes);
}

/**
 * The meat and potatoes of the pdf parser. Translates the token list into a series of calls to either a Factory class,
 * which creates a Java2D object (like GeneralPath, Font, Image, GlyphVector, etc.), or the markup handler, which does
 * the actual drawing.
 */
public void parse(List tokenList, byte pageBytes[]) 
{
    // save away the factory callback handler objects
    compatibility_sections = 0;
    
    // Cache old tokens and set token list that will be used by methods like getToken() (this method can be recursive)
    List oldTokens = _tokens; _tokens = tokenList;
    byte oldPageBytes[] = _pageBytes; _pageBytes = pageBytes;
    
    // Initialize current path. Note: path is not part of GState and so is not saved/restored by gstate ops
    path = null; future_clip = null;
    numops = 0;
    
    // Get the current gstate
    gs = _pntr.getGState();
    
    // Iterate over page contents tokens
    for(int i=0, iMax=_tokens.size(); i<iMax; i++) { PageToken token = getToken(i); _index = i;
    
        if(token.type==PageToken.PDFOperatorToken)
            paintOp(token);
            
        // It not an op, it must be an operand
        //else ++numops;
        // Catch up on that clipping.  Plus be anal and return an error, just like Acrobat.
        //if(didDraw) { if(future_clip!=null) _pntr.establishClip(future_clip, true); future_clip = null;
        //    path = null; }  // The current path and the current point are undefined after a draw
        //else if(future_clip != null) { } // TODO: an error unless the last token was W or W*
        // If swallowedToken==false, it's because there was no match, either because it
        // was an illegal token, or there were the wrong number of operands for the token.
        /*if(!swallowedToken) {
        // If in compatibility section, print warning, since we want alert about anything not supported.
        if(compatibility_sections > 0) System.err.println("Warning - ignoring " + token + " in compatibility section");
        else throw new PDFException("Error in content stream. Token = " + token); }*/
    
        numops = 0; // everything was fine, reset the number of operands
    }

    // restore previous token list
    _tokens = oldTokens; _pageBytes = oldPageBytes;
}

/**
 * The meat and potatoes of the pdf parser. Translates the token list into a series of calls to either a Factory class,
 * which creates a Java2D object (like GeneralPath, Font, Image, GlyphVector, etc.), or the markup handler, which does
 * the actual drawing.
 */
public void paintOp(PageToken aToken)
{
    String op = aToken.getString();
    
    switch(op) {
        case "b": b(); break;      // Closepath, fill, stroke
        case "b*": b_x(); break;   // Closepath, fill, stroke (EO)
        case "B": B(); break;      // Fill, stroke
        case "B*": B_x(); break;   // Fill, stroke (EO)
        case "BT": BT(); break;    // Begin Text
        case "BX": BX(); break;
        case "BI": BI(); break;
        case "BDC": case "BMC": BDC(); break;
        case "c": c(); break;      //
        case "cm": cm(); break;    // Concat matrix
        case "cs": cs(); break;    // Set colorspace
        case "CS": CS(); break;    // Set stroke colorspace
        case "d": d(); break;      // Set dash
        case "Do": Do(); break;    // Do xobject
        case "DP": DP(); break;    // Marked content
        case "ET": ET(); break;    // End text
        case "EX": EX(); break;
        case "EMC": EMC(); break;
        case "f": case "F": f(); break;     // Fill
        case "f*": case "F*": f_x(); break; // Fill (EO)
        case "g": g(); break;      // Set gray
        case "gs": gs(); break;    // Extended graphics state
        case "G": G(); break;      // Set stroke gray
        case "h": h(); break;      // Closepath
        case "i": i(); break;      // Set flatness
        case "ID": ID(); break;
        case "j": j(); break;      // Set linejoin
        case "J": J(); break;      // Set linecap
        case "k": k(); break;      // Set cmyk
        case "K": K(); break;      // Set stroke cmyk
        case "l": l(); break;      // Lineto
        case "m": m(); break;      // Moveto
        case "M": M(); break;      // Set miterlimit
        case "MP": MP(); break;
        case "n": n(); break;      // Endpath
        case "q": q(); break;      // GSave
        case "Q": Q(); break;      // GRestore
        case "re": re(); break;    // Append rect
        case "rg": rg(); break;    // Set rgb color
        case "ri": ri(); break;    // Set render intent
        case "RG": RG(); break;    // Set stroke rgb color
        case "s": s(); break;      // Closepath
        case "sc": sc(); break;
        case "scn": scn(); break;
        case "sh": sh(); break;
        case "S": S(); break;      // Stroke path
        case "SC": SC(); break;
        case "SCN": SCN(); break;
        case "T*": T_x(); break;   // Move to next line
        case "Tc": Tc(); break;    // Set character spacing
        case "Td": Td(); break;    // Move relative to current line start
        case "TD": TD(); break;    // Move relative to current line start and set leading to -ty
        case "Tf": Tf(); break;    // Set font
        case "Tj": Tj(); break;    // Show text
        case "TJ": TJ(); break;    // Show text array
        case "TL": TL(); break;    // Set text leading
        case "Tm": Tm(); break;    // Set text matrix
        case "Tr": Tr(); break;    // Set text rendering mode
        case "Ts": Ts(); break;    // Set text rise
        case "Tw": Tw(); break;    // Set text word spacing
        case "Tz": Tz(); break;    // Set text horizontal scale factor
        //case "\'": case "\"": quote(); break;
        case "v": v(); break;      // Curveto
        case "w": w(); break;      // Set linewidth
        case "W": W(); break;      // Set clip
        case "W*": W_x(); break;   // Set clip (EO)
        case "y": y(); break;      // Curveto
        default: System.err.println("PageParser: Unknown op: " + op);
    }
}
                
/**
 * Closepath, fill, stroke
 */
void b()
{
    path.setWindingRule(GeneralPath.WIND_NON_ZERO);
    path.closePath();
    _pntr.fillPath(gs, path);
    _pntr.strokePath(gs, path);
    didDraw(); //didDraw = true; swallowedToken = true;
}

/**
 * Closepath, fill, stroke (EO)
 */
void b_x()
{
    path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
    path.closePath();
    _pntr.fillPath(gs, path);
    _pntr.strokePath(gs, path);
    didDraw(); //didDraw = true; swallowedToken = true;
}

/**
 * Fill, stroke
 */
void B()
{
    path.setWindingRule(GeneralPath.WIND_NON_ZERO);
    _pntr.fillPath(gs, path);
    _pntr.strokePath(gs, path);
    didDraw(); //didDraw = true; swallowedToken = true;
}

/**
 * Fill, stroke (EO)
 */
void B_x()
{
    path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
    _pntr.fillPath(gs, path);
    _pntr.strokePath(gs, path);
    didDraw(); //didDraw = true; swallowedToken = true;
}

/**
 * Begin Text
 */
void BT()  { _textObj.begin(); } //swallowedToken = true;

/**
 * BX start (possibly nested) compatibility section
 */
void BX()  { ++compatibility_sections; } //swallowedToken = true;

/**
 * BI inline images
 */
void BI()
{
    _index = parseInlineImage(_index+1, _pageBytes); //swallowedToken = true;
}

/**
 * BDC, BMC
 */
void BDC()  { } //swallowedToken = true;

/**
 * curve to.
 */
void c()
{
    getPoint(_index, gs.cp);
    path.curveTo(getFloat(_index-6), getFloat(_index-5), getFloat(_index-4), getFloat(_index-3), gs.cp.x, gs.cp.y);
    //swallowedToken = true;
}

/**
 * cm - Concat matrix
 */
void cm()
{
    gs.trans.concatenate(getTransform(_index)); //swallowedToken = true;
}

/**
 * Set colorspace
 */
void cs()
{
    String space = getToken(_index-1).getName();
    gs.colorSpace = PDFColorSpace.getColorspace(space, _pfile, _page); //swallowedToken = true;
}

/**
 * Set stroke colorspace
 */
void CS()
{
    String space = getToken(_index-1).getName();
    gs.scolorSpace = PDFColorSpace.getColorspace(space, _pfile, _page); //swallowedToken = true;
}

/**
 * Set dash
 */
void d()
{
    gs.lineDash = getFloatArray(_index-2);
    gs.dashPhase = getFloat(_index-1);
    gs.lineStroke = gs.createStroke(); //swallowedToken=true;
}

/**
 * Do xobject
 */
void Do()
{
    String iname = getToken(_index-1).getName();
    Object xobj = getPage().getXObject(iname);
    if(xobj instanceof Image) {
        _pntr.drawImage((Image)xobj); } //swallowedToken = true; }
    else if(xobj instanceof PDFForm) {
        executeForm((PDFForm)xobj); } //swallowedToken = true; }
    else throw new PDFException("Error reading XObject");
}

/**
 * Marked content
 */
void DP() { } // swallowedToken = true;

/**
 * ET - End text
 */
void ET()  { _textObj.end(); } //swallowedToken = true;

/**
 * EMC
 */
void EMC() { } //swallowedToken = true;

/**
 * EX
 */
void EX()
{
    if(--compatibility_sections<0)
        throw new PDFException("Unbalanced BX/EX operators"); //swallowedToken = true;
}

/**
 * Fill
 */
void f()
{
    path.setWindingRule(GeneralPath.WIND_NON_ZERO);
    _pntr.fillPath(gs, path);
    didDraw(); //didDraw = true; swallowedToken = true;
}

/**
 * Fill (EO)
 */
void f_x()
{
    path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
    _pntr.fillPath(gs, path);
    didDraw(); //didDraw = true; swallowedToken = true;
}

/**
 * Set gray
 */
void g()
{
    ColorSpace cspace = PDFColorSpace.getColorspace("DeviceGray", _pfile, _page);
    gs.color = getColor(cspace,_index,numops); gs.colorSpace = cspace; //swallowedToken = true;
}

/**
 * Extended graphics state
 */
void gs()
{
    Map exg = getPage().getExtendedGStateNamed(getToken(_index-1).getName());
    readExtendedGState(gs, exg); //swallowedToken = true;
}

/**
 * Set stroke gray
 */
void G()
{
    ColorSpace cspace = PDFColorSpace.getColorspace("DeviceGray", _pfile, _page);
    gs.scolor = getColor(cspace,_index,numops); gs.scolorSpace = cspace; //swallowedToken = true;
}

/**
 * Closepath
 */
void h()
{
    path.closePath();
    Point2D lastPathPoint = path.getCurrentPoint(); 
    gs.cp.x = (float)lastPathPoint.getX();
    gs.cp.y = (float)lastPathPoint.getY(); //swallowedToken = true;
}

/**
 * Set flatness
 */
void i()  { gs.flatness = getFloat(_index-1); } //swallowedToken = true;

/**
 * ID
 */
void ID()  { }

/**
 * Set linejoin
 */
void j()
{
    gs.lineJoin = getInt(_index-1);
    gs.lineStroke = gs.createStroke(); //swallowedToken = true;
}

/**
 * Set linecap
 */
void J()
{
    gs.lineCap = getInt(_index-1);
    gs.lineStroke = gs.createStroke(); //swallowedToken = true;
}

/**
 * Set cmyk
 */
void k()
{
    ColorSpace cspace = PDFColorSpace.getColorspace("DeviceCMYK", _pfile, _page);
    Color acolor = getColor(cspace,_index,numops);
    gs.colorSpace = cspace; gs.color = acolor; //swallowedToken = true;
}

/**
 * Set stroke cmyk
 */
void K()
{
    ColorSpace cspace = PDFColorSpace.getColorspace("DeviceCMYK", _pfile, _page);
    Color acolor = getColor(cspace,_index,numops);
    gs.scolorSpace = cspace; gs.scolor = acolor; //swallowedToken = true;
}

/**
 * Lineto
 */
void l()
{
    getPoint(_index, gs.cp);
    path.lineTo(gs.cp.x, gs.cp.y); //swallowedToken = true;
}

/**
 * Moveto
 */
void m()
{
    getPoint(_index, gs.cp);
    if(path==null) path = new GeneralPath();
    path.moveTo(gs.cp.x, gs.cp.y); //swallowedToken = true;
}

/**
 * Set miterlimit
 */
void M()
{
    gs.miterLimit = getFloat(_index-1);
    gs.lineStroke = gs.createStroke(); //swallowedToken = true;
}

/**
 * Marked content point
 */
void MP()  { }

/**
 * Endpath
 */
void n()  { didDraw(); } //didDraw = true; swallowedToken=true;

/**
 * gsave
 */
void q()  { gs = _pntr.gsave(); } //swallowedToken = true;

/**
 * grestore.
 */
void Q()  { gs = _pntr.grestore(); } //swallowedToken = true;

/**
 * Append rectangle
 */
void re()
{
    float x = getFloat(_index-4), y = getFloat(_index-3);
    float w = getFloat(_index-2), h = getFloat(_index-1);
    
    // re either creates a new path or adds to the current one
    if(path==null) path = new GeneralPath();
    path.moveTo(x,y); path.lineTo(x+w,y); path.lineTo(x+w,y+h); path.lineTo(x,y+h);path.closePath();
    // reset current point to start of rect. TODO: Check that this is what really happens in pdf
    gs.cp.x = x; gs.cp.y = y; //swallowedToken = true;
}

/**
 * Set render intent
 */
void ri()
{
    gs.renderingIntent = PDFGState.getRenderingIntentID(getToken(_index-1).getString()); //swallowedToken=true;
}

/**
 * Set rgb color
 */
void rg()
{
    ColorSpace cspace = PDFColorSpace.getColorspace("DeviceRGB", _pfile, _page);
    gs.color = getColor(cspace,_index,numops);
    gs.colorSpace = cspace; //swallowedToken = true;
}

/**
 * Set stroke rgb color
 */
void RG()
{
    ColorSpace cspace = PDFColorSpace.getColorspace("DeviceRGB", _pfile, _page);
    gs.scolor = getColor(cspace,_index,numops);
    gs.scolorSpace = cspace; //swallowedToken = true;
}

/**
 * Closepath
 */
void s()
{
    path.closePath();
    _pntr.strokePath(gs, path);
    didDraw(); //didDraw = true; swallowedToken = true;
}

/**
 * Set color in colorspace
 */
void sc()
{
    gs.color = getColor(gs.colorSpace,_index,numops); //swallowedToken = true;
}

/**
 * scn
 */
void scn()
{
    if(gs.colorSpace instanceof PDFColorSpaces.PatternSpace && numops>=1) {
        String pname = getToken(_index-1).getName();
        PDFPattern pat = getPage().getPattern(pname);
        gs.color = pat.getPaint();                            
        // this is really stupid.  change this around
        if ((pat instanceof PDFPatterns.Tiling) && (gs.color==null)) {
            // Uncolored tiling patterns require color values be passed.
            // Note, however, that although you can draw an uncolored tiling pattern any number of
            // times in different colors, we only do it once (after which it will be cached)
            if (numops>1) {
                ColorSpace tileSpace=((PDFColorSpaces.PatternSpace)gs.colorSpace).tileSpace;
                if (tileSpace==null)
                    tileSpace=gs.colorSpace;
                gs.color = getColor(tileSpace,_index-1, numops-1);
            }
            this.executePatternStream((PDFPatterns.Tiling)pat);
            gs.color = pat.getPaint();
        }
    }
    else gs.color = getColor(gs.colorSpace,_index,numops); //swallowedToken = true;
}

/**
 * sh
 */
void sh()
{
    String shadename = getToken(_index-1).getName();
    java.awt.Paint oldPaint = gs.color;
    PDFPatterns.Shading shade = getPage().getShading(shadename);
    gs.color = shade.getPaint();  //save away old color
    // Get area to fill. If shading specifies bounds, use that, if not, use clip. else fill whole page.
    GeneralPath shadearea;
    if(shade.getBounds() != null)
        shadearea = new GeneralPath(shade.getBounds());
    else {
        Rectangle2D r = new Rectangle2D.Double(_bounds.x, _bounds.y, _bounds.width, _bounds.height);
        shadearea = gs.clip!=null? (GeneralPath)gs.clip.clone() : new GeneralPath(r);
        try { shadearea.transform(gs.trans.createInverse()); } // transform from page to user space
        catch(NoninvertibleTransformException e) { throw new PDFException("Invalid user space xform"); }
    }
    _pntr.fillPath(gs, shadearea);
    gs.color = oldPaint;  //restore the color
    didDraw(); //didDraw = true; swallowedToken = true;  // TODO:probably did draw... check this
}

/**
 * Stroke path
 */
void S()
{
    _pntr.strokePath(gs, path);
    didDraw(); //didDraw = true; swallowedToken = true;
}

/**
 * Set stroke color in normal colorspaces
 */
void SC()
{
    gs.scolor = getColor(gs.scolorSpace, _index, numops); //swallowedToken = true;
}

/**
 * strokecolor in normal colorspaces
 */
void SCN()
{
    gs.scolor = getColor(gs.scolorSpace, _index, numops); //swallowedToken = true;
}

/**
 * Move to next line
 */
void T_x()
{
     _textObj.positionText(0, -gs.tleading); //swallowedToken = true;
}

/**
 * Set character spacing
 */
void Tc()
{
    gs.tcs = getFloat(_index-1); //swallowedToken = true;
}

/**
 * Move relative to current line start
 */
void Td()
{
    float x = getFloat(_index-2);
    float y = getFloat(_index-1);
    _textObj.positionText(x,y);// swallowedToken = true;
}

/**
 * Move relative to current line start and set leading to -ty
 */
void TD()
{
    float x = getFloat(_index-2);
    float y = getFloat(_index-1);
    _textObj.positionText(x,y);
    gs.tleading = -y; //swallowedToken = true;
}

/**
 * Set font name and size
 */
void Tf()
{
    String fontalias = getToken(_index-2).getName(); // name in dict is key, so lose leading /
    gs.font = getPage().getFontDictForAlias(fontalias);
    gs.fontSize = getFloat(_index-1); //swallowedToken = true;
}

/**
 * Show text.
 */
void Tj()
{
    PageToken tok = getToken(_index-1);
    int tloc = tok.getStart(), tlen = tok.getLength();
    _textObj.showText(_pageBytes, tloc, tlen, gs, _pfile, _pntr); //swallowedToken = true;
}

/**
 * Show text with spacing adjustment array
 */
void TJ()
{
    List tArray = (List)(getToken(_index-1).value);
    _textObj.showText(_pageBytes, tArray, gs, _pfile, _pntr); //swallowedToken = true;
}

/**
 * Set text leading
 */
void TL()
{
    gs.tleading = getFloat(_index-1); //swallowedToken = true;
}

/**
 * Set text matrix
 */
void Tm()
{
    float a = getFloat(_index-6), b = getFloat(_index-5), c = getFloat(_index-4), d = getFloat(_index-3);
    float tx = getFloat(_index-2), ty = getFloat(_index-1);
    _textObj.setTextMatrix(a, b, c, d, tx, ty);
    //swallowedToken = true;
}

/**
 * Set text rendering mode
 */
void Tr()
{
    gs.trendermode = getInt(_index-1); //swallowedToken = true;
}

/**
 * Set text rise
 */
void Ts()
{
    gs.trise = getFloat(_index-1); //swallowedToken = true;
}

/**
 * Set text word spacing
 */
void Tw()
{
    gs.tws = getFloat(_index-1); //swallowedToken = true;
}

/**
 * Set text horizontal scale factor
 */
void Tz()
{
    gs.thscale = getFloat(_index-1)/100f; //swallowedToken = true;
}

/**
 * Curveto (first control point is current point)
 */
void v()
{
    double cp1x = gs.cp.x, cp1y = gs.cp.y;
    Point cp2 = getPoint(_index-2);
    getPoint(_index, gs.cp);
    path.curveTo(cp1x, cp1y, cp2.x, cp2.y, gs.cp.x, gs.cp.y); //swallowedToken = true;
}

/**
 * Set linewidth
 */
void w()
{
    gs.lineWidth = getFloat(_index-1);
    gs.lineStroke = gs.createStroke(); //swallowedToken = true;
}

/**
 * Set clip
 */
void W()
{
    // Somebody at Adobe's been smoking crack. The clipping operation doesn't modify the clipping in the gstate.
    // Instead, the next path drawing operation will do that, but only AFTER it draws.  
    // So a sequence like 0 0 99 99 re W f will fill the rect first and then set the clip path using the rect.
    // Because the W operation doesn't do anything, they had to introduce the 'n' operation, which is a drawing no-op,
    // in order to do a clip and not also draw the path. You might think it would be safe to just reset the clip here,
    // since the path it will draw is the same as the path it will clip to. However, there's at least one (admittedly
    // obscure) case I can think of where clip(path),draw(path)  is different from draw(path),clip(path): 
    //     W* f  %eoclip, nonzero-fill
    // Note also, Acrobat considers it an error to have a W not immediately followed by drawing op (f,f*,F,s,S,B,b,n)
    if(path != null) {
        path.setWindingRule(GeneralPath.WIND_NON_ZERO);
        future_clip = (GeneralPath)path.clone();
     }  //swallowedToken = true;
}

/**
 * Set clip (EO)
 */
void W_x()
{
    if(path != null) {
        path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        future_clip = (GeneralPath)path.clone();
     } //swallowedToken = true;
}

/**
 * Curveto (final point replicated)
 */
void y()
{
    Point cp1 = getPoint(_index-2);
    getPoint(_index, gs.cp);
    path.curveTo(cp1.x, cp1.y, gs.cp.x, gs.cp.y, gs.cp.x, gs.cp.y); //swallowedToken = true;
}

/**
 * quote
 */
void quote()
{
    //if(tlen==1 && parseTextOperator(c, i, numops, gs, pageBytes)) swallowedToken = true;
}

/**
 * 
 */
void double_quote_close()
{
    gs.tws = getFloat(_index-3); gs.tcs = getFloat(_index-2); numops = 1;
    _textObj.positionText(0, -gs.tleading);
}

/**
 * 
 */
void single_quote_close()
{
    _textObj.positionText(0, -gs.tleading); // Fall through
}

/**
 * Called after drawing op.
 */
void didDraw()
{
    // Note that unlike other ops that change gstate, there is a specific call into markup handler when clip changes.
    // Markup handler can choose whether to respond to clipping change or just to pull clip out of gstate when it draws.
    if(future_clip != null)
        _pntr.establishClip(future_clip, true); future_clip = null;

    // The current path and the current point are undefined after a draw
    path = null;
}

/** Returns the token at the given index as a float. */
private float getFloat(int i) { return getToken(i).floatValue(); }

/** Returns the token at the given index as an int. */
private int getInt(int i) { return getToken(i).intValue(); }

/** Returns the token at the given index as an array of floats */
private float[] getFloatArray(int i) 
{
    List <PageToken> ftokens = (List)(getToken(i).value);
    float farray[] = new float[ftokens.size()];
    for(int j=0, jMax=ftokens.size(); j<jMax; j++)  // We assume all tokens are floats
        farray[j] = ftokens.get(j).floatValue();
    return farray;
}

/** Returns a new point at the given index */
private Point getPoint(int i)
{
    double x = getToken(i-2).floatValue();
    double y = getToken(i-1).floatValue();
    return new Point(x,y);
}

/** Gets the token at the given index as a point. */
private void getPoint(int i, Point2D.Float pt)
{
    pt.x = getToken(i-2).floatValue();
    pt.y = getToken(i-1).floatValue();
}

/** Returns the token at the given index as a transform. */
private AffineTransform getTransform(int i)
{
    float a = getToken(i-6).floatValue(), b = getToken(i-5).floatValue();
    float c = getToken(i-4).floatValue(), d = getToken(i-3).floatValue();
    float tx = getToken(i-2).floatValue(), ty = getToken(i-1).floatValue();
    return new AffineTransform(a, b, c, d, tx, ty);
}

/** Called with any of the set color ops to create new color from values in stream. */
private Color getColor(ColorSpace space, int tindex, int numops)
{
    //if(numops != n) throw new PDFException("Wrong number of color components for colorspace");
    
    int n = space.getNumComponents();
    float varray[] = new float[n]; // how much of a performance hit is allocating this every time?
    for(int i=0; i<n; ++i) varray[i] = getFloat(tindex-(n-i));
    return PDFColorSpace.createColor(space, varray);
}

/**
 * The values for keys in inline images are limited to a small subset of names, numbers, arrays and maybe a dict.
 */
Object getInlineImageValue(PageToken token, byte pageBytes[])
{
    // Names (like /DeviceGray or /A85). Names can optionally be abbreviated.
    if(token.type==PageToken.PDFNameToken) { String abbrev = token.getName(); 
        for(int i=0, n=_inline_image_value_abbreviations.length; i<n; ++i) {
            if(_inline_image_value_abbreviations[i][0].equals(abbrev))
                return '/' + _inline_image_value_abbreviations[i][1]; }
        return '/' + abbrev;  // not found, so it's not an abbreviation.  We assume it's valid
    }
    
    // Numbers or bools
    else if(token.type==PageToken.PDFNumberToken || token.type==PageToken.PDFBooleanToken)
        return token.value;
        
    // An array of numbers or names (for Filter or Decode)
    else if(token.type==PageToken.PDFArrayToken) { List tokenarray = (List)token.value;
           List newarray = new ArrayList(tokenarray.size());
           for(int j=0, jMax=tokenarray.size(); j<jMax; ++j)     // recurse
               newarray.add(getInlineImageValue((PageToken)tokenarray.get(j), pageBytes));
           return newarray;
    }
    
    // Hex strings for indexed color spaces
    else if (token.type == PageToken.PDFStringToken)
        return token.byteArrayValue(pageBytes);
    
    // TODO: One possible key in an inline image is DecodeParms (DP). The normal decodeparms for an image is a dict.
    // The pdf spec doesn't give any information on the format of the dictionary.  Does it use the normal dictionary
    // syntax, or does it use the inline image key/value syntax? I have no idea, and I don't know how to generate a
    // pdf file that would have an inline image with a decodeparms dict.
    else { }
    throw new PDFException("Error parsing inline image dictionary");
}
        
/** 
 * Converts the tokens & data inside a BI/EI block into an image and draws it.
 * Returns the index of the last token consumed.
 */
public int parseInlineImage(int tIndex, byte[] pageBytes)
{
    Hashtable imageDict = new Hashtable();
    imageDict.put("Subtype", "/Image");

    // Get the inline image key/value pairs and create a normal image dictionary
    for(int i=tIndex, iMax=_tokens.size(); i<iMax; ++i) {
        PageToken token = getToken(i);

        // Handle NameToken: Translate key, get value, add translated key/value pair to the real dict
        if(token.type==PageToken.PDFNameToken) {
            String key = translateInlineImageKey(token.getName());
            if (++i<iMax) {
                token = getToken(i);
                Object value = getInlineImageValue(token, pageBytes);
                imageDict.put(key,value);
            }
        }
        
        // The actual inline data. Create stream with dict & data and create image. The image does not get cached.
        // The only way an inline image would ever get reused is if it were inside a form xobject.
        // First get a colorspace object.  Inline images can use any colorspace a regular image can.
        // Create stream, tell imageFactory to create image and draw it
        else if (token.type==PageToken.PDFInlineImageData) {
            Object space = imageDict.get("ColorSpace");
            ColorSpace imageCSpace = space==null ? null : PDFColorSpace.getColorspace(space, _pfile, _page);
            PDFStream imageStream = new PDFStream(pageBytes, token.getStart(), token.getLength(), imageDict);
            _pntr.drawImage(PDFImage.getImage(imageStream, imageCSpace, _pfile));
            return i; // return token index
        }
    }
    
    // should only get here on an error (like running out of tokens or having bad key/value pairs)
    throw new PDFException("Syntax error parsing inline image dictionary");
}

/** map for translating inline image abbreviations into standard tokens */
static final String _inline_image_key_abbreviations[][] = {
    {"BPC", "BitsPerComponent"}, {"CS", "ColorSpace"}, {"D", "Decode"}, {"DP", "DecodeParms"},
    {"F", "Filter"}, {"H", "Height"}, {"IM", "ImageMask"}, {"I", "Interpolate"}, {"W", "Width"}};

/** Looks up an abbreviation in the above map. */
private String translateInlineImageKey(String abbreviation)
{
    for(int i=0, n=_inline_image_key_abbreviations.length; i<n; ++i) {
        if(_inline_image_key_abbreviations[i][0].equals(abbreviation))
            return _inline_image_key_abbreviations[i][1]; }
    return abbreviation; // not found, so it's not an abbreviation
}

static final String _inline_image_value_abbreviations[][] = {
    {"G", "DeviceGray"}, {"RGB", "DeviceRGB"}, {"CMYK", "DeviceCMYK"}, {"I", "Indexed"}, {"AHx", "ASCIIHexDecode"},
    {"A85", "ASCII85Decode"}, {"LZW", "LZWDecode"}, {"Fl", "FlateDecode"}, {"RL", "RunLengthDecode"},
    {"CCF", "CCITTFaxDecode"}, {"DCT", "DCTDecode"}
};

public void executeForm(PDFForm aForm)
{
    Rectangle2D bbox = aForm.getBBox();
    AffineTransform xform = aForm.getTransform();
    
    // save the current gstate and set the transform in the newgstate
    PDFGState gs = _pntr.gsave();
    gs.trans.concatenate(xform);
    
    // clip to the form bbox
    _pntr.establishClip(new GeneralPath(bbox), true);
  
    // add the form's resources to the page resource stack
    getPage().pushResources(aForm.getResources(_pfile));
    parse(aForm.getTokens(), aForm.getBytes());  // recurse back into the parser with a new set of tokens
    getPage().popResources();    // restore the old resources, gstate,ctm, & clip
    _pntr.grestore();
}

/**
 * A pattern could execute its pdf over and over, like a form (above) but for performance reasons,
 * we only execute it once and cache a tile. To do this, we temporarily set the markup handler in the file to a new 
 * BufferedMarkupHander, add the pattern's resource dictionary and fire up the parser.
 */
public void executePatternStream(PDFPatterns.Tiling pat)
{
    // Create image painter and set
    PDFMarkupHandler pntr = PDFMarkupHandler.get();
    
    // By adding the pattern's resources to page's resource stack, it means pattern will have access to resources
    // defined by the page.  I'll bet Acrobat doesn't allow you to do this, but it shouldn't hurt anything.
    getPage().pushResources(pat.getResources());
    
    // save the current gstate
    PDFGState gs = pntr.gsave();
    
    // Establish the pattern's transformation
    gs.trans.concatenate(pat.getTransform());
    
    // Begin the markup handler. TODO:probably going to have to add a translate by -x, -y of the bounds rect
    Rectangle2D prect = pat.getBounds();
    pntr.beginPage(prect.getWidth(), prect.getHeight());
    
    // Get the pattern stream's tokens
    byte contents[] = pat.getContents();
    List <PageToken> tokens = PageToken.getTokens(contents);
    
    // Fire up parser
    PDFMarkupHandler opntr = _pntr; _pntr = pntr;
    parse(tokens, contents);
    _pntr = opntr;
    
    // Get the image and set the tile.  All the resources can be freed up now
    pat.setTile(pntr.getBufferedImage());
}

/**
 * Pull out anything useful from an extended gstate dictionary
 */
void readExtendedGState(PDFGState gs, Map exgstate)
{
    boolean strokeChanged = false;
    boolean transparencyChanged = false;
    
    if(exgstate==null) return;
    
    // The dictionary will have been read in by PDFReader, so
    // elements will have been converted into appropriate types, like Integer, Float, List, etc.
    
    Iterator entries = exgstate.entrySet().iterator();
    while(entries.hasNext()) {
        Map.Entry entry=(Map.Entry)entries.next();
        String key = (String)entry.getKey();
        Object val = entry.getValue();
        
        //line width, line cap, line join, & miter limit
        if(key.equals("LW")) { gs.lineWidth = ((Number)val).floatValue(); strokeChanged = true; }
        else if(key.equals("LC")) { gs.lineCap = ((Number)val).intValue(); strokeChanged = true; }
        else if(key.equals("LJ")) { gs.lineJoin = ((Number)val).intValue(); strokeChanged = true; }
        else if(key.equals("ML")) { gs.miterLimit = ((Number)val).floatValue(); strokeChanged = true; }
     
        // Dash:       "/D  [ [4 2 5 5] 0 ]"
        else if(key.equals("D")) {
            List dash = (List)val;
            gs.dashPhase = ((Number)dash.get(1)).floatValue();
            List dashArray = (List)dash.get(0);
            int n = dashArray.size();
            gs.lineDash = new float[n];
            for(int i=0; i<n; ++i) gs.lineDash[i] = ((Number)dashArray.get(i)).floatValue();
            strokeChanged = true;
        }
    
        // Rendering intent
        else if(key.equals("RI"))
            gs.renderingIntent = PDFGState.getRenderingIntentID((String)val);
    
        // Transparency blending mode
        else if(key.equals("BM")) {
            int bm = PDFPageParser.getBlendModeID((String)val);
            if(bm != gs.blendMode) { gs.blendMode = bm; transparencyChanged = true; }
        }
        
        // Transparency - whether to treat alpha values as shape or transparency
        else if(key.equals("AIS")) {
            boolean ais = ((Boolean)val).booleanValue();
            if(ais != gs.alphaIsShape) { gs.alphaIsShape = ais; transparencyChanged=true; }
        }
        
        // Soft mask 
        else if(key.equals("SMask")) {
            if(val.equals("/None")) gs.softMask = null;
            else System.err.println("Soft mask being specified : "+val);
        }
        
        // Transparency - stroke alpha
        else if (key.equals("CA")) {
           float a = ((Number)val).floatValue();
           if(a != gs.salpha) { gs.alpha = a; transparencyChanged = true; }
        }
        
        // Transparency - nonstroke alpha
        else if (key.equals("ca")) {
            float a = ((Number)val).floatValue();
            if (a != gs.alpha) { gs.alpha = a; transparencyChanged = true; }
        }
        // Some other possible entries in this dict that are not currently handled include:
        // Font, BG, BG2, UCR, UCR2, OP, op, OPM, TR, TR2, HT, FL, SM, SA,TK
    }
   
    // cache a new stroke object
    if(strokeChanged)
        gs.lineStroke = gs.createStroke();
    
    // cache new composite objects if necessary
    if(transparencyChanged) {
        gs.composite = PDFComposite.createComposite(gs.colorSpace, gs.blendMode, gs.alphaIsShape, gs.alpha);
        gs.scomposite = PDFComposite.createComposite(gs.colorSpace, gs.blendMode, gs.alphaIsShape, gs.salpha);
    }
}

static int getBlendModeID(String pdfName)
{
    if(pdfName.equals("/Normal") || pdfName.equals("/Compatible")) return PDFComposite.NormalBlendMode;
    if(pdfName.equals("/Multiply")) return PDFComposite.MultiplyBlendMode;
    if(pdfName.equals("/Screen")) return PDFComposite.ScreenBlendMode;
    if(pdfName.equals("/Overlay")) return PDFComposite.OverlayBlendMode;
    if(pdfName.equals("/Darken")) return PDFComposite.DarkenBlendMode;
    if(pdfName.equals("/Lighten")) return PDFComposite.LightenBlendMode;
    if(pdfName.equals("/ColorDodge")) return PDFComposite.ColorDodgeBlendMode;
    if(pdfName.equals("/ColorBurn")) return PDFComposite.ColorBurnBlendMode;
    if(pdfName.equals("/HardLight")) return PDFComposite.HardLightBlendMode;
    if(pdfName.equals("/SoftLight")) return PDFComposite.SoftLightBlendMode;
    if(pdfName.equals("/Difference")) return PDFComposite.DifferenceBlendMode;
    if(pdfName.equals("/Exclusion")) return PDFComposite.ExclusionBlendMode;
    if(pdfName.equals("/Hue")) return PDFComposite.HueBlendMode;
    if(pdfName.equals("/Saturation")) return PDFComposite.SaturationBlendMode;
    if(pdfName.equals("/Color")) return PDFComposite.ColorBlendMode;
    if(pdfName.equals("/Luminosity")) return PDFComposite.LuminosityBlendMode;
    throw new PDFException("Unknown blend mode name \""+pdfName+"\"");
}

}