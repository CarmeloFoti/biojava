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

package org.biojava.bio.program.homologene;

import java.util.Set;

/**
 * represents the Homologene Group.
 */
public interface HomoloGroup
{
    /**
     * retrieves name of this group.
     * Homologene itself does not assign names
     * or identifiers to groups.
     */
    public String getName();

    /**
     * set the name of this group.
     */
    public void setName(String name);

    /**
     * adds a specified Orthology relationship
     * to this group.
     */
    public void addOrthology(Orthology orthology);

    /**
     * removes a specified Orthology relationship
     * from this group.
     */
    public void removeOrthology(Orthology orthology);

    /**
     * no. of entries in this Homologene group
     */
    public int size();

    /**
     * get the taxa represented in this group
     */
    public Set getTaxa();
}

