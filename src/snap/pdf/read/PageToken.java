/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.pdf.read;
import java.io.UnsupportedEncodingException;
import java.util.*;
import snap.pdf.PDFException;

/**
 * The PageToken class represents an individual token in the page content stream.  It covers just the subset of
 * pdf token types that are legal within a content stream. Simple tokens (strings, operators, names) don't have
 * their own storage, but are piggybacked on top of the content stream using a PageToken.Range object.
 * Other possible contents of the value are Numbers and Lists.
 */
public class PageToken {
    public int type;
    public Object value;

    // Constants
    public static final int PDFOperatorToken = 0;
    public static final int PDFStringToken = 1;  // either from '(' or '<'
    public static final int PDFNumberToken = 2;  // int or float
    public static final int PDFArrayToken = 3;  // '['
    public static final int PDFNameToken = 4;   //  '/'
    public static final int PDFDictOpenToken = 5; // '<<'
    public static final int PDFDictCloseToken = 6; // '>>'
    public static final int PDFBooleanToken = 7; // "true" or "false"
    public static final int PDFInlineImageData = 8; // binary image data inside BI & EI pair
    
/**
 * Creates a new PageToken.
 */
public PageToken(int tokenType, Object objectValue)  { type = tokenType; value = objectValue; }

/**
 * Construct a token of a given type with an uninitialized value.
 */
public PageToken(int tokenType)  { this(tokenType,null); }

/**
 * Specific accessors for the token's value
 */
public int intValue() { return ((Number)value).intValue();  }
public float floatValue() { return ((Number)value).floatValue(); }
public boolean boolValue() { return ((Boolean)value).booleanValue(); }
public int tokenLocation() { return ((Range)value).location; }
public int tokenLength() { return ((Range)value).length; }


public String nameValue(byte pageBytes[])
{
    //TODO:  Name is preceded by a '/'.  Check that this is used consistently with the rest of the code.  I think the
    // general rule is that names used as dictionary keys never have '/' but names appearing elsewhere do.
    int tloc = tokenLocation(), tlen = tokenLength();
    return new String(pageBytes, tloc, tlen);
}

/**
 * Returns the PDF name object stripped of the leading '/'.
 * NB: In PDF, "/" is a valid name, and this routine will return an empty string for that.
 * TODO: Not sure how I feel about the names of these two routines.
 */
public String nameString(byte pageBytes[]) 
{
    int tloc = tokenLocation(), tlen = tokenLength();
    return new String(pageBytes, tloc+1, tlen-1);
}

/**
 * returns the token as an array of bytes.
 */
public byte[] byteArrayValue(byte pageBytes[]) 
{
    int tloc = tokenLocation(), tlen = tokenLength();
    byte sub[] = new byte[tlen];
    System.arraycopy(pageBytes, tloc, sub, 0, tlen);
    return sub;
}

/**
 * Standard toString implementation.
 */
public String toString() { return toString(null); }

/**
 * Standard toString implementation.
 */
public String toString(byte pageBytes[])
{
   Object tok = value;
   if(value instanceof Range && pageBytes!=null) {
       Range r = (Range)value;
       tok = new String(pageBytes, r.location, r.length);
   }
   return "[" + type + " \"" + tok + "\"]";
}

/**
 * The lexer.  Fills the tokens list from the page contents. 
 * Returns the index of the character after the last successfully consumed character.
 */
public static List <PageToken> getTokens(byte pageBytes[])
{
    List <PageToken> tokens = new ArrayList(32);
    getTokens(pageBytes, 0, pageBytes.length, tokens);
    return tokens;        
}

/**
 * The lexer.  Fills the tokens list from the page contents. 
 * Returns the index of the character after the last successfully consumed character.
 */
private static int getTokens(byte pageBytes[], int offset, int end, List theTokens)
{
    // Iterate over token string chars
    int i; for(i=offset; i<end; i++) { byte c = pageBytes[i];

        // Reset token
        PageToken aToken = null;
        
        // Handle comment - toss the rest of the line
        if(c=='%')
            while((++i<end) && (pageBytes[i] != '\r') && (pageBytes[i] != '\n'));
        
        // Handle array start (recurse)
        else if(c=='[') {
            List arrayTokens = new ArrayList(4);
            i = getTokens(pageBytes, i+1, end, arrayTokens);
            aToken = new PageToken(PageToken.PDFArrayToken, arrayTokens);
        }
        
        // Handle array close
        else if (c==']')
            return i;//+1;
        
        // Handle string start
        else if (c=='(') {
            aToken = new PageToken(PageToken.PDFStringToken);
            i = getPDFString(pageBytes, i+1, end, aToken);
        }
        
        // Handle hex string or dict
        else if(c=='<') {
            if((i<end-1) && (pageBytes[i+1]=='<')) {
                aToken = new PageToken(PageToken.PDFDictOpenToken); i++; }
            else {
                aToken = new PageToken(PageToken.PDFStringToken);
                i = getPDFHexString(pageBytes, i+1, end, aToken);
            }
        }
        
        // Handle hex string end
        else if ((c=='>') && (i<end-1) && (pageBytes[i+1]=='>')) {
            aToken = new PageToken(PageToken.PDFDictCloseToken); i++; }
        
        // Handle name
        else if(c=='/') {
            Range r = new Range(i,0);
            aToken = new PageToken(PageToken.PDFNameToken);
            while(++i<end) {
                c = pageBytes[i];
                // whitespace ends name
                if(c==' ' || c=='\t' || c=='\r' || c=='\n' || c=='\f') {
                    r.length = i-r.location; break; }
                // other delimeter.  end name and back up 
                if(c=='(' || c==')' || c=='<' || c=='>' || c=='[' || c==']' || c=='{' || c=='}' || c=='/' || c=='%') {
                    r.length = i-r.location; i--; break; }
            }
            // end of stream also ends name
            if(i==end)
                r.length = end-r.location;
            aToken.value = r;
        }
        
        // Handle number
        else if((c=='+') || (c=='-') || (c=='.') || ((c>='0') && (c<='9'))) {
            aToken = new PageToken(PageToken.PDFNumberToken);
            i = getPDFNumber(pageBytes, i, end, aToken);
        }
        
        // Handle boolean
        else if((c=='t' || c=='f') && ((aToken=checkPDFBoolean(pageBytes,i,end))!=null)) {
            if(aToken.boolValue()) i += 3;
            else i+= 4;
        }
        
        // Handle ID
        else if ((c=='I') && (i<end-4) && (pageBytes[i+1]=='D')) {
            i+= 2; // skip over the ID
            
            // Check for a single whitespace char.  This is present for data types other than asciihex & ascii85
            if(pageBytes[i]==' ' || pageBytes[i]=='\t' || pageBytes[i]=='\n')
                ++i;
            
            Range r = new Range(i,0);
            
            // Inline image data - slurp up all data up to EI token depending on encoding stream,
            // first byte might or might not be significant.
            // I don't understand this.  The data is arbitrary binary data, and unlike the stream object, which has a
            // /Length parameter, the inline image has no known length.  How then, can you be guaranteed that you're
            // not going to have a sequence like 'EI' somewhere in the middle of your data?
            while(i<end) {
                if((pageBytes[i]=='\n' || pageBytes[i]==' ' || pageBytes[i]=='\t') &&
                    (i+2<end) && (pageBytes[i+1]=='E') && (pageBytes[i+2]=='I'))
                    break;
                ++i;
            }
            if(i>=end) 
                throw new PDFException("Unterminated inline image data");
            aToken = new PageToken(PageToken.PDFInlineImageData);
            r.length = i-r.location; aToken.value = r; i+= 2;  // skip over 'EI'
        }
        
        // Handle
        else if(c!=' ' && c!='\t' && c!='\r' && c!='\n' && c!='\f') {
            Range r = new Range(i,0);
            aToken = new PageToken(PageToken.PDFOperatorToken);
            while(++i<end) {
                c = pageBytes[i];
                if(c==' ' || c=='\t' || c=='\r' || c=='\n' || c=='\f') {
                    r.length = i-r.location; break; }
                else if(c=='(' || c=='/' || c=='[' || c=='<' || c=='%') {
                    r.length = i-r.location; i--; break; }
            }
            if(i==end) r.length=i-r.location;
            aToken.value = r;
        }
        
        // Add new token to the array
        if(aToken!=null)
            theTokens.add(aToken);
    }
    
    // Return end index
    return i;
}

/**
 * Process any escape sequences. Note that this method and the one below are destructive. The pageBytes buffer gets
 * modified to the exact bytes represented by the escapes.  A buffer that starts out as "(He\154\154o)" would then
 * become "(Hello4\154o) and the token would point to "Hello". No new storage is required and everything can be
 * represented as a byte buffer. This means that if you wanted to parse the buffer a second time, you'd better get
 * the stream again from the PDFPage.
 */
private static int getPDFString(byte pageBytes[], int start, int end, PageToken tok)
{
    int parenDepth = 1, dest = start;
    Range r = new Range(start,start);
    
    while(start<end) {
        byte c = pageBytes[start++];
        if (c=='(')
            ++parenDepth;
        else if ((c==')') && (--parenDepth==0))
            break;
        else if (c=='\r') { //  replace '\r' or '\r\n' with a single '\n'
            c='\n';
            if ((start<end) && (pageBytes[start]=='\n'))
                ++start;
        }
        else if ((c=='\\') && (start<end)) {  // escapes
            c=pageBytes[start++];
            switch(c) {
            case 'n' : c='\n'; break;
            case 'r' : c='\r'; break;
            case 't' : c='\t'; break;
            case 'b' : c='\b'; break;
            case 'f' : c='\f'; break;
            case '(' : break;
            case ')' : break;
            case '\\' : break;
            case '\r' : if(start<end) {  // escape+EOL skips the EOL. skip \n also if EOL is \r\n
                            if(pageBytes[start+1]=='\n') 
                                ++start;
                            continue;
                        }
                        break;
            case '\n' : continue;
            default :
                if((c>='0') && (c<='7')) {
                    int octal = c-'0';
                    for(int i=1; i<3; ++i)
                        if (start<end) {
                            c=pageBytes[start++];
                            if((c>='0') && (c<='7')) octal = (octal<<3) | (c-'0');
                            else { start--; break; }
                        }
                     c = (byte)octal;
                }
                else { } // backslash ignored if not one of the above
           }
        }
        pageBytes[dest++]=c;
    }
    r.length = dest - r.location; tok.value = r;
    return start-1;
}

/**
 * Hex strings: <AABBCCDDEEFF0011...>. See comment above.
 */
private static int getPDFHexString(byte pageBytes[], int start, int end, PageToken tok)
{
    Range r = new Range(0,0);
    int nextChar = getPDFHexString(pageBytes, start, end, r);
    tok.value = r;
    return nextChar;
}

/**
 * Returns the decoded bytes for a PDF hex string (we probably already have this code somewhere).
 */
public static byte[] getPDFHexString(String s)
{
   byte asciihex[];
   char beginchar = s.charAt(0);
   int decodedLoc, decodedLen;
   
   // Strings beginning with '<' are hex, '(' are binary. I wonder what would happen if the binary string in the pdf
   // winds up starting with a unicode byte-order mark.  Does the string get weird?
   if (beginchar == '(') {
       int pos = 0, len = s.length();
       asciihex = new byte[len-2];
       for(int i = 1; i<len-1; ++i) {
           char c = s.charAt(i);
           if (c=='\\') {
               if (++i == len-1)
                   throw new PDFException("Bad character escape in binary string");
               c=s.charAt(i);
               if (c>='0' && c<='7') {
                   int oval = c-'0';
                   for(int j=0; (j<2) && (i<len-1); ++j) {
                       if (++i<len-1) {
                           c=s.charAt(i);
                           if(c>='0' && c<='7') oval = (oval*8)+(c-'0');
                           else { i--; break; }
                       }
                   }
                   c = (char)oval;
               }

           }
           else if (c==')') // probably never happen 
               break;
           asciihex[pos++]=(byte)c;
       }
       decodedLoc = 0; decodedLen = pos;
   }
   
   // Handle hex
   else if (beginchar == '<') {
      try { asciihex = s.getBytes("US-ASCII"); }
      catch(UnsupportedEncodingException e) { throw new PDFException("Internal error - can't decode ascii string"); }
      Range decodedRange = new Range(0,0);
      getPDFHexString(asciihex, 1, asciihex.length, decodedRange);  // decode the ascii
      decodedLoc = 1; decodedLen = decodedRange.length;
   }
   
   // Handle illegal format
   else throw new PDFException("Illegal character in binary string");
   
    // copy decoded bytes into an array matching the decoded size
    byte decoded[] = new byte[decodedLen];
    System.arraycopy(asciihex, decodedLoc, decoded, 0, decodedLen);
    return decoded;
}

/**
 * Replace ascii hex in pageBytes with actual bytes. Start points to first char after the '<', end is the upper limit
 * to seek through. r gets filled with the actual ranbge of the converted bytes return value is index of last character
 * swallowed. See comment for getPDFString... about destructive behavior.
 */
public static int getPDFHexString(byte pageBytes[], int start, int end, Range r)
{
    int dest = start;
    byte high = 0;
    boolean needhigh = true;
    
    r.location = start;
    while(start<end) {
        byte c = pageBytes[start++];
        if(c=='>') break;
        if((c>='a') && (c<='f')) c = (byte)((c-'a')+10);
        else if((c>='A') && (c<='F')) c = (byte)((c-'A')+10);
        else if((c>='0') && (c<='9')) c = (byte)(c-'0');
        else if ((c==' ') || (c=='\t') || (c=='\n') || (c=='\r') || (c=='\f')) continue;
        else { throw new PDFException("invalid character in hex string"); }
        
        if(needhigh) { high = (byte)(c<<4); needhigh = false; }
        else { pageBytes[dest++]=(byte)(high|c); needhigh = true; }
    }
    
    // odd number of hex chars - last nibble is 0
    if(!needhigh) pageBytes[dest++]=high;
    r.length = dest - r.location;
    return start - 1;
}

/** Numbers (floats or ints) Exponential notation not allowed in pdf */
private static int getPDFNumber(byte pageBytes[], int start, int end, PageToken tok)
{
    int parts[] = {0,0}, whichpart = 0, div = 1, sign = 1;
    boolean good = false;
    
    if (pageBytes[start]=='+')
        ++start;
    else if (pageBytes[start]=='-') {
        sign=-1; ++start; }
    
    while(start<end) {
        byte c = pageBytes[start];
        if (c=='.') {
            if (++whichpart>1)
                throw new PDFException("Illegal number");
        }
        else if ((c>='0') && (c<='9')) {
            parts[whichpart] = parts[whichpart]*10+(c-'0');
            if (whichpart==1)
                div*=10;
            good=true;
        }
        else break;
        ++start;
    }
    if (!good)
        throw new PDFException("Illegal number");
    
    if (whichpart==0)
        tok.value = sign*parts[0];
    else tok.value = sign*(parts[0]+((float)parts[1])/div);
    return start-1;
}

/**
 * Checks bytes to see if it's "true" or "false" and returns boolean token if so.
 * Should probably check next byte to make sure it's not "trueness", "falseness", etc.
 */
private static PageToken checkPDFBoolean(byte pageBytes[], int start, int end)
{
    int len = end-start; if(len<4) return null;
    byte b0 = pageBytes[start], b1 = pageBytes[start+1], b2 = pageBytes[start+2], b3 = pageBytes[start+3];
    byte b4 = len>4? pageBytes[start+4] : 0;
    if(b0=='t' && b1=='r' && b2=='u' && b3=='e')
        return new PageToken(PageToken.PDFBooleanToken, true);
    if(b0=='f' && b1=='a' && b2=='l' && b3=='s' && b4=='e')
        return new PageToken(PageToken.PDFBooleanToken, false);
    return null;
}

/**
 * A simple range object.
 */
private static class Range {
    public int location,length;
    public Range(int start, int len)  { location = start; length = len; }
}

}