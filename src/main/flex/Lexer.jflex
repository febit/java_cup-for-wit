package java_cup.core;

import java_cup.Main;

%%
%class Lexer
%type Symbol
%function nextToken
%line
%column
%buffer 8192
%{

    private final StringBuilder buffer = new StringBuilder();
    private int csline, cscolumn;

    public int getColumn(){
        return yycolumn + 1;
    }
    
    public int getLine(){
        return yyline + 1;
    }

    private char yychar(){
        return (char)yychar;
    }

    private Symbol symbol(int sym) {
        return new Symbol(sym, yyline + 1, yycolumn + 1, sym);
    }

    private Symbol symbol(int sym, Object val) {
        return new Symbol(sym, yyline + 1, yycolumn + 1, val);
    }
    
    private Symbol symbol(int sym, int line, int column, Object val) {
        return new Symbol(sym, line, column, val);
    }

    private void error(String message) {
        Main.error("Scanner at " + (yyline + 1) + "(" + (yycolumn + 1) + "): " + message);
    }
%}

Newline = \r | \n | \r\n
Whitespace = [ \t\f] | {Newline}

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment}
TraditionalComment = "/*" {CommentContent} \*+ "/"
EndOfLineComment = "//" [^\r\n]* {Newline}
CommentContent = ( [^*] | \*+[^*/] )*

ident = ([:jletter:] | "_" ) ([:jletterdigit:] | [:jletter:] | "_" )*


%eofval{
    return symbol(Tokens.EOF);
%eofval}

%state CODESEG CLASSNAME

%%  

<YYINITIAL> {

  {Whitespace}  {                                              }
  "$"           { return symbol(Tokens.DOLLAR);              }
  "?"           { return symbol(Tokens.QUESTION);              }
  ";"           { return symbol(Tokens.SEMI);                  }
  ","           { return symbol(Tokens.COMMA);                }
  "|"           { return symbol(Tokens.BAR);                    }
  "["           { return symbol(Tokens.LBRACK);              }
  "]"           { return symbol(Tokens.RBRACK);              }
  "("           { return symbol(Tokens.LPAREN); }
  ")"           { return symbol(Tokens.RPAREN); }
  ":"           { return symbol(Tokens.COLON);                }
  "::="         { return symbol(Tokens.COLON_COLON_EQUALS);   }
  "%prec"       { return symbol(Tokens.PERCENT_PREC);  }
  ">"           { return symbol(Tokens.GT);                      }
  "<"           { return symbol(Tokens.LT);                      }
  {Comment}     {                                              }
  "{:"          { buffer.setLength(0); csline=yyline+1; cscolumn=yycolumn+1; yybegin(CODESEG);    }
  "package"     { yybegin(CLASSNAME); buffer.setLength(0); return symbol(Tokens.PACKAGE);            } 
  "import"      { yybegin(CLASSNAME); buffer.setLength(0); return symbol(Tokens.IMPORT);	       }
  "terminal"    { return symbol(Tokens.TERMINAL);	       }
  "nonterminal" { return symbol(Tokens.NONTERMINAL);	       }
  "precedence"  { return symbol(Tokens.PRECEDENCE);      }
  "left"        { return symbol(Tokens.LEFT);		       }
  "right"       { return symbol(Tokens.RIGHT);		       }
  "nonassoc"    { return symbol(Tokens.NONASSOC);          }
  "super"       { return symbol(Tokens.SUPER);          }
  "extends"     { return symbol(Tokens.EXTENDS);          }
  {ident}       { return symbol(Tokens.ID, yytext());             }
}

<CODESEG> {
  ":}"          { yybegin(YYINITIAL); return symbol(Tokens.CODE, csline, cscolumn, buffer.toString()); }
  [^]           { buffer.append(yytext()); }
}

<CLASSNAME> {
  ";"           { yybegin(YYINITIAL); return symbol(Tokens.CLASSNAME, csline, cscolumn, buffer.toString().trim()); }
  [^]           { buffer.append(yytext()); }
}


// error fallback
[^]             { error("Unrecognized character '" +yytext()+"' -- ignored"); }
