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

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.*;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.*;

import org.biojava.bio.*;
import org.biojava.bio.seq.io.*;
import org.biojava.utils.*;
import org.biojava.utils.bytecode.*;
import org.biojava.utils.stax.*;

/**
 * Utility methods for working with Alphabets.  Also acts as a registry for
 * well-known alphabets.
 *
 * <p>
 * The alphabet interfaces themselves don't give you a lot of help in actually
 * getting an alphabet instance. This is where the AlphabetManager comes in
 * handy. It helps out in serialization, generating derived alphabets and
 * building CrossProductAlphabet instances. It also contains limited support for
 * parsing complex alphabet names back into the alphabets.
 * </p>
 *
 * @author Matthew Pocock
 * @author Thomas Down
 */

public final class AlphabetManager {
    /**
     * Return the ambiguity symbol which matches all symbols in
     * a given alphabet.
     *
     * @since 1.2
     */

    public static Symbol getAllAmbiguitySymbol(FiniteAlphabet alpha) {
        Set allSymbols = new HashSet();
        for (Iterator i = alpha.iterator(); i.hasNext(); ) {
            allSymbols.add(i.next());
        }
        try {
            return alpha.getAmbiguity(allSymbols);
        } catch (IllegalSymbolException ex) {
            throw new BioError(ex, "Assertion failure: coudn't recover all-ambiguity symbol");
        }
    }

    /**
     * Return a set containing all possible symbols which can be
     * considered members of a given alphabet, including ambiguous
     * symbols.  Warning, this method can return large sets!
     *
     * @since 1.2
     */

    public static Set getAllSymbols(FiniteAlphabet alpha) {
        Set allSymbols = new HashSet();
        List orderedAlpha = new ArrayList(alpha.size());
        for (Iterator i = alpha.iterator(); i.hasNext(); ) {
            orderedAlpha.add(i.next());
        }

        int atomicSyms = alpha.size();
        int totalSyms = 1 << atomicSyms;

        for (int cnt = 0; cnt < totalSyms; ++cnt) {
            Set matchSet = new HashSet();
            for (int atom = 0; atom < atomicSyms; ++atom) {
                if ((cnt & (1 << atom)) != 0) {
                    matchSet.add(orderedAlpha.get(atom));
                }
            }

            try {
                allSymbols.add(alpha.getAmbiguity(matchSet));
            } catch (IllegalSymbolException ex) {
                throw new BioError(ex, "Assertion failed: couldn't get ambiguity symbol");
            }
        }

        return allSymbols;
    }

  /**
   * Singleton instance.
   */
  static private AlphabetManager am;

  /**
   * Retrieve the singleton instance.
   *
   * @return the AlphabetManager instance
   * @deprecated all AlphabetManager methods have become static
   */
  static public AlphabetManager instance() {
    if(am == null)
      am = new AlphabetManager();
    return am;
  }

  static private Map nameToAlphabet;
  static private Map nameToSymbol;
  static private Map crossProductAlphabets;
  static private Map ambiguitySymbols;
  static private GapSymbol gapSymbol;
  static private Map gapBySize;
  static private Map alphabetToIndex = new HashMap();

  /**
   * Retrieve the alphabet for a specific name.
   *
   * @param name the name of the alphabet
   * @return the alphabet object
   * @throws NoSuchElementException if there is no alphabet by that name
   */
  static public Alphabet alphabetForName(String name)
  throws NoSuchElementException{
    Alphabet alpha = (Alphabet) nameToAlphabet.get(name);
    if(alpha == null) {
      if(name.startsWith("(") && name.endsWith(")")) {
        alpha = generateCrossProductAlphaFromName(name);
      } else {
        throw new NoSuchElementException(
          "No alphabet for name " + name + " could be found"
        );
      }
    }
    return alpha;
  }
   /**
    * Retrieve the symbol represented a String object
    *
    * @param name of the string whose symbol you want to get
    * @throws NoSuchElementException if the string name is invalid.
    */
  static public Symbol symbolForName(String name)
  throws NoSuchElementException {
    Symbol s = (Symbol) nameToSymbol.get(name);
    if(s == null) {
      throw new NoSuchElementException("Could not find symbol under the name " + name);
    }
    return s;
  }

  /**
   * Register an alphabet by name.
   *
   * @param name  the name by which it can be retrieved
   * @param alphabet the Alphabet to store
   */
  static public void registerAlphabet(String name, Alphabet alphabet) {
    nameToAlphabet.put(name, alphabet);
  }

  /**
   * Get an iterator over all alphabets known.
   *
   * @return an Iterator over Alphabet objects
   */
  static public Iterator alphabets() {
    return nameToAlphabet.values().iterator();
  }

  /**
   * <p>
   * Get the special `gap' Symbol.
   * </p>
   *
   * <p>
   * The gap symbol is a Symbol that has an empty alphabet of matches. As such
   *, ever alphabet contains gap, as there is no symbol that matches gap, so
   * there is no case where an alphabet doesn't contain a symbol that matches
   * gap.
   * </p>
   *
   * <p>
   * Gap can be thought of as an empty sub-space within the space of all
   * possible symbols. If you are working in a cross-product alphabet, you
   * should chose whether to use gap to represent 'no symbol', or a basis symbol
   * of the appropriate size built entirely of gaps to represent 'no symbol in
   * each of the slots'. Perhaps this could be explained better.
   * </p>
   *
   * @return the system-wide symbol that represents a gap
   */
  static public Symbol getGapSymbol() {
    return gapSymbol;
  }

  /**
   * <p>
   * Get the gap symbol appropriate to this list of alphabets.
   * </p>
   *
   * <p>
   * The gap symbol with have the same shape a the alphabet list. It will be as
   * long as the list, and if any of the alphabets in the list have a dimension
   * greater than 1, it will also insert the appropriate gap there.
   * </p>
   *
   * @param alphas  List of alphabets
   * @return the appropriate gap symbol for the alphabet list
   */
  static public Symbol getGapSymbol(List alphas) {
    SizeQueen sq = new SizeQueen(alphas);
    Symbol s = (Symbol) gapBySize.get(sq);

    if(s == null) {
      if(alphas.size() == 0) { // should never be needed
        s = gapSymbol;
      } else if(alphas.size() == 1) { // should never happen
        Alphabet a = (Alphabet) alphas.get(0);
        s = getGapSymbol(a.getAlphabets());
      } else {
        List symList = new ArrayList(alphas.size());
        for(Iterator i = alphas.iterator(); i.hasNext(); ) {
          Alphabet a = (Alphabet) i.next();
          symList.add(getGapSymbol(a.getAlphabets()));
        }
        try {
          s = new SimpleBasisSymbol(
            Annotation.EMPTY_ANNOTATION,
            symList,
            Alphabet.EMPTY_ALPHABET
          );
        } catch (IllegalSymbolException ise) {
          throw new BioError(
            ise,
            "Assertion Failure: Should be able to make gap basis"
          );
        }
      }
      gapBySize.put(sq, s);
    }

    return s;
  }

  /**
   * <p>
   * Generate a new AtomicSymbol instance with a name and Annotation.
   * </p>
   *
   * <p>
   * Use this method if you wish to create an AtomicSymbol instance. Initially it
   * will not be a member of any alphabet.
   * </p>
   *
   * @param name  the String returned by getName()
   * @param annotation the Annotation returned by getAnnotation()
   * @return a new AtomicSymbol instance
   */
  static public AtomicSymbol createSymbol(
    String name, Annotation annotation
  ) {
    AtomicSymbol as = new FundamentalAtomicSymbol(name, annotation);
    return as;
  }
  
  /**
   * <p>
   * Generate a new AtomicSymbol instance with a token, name and Annotation.
   * </p>
   *
   * <p>
   * Use this method if you wish to create an AtomicSymbol instance. Initially it
   * will not be a member of any alphabet.
   * </p>
   *
   * @param token  the Char token returned by getToken() (ignpred as of BioJava 1.2)
   * @param name  the String returned by getName()
   * @param annotatin the Annotation returned by getAnnotation()
   * @return a new AtomicSymbol instance
   * @deprecated Use the two-arg version of this method instead.
   */
  static public AtomicSymbol createSymbol(
    char token, String name, Annotation annotation
  ) {
    AtomicSymbol as = new FundamentalAtomicSymbol(name, annotation);
    return as;
  }

  /**
   * <p>
   * Generates a new Symbol instance that represents the tuple of Symbols in
   * symList.
   * </p>
   *
   * <p>
   * This method is most useful for writing Alphabet implementations. It should
   * not be invoked by casual users. Use alphabet.getSymbol(List) instead.
   * </p>
   *
   * @param token   the Symbol's token [ignored since 1.2]
   * @param symList a list of Symbol objects
   * @param alpha   the Alphabet that this Symbol will reside in
   * @return a Symbol that encapsulates that List
   * @deprecated use the new version, without the token argument
   */
  static public Symbol createSymbol(
    char token, Annotation annotation,
    List symList, Alphabet alpha
  ) throws IllegalSymbolException {
      return createSymbol(annotation, symList, alpha);
  }
  
  /**
   * <p>
   * Generates a new Symbol instance that represents the tuple of Symbols in
   * symList.
   * </p>
   *
   * <p>
   * This method is most useful for writing Alphabet implementations. It should
   * not be invoked by casual users. Use alphabet.getSymbol(List) instead.
   * </p>
   *
   * @param symList a list of Symbol objects
   * @param alpha   the Alphabet that this Symbol will reside in
   * @return a Symbol that encapsulates that List
   */
  static public Symbol createSymbol(
    Annotation annotation,
    List symList, Alphabet alpha
  ) throws IllegalSymbolException {
    Iterator i = symList.iterator();
    int basis = 0;
    int atomC = 0;
    while(i.hasNext()) {
      Symbol s = (Symbol) i.next();
      if(s instanceof BasisSymbol) {
        basis++;
        if(s instanceof AtomicSymbol) {
          atomC++;
        }
      }
    }

    if(atomC == symList.size()) {
      return new SimpleAtomicSymbol(annotation, symList);
    } else if(basis == symList.size()) {
      return new SimpleBasisSymbol(
        annotation,
        symList,
        new SimpleAlphabet(
          expandMatches(alpha, symList, new ArrayList())
        )
      );
    } else {
      return new SimpleSymbol(
        annotation,
        new SimpleAlphabet(
          expandBasis(alpha, symList, new ArrayList())
        )
      );
    }
  }

  /**
   * Expands a list of BasisSymbols into the set of AtomicSymbol instances
   * it matches.
   */
  private static Set expandBasis(Alphabet alpha, List symList, List built) {
    int indx = built.size();
    if(indx < symList.size()) {
      Symbol s = (Symbol) symList.get(indx);
      if(s instanceof AtomicSymbol) {
        built.add(s);
        return expandBasis(alpha, symList, built);
      } else {
        Set res = new HashSet();
        Iterator i = ((FiniteAlphabet) s.getMatches()).iterator();
        while(i.hasNext()) {
          AtomicSymbol as = (AtomicSymbol) i.next();
          List built2 = new ArrayList(built);
          built2.add(as);
          res.addAll(expandBasis(alpha, symList, built2));
        }
        return res;
      }
    } else {
      try {
        return Collections.singleton(alpha.getSymbol(built));
      } catch (IllegalSymbolException ise) {
        throw new BioError(
          ise,
          "Assertion Failure: Should just have legal AtomicSymbol instances."
        );
      }
    }
  }

  /**
   * <p>
   * Generates a new Symbol instance that represents the tuple of Symbols in
   * symList.
   * </p>
   *
   * <p>
   * This method is most useful for writing Alphabet implementations. It should
   * not be invoked by users. Use alphabet.getSymbol(Set) instead.
   * </p>
   *
   * @param token   the Symbol's token [ignored since 1.2]
   * @param name    the Symbol's name
   * @param symSet  a Set of Symbol objects
   * @param alpha   the Alphabet that this Symbol will reside in
   * @return a Symbol that encapsulates that List
   * @deprecated use the three-arg version of this method instead.
   */
  static public Symbol createSymbol(
    char token, Annotation annotation,
    Set symSet, Alphabet alpha
  ) throws IllegalSymbolException {
      return createSymbol(annotation, symSet, alpha);
  }
  
  /**
   * <p>
   * Generates a new Symbol instance that represents the tuple of Symbols in
   * symList.
   * </p>
   *
   * <p>
   * This method is most useful for writing Alphabet implementations. It should
   * not be invoked by users. Use alphabet.getSymbol(Set) instead.
   * </p>
   *
   * @param name    the Symbol's name
   * @param symSet  a Set of Symbol objects
   * @param alpha   the Alphabet that this Symbol will reside in
   * @return a Symbol that encapsulates that List
   */
  static public Symbol createSymbol(
    Annotation annotation,
    Set symSet, Alphabet alpha
  ) throws IllegalSymbolException {
    if(symSet.size() == 0) {
      return getGapSymbol();
    }
    Set asSet = new HashSet();
    int len = -1;
    for(
      Iterator i = symSet.iterator();
      i.hasNext();
    ) {
      Symbol s = (Symbol) i.next();
      if(s instanceof AtomicSymbol) {
        AtomicSymbol as = (AtomicSymbol) s;
        int l = as.getSymbols().size();
        if(len == -1) {
          len = l;
        } else if(len != l) {
          throw new IllegalSymbolException(
            "Can't build ambiguity symbol as the symbols have inconsistent " +
            "length"
          );
        }
        asSet.add(as);
      } else {
        for(Iterator j = ((FiniteAlphabet) s.getMatches()).iterator();
          j.hasNext();
        ) {
          AtomicSymbol as = ( AtomicSymbol) j.next();
          int l = as.getSymbols().size();
          if(len == -1) {
            len = l;
          } else if(len != l) {
            throw new IllegalSymbolException(
              "Can't build ambiguity symbol as the symbols have inconsistent " +
              "length"
            );
          }
          asSet.add(as);
        }
      }
    }
    if(asSet.size() == 0) {
      return getGapSymbol();
    } else if(asSet.size() == 1) {
      return (Symbol) asSet.iterator().next();
    } else {
      if(len == 1) {
        return new SimpleBasisSymbol(
          annotation, new SimpleAlphabet(asSet)
        );
      } else {
        List fs = factorize(alpha, asSet);
        if(fs == null) {
          return new SimpleSymbol(
            annotation,
            new SimpleAlphabet(asSet)
          );
        } else {
          return new SimpleBasisSymbol(
            annotation,
            fs, new SimpleAlphabet(
              expandBasis(alpha, fs, new ArrayList())
            )
          );
        }
      }
    }
  }

  /**
   * Generates a new CrossProductAlphabet from the give name.
   *
   * @param name  the name to parse
   * @return the associated Alphabet
   */
  static public Alphabet generateCrossProductAlphaFromName(
    String name
  ) {
    if(!name.startsWith("(") || !name.endsWith(")")) {
      throw new BioError(
        "Can't parse " + name +
        " into a cross-product alphabet as it is not bracketed"
      );
    }

    name = name.substring(1, name.length()-1).trim();
    List aList = new ArrayList(); // the alphabets
    int i = 0;
    while(i < name.length()) {
      String alpha = null;
      if(name.charAt(i) == '(') {
        int depth = 1;
        int j = i+1;
        while(j < name.length() && depth > 0) {
          char c = name.charAt(j);
          if(c == '(') {
            depth++;
          } else if(c == ')') {
            depth--;
          }
          j++;
        }
        if(depth == 0) {
          aList.add(alphabetForName(name.substring(i, j)));
          i = j;
        } else {
          throw new BioError(
            "Error parsing alphabet name: could not find matching bracket\n" +
            name.substring(i)
          );
        }
      } else {
        int j = name.indexOf(" x ", i);
        if(j < 0) {
          aList.add(alphabetForName(name.substring(i).trim()));
          i = name.length();
        } else {
          if(i != j){
            aList.add(alphabetForName(name.substring(i, j).trim()));
          }
          i = j + " x ".length();
        }
      }
    }

    return getCrossProductAlphabet(aList);
  }

  /**
   * <p>
   * Retrieve a CrossProductAlphabet instance over the alphabets in aList.
   * </p>
   *
   * <p>
   * If all of the alphabets in aList implements FiniteAlphabet then the
   * method will return a FiniteAlphabet. Otherwise, it returns a non-finite
   * alphabet.
   * </p>
   *
   * <p>
   * If you call this method twice with a list containing the same alphabets,
   * it will return the same alphabet. This promotes the re-use of alphabets
   * and helps to maintain the 'flyweight' principal for finite alphabet
   * symbols.
   * </p>
   *
   * <p>
   * The resulting alphabet cpa will be retrievable via
   * AlphabetManager.alphabetForName(cpa.getName())
   * </p>
   *
   * @param aList a list of Alphabet objects
   * @return a CrossProductAlphabet that is over the alphabets in aList
   */
  static public Alphabet getCrossProductAlphabet(List aList) {
    return getCrossProductAlphabet(aList, (Alphabet) null);
  }

  static public Alphabet getCrossProductAlphabet(List aList, String name)
  throws IllegalAlphabetException {
    Alphabet currentAlpha = (Alphabet) nameToAlphabet.get(name);
    if(currentAlpha != null) {
      if(currentAlpha.getAlphabets().equals(aList)) {
        return currentAlpha;
      } else {
        throw new IllegalAlphabetException(name + " already registered");
      }
    } else {
      Alphabet alpha = getCrossProductAlphabet(aList);
      registerAlphabet(name, alpha);
      return alpha;
    }
  }

  /**
   * <p>
   * Retrieve a CrossProductAlphabet instance over the alphabets in aList.
   * </p>
   *
   * <p>
   * This method is most usefull for implementors of cross-product alphabets,
   * allowing them to safely build the matches alphabets for ambiguity symbols.
   * </p>
   *
   * <p>
   * If all of the alphabets in aList implements FiniteAlphabet then the
   * method will return a FiniteAlphabet. Otherwise, it returns a non-finite
   * alphabet.
   * </p>
   *
   * <p>
   * If you call this method twice with a list containing the same alphabets,
   * it will return the same alphabet. This promotes the re-use of alphabets
   * and helps to maintain the 'flyweight' principal for finite alphabet
   * symbols.
   * </p>
   *
   * <p>
   * The resulting alphabet cpa will be retrievable via
   * AlphabetManager.alphabetForName(cpa.getName())
   * </p>
   *
   * @param aList a list of Alphabet objects
   * @param parent a parent alphabet
   * @return a CrossProductAlphabet that is over the alphabets in aList
   */
  static public Alphabet getCrossProductAlphabet(
    List aList, Alphabet parent
  ) {
    // This trap means that the `product' operator can be
    // safely applied to a single alphabet.

    if (aList.size() == 1)
        return (Alphabet) aList.get(0);

    if(crossProductAlphabets == null) {
      crossProductAlphabets = new HashMap();
    }

    Alphabet cpa = (Alphabet) crossProductAlphabets.get(aList);

    int size = 1;
    if(cpa == null) {
      for(Iterator i = aList.iterator(); i.hasNext(); ) {
        Alphabet aa = (Alphabet) i.next();
        if(! (aa instanceof FiniteAlphabet) ) {
          cpa =  new InfiniteCrossProductAlphabet(aList);
          break;
        }
        if(size <= 1000) {
          size *= ((FiniteAlphabet) aa).size();
        }
      }
      if(cpa == null) {
        try {
          if(size >= 0 && size < 1000) {
            cpa = new SimpleCrossProductAlphabet(aList, parent);
          } else {
            cpa = new SparseCrossProductAlphabet(aList);
          }
        } catch (IllegalAlphabetException iae) {
          throw new BioError(
            "Could not create SimpleCrossProductAlphabet for " + aList +
            " even though we should be able to. No idea what is wrong."
          );
        }
      }
      crossProductAlphabets.put(new ArrayList(aList), cpa);
      registerAlphabet(cpa.getName(), cpa);
    }

    return cpa;
  }

  private static Set expandMatches(Alphabet parent, List symList, List built) {
    int indx = built.size();
    if(indx < symList.size()) {
      BasisSymbol bs = (BasisSymbol) symList.get(indx);
      if(bs instanceof AtomicSymbol) {
        built.add(bs);
        return expandMatches(parent, symList, built);
      } else {
        Set syms = new HashSet();
        Iterator i = ((FiniteAlphabet) bs.getMatches()).iterator();
        while(i.hasNext()) {
          List built2 = new ArrayList(built);
          built2.add((AtomicSymbol) i.next());
          syms.addAll(expandMatches(parent, symList, built2));
        }
        return syms;
      }
    } else {
      try {
        Symbol s = parent.getSymbol(built);
        if(s instanceof AtomicSymbol) {
          return Collections.singleton((AtomicSymbol) s);
        } else {
          Set syms = new HashSet();
          for(Iterator i = ((FiniteAlphabet) s.getMatches()).iterator(); i.hasNext(); ) {
            syms.add((AtomicSymbol) i.next());
          }
          return syms;
        }
      } catch (IllegalSymbolException ise) {
        throw new BioError(ise, "Assertion Failure: Couldn't create symbol.");
      }
    }
  }

  /**
   * <p>
   * Return a list of BasisSymbol instances that uniquely sum up all
   * AtomicSymbol
   * instances in symSet. If the symbol can't be represented by a single list of
   * BasisSymbol instances, return null.
   * </p>
   *
   * <p>
   * This method is most useful for implementers of Alphabet and Symbol. It
   * probably should not be invoked by users.
   * </p>
   *
   * @param symSet  the Set of AtomicSymbol instances
   * @param alpha   the Alphabet instance that the Symbols are from
   * @return a List of BasisSymbols
   */
  public static List factorize(Alphabet alpha, Set symSet)
  throws IllegalSymbolException {
    List alphas = alpha.getAlphabets();
    List facts = new ArrayList();
    int size = symSet.size();
    Set syms = new HashSet();
    for(int col = 0; col < alphas.size(); col++) {
      Alphabet a = (Alphabet) alphas.get(col);
      for(Iterator i = symSet.iterator(); i.hasNext(); ) {
        syms.add(
          (AtomicSymbol) ((AtomicSymbol)
          i.next()).getSymbols().get(col)
        );
      }
      int s = syms.size();
      if( (size % s) != 0 ) {
        return null;
      }
      size /= s;
      facts.add(a.getAmbiguity(syms));
      syms.clear();
    }
    if(size != 1) {
      return null;
    }
    return facts;
  }



  /**
   * <p>
   * Initialize the static AlphabetManager resources.
   * </p>
   *
   * <p>
   * This parses the resource
   * <code>org/biojava/bio/seq/tools/AlphabetManager.xml</code>
   * and builds a basic set of alphabets.
   * </p>
   */
  static {
    nameToAlphabet = new HashMap();
    nameToSymbol = new HashMap();
    ambiguitySymbols = new HashMap();

    gapSymbol = new GapSymbol();
    gapBySize = new HashMap();
    gapBySize.put(new SizeQueen(new ArrayList()), gapSymbol);

    nameToAlphabet.put("INTEGER", IntegerAlphabet.getInstance());
    nameToAlphabet.put("DOUBLE", DoubleAlphabet.getInstance());

    try {
      gapBySize.put(
        new SizeQueen(Arrays.asList(
                new Alphabet[] { DoubleAlphabet.getInstance() }
        )),
        new SimpleBasisSymbol(
                Annotation.EMPTY_ANNOTATION,
                Arrays.asList(new Symbol[] { gapSymbol }),
                Alphabet.EMPTY_ALPHABET
        )
      );
    } catch (IllegalSymbolException ise) {
      throw new BioError(
        ise,
        "Assertion Failure: Should be able to make gap basis"
      );
    }

    ambiguitySymbols.put(new HashSet(), gapSymbol);
    try {
      InputStream alphabetStream = AlphabetManager.class.getClassLoader().getResourceAsStream(
        "org/biojava/bio/symbol/AlphabetManager.xml"
      );
      if (alphabetStream == null) {
          throw new BioError("Couldn't locate AlphabetManager.xml.  This probably means that your biojava.jar file is corrupt or incorrectly built.");
      }
      InputSource is = new InputSource(alphabetStream);
      loadAlphabets(is);
    } catch (Exception t) {
      throw new BioError(t, "Unable to initialize AlphabetManager");
    }
  }

    /**
     * Load additional Alphabets, defined in XML format, into the AlphabetManager's registry.
     * These can the be retrieved by calling <code>alphabetForName</code>.
     *
     * @param is an <code>InputSource</code> encapsulating the document to be parsed
     * @throws IOException if there is an error accessing the stream
     * @throws SAXException if there is an error while parsing the document
     * @throws BioException if a problem occurs when creating the new Alphabets.
     * @since 1.3
     */

    public static void loadAlphabets(InputSource is)
        throws SAXException, IOException, BioException
    {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            XMLReader parser = spf.newSAXParser().getXMLReader();
            parser.setContentHandler(new SAX2StAXAdaptor(new AlphabetManagerHandler()));
            parser.parse(is);
        } catch (ParserConfigurationException ex) {
            throw new BioException(ex, "Unable to create XML parser");
        }
    }

    /**
     * StAX handler for the alphabetManager element
     */

    private static class AlphabetManagerHandler extends StAXContentHandlerBase {
        public void startElement(String nsURI,
                                             String localName,
                                             String qName,
                                             Attributes attrs,
                                             DelegationManager dm)
             throws SAXException
         {
             if (localName.equals("alphabetManager")) {
                 // ignore
             } else if (localName.equals("symbol")) {
                 String name = attrs.getValue("name");
                 dm.delegate(new SymbolHandler(name));
             } else if (localName.equals("alphabet")) {
                 String name = attrs.getValue("name");
                 String parent = attrs.getValue("parent");
                 FiniteAlphabet parentAlpha = null;
                 if (parent != null && parent.length() > 0) {
                     parentAlpha = (FiniteAlphabet) nameToAlphabet.get(parent);
                 }
                 dm.delegate(new AlphabetHandler(name, parentAlpha));
             } else {
                 throw new SAXException("Unknown element in alphabetManager: " + localName);
             }
         }

         public void endElement(String nsURI,
                                            String localName,
                                String qName,
                                StAXContentHandler delegate)
            throws SAXException
         {
             if (delegate instanceof SymbolHandler) {
                 SymbolHandler sh = (SymbolHandler) delegate;
                 String name = sh.getName();
                 Symbol symbol = sh.getSymbol();
                 if (nameToSymbol.containsKey("name")) {
                     throw new SAXException("There is already a top-level symbol named " + name);
                 }
                 nameToSymbol.put(name, symbol);
             } else if (delegate instanceof AlphabetHandler) {
                 AlphabetHandler ah = (AlphabetHandler) delegate;
                 String name = ah.getName();
                 FiniteAlphabet alpha = ah.getAlphabet();
                 registerAlphabet(name, alpha);
             }
         }

         private class SymbolHandler extends StAXContentHandlerBase {
             private String name;
             private Symbol symbol;
             private Annotation annotation = new SmallAnnotation();

             public SymbolHandler(String name) {
                 this.name = name;
             }

             public void startElement(String nsURI,
                                                 String localName,
                                     String qName,
                                     Attributes attrs,
                                     DelegationManager dm)
                  throws SAXException
             {
                 if (localName.equals("symbol")) {
                     // ignore
                 } else if (localName.equals("description")) {
                     dm.delegate(new StringElementHandlerBase() {
                         protected void setStringValue(String s) {
                             try {
                                 annotation.setProperty("description", s);
                             } catch (ChangeVetoException ex) {
                                 throw new BioError(ex, "Assertion failure: veto while modifying new Annotation");
                             }
                         }
                     } );
                 } else {
                     throw new SAXException("Unknown element in symbol: " + localName);
                 }
             }

             public void endTree() {
                 symbol = new FundamentalAtomicSymbol(name, annotation);
             }

             Symbol getSymbol() {
                 return symbol;
             }

             String getName() {
                 return name;
             }
         }

         private class AlphabetHandler extends StAXContentHandlerBase {
             private String name;
             private Map localSymbols;
             private WellKnownAlphabet alpha;
             private ImmutableWellKnownAlphabetWrapper alphaWrapper;

             String getName() {
                 return name;
             }

             FiniteAlphabet getAlphabet() {
                 return alphaWrapper;
             }

             public void endTree() {
                 alpha.addChangeListener(ChangeListener.ALWAYS_VETO, ChangeType.UNKNOWN);
             }
             
             public AlphabetHandler(String name, FiniteAlphabet parent) {
                 this.name = name;
                 localSymbols = new OverlayMap(nameToSymbol);
                 alpha = new WellKnownAlphabet();
                 alpha.setName(name);
                 alphaWrapper = new ImmutableWellKnownAlphabetWrapper(alpha);
                 if (parent != null) {
                     for (Iterator i = parent.iterator(); i.hasNext(); ) {
                         Symbol sym = (Symbol) i.next();
                         try {
                             alpha.addSymbol(sym);
                         } catch (Exception ex) {
                             throw new BioError(ex, "Couldn't initialize alphabet from parent");
                         }
                         localSymbols.put(sym.getName(), sym);
                     }
                 }
             }

             public void startElement(String nsURI,
                                                 String localName,
                                     String qName,
                                     Attributes attrs,
                                     DelegationManager dm)
                  throws SAXException
             {
                 if (localName.equals("alphabet")) {
                     // ignore
                 } else if (localName.equals("symbol")) {
                     String name = attrs.getValue("name");
                     dm.delegate(new SymbolHandler(name));
                 } else if (localName.equals("symbolref")) {
                     String name = attrs.getValue("name");
                     Symbol sym = (Symbol) localSymbols.get(name);
                     if (sym == null) {
                         throw new SAXException("Reference to non-existent symbol " + name);
                     }
                     addSymbol(sym);
                 } else if (localName.equals("characterTokenization")) {
                     String name = attrs.getValue("name");
                     boolean caseSensitive = "true".equals(attrs.getValue("caseSensitive"));
                     dm.delegate(new CharacterTokenizationHandler(name, alphaWrapper, localSymbols, caseSensitive));
                 } else if (localName.equals("description")) {
                     dm.delegate(new StringElementHandlerBase() {
                         protected void setStringValue(String s) {
                             try {
                                 alpha.getAnnotation().setProperty("description", s);
                             } catch (ChangeVetoException ex) {
                                 throw new BioError(ex, "Assertion failure: veto while modifying new Annotation");
                             }
                         }
                     } );
                 } else {
                     throw new SAXException("Unknown element in alphabetl: " + localName);
                 }
             }

             public void endElement(String nsURI,
                                                String localName,
                                    String qName,
                                    StAXContentHandler delegate)
                  throws SAXException
             {
                 if (delegate instanceof SymbolHandler) {
                     SymbolHandler sh = (SymbolHandler) delegate;
                     String name = sh.getName();
                     Symbol symbol = sh.getSymbol();
                     localSymbols.put(name, symbol);
                     addSymbol(symbol);
                 } else if (delegate instanceof CharacterTokenizationHandler) {
                     CharacterTokenizationHandler cth = (CharacterTokenizationHandler) delegate;
                     String name = cth.getName();
                     SymbolTokenization toke = cth.getTokenization();
                     alpha.putTokenization(name, toke);
                 }
             }

             private void addSymbol(Symbol sym)
                 throws SAXException
             {
                 try {
                     alpha.addSymbol(sym);
                 } catch (ChangeVetoException cve) {
                     throw new BioError(cve, "Assertion failure: veto while modifying new Alphabet");
                 } catch (IllegalSymbolException ex) {
                     throw new SAXException("IllegalSymbolException adding symbol to alphabet");
                 }
             }
         }

         private class CharacterTokenizationHandler extends StAXContentHandlerBase {
             private String name;
             private Map localSymbols;
             private CharacterTokenization toke;

             String getName() {
                 return name;
             }

             SymbolTokenization getTokenization() {
                 return toke;
             }

             public CharacterTokenizationHandler(String name,
                                                 FiniteAlphabet alpha,
                                                 Map localSymbols,
                                                 boolean caseSensitive)
             {

                 this.name = name;
                 this.localSymbols = new HashMap();
                 for (Iterator i = alpha.iterator(); i.hasNext(); ) {
                     Symbol sym = (Symbol) i.next();
                     this.localSymbols.put(sym.getName(), sym);
                 }
                 toke = new CharacterTokenization(alpha, caseSensitive);
             }

             public void startElement(String nsURI,
                                                 String localName,
                                     String qName,
                                     Attributes attrs,
                                     DelegationManager dm)
                  throws SAXException
             {
                 if (localName.equals("characterTokenization")) {
                     // ignore
                 } else if (localName.equals("atomicMapping")) {
                     dm.delegate(new MappingHandler(true));
                 } else if (localName.equals("ambiguityMapping")) {
                     dm.delegate(new MappingHandler(false));
                 } else {
                     throw new SAXException("Unknown element in characterTokenization: " + localName);
                 }
             }

             private class MappingHandler extends StAXContentHandlerBase {
                 public MappingHandler(boolean isAtomic) {
                     this.isAtomic = isAtomic;
                 }

                 boolean isAtomic;
                 Set symbols = new HashSet();
                 char c = '\0';
                 int level = 0;

                 public void startElement(String nsURI,
                                          String localName,
                                          String qName,
                                          Attributes attrs,
                                          DelegationManager dm)
                     throws SAXException
                 {
                     if (level == 0) {
                         c = attrs.getValue("token").charAt(0);
                     } else {
                         if (localName.equals("symbolref")) {
                             String name = attrs.getValue("name");
                             Symbol sym = (Symbol) localSymbols.get(name);
                             if (sym == null) {
                                 throw new SAXException("Reference to non-existent symbol " + name);
                             }
                             symbols.add(sym);
                         } else {
                             throw new SAXException("Unknown element in mapping: " + localName);
                         }
                     }
                     ++level;
                 }

                 public void endElement(String nsURI,
                                                    String localName,
                                        String qName,
                                        StAXContentHandler delegate)
                     throws SAXException
                 {
                     --level;
                 }

                 public void endTree()
                     throws SAXException
                 {
                     try {
                         Symbol ambiSym = toke.getAlphabet().getAmbiguity(symbols);
                         toke.bindSymbol(ambiSym, c);
                     } catch (IllegalSymbolException ex) {
                         ex.printStackTrace();
                         throw new SAXException("IllegalSymbolException binding mapping for " + c);
                     }
                 }
             }
         }
    }

    private static class WellKnownTokenizationWrapper 
        extends Unchangeable
        implements SymbolTokenization, Serializable 
    {
        private String name;
        private Alphabet alphabet;
        private SymbolTokenization toke;

        WellKnownTokenizationWrapper(Alphabet alpha, SymbolTokenization toke, String name) {
            super();
            this.alphabet = alpha;
            this.name = name;
            this.toke = toke;
        }

        public Alphabet getAlphabet() {
            return alphabet;
        }
        
        public TokenType getTokenType() {
            return toke.getTokenType();
        }
        
        public StreamParser parseStream(SeqIOListener listener) {
            return toke.parseStream(listener);
        }
        
        public Symbol parseToken(String s)
            throws IllegalSymbolException
        {
            return toke.parseToken(s);
        }
        
        public String tokenizeSymbol(Symbol s)
            throws IllegalSymbolException
        {
            return toke.tokenizeSymbol(s);
        }
        
        public String tokenizeSymbolList(SymbolList sl)
            throws IllegalAlphabetException, IllegalSymbolException
        {
            return toke.tokenizeSymbolList(sl);
        }
        
        public Annotation getAnnotation() {
            return toke.getAnnotation();
        }
        
        public Object writeReplace() {
            return new OPH(getAlphabet().getName(), name);
        }

        private static class OPH implements Serializable {
            private String alphaName;
            private String name;

            OPH(String alphaName, String name) {
                this.alphaName = alphaName;
                this.name = name;
            }

            private Object readResolve() throws ObjectStreamException {
                try {
                    Alphabet alphabet = alphabetForName(alphaName);
                    return alphabet.getTokenization(name);
                } catch (Exception ex) {
                    throw new InvalidObjectException("Couldn't resolve tokenization " + name + " in alphabet " + alphaName);
                }
            }
        }
    }

    /**
     * An alphabet contained WellKnownSymbols
     */

    private static class WellKnownAlphabet
        extends SimpleAlphabet
    {
      protected void addSymbolImpl(AtomicSymbol s)
          throws IllegalSymbolException, ChangeVetoException
      {
          super.addSymbolImpl(new WellKnownAtomicSymbol(this, s));
      }
      
      protected Symbol getAmbiguityImpl(Set s)
          throws IllegalSymbolException
      {
          Symbol sym = new WellKnownBasisSymbol(
                this,
                (BasisSymbol) super.getAmbiguityImpl(s)
          );
          return sym;
      }
    }
    
    /**
     * A wrapper which makes an Alphabet unchangable, and also fixes serialization
     */
     
    private static class ImmutableWellKnownAlphabetWrapper
        extends Unchangeable 
        implements FiniteAlphabet, Serializable 
    {
        private FiniteAlphabet alpha;
        private Map tokenizationsByName = new HashMap();
        
        public ImmutableWellKnownAlphabetWrapper(FiniteAlphabet alpha) {
            super();
            this.alpha = alpha;
        }
        
        private Object writeReplace() {
            return new OPH(getName());
        }
        
        public SymbolTokenization getTokenization(String name)
            throws BioException
        {
            SymbolTokenization toke = (SymbolTokenization) tokenizationsByName.get(name);
            if (toke == null) {
                if ("name".equals(name)) {
                    toke = new NameTokenization(this);
                } else {
                    toke = new WellKnownTokenizationWrapper(this, alpha.getTokenization(name), name);
                }
                tokenizationsByName.put(name, toke);
            }
            return toke;
        }
      
        /**
         * Placeholder for a WellKnownAlphabet in a serialized
         * object stream.
         */

         private static class OPH implements Serializable {
             private String name;
             
             public OPH(String name) {
                 this.name = name;
             }
             
             public OPH() {
             }
             
             private Object readResolve() throws ObjectStreamException {
                 try {
                     Alphabet a = AlphabetManager.alphabetForName(name);
                     return a;
                 } catch (NoSuchElementException ex) {
                     throw new InvalidObjectException("Couldn't resolve alphabet " + name);
                 }
             }
         }
      
        public boolean contains(Symbol s) {
            return alpha.contains(s);
        }
        
        public List getAlphabets() {
            return alpha.getAlphabets();
        }
        
        public Symbol getAmbiguity(Set s) 
            throws IllegalSymbolException
        {
            return alpha.getAmbiguity(s);
        }
        
        public Symbol getGapSymbol() {
            return alpha.getGapSymbol();
        }
        
        public String getName() {
            return alpha.getName();
        }
        
        public Symbol getSymbol(List l) 
            throws IllegalSymbolException
        {
            return alpha.getSymbol(l);
        }
        
        public void validate(Symbol s)
            throws IllegalSymbolException
        {
                alpha.validate(s);
        }
        
        public void addSymbol(Symbol s)
            throws ChangeVetoException
        {
            throw new ChangeVetoException("Can't add symbols to Well Known Alphabets");
        }
        
        public void removeSymbol(Symbol s)
            throws ChangeVetoException
        {
            throw new ChangeVetoException("Can't remove symbols from Well Known Alphabets");
        }
        
        public Iterator iterator() {
            return  alpha.iterator();
        }
        
        public int size() {
            return alpha.size();
        }
        
        public SymbolList symbols() {
            return alpha.symbols();
        }
        
        public Annotation getAnnotation() {
            return alpha.getAnnotation();
        }
    }

    /**
     * A well-known symbol.  Replaced by a placeholder in
     * serialized data.
     */

    private static class WellKnownAtomicSymbol extends WellKnownBasisSymbol implements AtomicSymbol {
        WellKnownAtomicSymbol(WellKnownAlphabet alpha, AtomicSymbol symbol) {
            super(alpha, symbol);
        }
        
        public Alphabet getMatches() {
            return new SingletonAlphabet(this);
        }
        
        private Object writeReplace() {
            return new OPH(alpha.getName(), getName());
        }
    }
     
    private static class WellKnownSubAlphabet extends Unchangeable implements FiniteAlphabet {
        private FiniteAlphabet alpha;
        private FiniteAlphabet superAlpha;
        
        public WellKnownSubAlphabet(FiniteAlphabet alpha, FiniteAlphabet superAlpha) {
            this.alpha = alpha;
            this.superAlpha = superAlpha;
        }
        
        public boolean contains(Symbol s) {
            if (s instanceof AtomicSymbol) {
                return alpha.contains(s);
            } else {
                for (Iterator i = ((FiniteAlphabet) s.getMatches()).iterator(); i.hasNext(); ) {
                    if (!alpha.contains((Symbol) i.next())) {
                        return false;
                    }
                }
                return true;
            }
        }
        
        public List getAlphabets() {
            return alpha.getAlphabets();
        }
        
        public Symbol getAmbiguity(Set s) 
            throws IllegalSymbolException
        {
            for (Iterator i = s.iterator(); i.hasNext(); ) {
                validate((Symbol) i.next());
            }
            return superAlpha.getAmbiguity(s);
        }
        
        public Symbol getGapSymbol() {
            return superAlpha.getGapSymbol();
        }
        
        public String getName() {
            return alpha.getName();
        }
        
        public Symbol getSymbol(List l) 
            throws IllegalSymbolException
        {
            return alpha.getSymbol(l);
        }
        
        public SymbolTokenization getTokenization(String name)
            throws BioException
        {
            return superAlpha.getTokenization(name);
        }
        
        public void validate(Symbol s)
            throws IllegalSymbolException
        {
            if (s instanceof AtomicSymbol) {
                alpha.validate(s);
            } else {
                for (Iterator i = ((FiniteAlphabet) s.getMatches()).iterator(); i.hasNext(); ) {
                    alpha.validate((Symbol) i.next());
                }
            }
        }
        
        public void addSymbol(Symbol s)
            throws ChangeVetoException
        {
            throw new ChangeVetoException("Can't add symbols to WellKnownSubAlphabets");
        }
        
        public void removeSymbol(Symbol s)
            throws ChangeVetoException
        {
            throw new ChangeVetoException("Can't remove symbols from WellKnownSubAlphabets");
        }
        
        public Iterator iterator() {
            return  alpha.iterator();
        }
        
        public int size() {
            return alpha.size();
        }
        
        public SymbolList symbols() {
            return alpha.symbols();
        }
        
        public Annotation getAnnotation() {
            return alpha.getAnnotation();
        }
    }
     
    private static class WellKnownBasisSymbol extends Unchangeable implements BasisSymbol, Serializable {
        protected WellKnownAlphabet alpha;
        protected BasisSymbol symbol;
        
        WellKnownBasisSymbol(WellKnownAlphabet alpha, BasisSymbol symbol) {
            super();
            symbol.addChangeListener(ChangeListener.ALWAYS_VETO, ChangeType.UNKNOWN); // Immutable
            this.alpha = alpha;
            this.symbol = symbol;
        }
        
        Symbol getSymbol() {
            return symbol;
        }
        
        public int hashCode() {
            return symbol.hashCode();
        }
        
        public boolean equals(Object o) {
            if (o instanceof WellKnownBasisSymbol) {
                return symbol.equals(((WellKnownBasisSymbol) o).getSymbol());
            } else {
                return false;
            }
        }
        
        public String getName() {
            return symbol.getName();
        }
        
        public Alphabet getMatches() {
            return new WellKnownSubAlphabet((FiniteAlphabet) symbol.getMatches(), alpha);
        }
        
        public List getSymbols() {
            return symbol.getSymbols();
        }
        
        public Annotation getAnnotation() {
            return symbol.getAnnotation();
        }
        
        private Object writeReplace() {
            return new OPH(alpha.getName(), getName());
        }

        
        protected static class OPH implements Serializable {
            private String alphaName;
            private String name;
            
            public OPH(String alphaName, String name) {
                this.alphaName = alphaName;
                this.name = name;
            }
            
            private Object readResolve() throws ObjectStreamException {
                try {
                    Alphabet alpha = alphabetForName(alphaName);
                    return alpha.getTokenization("name").parseToken(name);
                } catch (NoSuchElementException ex) {
                    throw new InvalidObjectException(
                        "Couldn't resolve symbol " + alphaName + ":" + name
                    );
                } catch (BioException ise) {
                    throw new InvalidObjectException(
                        "Couldn't resolve symbol " + name + ": " + ise.getMessage()
                    );
                }
            }
        }
    }

  /**
   * <p>
   * The class representing the Gap symbol.
   * </p>
   *
   * <p>
   * The gap is quite special. It is an ambiguity symbol with an empty alphabet.
   * This means that it notionaly represents an unfilled slot in a sequence.
   * It should be a singleton, hence the
   * placement in AlphabetManager and also the method normalize.
   * </p>
   *
   * @author Matthew Pocock
   */
  private static class GapSymbol
    extends
      Unchangeable
    implements
      Symbol,
      Serializable
  {
      public GapSymbol() {
      }

      public String getName() {
          return "gap";
      }

      public char getToken() {
          return '-';
      }

      public Annotation getAnnotation() {
          return Annotation.EMPTY_ANNOTATION;
      }

      public Alphabet getMatches() {
          return Alphabet.EMPTY_ALPHABET;
      }
  }

  /**
   * Get an indexer for a specified alphabet.
   *
   * @param alpha The alphabet to index
   * @return an AlphabetIndex instance
   * @since 1.1
   */

  /**
   * Get an indexer for a specified alphabet.
   *
   * @param alpha The alphabet to index
   * @return an AlphabetIndex instance
   * @since 1.1
   */
  public static AlphabetIndex getAlphabetIndex(
    FiniteAlphabet alpha
  ) {
    final int generateIndexSize = 160;
    AlphabetIndex ai = (AlphabetIndex) alphabetToIndex.get(alpha);
    if(ai == null) {
      int size = alpha.size();
      if(size <= generateIndexSize) {
        ai = new LinearAlphabetIndex(alpha);
      } else {
        ai = new HashedAlphabetIndex(alpha);
      }
      alphabetToIndex.put(alpha, ai);
    }
    return ai;
  }

  /**
   * Get an indexer for an array of symbols.
   *
   * @param syms the Symbols to index in that order
   * @return an AlphabetIndex instance
   * @since 1.1
   */
  public static AlphabetIndex getAlphabetIndex (
    Symbol[] syms
  ) throws IllegalSymbolException, BioException {
    return new LinearAlphabetIndex(syms);
  }

  private static final class SizeQueen extends AbstractList {
    private final List alphas;

    public SizeQueen(List alphas) {
      this.alphas = alphas;
    }

    public int size() {
      return alphas.size();
    }

    public Object get(int pos) {
      Alphabet a = (Alphabet) alphas.get(pos);
      List al = a.getAlphabets();
      int size = al.size();
      if(size > 1) {
        return new SizeQueen(al);
      } else {
        return new Integer(size);
      }
    }
  }
}
