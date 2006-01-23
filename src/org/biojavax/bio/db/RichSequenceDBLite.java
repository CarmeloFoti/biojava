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

package org.biojavax.bio.db;

import java.util.Set;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.db.IllegalIDException;
import org.biojava.bio.seq.db.SequenceDBLite;
import org.biojavax.bio.seq.RichSequence;

/**
 * A database of RichSequences. This may have several implementations with
 * rich behaviour, but basically most of the time you will just use
 * the interface methods to do stuff. A RichSequence database contains a
 * finite number of RichSequences stored under unique keys.
 *
 * @author Matthew Pocock
 * @author <A href="mailto:Gerald.Loeffler@vienna.at">Gerald Loeffler</A>
 * @author Thomas Down
 * @author Richard Holland
 */
public interface RichSequenceDBLite extends SequenceDBLite {
    /**
     * {@inheritDoc}
     * Will always return RichSequence instances.
     */
    public Sequence getSequence(String id) throws IllegalIDException, BioException;
    
    /**
     * Retrieve a single RichSequence by its id.
     *
     * @param id the id to retrieve by
     * @return  the Sequence with that id
     * @throws IllegalIDException if the database doesn't know about the id
     */
    public RichSequence getRichSequence(String id) throws BioException,IllegalIDException;
    
    /**
     * Retrieve multiple RichSequence by its id.
     *
     * @param id a set of ids to retrieve by
     * @return  the RichSequences with that id
     * @throws IllegalIDException if the database doesn't know about the id
     */
    public RichSequenceDB getRichSequences(Set ids) throws BioException,IllegalIDException;
    
    /**
     * Retrieve multiple RichSequence into a specific sequence database. If
     * that database is null, a new HashRichSequenceDB is used.
     *
     * @param id a set of ids to retrieve by
     * @param db a database to load the seqs into
     * @return  the RichSequences with that id
     * @throws IllegalIDException if the database doesn't know about the id
     */
    public RichSequenceDB getRichSequences(Set ids, RichSequenceDB db) throws BioException,IllegalIDException;
}
