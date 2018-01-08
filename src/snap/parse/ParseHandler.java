/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.parse;
import java.lang.reflect.*;

/**
 * A class called when child rules are parsed.
 */
public class ParseHandler <T> {

    // The part generated by this handler
    public T         _part;
    
    // The token where the current part started
    Token            _startToken;
    
    // Whether handler is in use
    boolean          _inUse;
    
    // Whether handler has been told to bypass further rules or fail
    boolean          _bypass, _fail;
    
    // The backup handler
    ParseHandler     _backupHandler;

/**
 * Called when a child rule has been successfully parsed into given node.
 */
protected void parsedOne(ParseNode aNode)
{
    if(_startToken==null) _startToken = aNode.getStartToken();
    parsedOne(aNode, aNode.getId());
}

/**
 * Called when a child rule has been successfully parsed into given node.
 */
protected void parsedOne(ParseNode aNode, String anId)  { }

/**
 * Called when all child rules have been successfully parsed.
 */
public T parsedAll()  { T part = _part; reset(); return part; }

/**
 * Returns the part.
 */
public T getPart()  { return _part!=null? _part : (_part=createPart()); }

/**
 * Creates the part.
 */
protected T createPart()
{
    try { return getPartClass().newInstance(); }
    catch(Exception e) { throw new RuntimeException(e); }
}

/**
 * Returns the part class.
 */
protected Class<T> getPartClass()  { return getTypeParameterClass(getClass()); }

/**
 * Returns the token where the current part started.
 */
public Token getStartToken()  { return _startToken; }

/**
 * Whether parsing should bypass succeeding rules.
 */
public boolean isBypass()  { return _bypass; }

/**
 * Indicates that parsing should short ciricuit.
 */
public void bypass()  { _bypass = true; }

/**
 * Whether parsing should fail on current rule.
 */
public boolean isFail()  { return _fail; }

/**
 * Indicates that parsing should fail on current rule.
 */
public void fail()  { _fail = true; }

/**
 * Returns a handler that is not in use.
 * This method should be synchronized, but that makes TeaVM unhappy.
 */
public ParseHandler getAvailableHandler()
{
    ParseHandler handler = this;
    while(handler._inUse) handler = handler.getBackupHandler();
    handler._inUse = true;
    return handler;
}

/**
 * Returns a backup handler.
 */
private ParseHandler getBackupHandler()
{
    // If already set, just return
    if(_backupHandler!=null) return _backupHandler;
    
    // Create and return
    try { _backupHandler = getClass().newInstance(); }
    catch(InstantiationException e) { throw new RuntimeException(e); }
    catch(IllegalAccessException e) { throw new RuntimeException(e); }
    return _backupHandler;
}

/**
 * Resets the handler.
 */
public void reset()  { _part = null; _startToken = null; _inUse = _bypass = _fail = false; }

/** Returns a type parameter class. */
private static Class getTypeParameterClass(Class aClass)
{
    Type type = aClass.getGenericSuperclass();
    if(type instanceof ParameterizedType) { ParameterizedType ptype = (ParameterizedType)type;
        Type type2 = ptype.getActualTypeArguments()[0];
        if(type2 instanceof Class)
            return (Class)type2;
        if(type2 instanceof ParameterizedType) { ParameterizedType ptype2 = (ParameterizedType)type2;
            if(ptype2.getRawType() instanceof Class)
                return (Class)ptype2.getRawType(); }
    }
    
    // Try superclass
    Class scls = aClass.getSuperclass();
    if(scls!=null)
        return getTypeParameterClass(scls);

    // Complain and return null
    System.err.println("ParseHandler.getTypeParameterClass: Type Parameter Not Found for " + aClass.getName());
    return null;
}

}