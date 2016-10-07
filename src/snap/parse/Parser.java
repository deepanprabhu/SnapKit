/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.parse;
import java.util.*;

/**
 * A class to parse a given input (string) using given rule(s).
 */
public class Parser {

    // The top level rule
    ParseRule                  _rule;
    
    // The current parse character input
    CharSequence               _input;
    
    // The tokenizer
    Tokenizer                  _tokenizer;
    
    // The current token
    Token                      _token;
    
    // The list of current look ahead tokens
    List <Token>               _lookAheadTokens = new ArrayList();
    
    // The shared node used to report parse success
    ParseNode                  _sharedNode = new ParseNode();
    
/**
 * Creates a new Parser.
 */
public Parser()  { }

/**
 * Creates a new Parser for given rule.
 */
public Parser(ParseRule aRule)  { setRule(aRule); }

/**
 * Returns the top level rule.
 */
public ParseRule getRule()  { return _rule!=null? _rule : (_rule=createRule()); }

/**
 * Sets the top level rule.
 */
public void setRule(ParseRule aRule)  { _rule = aRule; }

/**
 * Creates the top level rule. Default version tries to load rules from ClassName.txt.
 */
protected ParseRule createRule()  { return ParseUtils.loadRule(getClass(), null); }

/**
 * Returns a named rule.
 */
public ParseRule getRule(String aName)  { return getRule().getRule(aName); }

/**
 * Returns the current parse character input.
 */
public CharSequence getInput()  { return _input; }

/**
 * Sets the current parse string.
 */
public Parser setInput(CharSequence aSequence)
{
    _input = aSequence;
    getTokenizer().setInput(_input);
    setCharIndex(0);
    return this;
}

/**
 * Returns the current parse char location.
 */
public int getCharIndex()  { return getTokenizer().getCharIndex(); }

/**
 * Sets the current parse location.
 */
public void setCharIndex(int aLoc)
{
    getTokenizer().setCharIndex(aLoc);
    _lookAheadTokens.clear();
    _token = null;
}

/**
 * Returns the tokenizer.
 */
public Tokenizer getTokenizer()
{
    if(_tokenizer==null) setTokenizer(createTokenizer());  // If tokenizer not set, create and set
    return _tokenizer;
}

/**
 * Creates the tokenizer instance and initializes patters from rule.
 */
protected Tokenizer createTokenizer()
{
    Tokenizer tokenizer = createTokenizerImpl(); // Create instance
    tokenizer.addPatterns(getRule());  // Init patterns from rule
    return tokenizer;
}

/**
 * Creates the tokenizer instance.
 */
protected Tokenizer createTokenizerImpl()  { return new Tokenizer(); }

/**
 * Sets the tokenizer.
 */
protected void setTokenizer(Tokenizer aTokenizer)  { _tokenizer = aTokenizer; }

/**
 * Returns the current token.
 */
public Token getToken()  { return _token!=null? _token : (_token=getNextToken()); }

/**
 * Fetches and returns the next token.
 */
protected Token getNextToken()
{
    if(_lookAheadTokens.size()>0)
        return _lookAheadTokens.remove(0);
    return getTokenizer().getNextToken();
}

/**
 * Returns the look ahead token at given index.
 */
protected Token getLookAheadToken(int anIndex)
{
    if(anIndex==0)
        return getToken();
    while(anIndex>_lookAheadTokens.size())
        _lookAheadTokens.add(getTokenizer().getNextToken());
    return _lookAheadTokens.get(anIndex-1);
}

/**
 * Parses a given input and returns ParseNode (convenience).
 */
public ParseNode parse(CharSequence anInput)  { setInput(anInput); return parse(); }

/**
 * Parses input and returns ParseNode.
 */
public ParseNode parse()  { return parse(getRule()); }

/**
 * Parses input and returns ParseNode.
 */
public ParseNode parse(ParseRule aRule)
{
    ParseNode node = parse(aRule, null);
    return node!=null && node.getCustomNode() instanceof ParseNode? (ParseNode)node.getCustomNode() : node;
}

/**
 * Parses input and returns custom parse tree node.
 */
public <T> T parseCustom(Class <T> aClass)  { return parseCustom(getRule(), aClass); }

/**
 * Parses input and returns custom parse tree node.
 */
public <T> T parseCustom(ParseRule aRule, Class <T> aClass)
{
    ParseNode node = parse(aRule, null);
    return node!=null? node.getCustomNode(aClass) : null;
}

/**
 * Returns a parse node if this rule matches string.
 */
protected ParseNode parse(ParseRule aRule, HandlerRef aHRef)
{
    // Get current token (if no token, just return null)
    Token token = getToken(); if(token==null) return null;
    
    // Get handler reference for given rule: Reuse if no Rule.Handler, otherwise create new HandlerRef for rule
    HandlerRef href = aRule.getHandler()==null? aHRef : new HandlerRef(aRule);
    
    // Handle ops
    switch(aRule.getOp()) {
    
        // Handle Or: Parse rules and break if either passes (return null if either fail)
        case Or: { ParseRule r0 = aRule.getChild0(), r1 = aRule.getChild1();
            if(parseSimple(r0, href)) break;
            if(parseSimple(r1, href)) break;
            return null;
        }
        
        // Handle And
        case And: { ParseRule r0 = aRule.getChild0(), r1 = aRule.getChild1();
        
            // Handle rule 0 LookAhead(x)
            if(r0.isLookAhead() && r0.getChild0()==null) {
                if(lookAhead(r1, r0.getLookAhead(), 0)<0) return null;
                if(parseSimple(r1, href)) break;
                parseFailed(r1, href.handler());
                break;
            }
            
            // Handle normal And
            boolean parsed0 = parseSimple(r0, href); if(!parsed0 && !r0.isOptional()) return null;
            if(parseSimple(r1, href)) break;
            if(parsed0 && r1.isOptional()) break;
            if(!parsed0) return null;
            parseFailed(r1, href.handler());
            break;
        }
        
        // Handle ZeroOrOne
        case ZeroOrOne: { ParseRule r0 = aRule.getChild0();
            if(!parseSimple(r0, href)) return null;
            break;
        }
        
        // Handle ZeroOrMore
        case ZeroOrMore: { ParseRule r0 = aRule.getChild0();
            if(!parseSimple(r0, href)) return null;
            while(parseSimple(r0, href));
            break;
        }
        
        // Handle OneOrMore
        case OneOrMore: { ParseRule r0 = aRule.getChild0();
            if(!parseSimple(r0, href)) return null;
            while(parseSimple(r0, href));
            break;
        }
            
        // Handle Pattern
        case Pattern: {
            
            // Check pattern
            if(aRule.getPattern()==token.getPattern()) {
                ParseNode node = createNode(aRule, token, token);
                if(href!=aHRef) {
                    href.handler().parsedOne(node);
                    node._customNode = href.handler().parsedAll();
                }
                _token = null; //getNextToken();
                return node;
            }
            return null;
        }
        
        // Handle LookAhead
        case LookAhead: { ParseRule rule = aRule.getChild0(); int tcount = aRule.getLookAhead();
            if(lookAhead(rule, tcount, 0)<0) return null;
            break;
        }
    }
    
    // Create new node and return
    ParseNode node = createNode(aRule, token, _sharedNode.getEndToken());
    if(href!=aHRef)
        node._customNode = href.handler().parsedAll();
    return node;
}

/**
 * Simple parse - returns true if rule parsed.
 */
boolean parseSimple(ParseRule aRule, HandlerRef anHRef)
{
    ParseNode node = parse(aRule, anHRef);
    if(node==null)
        return false;
    if(anHRef!=null && !aRule.isAnonymous())
        anHRef.handler().parsedOne(node);
    return true;
}

/**
 * Looks ahead given number of tokens and returns the remainder or -1 if it fails.
 */
protected int lookAhead(ParseRule aRule, int aTokenCount, int aTokenIndex)
{
    // Handle ops
    switch(aRule.getOp()) {
    
        // Handle Or
        case Or: { ParseRule r0 = aRule.getChild0(), r1 = aRule.getChild1();
            int remainder = lookAhead(r0, aTokenCount, aTokenIndex); if(remainder>=0) return remainder;
            return lookAhead(r1, aTokenCount, aTokenIndex);
        }
        
        // Handle And
        case And: { ParseRule r0 = aRule.getChild0(), r1 = aRule.getChild1();
            
            // Handle rule 0 LookAhead(x)
            if(r0.isLookAhead() && r0.getChild0()==null) {
                if(lookAhead(r1, r0.getLookAhead(), aTokenIndex)<0) return -1;
                return lookAhead(r1, aTokenCount, aTokenIndex);
            }
                
            // Handle normal And
            int rmdr0 = lookAhead(r0,aTokenCount,aTokenIndex); if(rmdr0<0 && !r0.isOptional() || rmdr0==0) return rmdr0;
            boolean parsed0 = rmdr0>0; if(!parsed0) rmdr0 = aTokenCount;
            int rmdr1 = lookAhead(r1, rmdr0, aTokenIndex + aTokenCount - rmdr0); if(rmdr1>=0) return rmdr1;
            if(parsed0 && r1.isOptional()) return rmdr0;
            return -1;
        }
        
        // Handle ZeroOrOne
        case ZeroOrOne: return lookAhead(aRule.getChild0(), aTokenCount, aTokenIndex);
        
        // Handle ZeroOrMore
        case ZeroOrMore: { ParseRule r0 = aRule.getChild0();
            int remainder = lookAhead(r0, aTokenCount, aTokenIndex), r = remainder;
            while(r>0) { r = lookAhead(r0, r, aTokenIndex + aTokenCount - r); if(r>=0) remainder = r; }
            return remainder;
        }
        
        // Handle OneOrMore
        case OneOrMore: { ParseRule r0 = aRule.getChild0();
            int remainder = lookAhead(r0, aTokenCount, aTokenIndex), r = remainder;
            while(r>0) { r = lookAhead(r0, r, aTokenIndex + aTokenCount - r); if(r>=0) remainder = r; }
            return remainder;
        }
            
        // Handle Pattern
        case Pattern: { Token token = getLookAheadToken(aTokenIndex);
            if(token!=null && aRule.getPattern()==token.getPattern())
                return aTokenCount - 1;
            return -1;
        }
        
        // Handle LookAhead
        case LookAhead: { ParseRule r0 = aRule.getChild0(); int tcount = aRule.getLookAhead();
            if(lookAhead(r0, tcount, aTokenIndex)<0) return -1;
            return aTokenCount;
        }
        
        // Complain
        default: throw new RuntimeException("Parser.lookAhead: Bogus op " + aRule.getOp());
    }
}

/**
 * Creates a node for given rule and start/end tokens (returns a shared node by default).
 */
protected ParseNode createNode(ParseRule aRule, Token aStartToken, Token anEndToken)
{
    _sharedNode.init(this, aRule, aStartToken, anEndToken); return _sharedNode;
}

/**
 * Called when parse fails.
 */
protected void parseFailed(ParseRule aRule, ParseHandler aHandler)
{
    if(aHandler!=null) aHandler.reset();
    throw new ParseException(this, aRule);
}

/**
 * Parses given input and returns custom parse tree node (convenience).
 */
public <T> T parseCustom(CharSequence anInput, Class <T> aClass)  { setInput(anInput); return parseCustom(aClass); }

/**
 * A class to pass Handler by reference, allowing it to be created lazily, but used higher up in stack.
 */
public static final class HandlerRef {
    final ParseRule _rule; ParseHandler _handler;
    HandlerRef(ParseRule aRule) { _rule = aRule; }
    private final ParseHandler handler()  {
        return _handler!=null? _handler : (_handler=_rule.getHandler().getAvailableHandler()); }
}

}