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

package org.biojavax.bio;

/**
 *
 * @author Richard Holland
 */
public class SimpleBioEntryComment implements BioEntryComment {
    

    private String comment;
    private int rank;
    private BioEntry parent;
    
    public SimpleBioEntryComment(BioEntry parent, String comment, int rank) {
        this.parent = parent;
        this.comment = comment;
        this.rank = rank;
    }
    
    // Hibernate requirement - not for public use.
    private SimpleBioEntryComment() {}
    
    // Hibernate requirement - not for public use.
    private void setComment(String comment) { this.comment = comment; }

    public String getComment() {
        return this.comment;
    }
    
    // Hibernate requirement - not for public use.
    private void setRank(int rank) { this.rank = rank; }

    public int getRank() {
        return this.rank;
    }
    
    // Hibernate requirement - not for public use.
    private void setParent(BioEntry parent) { this.parent = parent; }

    public BioEntry getParent() {
        return this.parent;
    }
    
        public boolean equals(Object obj) {

        if (this == obj) return true;

        if (obj==null || !(obj instanceof BioEntryComment)) return false;

        else {

            BioEntryComment them = (BioEntryComment)obj;

            return (this.getParent().equals(them.getParent()) &&
                    this.getRank()==them.getRank() &&
                    this.getComment().equals(them.getComment()));

        }

    }

    

    /**

     * Compares this object with the specified object for order.  Returns a

     * negative integer, zero, or a positive integer as this object is less

     * than, equal to, or greater than the specified object.

     * @return a negative integer, zero, or a positive integer as this object

     * 		is less than, equal to, or greater than the specified object.

     * @param o the Object to be compared.

     */

    public int compareTo(Object o) {

        BioEntryComment them = (BioEntryComment)o;

        if (!this.getParent().equals(them.getParent())) return this.getParent().compareTo(them.getParent());

        if (this.getRank()!=them.getRank()) return this.getRank()-them.getRank();
        
        return this.getComment().compareTo(them.getComment());
    }

    

    /**

     * Returns a hash code value for the object. This method is

     * supported for the benefit of hashtables such as those provided by

     * <code>Hashtable</code>.

     * @return  a hash code value for this object.

     * @see     java.lang.Object#equals(java.lang.Object)

     * @see     java.util.Hashtable

     */

    public int hashCode() {

        int code = 17;

        code = 37*code + this.getParent().hashCode();

        code = 37*code + this.getComment().hashCode();
        
        code = 37*code + this.getRank();

        return code;

    }
    
    // Hibernate requirement - not for public use.
    private Long id;
    
    
    // Hibernate requirement - not for public use.
    private Long getId() {
        
        return this.id;
    }
    
    
    // Hibernate requirement - not for public use.
    private void setId(Long id) {
        
        this.id = id;
    }
}