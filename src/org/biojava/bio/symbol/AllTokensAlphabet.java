/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 */


package org.biojava.bio.symbol;

import java.util.*;
import java.io.*;

import org.biojava.bio.*;

/**
 * An implementation of FiniteAlphabet that grows the alphabet to accomodate all
 * the characters seen while parsing a file.
 * <P>
 * The contains and validate methods will still work as for other alphabets, but
 * the parsers will generate new symbol objects for each token or name seen.
 * <P>
 * This is particularly useful when reading in arbitrary alphabet files where
 * you don't want to invest the time and effort writing a formal alphabet.
 *
 * @author Matthew Pocock
 */
public class AllTokensAlphabet implements FiniteAlphabet, Serializable {
  private Map tokenToSymbol; // token->Symbol
  private Map nameToSymbol; // name->Symbol
  private Set symbols;
  private String name;
  
  private Annotation annotation;

  protected void addSymbol(Symbol r) {
    symbols.add(r);
    Character token = new Character(r.getToken());
    if(!tokenToSymbol.keySet().contains(token)) {
      tokenToSymbol.put(token, r);
    }
    nameToSymbol.put(r.getName(), r);
  }

  public Iterator iterator() {
    return symbols.iterator();
  }
  
  public Annotation getAnnotation() {
    if(annotation == null)
      annotation = new SimpleAnnotation();
    return annotation;
  }
  
  public boolean contains(Symbol r) {
    return symbols.contains(r);
  }
  
  public String getName() {
    return name;
  }
  
  public SymbolParser getParser(String name)
  throws NoSuchElementException {
    if(name.equals("name")) {
      return new NameParser(nameToSymbol) {
        public Symbol parseToken(String token) throws IllegalSymbolException {
          Symbol res = (Symbol) nameToSymbol.get(token);
          if(res == null) {
            res = new SimpleSymbol(token.charAt(0), token, null);
            addSymbol(res);
          }
          return res;
        }
      };
    } else if(name.equals("token")) {
      return new SymbolParser() {
        public Alphabet alphabet() {
          return AllTokensAlphabet.this;
        }
        public SymbolList parse(String seq) {
          List resList = new ArrayList(seq.length());
          for(int i = 0; i < seq.length(); i++)
            resList.add(parseToken(seq.substring(i, i+1)));
          return new SimpleSymbolList(alphabet(), resList);
        }
        public Symbol parseToken(String token) {
          char c = token.charAt(0);
          Character ch = new Character(c);
          Symbol r = (Symbol) tokenToSymbol.get(ch);
          if(r == null) {
            r = new SimpleSymbol(c, token, null);
            addSymbol(r);
          }
          return r;
        }
      };
    } else {
      throw new NoSuchElementException("No parser for " + name +
      " known in alphabet " + getName());
    }
  }
  
  public SymbolList symbols() {
    return new SimpleSymbolList(this, new ArrayList(symbols));
  }
  
  public int size() {
    return symbols.size();
  }
  
  public void validate(Symbol r)
  throws IllegalSymbolException {
    if(contains(r))
      return;
    throw new IllegalSymbolException("No symbol " + r.getName() +
                                      " in alphabet " + getName());
  }
  
  public AllTokensAlphabet(String name) {
    this.name = name;
    this.symbols = new HashSet();
    this.tokenToSymbol = new HashMap();
    this.nameToSymbol = new HashMap();
  }
}
