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

package org.biojavax;

import org.biojavax.bio.seq.RichLocation;

/**
 * Represents a documentary reference. Relates to the bioentryreference table 
 * in BioSQL.
 * @author Richard Holland
 * @see DocRef
 * @since 1.5
 */
public interface RankedDocRef extends Comparable {
    
    /**
     * Represents a reference to a document. This value is intended to be set by 
     * the constructor of the implementing class.
     * @return the document reference.
     */
    public DocRef getDocumentReference();
    
    /**
     * The start position in the sequence that this reference is referred to from.
     * This value is intended to be set by the constructor of the implementing
     * class. The position returned is from 1 to the length of the sequence.
     * @return the start position.
     */
    public Integer getStart();
    
    /**
     * The end position in the sequence that this reference is referred to from.
     * This value is intended to be set by the constructor of the implementing
     * class. The position returned is from 1 to the length of the sequence.
     * @return the end position.
     */
    public Integer getEnd();
    
    /**
     * If this object was constructed using a location instead of two integers,
     * then this method will return that location. The getStart() and getEnd()
     * methods will then return the min and max of that location, using the
     * default location position resolver to resolve them to exact positions.
     * If this object was constructed using two integers, then this method will
     * return a simple location whose min and max correspond to those integers.
     * @return the location of this reference on the sequence. 
     */
    public RichLocation getLocation();
    
    /**
     * The rank of this reference. This value is intended to be set by the constructor
     * of the implementing class.
     * @return the rank.
     */
    public int getRank();
    
    /**
     * Sets the rank of this reference.
     * @param rank the rank.
     */
    public void setRank(int rank);
}


