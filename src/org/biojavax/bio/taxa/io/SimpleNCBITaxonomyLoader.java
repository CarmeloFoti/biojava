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

package org.biojavax.bio.taxa.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import org.biojava.bio.seq.io.ParseException;
import org.biojava.utils.ChangeVetoException;
import org.biojavax.RichObjectFactory;
import org.biojavax.bio.taxa.NCBITaxon;
import org.biojavax.bio.taxa.SimpleNCBITaxon;

/**
 * Loads NCBI taxon information from names.dmp and nodes.dmp, which are
 * two of the files in the archive downloadable at ftp://ftp.ncbi.nih.gov/pub/taxonomy/ .
 * This simple implementation makes no attempt to process deletions
 * or merges - it merely creates instances as it goes along, reusing
 * any that may already exist.
 *
 * @author Richard Holland
 */
public class SimpleNCBITaxonomyLoader implements NCBITaxonomyLoader {
    
    private BufferedReader nodes;
    private BufferedReader names;
    private boolean parsed;
    
    /**
     * Constructs a new parser, based around two files.
     * @param nodes the nodes.dmp file
     * @param names the names.dmp file
     * @throws IllegalArgumentException if either parameter is null
     */
    public SimpleNCBITaxonomyLoader(BufferedReader nodes, BufferedReader names) {
        if (nodes==null) throw new IllegalArgumentException("Nodes file cannot be null");
        if (names==null) throw new IllegalArgumentException("Names file cannot be null");
        this.nodes = nodes;
        this.names = names;
    }
    
    /**
     * {@inheritDoc}
     */
    public void parseTaxonomyFile() throws IOException, ParseException {
        if (this.parsed) return;
        else {
            String line;
            // parse nodes first
            while ((line=nodes.readLine())!=null) {
                /* separated by '\t|\t'
        tax_id					-- node id in GenBank taxonomy database
        parent tax_id				-- parent node id in GenBank taxonomy database
        rank					-- rank of this node (superkingdom, kingdom, ...)
        embl code				-- locus-name prefix; not unique
        division id				-- see division.dmp file
        inherited div flag  (1 or 0)		-- 1 if node inherits division from parent
        genetic code id				-- see gencode.dmp file
        inherited GC  flag  (1 or 0)		-- 1 if node inherits genetic code from parent
        mitochondrial genetic code id		-- see gencode.dmp file
        inherited MGC flag  (1 or 0)		-- 1 if node inherits mitochondrial gencode from parent
        GenBank hidden flag (1 or 0)            -- 1 if name is suppressed in GenBank entry lineage
        hidden subtree root flag (1 or 0)       -- 1 if this subtree has no sequence data yet
        comments				-- free-text comments and citations
                 */
                String[] parts = line.split("\\|");
                Integer tax_id = Integer.valueOf(parts[0].trim());
                String pti = parts[1].trim();
                Integer parent_tax_id = pti.length()>0?new Integer(pti):null;
                String rank = parts[2].trim();
                Integer genetic_code = new Integer(parts[6].trim());
                Integer mito_code = new Integer(parts[8].trim());
                // by getting it from the factory, it auto-creates. If the user is using the
                // HibernateRichObjectFactory, then it even auto-persists. Magic!
                NCBITaxon t = (NCBITaxon)RichObjectFactory.getObject(SimpleNCBITaxon.class,new Object[]{tax_id});
                try {
                    t.setParentNCBITaxID(parent_tax_id);
                    t.setNodeRank(rank);
                    t.setGeneticCode(genetic_code);
                    t.setMitoGeneticCode(mito_code);
                    // clear out the existing names (to be repopulated from file later)
                    for (Iterator i = t.getNameClasses().iterator(); i.hasNext(); ) t.getNames((String)i.next()).clear();
                } catch (ChangeVetoException e) {
                    throw new ParseException(e);
                }
            }
            // now parse names
            while ((line=names.readLine())!=null) {
                /* separated by '\t|\t'
        tax_id					-- the id of node associated with this name
        name_txt				-- name itself
        unique name				-- the unique variant of this name if name not unique
        name class				-- (synonym, common name, ...)
                 */
                String[] parts = line.split("\\|");
                Integer tax_id = Integer.valueOf(parts[0].trim());
                String name = parts[1].trim();
                String nameClass = parts[3].trim();
                // look up the taxon from the factory
                NCBITaxon t = (NCBITaxon)RichObjectFactory.getObject(SimpleNCBITaxon.class,new Object[]{tax_id});
                // add the name
                try {
                    t.addName(nameClass,name);
                } catch (ChangeVetoException e) {
                    throw new ParseException(e);
                }
            }
            // all done!
            this.parsed = true;
        }
    }
}