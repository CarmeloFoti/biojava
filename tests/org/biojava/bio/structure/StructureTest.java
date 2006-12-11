package org.biojava.bio.structure;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.biojava.bio.structure.io.PDBFileParser;
import org.biojava.bio.structure.jama.Matrix;


/**
 *
 * @author Andreas Prlic
 * @since 1.5
 */


public class StructureTest extends TestCase {

    Structure structure;

    protected void setUp()
    {
	InputStream inStream = this.getClass().getResourceAsStream("/files/5pti.pdb");
        assertNotNull(inStream);


	PDBFileParser pdbpars = new PDBFileParser();
	try {
	    structure = pdbpars.parsePDBFile(inStream) ;
	} catch (IOException e) {
	    e.printStackTrace();
	}

	assertNotNull(structure);
		
	assertEquals("structure does not contain one chain ", 1 ,structure.size());	
    }


    /** test if a PDB file can be parsed */
    public void testReadPDBFile() throws Exception {

	assertEquals("pdb code not set!","5PTI",structure.getPDBCode());


	Chain c = structure.getChain(0);
	assertEquals("did not find the expected 58 amino acids!",58,c.getGroups("amino").size());

	assertTrue(c.getGroups("hetatm").size()     == 65);
	assertTrue(c.getGroups("nucelotide").size() == 0 );
    }

    



    /** Tests that standard amino acids are working properly */
    public void testStandardAmino() throws Exception {

	AminoAcid arg = StandardAminoAcid.getAminoAcid("ARG");
	assertTrue(arg.size() == 11 );
	
	AminoAcid gly = StandardAminoAcid.getAminoAcid("G");
	assertTrue(gly.size() == 4);

    }



    public void testMutation() throws Exception {

	Group g1 = (Group)structure.getChain(0).getGroup(21).clone();
	assertTrue(g1 != null);

	Group g2 = (Group)structure.getChain(0).getGroup(53).clone();
	assertTrue(g2 != null);

	if ( g1.getPDBName().equals("GLY")){
	    if ( g1 instanceof AminoAcid){ 
		Atom cb = Calc.createVirtualCBAtom((AminoAcid)g1);
		g1.addAtom(cb);
	    }
	}
	
	if ( g2.getPDBName().equals("GLY")){
	    if ( g2 instanceof AminoAcid){ 
		Atom cb = Calc.createVirtualCBAtom((AminoAcid)g2);
		g2.addAtom(cb);
	    }
	}
	
            
	Atom[] atoms1 = new Atom[3];
	Atom[] atoms2 = new Atom[3];
	
	atoms1[0] = g1.getAtom("N");
	atoms1[1] = g1.getAtom("CA");
	atoms1[2] = g1.getAtom("CB");
	
            
	atoms2[0] = g2.getAtom("N");
	atoms2[1] = g2.getAtom("CA");
	atoms2[2] = g2.getAtom("CB");
	
                       
	SVDSuperimposer svds = new SVDSuperimposer(atoms1,atoms2);
	
	
	Matrix rotMatrix = svds.getRotation();
	Atom   tran      = svds.getTranslation();
	
	Group newGroup = (Group)g2.clone();
	                        
	Calc.rotate(newGroup,rotMatrix);
	
	Calc.shift(newGroup,tran);   
	



	Atom ca1    =       g1.getAtom("CA");	
	Atom oldca2 =       g2.getAtom("CA");    
	Atom newca2 = newGroup.getAtom("CA");

	// this also tests the cloning ...
	double olddistance = Calc.getDistance(ca1,oldca2);
	assertTrue( olddistance > 10 );

	// final test check that the distance between the CA atoms is small ;

	double newdistance = Calc.getDistance(ca1,newca2);	
	assertTrue( newdistance < 0.1);
	

    }



}
