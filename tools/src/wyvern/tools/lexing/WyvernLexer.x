import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wyvern.tools.parsing.transformers.*;
import wyvern.tools.typedAST.core.*;
import wyvern.tools.typedAST.interfaces.*;
import wyvern.tools.typedAST.core.expressions.*;
import wyvern.tools.typedAST.core.binding.*;
import wyvern.tools.typedAST.core.values.*;
import wyvern.tools.typedAST.extensions.*;
import wyvern.tools.typedAST.core.declarations.*;
import wyvern.tools.typedAST.abs.*;
import wyvern.tools.types.*;
import wyvern.tools.types.extensions.*;
import wyvern.tools.util.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.*;
import wyvern.tools.errors.FileLocation;
import java.net.URI;
import edu.umn.cs.melt.copper.runtime.logging.CopperParserException;
import wyvern.tools.parsing.coreparser.Token;
import static wyvern.tools.parsing.coreparser.WyvernParserConstants.*;

%%
%parser WyvernLexer

%aux{
	/********************** LEXER STATE ************************/
	boolean foundTilde = false;						// is there a tilde ~ in the current line?
	boolean DSLNext = false;						// is the next line a DSL?
	boolean inDSL = false;							// are we in a DSL?
	Stack<String> indents = new Stack<String>();	// the stack of indents
	
	/********************** HELPER FUNCTIONS ************************/
	
	/** @returns 1 for an indent, -n for n dedents, or 0 for the same indentation level
	 */
	int adjustIndent(String newIndent) throws CopperParserException {
		String currentIndent = indents.peek();
		if (newIndent.length() < currentIndent.length()) {
			// dedent(s)
			int dedentCount = 0;
			while (newIndent.length() < currentIndent.length()) {
				indents.pop();
				currentIndent = indents.peek();
				dedentCount--;
			}
			if (newIndent.equals(currentIndent))
				return dedentCount;
			else
				throw new CopperParserException("Illegal dedent: does not match any previous indent level");
		} else if (newIndent.length() > currentIndent.length()) {
			// indent
			if (newIndent.startsWith(currentIndent)) {
				indents.push(newIndent);
				return 1;
			} else {
				throw new CopperParserException("Illegal indent: not a superset of previous indent level");
			}
		} else {
			return 0;
		}
	}

	void addDedentsForChange(List<Token> tokenList, int indentChange, Token tokenLoc) {
		while (indentChange < 0) {
			Token t = makeToken(DEDENT,"",tokenLoc);
			tokenList.add(t);
			indentChange++;
		}
	}
	
	List<Token> tokensForIndent(Token newIndent) throws CopperParserException {
		int indent = adjustIndent(newIndent.image);
		if (indent == 0)
			return makeList(newIndent);
		if (indent == 1) {
			newIndent.kind = INDENT;
			return makeList(newIndent);
		}
		List<Token> tokenList = makeList(newIndent);
		addDedentsForChange(tokenList, indent, newIndent);
		return tokenList;
	}
	
	Token makeToken(int kind, String s, Token tokenLoc) {
		return makeToken(kind, s,tokenLoc.beginLine, tokenLoc.beginColumn);
	}
	
	Token makeToken(int kind, String s, int beginLine, int beginColumn) {
		Token t = new Token(kind, s);
		t.beginLine = beginLine;
		t.beginColumn = beginColumn;
		return t;
	}
	
	/** Wraps the lexeme s in a Token, setting the begin line/column and kind appropriately */
	Token token(int kind, String s) {
		return makeToken(kind, s, virtualLocation.getLine(), virtualLocation.getColumn());
	}
	
	List<Token> emptyList() {
		return new LinkedList<Token>();
	}
	
	List<Token> makeList(Token t) {
		List<Token> l = emptyList();
		l.add(t);
		return l;
	}
	
	boolean isSpecial(Token t) {
		switch (t.kind) {
			case SINGLE_LINE_COMMENT:
			case MULTI_LINE_COMMENT:
			case WHITESPACE:
					return true;
			default:
					return false;
		}
	}
	
	boolean hasNonSpecialToken(List<Token> l) {
		for (Token t : l)
			if (!isSpecial(t))
				return true; 
		return false;
	}
	
%aux}

%init{
	indents.push("");
%init}

%lex{
	class keywds;
    class specialNumbers;
    
    terminal Token whitespace_t ::= /[ \t]+/ {: RESULT = token(WHITESPACE,lexeme); :};
    terminal Token dsl_indent_t ::= /[ \t]+/ {: RESULT = token(WHITESPACE,lexeme); :};
    terminal Token indent_t ::= /[ \t]+/ {: RESULT = token(WHITESPACE,lexeme); :};

    terminal Token newline_t ::= /(\n|(\r\n))/ {: RESULT = token(WHITESPACE,lexeme); :};
    
    terminal Token continue_line_t ::= /\\(\n|(\r\n))/ {: RESULT = token(WHITESPACE,lexeme); :};

	terminal Token comment_t  ::= /\/\/([^\r\n])*/ {: RESULT = token(SINGLE_LINE_COMMENT,lexeme); :};
	terminal Token multi_comment_t  ::= /\/\*(.|\n|\r)*?\*\// {: RESULT = token(MULTI_LINE_COMMENT,lexeme); :};
	
 	terminal Token identifier_t ::= /[a-zA-Z_][a-zA-Z_0-9]*/ in (), < (keywds), > () {:
 		RESULT = token(IDENTIFIER,lexeme);
 	:};

    terminal classKwd_t ::= /class/ in (keywds);
	terminal typeKwd_t 	::= /type/ in (keywds);
	terminal valKwd_t 	::= /val/ in (keywds);
	terminal defKwd_t 	::= /def/ in (keywds);
	terminal varKwd_t 	::= /var/ in (keywds);
	terminal fnKwd_t 	::= /fn/ in (keywds);
	terminal metadataKwd_t 	::= /metadata/ in (keywds);
	terminal newKwd_t 	::= /new/ in (keywds);
 	terminal importKwd_t   ::= /import/ in (keywds);
 	terminal moduleKwd_t   ::= /module/ in (keywds);
	terminal ifKwd_t 	::= /if/ in (keywds);
 	terminal thenKwd_t   ::= /then/ in (keywds);
 	terminal elseKwd_t   ::= /else/ in (keywds);
 	terminal objtypeKwd_t   ::= /objtype/ in (keywds);

	terminal taggedKwd_t  ::= /tagged/  in (keywds);
    terminal matchKwd_t   ::= /match/   in (keywds);
    terminal defaultKwd_t ::= /default/ in (keywds);
    terminal caseKwd_t ::= /case/ in (keywds);
    terminal ofKwd_t ::= /of/ in (keywds);
    terminal comprisesKwd_t ::= /comprises/ in (keywds);

 	terminal Token decimalInteger_t ::= /([1-9][0-9]*)|0/  {: RESULT = token(DECIMAL_LITERAL,lexeme); :};

	terminal Token tilde_t ::= /~/ {: RESULT = token(TILDE,lexeme); :};
	terminal Token plus_t ::= /\+/ {: RESULT = token(PLUS,lexeme); :};
	terminal Token dash_t ::= /-/ {: RESULT = token(DASH,lexeme); :};
	terminal Token mult_t ::= /\*/ {: RESULT = token(MULT,lexeme); :};
	terminal Token divide_t ::= /\// {: RESULT = token(DIVIDE,lexeme); :};
	terminal Token equals_t ::= /=/ {: RESULT = token(EQUALS,lexeme); :};
	terminal Token equalsequals_t ::= /==/ {: RESULT = token(EQUALSEQUALS,lexeme); :};
	terminal Token openParen_t ::= /\(/ {: RESULT = token(LPAREN,lexeme); :};
 	terminal Token closeParen_t ::= /\)/ {: RESULT = token(RPAREN,lexeme); :};
 	terminal Token comma_t ::= /,/  {: RESULT = token(COMMA,lexeme); :};
 	terminal Token arrow_t ::= /=\>/  {: RESULT = token(ARROW,lexeme); :};
 	terminal Token tarrow_t ::= /-\>/  {: RESULT = token(TARROW,lexeme); :};
 	terminal Token dot_t ::= /\./ {: RESULT = token(DOT,lexeme); :};
 	terminal Token colon_t ::= /:/ {: RESULT = token(COLON,lexeme); :};
 	terminal Token pound_t ::= /#/ {: RESULT = token(POUND,lexeme); :};
 	terminal Token question_t ::= /?/ {: RESULT = token(QUESTION,lexeme); :};
 	terminal Token bar_t ::= /\|/ {: RESULT = token(BAR,lexeme); :};
 	terminal Token and_t ::= /&/ {: RESULT = token(AND,lexeme); :};
 	terminal Token gt_t ::= />/ {: RESULT = token(GT,lexeme); :};
 	terminal Token lt_t ::= /</ {: RESULT = token(LT,lexeme); :};
    terminal Token oSquareBracket_t ::= /\[/ {: RESULT = token(LBRACK,lexeme); :};
    terminal Token cSquareBracket_t ::= /\]/ {: RESULT = token(RBRACK,lexeme); :};

 	terminal shortString_t ::= /(('([^'\n]|\\.|\\O[0-7])*')|("([^"\n]|\\.|\\O[0-7])*"))|(('([^']|\\.)*')|("([^"]|\\.)*"))/ {:
 		RESULT = lexeme.substring(1,lexeme.length()-1);
 	:};

 	terminal Token oCurly_t ::= /\{/ {: RESULT = token(LBRACE,lexeme); :};
 	terminal Token cCurly_t ::= /\}/ {: RESULT = token(RBRACE,lexeme); :};
 	terminal notCurly_t ::= /[^\{\}]*/ {: RESULT = lexeme; :};
 	
 	terminal Token dslLine_t ::= /[^\n]*(\n|(\r\n))/ {: RESULT = token(DSLLINE,lexeme); :};
 	
 	// error if DSLNext but not indented further
 	// DSL if DSLNext and indented (unsets DSLNext, sets inDSL)
 	// DSL if inDSL and indented
 	// intent_t otherwise 
	disambiguate d1:(dsl_indent_t,indent_t)
	{:
		String currentIndent = indents.peek();
		if (lexeme.length() > currentIndent.length() && lexeme.startsWith(currentIndent)) {
			// indented
			if (DSLNext || inDSL) {
				DSLNext = false;
				inDSL = true;
				return dsl_indent_t;
			} else {
				return indent_t;
			}
		}
		if (DSLNext)
			throw new CopperParserException("Indicated DSL with ~ but then did not indent");
		inDSL = false;
		return indent_t;
	:};
%lex}

%cf{
	non terminal List<Token> program;
	non terminal List<Token> logicalLine;
	non terminal List<Token> dslLine;
	non terminal List<Token> anyLineElement;
	non terminal List<Token> nonWSLineElement;
	non terminal List lineElementSequence;
	non terminal List<Token> parens;
	non terminal List<Token> parenContents;
	non terminal Token operator;
	non terminal List<Token> aLine;

	start with program;
	
	parenContents ::= anyLineElement:e {: RESULT = e; :}
	                | newline_t:t {: RESULT = makeList(t); :}
	                | {: RESULT = emptyList(); :};
	
	parens ::= openParen_t:t1 parenContents:list closeParen_t:t2 {: RESULT = makeList(t1); RESULT.addAll(list); RESULT.add(t2); :}
	         | oSquareBracket_t:t1 parenContents:list cSquareBracket_t:t2  {: RESULT = makeList(t1); RESULT.addAll(list); RESULT.add(t2); :};
	
	operator ::= tilde_t:t {: foundTilde = true; RESULT = t; :}
	           | plus_t:t {: RESULT = t; :}
	           | dash_t:t {: RESULT = t; :}
	           | mult_t:t {: RESULT = t; :}
	           | divide_t:t {: RESULT = t; :}
	           | equals_t:t {: RESULT = t; :}
	           | comma_t:t {: RESULT = t; :}
	           | arrow_t:t {: RESULT = t; :}
	           | tarrow_t:t {: RESULT = t; :}
	           | dot_t:t {: RESULT = t; :}
	           | colon_t:t {: RESULT = t; :}
	           | pound_t:t {: RESULT = t; :}
	           | question_t:t {: RESULT = t; :}
	           | bar_t:t {: RESULT = t; :}
	           | and_t:t {: RESULT = t; :}
	           | gt_t:t {: RESULT = t; :}
	           | lt_t:t {: RESULT = t; :}
	           ;
	           
	anyLineElement ::= whitespace_t:n {: RESULT = makeList(n); :}
	                 | nonWSLineElement:n {: RESULT = n; :};
	                 
	nonWSLineElement ::= identifier_t:n {: RESULT = makeList(n); :}
	                   | comment_t:n {: RESULT = makeList(n); :}
	                   | multi_comment_t:n {: RESULT = makeList(n); :}
	                   | continue_line_t:t  {: RESULT = makeList(t); :}
	                   | operator:t {: RESULT = makeList(t); :}
	                   | parens:l {: RESULT = l; :};

    dslLine ::= dsl_indent_t:t dslLine_t:line {: RESULT = makeList(t); RESULT.add(line); :};
	
	logicalLine ::= lineElementSequence:list newline_t:n
					{:	if (hasNonSpecialToken(list))
							n.kind = NEWLINE;
						list.add(n);
					    if (foundTilde) {
						    DSLNext = true;
						    foundTilde = false;
						}
						RESULT = list;
					:}
				  | newline_t:n {: RESULT = makeList(n); :};
				  
	lineElementSequence ::= indent_t:n {: RESULT = tokensForIndent(n); :}
	                      | nonWSLineElement:n {:
	                      		int levelChange = adjustIndent("");
	                      		RESULT = emptyList(); 
	                      		addDedentsForChange(RESULT, levelChange, n.get(0));
	                            RESULT.addAll(n);
	                        :}
	                      | lineElementSequence:list anyLineElement:n {: list.addAll(n); RESULT = list; :};
	
	aLine ::= dslLine:line {: RESULT = line; :}
	        | logicalLine:line  {: RESULT = line; :}; 
	          
	program ::= logicalLine:line {: RESULT = line; :}
	          | program:p aLine:line {: p.addAll(line); RESULT = p; :};
%cf}