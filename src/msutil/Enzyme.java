/***************************************************************************
  * Title:          
  * Author:         Sangtae Kim
  * Last modified:  
  *
  * Copyright (c) 2008-2009 The Regents of the University of California
  * All Rights Reserved
  * See file LICENSE for details.
  ***************************************************************************/
package msutil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import params.ParamObject;
import params.UserParam;

/**
 * This class represents an enzyme.
 * 
 * @author sangtaekim
 */
public class Enzyme implements ParamObject {
	
	/** True if the enzyme cleaves n-terminus, false otherwise. */
	private boolean isNTerm;
	
	/** Name of the enzyme. */
	private String name;
	
	/** Description */
	private String description;
	
	/** Amino acid residues cleaved by the enzyme. */
	private char[] residues;
	private boolean[] isResidueCleavable; 
	
	// the probability that a peptide generated by this enzyme follows the cleavage rule
	// E.g. for trypsin, probability that a peptide ends with K or R
	private float peptideCleavageEfficiency = 0;	
	
	// the probability that a neighboring amino acid follows the enzyme rule
	// E.g. for trypsin, probability that the preceding amino acid is K or R
	private float neighboringAACleavageEfficiency = 0;
	
	/**
	 * Instantiates a new enzyme.
	 * 
	 * @param name the name
	 * @param residues the residues cleaved by the enzyme (String)
	 * @param isNTerm N term or C term (true if it cleaves N-term)
	 */
	private Enzyme(String name, String residues, boolean isNTerm, String description) 
	{
		this.name = name;
		this.description = description;
		if(residues != null)
		{
			this.residues = new char[residues.length()];
			this.isResidueCleavable = new boolean[128];
			for(int i=0; i<residues.length(); i++)
			{
				char residue = residues.charAt(i);
				if(!Character.isUpperCase(residue))
				{
					System.err.println("Enzyme residues must be upper cases: " + residue);
					System.exit(-1);
				}
				this.residues[i] = residue;
				isResidueCleavable[residue] = true;
			}
		}
		this.isNTerm = isNTerm;
	}

	/**
	 * Sets the neighboring amino acid efficiency as the probability that a neighboring amino acid follows the enzyme rule
	 * @param neighboringAACleavageEfficiency neighboring amino acid effieicncy
	 * @return this object
	 */
	private void setNeighboringAAEfficiency(float neighboringAACleavageEfficiency)
	{
		this.neighboringAACleavageEfficiency = neighboringAACleavageEfficiency;
	}

	/**
	 * Gets the neighboring amino acid efficiency
	 * @return neighboring amino acid efficiency
	 */
	public float getNeighboringAACleavageEffiency() { return neighboringAACleavageEfficiency; }
	
	/**
	 * Sets the peptide cleavage efficiency as the probability that a peptide generated by this enzyme follows the cleavage rule
	 * @param peptideCleavageEfficiency peptide cleagave efficiency
	 * @return this object
	 */
	private void setPeptideCleavageEffiency(float peptideCleavageEfficiency)
	{
		this.peptideCleavageEfficiency = peptideCleavageEfficiency;
	}
	
	/**
	 * Gets the peptide efficiency.
	 * @return peptide efficiency
	 */
	public float getPeptideCleavageEfficiency()	{ return peptideCleavageEfficiency; }
	
	/**
	 * Returns the name of the enzyme.
	 * 
	 * @return the name of the enzyme.
	 */
	public String getName()		{ return name; }
	
	/**
	 * Returns the description of the enzyme.
	 * 
	 * @return the description of the enzyme.
	 */
	public String getDescription()	{ return description; }
	
	/**
	 * Returns the description of the enzyme when it is showed in the usage info.
	 * 
	 * @return the description of the enzyme when it is showed in the usage info.
	 */
	@Override
	public String getParamDescription()	{ return description; }
	
	/**
	 * Checks if this enzyme cleaves N term.
	 * 
	 * @return true, if it cleaves N term.
	 */
	public boolean isNTerm()	{ return isNTerm; }
	
	/**
	 * Checks if this enzyme cleaves C term.
	 * 
	 * @return true, if it cleaves C term.
	 */
	public boolean isCTerm()	{ return !isNTerm; }
	
	/**
	 * Checks if the amino acid is cleavable.
	 * 
	 * @param aa the amino acid
	 * 
	 * @return true, if aa is cleavable
	 */
	public boolean isCleavable(AminoAcid aa)
	{
		if(this.residues == null)
			return true;
		for(char r : this.residues)
			if(r == aa.getUnmodResidue())
				return true;
		return false;
	}

	/**
	 * Checks if the amino acid is cleavable.
	 * 
	 * @param residue amino acid residue
	 * 
	 * @return true, if residue is cleavable
	 */
	public boolean isCleavable(char residue)
	{
		if(isResidueCleavable == null)
			return true;
		return isResidueCleavable[residue];
	}
	
	/**
	 * Checks if the peptide is cleaved by the enzyme.
	 * @param p peptide
	 * @return true if p is cleaved, false otherwise.
	 */
	public boolean isCleaved(Peptide p)
	{
		AminoAcid aa;
		if(isNTerm)
			aa = p.get(0);
		else
			aa = p.get(p.size()-1);
		return isCleavable(aa.getResidue());
	}
	
	/**
	 * Returns the number of cleavaged termini
	 * @param annotation annotation (e.g. K.DLFGEK.I)
	 * @return the number of cleavaged termini
	 */
	public int getNumCleavedTermini(String annotation, AminoAcidSet aaSet)
	{
		int nCT = 0;
		String pepStr = annotation.substring(annotation.indexOf('.')+1, annotation.lastIndexOf('.'));
		Peptide peptide = aaSet.getPeptide(pepStr);
		if(this.isCleaved(peptide))
			nCT++;
		
		AminoAcid precedingAA = aaSet.getAminoAcid(annotation.charAt(0));
		AminoAcid nextAA = aaSet.getAminoAcid(annotation.charAt(annotation.length()-1));
		if(this.isNTerm)
		{
			if(nextAA == null || this.isCleavable(nextAA))
				nCT++;
		}
		else
		{
			if(precedingAA == null || this.isCleavable(precedingAA))
				nCT++;
		}
		
		return nCT;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	/**
	 * Gets the residues.
	 * 
	 * @return the residues
	 */
	public char[] getResidues()	{ return residues; }
	
	/** The Constant TRYPSIN. */
	public static final Enzyme NOENZYME;
	
	/** The Constant TRYPSIN. */
	public static final Enzyme TRYPSIN;
	
	/** The Constant CHYMOTRYPSIN. */
	public static final Enzyme CHYMOTRYPSIN;
	
	/** The Constant LysC. */
	public static final Enzyme LysC;
	
	/** The Constant LysN. */
	public static final Enzyme LysN;
	
	/** The Constant GluC. */
	public static final Enzyme GluC;
	
	/** The Constant ArgC. */
	public static final Enzyme ArgC;
	
	/** The Constant AspN. */
	public static final Enzyme AspN;
	
	/** The Constant ALP */
	public static final Enzyme ALP;
	
	/** Endogenous peptides */
	public static final Enzyme Peptidomics;
	
	public static Enzyme getEnzymeByName(String name)
	{
		return enzymeTable.get(name);
	}
	
	public static Enzyme[] getAllRegisteredEnzymes()
	{
		return registeredEnzymeList.toArray(new Enzyme[0]);
	}
	
	public static Enzyme register(String name, String residues, boolean isNTerm, String description)
	{
		return null;
	}
	
	private static HashMap<String,Enzyme> enzymeTable;
	private static ArrayList<Enzyme> registeredEnzymeList;
	
	private static void register(String name, Enzyme enzyme)
	{
		if(enzymeTable.put(name, enzyme) == null)
			registeredEnzymeList.add(enzyme);
	}
	
	static {
		NOENZYME = new Enzyme("NoEnzyme", null, false, "No enzyme");
		TRYPSIN = new Enzyme("Tryp", "KR", false, "Trypsin");
//		TRYPSIN.setNeighboringAAEfficiency(0.9148273f);
//		TRYPSIN.setPeptideCleavageEffiency(0.98173124f);
		
//		TRYPSIN.setNeighboringAAEfficiency(0.9523f);
//		TRYPSIN.setPeptideCleavageEffiency(0.9742f);

		// Modified by Sangtae to boost the performance
		TRYPSIN.setNeighboringAAEfficiency(0.99999f);
		TRYPSIN.setPeptideCleavageEffiency(0.99999f);

		CHYMOTRYPSIN = new Enzyme("CHYMOTRYPSIN", "FYWL", false, "Chymotrypsin");
		
		LysC = new Enzyme("LysC", "K", false, "Lys-C");
//		LysC.setNeighboringAAEfficiency(0.79f);
//		LysC.setPeptideCleavageEffiency(0.89f);
		LysC.setNeighboringAAEfficiency(0.999f);
		LysC.setPeptideCleavageEffiency(0.999f);
		
		LysN = new Enzyme("LysN", "K", true, "Lys-N");
		LysN.setNeighboringAAEfficiency(0.79f);
		LysN.setPeptideCleavageEffiency(0.89f);
		
		GluC = new Enzyme("GluC","E",false, "Glu-C");
		ArgC = new Enzyme("ArgC","R",false, "Arg-C");
		AspN = new Enzyme("AspN","D",true, "Asp-N");
		
		ALP = new Enzyme("aLP", null, false, "alphaLP");
		
		Peptidomics = new Enzyme("Peptidomics", null, false, "Endogenous peptides");
		
		enzymeTable = new HashMap<String,Enzyme>();
		registeredEnzymeList = new ArrayList<Enzyme>();
		
		registeredEnzymeList.add(NOENZYME);
		register("Tryp", TRYPSIN);
		register("CHYMOTRYPSIN", CHYMOTRYPSIN);
		register("LysC", LysC);
		register("LysN", LysN);
		register("GluC", GluC);
		register("ArgC", ArgC);
		register("AspN", AspN);
		register("aLP", ALP);
		register("Peptidomics", Peptidomics);
		
		// Add user-defined enzymes
		File enzymeFile = new File("params/enzymes.txt");
		if(enzymeFile.exists())
		{
//			System.out.println("Loading " + enzymeFile.getAbsolutePath());
			ArrayList<String> paramStrs = UserParam.parseFromFile(enzymeFile.getPath(), 4);
			for(String paramStr : paramStrs)
			{
				String[] token = paramStr.split(",");
				String shortName = token[0];
				String cleaveAt = token[1];
				if(cleaveAt.equalsIgnoreCase("null"))
					cleaveAt = null;
				else
				{
					for(int i=0; i<cleaveAt.length(); i++)
					{
						if(!AminoAcid.isStdAminoAcid(cleaveAt.charAt(i)))
						{
							System.err.println("Illegal user-defined enzyme at " + enzymeFile.getAbsolutePath() + ": " + paramStr);
							System.err.println("Unrecognizable aa residue: " + cleaveAt.charAt(i));
							System.exit(-1);
						}
					}
				}
				boolean isNTerm = false;	// C-Term: false, N-term: true
				if(token[2].equals("C"))
					isNTerm = false;
				else if(token[2].equals("N"))
					isNTerm = true;
				else
				{
					System.err.println("Illegal user-defined enzyme at " + enzymeFile.getAbsolutePath() + ": " + paramStr);
					System.err.println(token[2] + " must be 'C' or 'N'.");
					System.exit(-1);
				}
				String description = token[3];

				Enzyme userEnzyme = new Enzyme(shortName, cleaveAt, isNTerm, description);
				register(shortName, userEnzyme);
			}
		}
	}
}
