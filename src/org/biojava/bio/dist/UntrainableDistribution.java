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

package org.biojava.bio.dist;

import java.util.*;
import java.io.*;

import org.biojava.utils.*;
import org.biojava.bio.*;
import org.biojava.bio.symbol.*;

/**
 * A distribution which does not interact with the training framework.
 * This class behaves in exactly the same manner as SimpleDistribution,
 * except that it has a no-op <code>registerWithTrainer</code> method.
 * It is useful for building Markov models where you wish to train only
 * a subset of the Distributions.
 *
 * @author Thomas Down
 * @since 1.3
 */
 
public class UntrainableDistribution extends SimpleDistribution {
    /**
     * Construct a new untrainable distribution over the specified alphabet
     */
    
    public UntrainableDistribution(FiniteAlphabet alpha) {
        super(alpha);
    }
    
    /**
     * Dummy implementation which registers an IgnoreCountsTrainer.
     */
    
    public void registerWithTrainer(DistributionTrainerContext dtc) {
        dtc.registerTrainer(this, IgnoreCountsTrainer.getInstance());
    }
}
