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

package org.biojava.bio.program.ssbind;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;

import org.biojava.bio.program.sax.FastaSearchSAXParser;
import org.biojava.bio.search.SeqSimilaritySearchHit;
import org.biojava.bio.search.SeqSimilaritySearchResult;
import org.biojava.bio.seq.StrandedFeature;

/**
 * <code>SSBindFasta3_3t08Test</code> tests object bindings for
 * Blast-like SAX events.
 *
 * @author <a href="mailto:kdj@sanger.ac.uk">Keith James</a>
 * @since 1.2
 */
public class SSBindFasta3_3t08Test extends SSBindCase
{
    public SSBindFasta3_3t08Test(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        setTopHitValues(3266.4d, "CISY_ECOLI",
                        1, 427, StrandedFeature.POSITIVE,
                        1, 427, StrandedFeature.POSITIVE);

        setBotHitValues(2032.3d, "CISY_RICCN",
                        6, 422, StrandedFeature.POSITIVE,
                        12, 430, StrandedFeature.POSITIVE);

        String fastaOutputFileName = "fasta_3.3t08.out.gz";

        URL fastaOutputURL = SSBindFasta3_3t08Test.class
            .getResource(fastaOutputFileName);
        File fastaOutputFile = new File(fastaOutputURL.getFile());

        searchStream = new GZIPInputStream(new
            FileInputStream(fastaOutputFile));

        // XMLReader -> (SAX events) -> adapter -> builder -> objects
        XMLReader reader = (XMLReader) new FastaSearchSAXParser();

        reader.setContentHandler(adapter);
        reader.parse(new InputSource(searchStream));
    }

    public void testResultHitCount()
    {
        SeqSimilaritySearchResult result =
            (SeqSimilaritySearchResult) searchResults.get(0);

        assertEquals(20, result.getHits().size());
    }
}
