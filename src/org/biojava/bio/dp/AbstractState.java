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


package org.biojava.bio.dp;

import java.util.*;

import org.biojava.bio.*;
import org.biojava.bio.symbol.*;

/**
 * An abstract implementation of the State interface.
 * <P>
 * You only need to define the methods getWeight, setWeight, getTrainer
 * and advance.
 * If you know a lot about the probability distribution of your symbols,
 * you may wish to override sampleSymbol as well.
 *
 * @author Matthew Pocock
 */
public abstract class AbstractState implements EmissionState {
  private final FiniteAlphabet alpha;
  private Annotation annotation;
  private String name;
  
  public char getToken() {
    return name.charAt(0);
  }

  public String getName() {
    return name;
  }

  public Annotation getAnnotation() {
    if(annotation == null)
      annotation = new SimpleAnnotation();
    return annotation;
  }


  public final Alphabet alphabet() {
    return alpha;
  }

  public void setName(String name) {
    this.name = name;
  }

  public abstract void setWeight(Symbol res, double weight) throws IllegalSymbolException;
  
  public AbstractState(FiniteAlphabet alpha)
  throws IllegalArgumentException {
    if(alpha == null)
      throw new IllegalArgumentException("alpha can not be null");
    this.alpha = alpha;
  }

  protected AbstractState() {
    this.alpha = null;
  }
  
  public Symbol sampleSymbol()
  throws BioError {
    double p = Math.random();
    try {
      for(Iterator i = alpha.iterator(); i.hasNext(); ) {
        Symbol r = (Symbol) i.next();
        p -= Math.exp(getWeight(r));
        if( p <= 0) {
          return r;
        }
      }
    
      StringBuffer sb = new StringBuffer();
      for(Iterator i = alpha.iterator(); i.hasNext(); ) {
        Symbol r = (Symbol) i.next();
        double w = Math.exp(getWeight(r));
        if(w > 0.0)
          sb.append("\t" + r.getName() + " -> " + w + "\n");
      }
      throw new BioError(
        "Could not find a symbol emitted from state " + this.getName() +
        ". Do the probabilities sum to 1?" +
        "\np=" + p + "\n" + sb.toString()
      );
    } catch (IllegalSymbolException ire) {
      throw new BioError(
        ire,
        "Unable to itterate over all symbols in alphabet - " +
        "things changed beneath me!"
      );
    }
  }
}
