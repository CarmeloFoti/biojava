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

package org.biojava.bio.program.phred;

import java.io.*;
import java.util.*;

import org.biojava.bio.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.dist.*;
import org.biojava.bio.symbol.*;
import org.biojava.bio.seq.db.*;
import org.biojava.bio.seq.io.*;
import org.biojava.bio.seq.impl.*;
import org.biojava.utils.*;

/**
 * Title:        PhredTools
 * Description:  Static methods for working with phred quality data
 * Copyright:    Copyright (c) 2001
 * Company:      AgResearch
 * @author Mark Schreiber
 * @since 1.1
 *
 * Note that Phred is a copyright of CodonCode Corporation
 */

public final class PhredTools {

   static{
     List l = new ArrayList(2);
     l.add(DNATools.getDNA());
     l.add(IntegerAlphabet.getSubAlphabet(0,99));
     SimpleAlphabet a = new SimpleAlphabet(
       new HashSet(
         ((FiniteAlphabet)AlphabetManager.getCrossProductAlphabet(l)).symbols().toList()
        ),"PHRED"
     );
     AlphabetManager.registerAlphabet("PHRED",a);
   }

  /**
   * Retrieves the PHRED alphabet from the AlphabetManager. The Phred alphabet
   * is a cross product of a subset of the IntegerAlphabet from 0...99 and the
   * DNA alphabet. The Phred alphabet is finite.
   *
   * The Phred Alphabet contains 400 BasisSymbols named, for example, (guanine 47).
   * The BasisSymbols can be fragmented into their component AtomicSymbols using
   * the <code>getSymbols()</code> method of BasisSymbol.
   */
  public static final FiniteAlphabet getPhredAlphabet(){
    return (FiniteAlphabet)AlphabetManager.alphabetForName("PHRED");
  }

  /**
   * Retrives the DNA symbol component of the Phred BasisSymbol from the
   * PHRED alphabet.
   * @throws IllegalSymbolException if the provided symbol is not from the
   * PHRED alphabet.
   */
  public static final AtomicSymbol dnaSymbolFromPhred(Symbol phredSym)
    throws IllegalSymbolException{
    //validate the symbol
    getPhredAlphabet().validate(phredSym);
    //get the DNA component of the Phred Symbol
    List l = ((BasisSymbol)phredSym).getSymbols();
    //the first symbol should be DNA
    return (AtomicSymbol)(l.get(0));
  }

  /**
   * Retrives the IntegerSymbol component of the Phred BasisSymbol from the
   * PHRED alphabet.
   * @throws IllegalSymbolException if the provided symbol is not from the
   * PHRED alphabet.
   */
  public static final IntegerAlphabet.IntegerSymbol integerSymbolFromPhred(Symbol phredSym)
    throws IllegalSymbolException{
    //validate the symbol
    getPhredAlphabet().validate(phredSym);
    //get the IntegerSymbol component of the Phred Symbol
    List l = ((BasisSymbol)phredSym).getSymbols();
    //the second symbol should be the IntegerSymbol
    return (IntegerAlphabet.IntegerSymbol)(l.get(1));
  }

  /**
   * Merges a Symbol List from the DNA alphabet with a SymbolList from the
   * [0..99] subset of the IntegerAlphabet into a SymbolList from
   * the PHRED alphabet.
   * @throws IllegalAlphabetException if the alphabets are not of the required alphabets
   * @throws IllegalArgumentException if the two SymbolLists are not of equal length.
   * @throws IllegalSymbolException if a combination of Symbols cannot be represented by
   * the PHRED alphabet.
   */
  public static SymbolList createPhred(SymbolList dna, SymbolList quality)
    throws IllegalArgumentException, IllegalAlphabetException, IllegalSymbolException{
    //perform initial checks
    if(dna.length() != quality.length()){
      throw new IllegalArgumentException("SymbolLists must be of equal length "+
        dna.length()+" : "+quality.length());
    }
    if(dna.getAlphabet() != DNATools.getDNA()){
      throw new IllegalAlphabetException(
        "Expecting SymbolList 'dna' to use the DNA alphabet, uses "
        +dna.getAlphabet().getName());
    }
    Alphabet subint = IntegerAlphabet.getSubAlphabet(0,99);
    if(quality.getAlphabet() != subint && quality.getAlphabet() != IntegerAlphabet.getInstance()){
      throw new IllegalAlphabetException(
        "Expecting SymbolList quality to use the "+subint.getName()+" alphabet"+
        "or IntegerAlphabet instead uses "+
        quality.getAlphabet().getName());
    }

    //build the symbollist
    SimpleSymbolList sl = new SimpleSymbolList(getPhredAlphabet());

    for(int i = 1; i <= dna.length(); i++){
      Symbol d = dna.symbolAt(i);
      Symbol q = quality.symbolAt(i);
      try{
        sl.addSymbol(getPhredSymbol(d,q));
      }catch(ChangeVetoException e){
        throw new NestedError(e);
      }
    }

    return sl;
  }

  /**
   * Creates a symbol from the PHRED alphabet by combining a Symbol from the
   * DNA alphabet and a Symbol from the IntegerAlphabet (or one of its subsets).
   * @throws IllegalSymbolException if there is no Symbol in the PHRED alphabet
   * that represents the two arguments.
   */
  public static final Symbol getPhredSymbol(Symbol dna, Symbol integer)
    throws IllegalSymbolException{
    try{
      SymbolTokenization toke = getPhredAlphabet().getTokenization("name");
      String word = "("+dna.getName()+" "+integer.getName()+")";
      return toke.parseToken(word);
    }catch(BioException e){
      throw new NestedError(e);
    }
  }

  /**
   * Writes Phred quality data in a Fasta type format.
   * @param db a bunch of PhredSequence objects
   * @param qual the OutputStream to write the quality data to.
   * @param seq the OutputStream to write the sequence data to.
   * @since 1.2
   */
   public static void writePhredQuality(OutputStream qual, OutputStream seq, SequenceDB db)
    throws IOException, BioException{
      StreamWriter qualw = new StreamWriter(qual,new PhredFormat());
      StreamWriter seqw = new StreamWriter(seq, new FastaFormat());
      SequenceDB qualDB = new HashSequenceDB(IDMaker.byName);
      //Get the quality SymbolLists and add them to a SeqDB
      for(SequenceIterator i = db.sequenceIterator(); i.hasNext();){
        Sequence p = i.nextSequence();
        if(p instanceof PhredSequence){
          PhredSequence ps = (PhredSequence)p;
          SymbolList ql = ps.getQuality();
          try{
            qualDB.addSequence( new SimpleSequence(ql,p.getURN(),p.getName(),p.getAnnotation()));
          }catch(ChangeVetoException cve){
            throw new NestedError(cve,"Cannot Add Quality Sequences to Database");
          }
        }
        else{
          throw new BioException("Expecting PhredSequence, got " + p.getClass().getName());
        }
      }
      qualw.writeStream(qualDB.sequenceIterator());
      seqw.writeStream(db.sequenceIterator());//this works as sequence methods act on the underlying SimpleSequence
   }

  /**
   * Constructs a StreamReader to read in Phred quality data in FASTA format.
   * The data is converted into sequences consisting of Symbols from the IntegerAlphabet.
   */
  public static StreamReader readPhredQuality(BufferedReader br){
    return new StreamReader(br,
      new PhredFormat(),
      getQualityParser(),
      getFastaBuilderFactory());
  }



  /**
   * Calls SeqIOTools.readFastaDNA(br), added here for convinience.
   */
  public static StreamReader readPhredSequence(BufferedReader br){
    return (StreamReader)SeqIOTools.readFastaDNA(br);
  }


  private static SequenceBuilderFactory _fastaBuilderFactory;

    /**
     * Get a default SequenceBuilderFactory for handling FASTA
     * files.
     */
  private static SequenceBuilderFactory getFastaBuilderFactory() {
      if (_fastaBuilderFactory == null) {
          _fastaBuilderFactory = new FastaDescriptionLineParser.Factory(SimpleSequenceBuilder.FACTORY);
      }
      return _fastaBuilderFactory;
  }

  /**
   * returns the IntegerAlphabet parser
   */
  private static SymbolTokenization getQualityParser() {
    return IntegerAlphabet.getInstance().getTokenization("token");
  }

  /**
   * The quality value is related to the base call error probability
   * by the formula  QV = - 10 * log_10( P_e )
   * where P_e is the probability that the base call is an error.
   * @return a <code>double</code> value, note that for most Phred scores this will be rounded
   * to the nearest <code>int</code>
   */
   public static double qualityFromP(double probOfError){
     return (-10 * (Math.log(probOfError)/Math.log(10.0)));
   }

   /**
    * Calculates the probability of an error from the quality score via the formula
    *  P_e = 10**(QV/-10)
    */
    public static double pFromQuality(double quality){
      return Math.pow(10.0,(quality/-10.0));
    }

    /**
     * Calculates the probability of an error from the quality score via the formula
     *  P_e = 10**(QV/-10)
     */
    public static double pFromQuality(int quality){
      return pFromQuality((double)quality);
    }

    /**
     * Calculates the probability of an error from the quality score via the formula
     *  P_e = 10**(QV/-10)
     */
    public static double pFromQuality(IntegerAlphabet.IntegerSymbol quality){
      return pFromQuality(quality.intValue());
    }

    /**
     * Converts a Phred sequence to an array of distributions. Essentially a fuzzy sequence
     * Assumes that all of the non called bases are equiprobable
     */
    public static Distribution[] phredToDistArray(PhredSequence s){
      Distribution[] pos = new Distribution[s.length()];
      DistributionTrainerContext dtc = new SimpleDistributionTrainerContext();

      for (int i = 0; i < s.length(); i++) {// for each symbol in the phred sequence
        Symbol qual = s.getQualityAt(i);
        Symbol base = s.getDNAAt(i);
        double pBase = pFromQuality((IntegerAlphabet.IntegerSymbol)qual);
        double pOthers = (1.0 - pBase)/3;

        try{
          pos[i] = DistributionFactory.DEFAULT.createDistribution(DNATools.getDNA());
          dtc.registerDistribution(pos[i]);

          for(Iterator iter = (DNATools.getDNA().iterator()); iter.hasNext();){
            Symbol sym = (Symbol)iter.next();
            if(sym.equals(base)) pos[i].setWeight(sym,pBase);
            else pos[i].setWeight(sym,pOthers);
          }

          dtc.train();
        }catch(IllegalAlphabetException iae){
          throw new NestedError(iae,"Sequence "+s.getName()+" contains an illegal alphabet");
        }catch(ChangeVetoException cve){
          throw new NestedError(cve, "The Distribution has become locked");
        }catch(IllegalSymbolException ise){
          throw new NestedError(ise, "Sequence "+s.getName()+" contains an illegal symbol");
        }
      }
      return pos;
    }

    /**
     * converts an Alignment of PhredSequences to a Distribution[] where each position is the average
     * distribution of the underlying column of the alignment.
     * @throws ClassCastException if the sequences in the alignment are not instances of PhredSequence
     */
    public static Distribution[] phredAlignmentToDistArray(Alignment a){
      List labels = a.getLabels();
      int depth = labels.size();
      Distribution [] average = new Distribution[a.length()];

      Distribution[][] matrix = new Distribution[labels.size()][];
      for(int y = 0; y < a.length(); y++){// for eaxh position
        for(Iterator i = labels.iterator(); i.hasNext();){
          SymbolList sl = a.symbolListForLabel(i.next());
          matrix[y] = phredToDistArray((PhredSequence)sl);
        }
        average[y] = DistributionTools.average(matrix[y]);
      }

      return average;
    }
}
