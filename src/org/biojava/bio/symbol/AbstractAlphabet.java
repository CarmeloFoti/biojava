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

import org.biojava.utils.*;

/**
 * An abstract implementation of Alphabet.
 * <P>
 * This provides the frame-work for maintaining the SymbolParser <-> name
 * mappings and also for the ChangeListeners.
 * <P>
 * This class is for developers to derive from, not for use directly.
 *
 * @author Matthew Pocock
 */
public abstract class AbstractAlphabet implements FiniteAlphabet {
  private Map parserByName;
  private ChangeSupport changeSupport;

  {
    parserByName = new HashMap();
  }
  
  protected boolean hasListeners() {
    return changeSupport == null;
  }
  
  protected ChangeSupport getChangeSupport(ChangeType ct) {
    if(changeSupport == null) {
      changeSupport = new ChangeSupport();
    }
    
    return changeSupport;
  }
  
  /**
  *Assigns a symbol parser to a string object.  Afterwards, the parser can be retrieved using the getParser method.
  *
  *@param name Name of the string to associate with a parser.
  *@param parser The parser to associate your string with.
  */
  public void putParser(String name, SymbolParser parser) {
    parserByName.put(name, parser);
  }

  public SymbolParser getParser(String name)
         throws NoSuchElementException {
    SymbolParser parser = (SymbolParser) parserByName.get(name);
    if(parser == null) {
      if(name.equals("token")) {
        parser = new TokenParser(this);
        putParser(name, parser);
      } else if(name.equals("name")) {
        parser = new NameParser(this);
        putParser(name, parser);
      } else {
        throw new NoSuchElementException("There is no parser '" + name +
                                         "' defined in alphabet " + getName());
      }
    }
    return parser;
  }

  public void addChangeListener(ChangeListener cl) {
    getChangeSupport(null).addChangeListener(cl);
  }
  
  public void addChangeListener(ChangeListener cl, ChangeType ct) {
    getChangeSupport(ct).addChangeListener(cl);
  }
  
  public void removeChangeListener(ChangeListener cl) {
    getChangeSupport(null).removeChangeListener(cl);
  }
  
  public void removeChangeListener(ChangeListener cl, ChangeType ct) {
    getChangeSupport(ct).removeChangeListener(cl);
  } 
  
  protected AbstractAlphabet() {}
}
