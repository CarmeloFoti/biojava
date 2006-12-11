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
 * Created on 05.03.2004
 * @author Andreas Prlic
 *
 */
package org.biojava.bio.structure;
import java.util.HashMap;
import java.util.Map;

import org.biojava.bio.structure.io.PDBParseException;

/**
 *
 *  AminoAcid inherits most from Hetatom.  Adds a few AminoAcid
 *  specific methods.
 * @author Andreas Prlic
 * @since 1.4
 * @version %I% %G%
 * 
 */
public class   AminoAcidImpl 
extends    HetatomImpl
implements AminoAcid 
{
	/** this is an Amino acid. type is "amino". */
	public static final String type = "amino";

	/* IUPAC amino acid residue names 
	 */
	Character amino_char ;

	HashMap   secstruc   ;


	/*
	 * inherits most from Hetero and has just a few extensions.
	 */
	public AminoAcidImpl() {
		super();

		amino_char = null;
		secstruc = new HashMap();
	}


	public String getType(){ return type;}

	/** set the secondary structure data for this amino acid. the data
	 * is a Map with the following indeces (@see Secstruc)
	 *
	 * @param secstr  a Map object specifying the sec struc value
	 * @see #getSecStruc
	 */
	public void setSecStruc(Map secstr) {
		secstruc = (HashMap) secstr ;
	}

	/** get secondary structure data .
	 *
	 * @return a Map object representing the sec struc value
	 *
	 * @see #setSecStruc
	 */
	public Map getSecStruc(){
		return secstruc ;
	}

	/** get N atom.
	 *
	 * @return an Atom object
	 * @throws StructureException ...
	 */
	public Atom getN()  throws StructureException {return getAtom("N");  }

	/** get CA atom.
	 * @return an Atom object
	 * @throws StructureException ...
	 */
	public Atom getCA() throws StructureException {return getAtom("CA"); }

	/** get C atom.
	 * @return an Atom object
	 * @throws StructureException ...
	 */
	public Atom getC()  throws StructureException {return getAtom("C");  }

	/** get O atom.
	 * @return an Atom object
	 * @throws StructureException ...
	 */
	public Atom getO()  throws StructureException {return getAtom("O");  }

	/** get CB atom.
	 * @return an Atom object
	 * @throws StructureException ...
	 */
	public Atom getCB() throws StructureException {return getAtom("CB"); }




	/** returns the name of the AA, in single letter code.
	 *
	 * @return a Character object representing the amino type value
	 * @see #setAminoType
	 */
	public  Character getAminoType() {
		return amino_char;
	}

	/** set the name of the AA, in single letter code .
	 *
	 * @param aa  a Character object specifying the amino type value
	 * @see #getAminoType
	 */
	public void setAminoType(Character aa){
		amino_char  = aa ;
	}

	/** string representation. */
	public String toString(){

		String str = "PDB: "+ pdb_name + " " + amino_char + " " + pdb_code +  " "+ pdb_flag;
		if (pdb_flag) {
			str = str + "atoms: "+atoms.size();
		}
		return str ;

	}
	/** set three character name of AminoAcid. 
	 *
	 * @param s  a String specifying the PDBName value
	 * @see #getPDBName()
	 * @throws PDBParseException ...
	 */
	public void setPDBName(String s) 
	throws PDBParseException
	{
		if (s.length() != 3) {
			throw new PDBParseException("amino acid name is not of length 3! (" + s +")");
		}
		pdb_name =s ;
	}


	/** returns and identical copy of this Group object .
	 * @return  and identical copy of this Group object 
	 */
	public Object clone(){
		AminoAcidImpl n = new AminoAcidImpl();
		n.setPDBFlag(has3D());
		n.setPDBCode(getPDBCode());
		try {
			n.setPDBName(getPDBName());
		} catch (PDBParseException e) {
			e.printStackTrace();
		}
		n.setAminoType(getAminoType());

		// copy the atoms
		for (int i=0;i<atoms.size();i++){
			Atom atom = (Atom)atoms.get(i);
			n.addAtom((Atom)atom.clone());
		}
		return n;
	}


}
