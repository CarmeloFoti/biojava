package org.biojava.bio.structure.align.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;


import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.StructureTools;
import org.biojava.bio.structure.align.webstart.PersistentConfig;
import org.biojava.bio.structure.align.util.UserConfiguration;
import org.biojava.bio.structure.align.webstart.WebStartMain;
import org.biojava.bio.structure.io.MMCIFFileReader;
import org.biojava.bio.structure.io.PDBFileReader;
import org.biojava.bio.structure.io.StructureIOFile;
import org.biojava.bio.structure.gui.util.PDBUploadPanel;
import org.biojava.bio.structure.gui.util.StructurePairSelector;


public class SelectPDBPanel 
extends JPanel 
implements StructurePairSelector{

	/** the system property PDB_DIR can be used to configure the 
	 * default location for PDB files.
	 */
	public static final String PDB_DIR = "PDB_DIR";

	boolean debug = true;

	JTextField pdbDir;
	JTextField f1;
	JTextField f2;
	JTextField c1;
	JTextField c2;
	JTextField r1;
	JTextField r2;

	JCheckBox pdbSplit;
	JCheckBox fromFtp;

	UserConfiguration config;

	JComboBox fileType;
	JTabbedPane configPane;

	/**
	 * 
	 */
	private static final long serialVersionUID = 4002475313717172193L;



	public SelectPDBPanel(){
		this(true);
	}
	public SelectPDBPanel(boolean show2PDBs) {	


		fileType = PDBUploadPanel.getFileFormatSelect();

		Box vBox = Box.createVerticalBox();

		JLabel help = new JLabel("Optional: specify chain ID or range.");
		Box hBox1 = Box.createHorizontalBox();
		hBox1.add(Box.createGlue());
		hBox1.add(help);
		vBox.add(hBox1);


		//pdbDir = new JTextField(20);

		int pdbfSize = 4;

		f1 = new JTextField(pdbfSize);
		c1 = new JTextField(1);
		r1 = new JTextField(5);
		Box p1 = getPDBFilePanel(1,f1,c1,r1);
		vBox.add(p1);

		f2 = new JTextField(pdbfSize);
		c2 = new JTextField(1);
		r2 = new JTextField(5);
		Box p2 = getPDBFilePanel(2, f2,c2,r2);

		if ( show2PDBs)
			vBox.add(p2);	




		//vBox.setBorder(BorderFactory.createLineBorder(Color.black));
		this.add(vBox);
	}

	public String getName1() {
		
		String chainId = c1.getText().trim();
		
		String name = f1.getText().trim(); 
		
		if ( ! chainId.equals("") ){
			name += "." + chainId;
		}
		return  name;
	}
	public String getName2() {
		String chainId = c2.getText().trim();
		
		String name = f2.getText().trim();
		
		if ( ! chainId.equals("") ){
			name += "." + chainId;
		}
		return  name;
	}
	public Structure getStructure1() throws StructureException{
		return fromPDB(f1,c1,r1);
	}

	public Structure getStructure2() throws StructureException{    
		return fromPDB(f2,c2,r2);
	}


	public JTextField getPDBDirField(){
		return pdbDir;
	}
	public void setPDBDirField(JTextField dir){
	   pdbDir = dir;
	}
	
	
	
	public JCheckBox getFromFtp()
   {
      return fromFtp;
   }
   public void setFromFtp(JCheckBox fromFtp)
   {
      System.out.println(fromFtp);
      this.fromFtp = fromFtp;
   }
   public JCheckBox getPdbSplit()
   {
      return pdbSplit;
   }
   public void setPdbSplit(JCheckBox pdbSplit)
   {
      this.pdbSplit = pdbSplit;
   }
   public void persistCurrentConfig(){

		config = getConfiguration();

		try {
			PersistentConfig webstartConfig = new PersistentConfig();

			webstartConfig.save(config);

		} catch (Exception e){
			e.printStackTrace();
		}

	}
	
	public UserConfiguration getConfiguration(){
		if ( config == null){
			config = WebStartMain.getWebStartConfig();
		}

		String dir = pdbDir.getText();
		config.setPdbFilePath(dir);
		boolean isSplit = pdbSplit.isSelected();
		config.setSplit(isSplit);
		boolean fromFtpF = fromFtp.isSelected();
		config.setAutoFetch(fromFtpF);
		return config;
	}

	private Structure fromPDB(JTextField f, JTextField c,JTextField r) throws StructureException{
		String pdb = f.getText().trim();

		if ( pdb.length() < 4) {
			f.setText("!!!");
			return null;
		}

		String chain = c.getText().trim();
		if ( debug )
			System.out.println("file :" + pdb + " " +  chain);


		String range = r.getText().trim();

		String fileFormat = (String)fileType.getSelectedItem();

		StructureIOFile reader = null;
		if ( fileFormat.equals(PDBUploadPanel.PDB_FORMAT)){
			PDBFileReader re = new PDBFileReader();
			re.setAlignSeqRes(false);
			reader = re;
		} else if ( fileFormat.equals(PDBUploadPanel.MMCIF_FORMAT)){						
			reader = new MMCIFFileReader();
		} else {
			throw new StructureException("Unkown file format " + fileFormat);
		}

		String dir = pdbDir.getText();
		reader.setPath(dir);
		boolean isSplit = pdbSplit.isSelected();
		reader.setPdbDirectorySplit(isSplit);
		boolean fromFtpF = fromFtp.isSelected();
		reader.setAutoFetch(fromFtpF);
		
		if ( debug )
			System.out.println("dir: " + dir);

		Structure structure ;
		try {
			structure = reader.getStructureById(pdb);
		} catch (IOException e) {
			throw new StructureException("Could not read structure " + pdb ,e);
		}


		//System.out.println(" got range: " + range);
		if ( range != null && ( ! range.equals(""))){
			Structure s = StructureTools.getSubRanges(structure, range);
			//System.out.println("got atoms: " + StructureTools.getAtomCAArray(s).length);
			return s;
		} 
		Structure s = StructureTools.getReducedStructure(structure,chain);
		//System.out.println("got atoms: " + StructureTools.getAtomCAArray(s).length);
		return s;

	}

	private Box getPDBFilePanel(int pos ,JTextField f, JTextField c, JTextField r){

		//JPanel panel = new JPanel();
		//panel.setBorder(BorderFactory.createLineBorder(Color.black));

		JLabel l01 = new JLabel("PDB code ");

		//panel.add(l01);
		Box hBox = Box.createHorizontalBox();
		hBox.add(Box.createGlue());
		hBox.add(l01);

		JLabel l11 = new JLabel(pos + ":");
		f.setMaximumSize(new Dimension(Short.MAX_VALUE,30));
		f.setToolTipText("Provide 4-character PDB code here. Example: 4hhb");
		hBox.add(l11);
		hBox.add(Box.createVerticalGlue());
		hBox.add(f, BorderLayout.CENTER);
		hBox.add(Box.createGlue());

		//panel.add(hBox11);

		//Box hBox21 = Box.createHorizontalBox();
		JLabel l21 = new JLabel("Chain" + pos + ":");
		hBox.add(l21);

		c.setMaximumSize(new Dimension(Short.MAX_VALUE,30));
		//hBox.add(Box.createGlue());
		hBox.add(c, BorderLayout.CENTER);

		String msg1 = "Both chainID and range specification are optional. If both are provided, range has preference.";
		l21.setToolTipText(msg1);
		c.setToolTipText(msg1);

		JLabel rangeL = new JLabel(" Range " + pos + ":");
		hBox.add(Box.createGlue());
		hBox.add(rangeL);
		r.setMaximumSize(new Dimension(Short.MAX_VALUE,30));

		// set help text:
		String msg ="Syntax example: A:407-495,A:582-686";
		rangeL.setToolTipText(msg);
		r.setToolTipText(msg);

		//hBox.add(Box.createGlue());
		hBox.add(r,BorderLayout.CENTER);

		//hBox21.add(Box.createGlue());

		//panel.add(hBox21);



		return hBox;
	}



	public  JPanel getPDBDirPanel(JTextField f){

		if ( config == null)
			config = WebStartMain.getWebStartConfig();

		JPanel panel = new JPanel();
		//panel.setBorder(BorderFactory.createLineBorder(Color.black));

		Box vBox = Box.createVerticalBox();
		Box hBox = Box.createHorizontalBox();

		JLabel l01 = new JLabel("Directory containing PDBs");
		//panel.add(l01);
		//panel.add(f);

		hBox.add(l01);
		hBox.add(f);
		vBox.add(hBox);

		if ( config != null) {
			pdbDir.setText( config.getPdbFilePath() );
		}

		Action action = new ChooseDirAction(pdbDir, config);

		JButton chooser = new JButton(action);
		hBox.add(chooser);

		Box hBox2 = Box.createHorizontalBox();

		JLabel label = new JLabel("Files in split directories:");

		pdbSplit = new JCheckBox();
		pdbSplit.setMnemonic(KeyEvent.VK_S);

		pdbSplit.setSelected(true);
		if ( config != null){
			pdbSplit.setSelected(config.isSplit());
		}

		hBox2.add(Box.createGlue());
		hBox2.add(label);
		hBox2.add(pdbSplit);

		vBox.add(hBox2);

		Box hBox3 = Box.createHorizontalBox();
		JLabel label2 = new JLabel("Fetch missing PDBs from ftp site:");
		fromFtp = new JCheckBox();
		fromFtp.setMnemonic(KeyEvent.VK_F);
		fromFtp.setSelected(true);
		if ( config != null)
			fromFtp.setSelected(config.getAutoFetch());




		JLabel ftype = new JLabel("File format:");

		hBox3.add(Box.createGlue());
		hBox3.add(ftype);
		hBox3.add(fileType);
		hBox3.add(Box.createGlue());
		hBox3.add(label2);
		hBox3.add(fromFtp);

		vBox.add(hBox3);

		panel.add(vBox);
		return panel;
	}


}
