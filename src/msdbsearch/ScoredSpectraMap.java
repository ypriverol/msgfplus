package msdbsearch;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import sequences.Constants;

import msgf.NominalMass;
import msgf.ScoredSpectrum;
import msgf.ScoredSpectrumSum;
import msgf.Tolerance;
import msscorer.DBScanScorer;
import msscorer.FastScorer;
import msscorer.NewRankScorer;
import msscorer.NewScoredSpectrum;
import msscorer.NewScorerFactory;
import msscorer.SimpleDBSearchScorer;
import msscorer.NewScorerFactory.SpecDataType;
import msutil.ActivationMethod;
import msutil.Composition;
import msutil.Enzyme;
import msutil.InstrumentType;
import msutil.SpecKey;
import msutil.Spectrum;
import msutil.SpectrumAccessorBySpecIndex;

public class ScoredSpectraMap {
	private final SpectrumAccessorBySpecIndex specMap;
	private final Tolerance leftParentMassTolerance;
	private final Tolerance rightParentMassTolerance;
	private final int numAllowedC13;
	private final SpecDataType specDataType;
	
	private SortedMap<Double,SpecKey> pepMassSpecKeyMap;
	private Map<SpecKey,SimpleDBSearchScorer<NominalMass>> specKeyScorerMap;

	public ScoredSpectraMap(
			SpectrumAccessorBySpecIndex specMap,
    		Tolerance leftParentMassTolerance, 
    		Tolerance rightParentMassTolerance, 
			int numAllowedC13,
			SpecDataType specDataType
			)
	{
		this.specMap = specMap;
		this.leftParentMassTolerance = leftParentMassTolerance;
		this.rightParentMassTolerance = rightParentMassTolerance;
		this.numAllowedC13 = numAllowedC13;
		this.specDataType = specDataType;
		
		pepMassSpecKeyMap = Collections.synchronizedSortedMap(new TreeMap<Double,SpecKey>());
		specKeyScorerMap = Collections.synchronizedMap(new HashMap<SpecKey,SimpleDBSearchScorer<NominalMass>>());
	}
	
	public void preProcessSpectra(List<SpecKey> specKeyList)
	{
		if(specDataType.getActivationMethod() != ActivationMethod.FUSION)
			preProcessIndividualSpectra(specKeyList);
		else
			preProcessFusedSpectra(specKeyList);
	}
	
	private void preProcessIndividualSpectra(List<SpecKey> specKeyList)
	{
		NewRankScorer scorer = null;
		ActivationMethod activationMethod = specDataType.getActivationMethod();
		InstrumentType instType = specDataType.getInstrumentType();
		Enzyme enzyme = specDataType.getEnzyme();
		
		if(activationMethod != null && activationMethod != ActivationMethod.FUSION)
			scorer = NewScorerFactory.get(activationMethod, instType, enzyme);
		
		for(SpecKey specKey : specKeyList)
		{
			int specIndex = specKey.getSpecIndex();
			Spectrum spec = specMap.getSpectrumBySpecIndex(specIndex);
			if(spec.size() < Constants.MIN_NUM_PEAKS_PER_SPECTRUM)
			{
//				System.out.println("Spectrum " + spec.getScanNum() + " has too few peaks (#Peaks: " + spec.size()+"): ignored.");
				continue;
			}
			if(activationMethod == null || activationMethod == ActivationMethod.FUSION)
				scorer = NewScorerFactory.get(spec.getActivationMethod(), instType, enzyme);
			
			int charge = specKey.getCharge();
			spec.setCharge(charge);
			NewScoredSpectrum<NominalMass> scoredSpec = scorer.getScoredSpectrum(spec);
			
			float peptideMass = spec.getParentMass() - (float)Composition.H2O;
			float tolDaLeft = leftParentMassTolerance.getToleranceAsDa(peptideMass);
			int maxNominalPeptideMass = NominalMass.toNominalMass(peptideMass) + Math.round(tolDaLeft-0.4999f);
			
			if(scorer.supportEdgeScores())
				specKeyScorerMap.put(specKey, new DBScanScorer(scoredSpec, maxNominalPeptideMass));
			else
				specKeyScorerMap.put(specKey, new FastScorer(scoredSpec, maxNominalPeptideMass));
			
			double peptideMassKey = (double)peptideMass;
			while(pepMassSpecKeyMap.get(peptideMassKey) != null)	// for speeding up
				peptideMassKey = Math.nextUp(peptideMassKey);
			pepMassSpecKeyMap.put(peptideMassKey, specKey);
			
			float tolDaRight = rightParentMassTolerance.getToleranceAsDa(peptideMass);
			if(numAllowedC13 > 0 && tolDaRight < 0.5f)
			{
				if(numAllowedC13 >= 1)
				{
					float mass1 = peptideMass-(float)Composition.ISOTOPE;
					double mass1Key = (double)mass1;
					while(pepMassSpecKeyMap.get(mass1Key) != null)
						mass1Key = Math.nextUp(mass1Key);
					pepMassSpecKeyMap.put(mass1Key, specKey);
				}
				
				if(numAllowedC13 >= 2)
				{
					float mass2 = peptideMass-2*(float)Composition.ISOTOPE;
					double mass2Key = (double)mass2;
					while(pepMassSpecKeyMap.get(mass2Key) != null)
						mass2Key = Math.nextUp(mass2Key);
					pepMassSpecKeyMap.put(mass2Key, specKey);
				}
			}				
		}				
	}
	
	private void preProcessFusedSpectra(List<SpecKey> specKeyList)
	{
		InstrumentType instType = specDataType.getInstrumentType();
		Enzyme enzyme = specDataType.getEnzyme();
		
		for(SpecKey specKey : specKeyList)
		{
			ArrayList<Integer> specIndexList = specKey.getSpecIndexList();
			if(specIndexList == null)
			{
				specIndexList = new ArrayList<Integer>();
				specIndexList.add(specKey.getSpecIndex());
			}
			ArrayList<ScoredSpectrum<NominalMass>> scoredSpecList = new ArrayList<ScoredSpectrum<NominalMass>>();
			for(int specIndex : specIndexList)
			{
				Spectrum spec = specMap.getSpectrumBySpecIndex(specIndex);
				if(spec.size() < Constants.MIN_NUM_PEAKS_PER_SPECTRUM)
				{
//					System.out.println("Spectrum " + spec.getScanNum() + " has too few peaks (#Peaks: " + spec.size()+"): ignored.");
					continue;
				}
				
				NewRankScorer scorer = NewScorerFactory.get(spec.getActivationMethod(), instType, enzyme);
				int charge = specKey.getCharge();
				spec.setCharge(charge);
				NewScoredSpectrum<NominalMass> sSpec = scorer.getScoredSpectrum(spec);
				scoredSpecList.add(sSpec);
			}
			
			if(scoredSpecList.size() == 0)
				continue;
			ScoredSpectrumSum<NominalMass> scoredSpec = new ScoredSpectrumSum<NominalMass>(scoredSpecList);
			float peptideMass = scoredSpec.getPrecursorPeak().getMass() - (float)Composition.H2O;
			float tolDaLeft = leftParentMassTolerance.getToleranceAsDa(peptideMass);
			int maxNominalPeptideMass = NominalMass.toNominalMass(peptideMass) + Math.round(tolDaLeft-0.4999f);
			specKeyScorerMap.put(specKey, new FastScorer(scoredSpec, maxNominalPeptideMass));
			
			double peptideMassKey = (double)peptideMass;
			while(pepMassSpecKeyMap.get(peptideMassKey) != null)	// for speeding up
				peptideMassKey = Math.nextUp(peptideMassKey);
			pepMassSpecKeyMap.put(peptideMassKey, specKey);
			
			float tolDaRight = rightParentMassTolerance.getToleranceAsDa(peptideMass);
			if(numAllowedC13 > 0 && tolDaRight < 0.5f)
			{
				if(numAllowedC13 >= 1)
				{
					float mass1 = peptideMass-(float)Composition.ISOTOPE;
					double mass1Key = (double)mass1;
					while(pepMassSpecKeyMap.get(mass1Key) != null)
						mass1Key = Math.nextUp(mass1Key);
					pepMassSpecKeyMap.put(mass1Key, specKey);
				}
				
				if(numAllowedC13 >= 2)
				{
					float mass2 = peptideMass-2*(float)Composition.ISOTOPE;
					double mass2Key = (double)mass2;
					while(pepMassSpecKeyMap.get(mass2Key) != null)
						mass2Key = Math.nextUp(mass2Key);
					pepMassSpecKeyMap.put(mass2Key, specKey);
				}
			}				
		}				
	}
	
	public SortedMap<Double,SpecKey> getPepMassSpecKeyMap()		{ return pepMassSpecKeyMap; }
	public Map<SpecKey,SimpleDBSearchScorer<NominalMass>> getSpecKeyScorerMap()	{ return specKeyScorerMap; }
	public Tolerance getLeftParentMassTolerance()				{ return leftParentMassTolerance; }
	public Tolerance getRightParentMassTolerance()				{ return rightParentMassTolerance; }
	public int getNumAllowedC13()								{ return numAllowedC13; }
	
}
