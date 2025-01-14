package edu.ucsd.msjava.fdr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

public class TargetDecoyAnalysis {
    final PSMSet target;
    final PSMSet decoy;
    final boolean isGreaterBetter;
    final float pit;    // portion of incorrect target PSMs

    TreeMap<Float, Float> psmLevelFDRMap;    // PSMScore -> FDR
    TreeMap<Float, Float> pepLevelFDRMap;    // Peptide -> PepFDR

    public TargetDecoyAnalysis(PSMSet target, PSMSet decoy) {
        this(target, decoy, 1);
    }

    public TargetDecoyAnalysis(PSMSet target, PSMSet decoy, float pit) {
        this.target = target;
        this.decoy = decoy;
        this.isGreaterBetter = target.isGreaterBetter();
        this.pit = pit;
        psmLevelFDRMap = getFDRMap(target.getPSMScores(), decoy.getPSMScores(), isGreaterBetter, pit);
        pepLevelFDRMap = getFDRMap(target.getPepScores(), decoy.getPepScores(), isGreaterBetter, pit);
    }

//    public TargetDecoyPSMSet(
//            File concatenatedFile, 
//            String delimiter,
//            boolean hasHeader,
//            int scoreCol, 
//            boolean isGreaterBetter, 
//            int specFileCol,
//            int specIndexCol, 
//            int pepCol,
//            ArrayList<Pair<Integer,ArrayList<String>>> reqStrList,
//            int dbCol, String decoyPrefix)
//    {
//        target = new TSVPSMSet(concatenatedFile, delimiter, hasHeader, scoreCol, isGreaterBetter, specFileCol, specIndexCol, pepCol, reqStrList).decoy(dbCol, decoyPrefix, true).read();
//        decoy = new TSVPSMSet(concatenatedFile, delimiter, hasHeader, scoreCol, isGreaterBetter, specFileCol, specIndexCol, pepCol, reqStrList).decoy(dbCol, decoyPrefix, false).read();
//        this.isGreaterBetter = isGreaterBetter;
//        isConcatenated = true;
//        psmLevelFDRMap = getFDRMap(target.getPSMScores(), decoy.getPSMScores(), isGreaterBetter, isConcatenated, 1);
//        pepLevelFDRMap = getFDRMap(target.getPepScores(), decoy.getPepScores(), isGreaterBetter, isConcatenated, 1);
//    }
//
//    public TargetDecoyPSMSet(
//            File targetFile, 
//            File decoyFile, 
//            String delimiter,
//            boolean hasHeader,
//            int scoreCol, 
//            boolean isGreaterBetter, 
//            int specFileCol,
//            int specIndexCol, 
//            int pepCol,
//            ArrayList<Pair<Integer,ArrayList<String>>> reqStrListPSMSet,
//            float pit
//            )
//    {
//        target = new TSVPSMSet(targetFile, delimiter, hasHeader, scoreCol, isGreaterBetter, specFileCol, specIndexCol, pepCol, reqStrListPSMSet).read();
//        decoy = new TSVPSMSet(decoyFile, delimiter, hasHeader, scoreCol, isGreaterBetter, specFileCol, specIndexCol, pepCol, reqStrListPSMSet).read();
//        isConcatenated = false;
//        psmLevelFDRMap = getFDRMap(target.getPSMScores(), decoy.getPSMScores(), isGreaterBetter, isConcatenated, pit);
//        pepLevelFDRMap = getFDRMap(target.getPepScores(), decoy.getPepScores(), isGreaterBetter, isConcatenated, pit);
//    }

    public PSMSet getTargetPSMSet() {
        return target;
    }

    public PSMSet getDecoyPSMSet() {
        return decoy;
    }

    public TreeMap<Float, Float> getPSMLevelFDRMap() {
        return psmLevelFDRMap;
    }

    public TreeMap<Float, Float> getPepLevelFDRMap() {
        return pepLevelFDRMap;
    }

    public float getPSMQValue(float score) {
        float fdr;
        if (isGreaterBetter)
            fdr = psmLevelFDRMap.lowerEntry(score).getValue();
        else
            fdr = psmLevelFDRMap.higherEntry(score).getValue();
        return fdr;
    }

    public float getPepFDR(float score) {
        float fdr;
        if (isGreaterBetter)
            fdr = pepLevelFDRMap.lowerEntry(score).getValue();
        else
            fdr = pepLevelFDRMap.higherEntry(score).getValue();
        return fdr;
    }

    public Float getPepQValueFromAnnotation(String annotation) {
        String pep = TSVPSMSet.getPeptideFromAnnotation(annotation);

        Float score = target.getPeptideScoreTable().get(pep);
        if (score == null) {
            score = decoy.getPeptideScoreTable().get(pep);
            if (score == null)
                return null;
        }
        return getPepFDR(score);
    }

    public Float getPepQValue(String pep) {
        Float score = target.getPeptideScoreTable().get(pep);
        if (score == null) {
            score = decoy.getPeptideScoreTable().get(pep);
            if (score == null)
                return null;
        }
        return getPepFDR(score);
    }

    // returns threshold where FDR(t>threshold)<=fdrThreshold && FDR(t<=threshold)>fdrThreshold
    public float getThresholdScore(float fdrThreshold, boolean isPeptideLevel) {
        TreeMap<Float, Float> map;
        if (!isPeptideLevel)
            map = psmLevelFDRMap;    // PSMScore -> FDR
        else
            map = pepLevelFDRMap;

        float threshold;
        if (isGreaterBetter) {
            threshold = Float.MAX_VALUE;
            for (Entry<Float, Float> entry : map.descendingMap().entrySet()) {
                if (entry.getValue() > fdrThreshold)
                    break;
                else
                    threshold = entry.getKey();

            }
        } else {
            threshold = Float.MIN_VALUE;

            for (Entry<Float, Float> entry : map.entrySet()) {
//                System.out.println(entry.getKey()+"\t"+entry.getValue());
                if (entry.getValue() > fdrThreshold)
                    break;
                else
                    threshold = entry.getKey();
            }
        }
        return threshold;
    }

    public static TreeMap<Float, Float> getFDRMap(ArrayList<Float> target, ArrayList<Float> decoy,
                                                  boolean isGreaterBetter, float pit) {
        TreeMap<Float, Float> fdrMap = new TreeMap<Float, Float>();
        if (!isGreaterBetter) {
            Collections.sort(target);
            Collections.sort(decoy);
        } else {
            Collections.sort(target, Collections.reverseOrder());
            Collections.sort(decoy, Collections.reverseOrder());
        }

        int targetIndex = 0;
        float prevDecoyScore = Float.NEGATIVE_INFINITY;

        if (isGreaterBetter) {
            fdrMap.put(Float.POSITIVE_INFINITY, 0f);
            fdrMap.put(Float.NEGATIVE_INFINITY, 1f);
        } else {
            fdrMap.put(Float.POSITIVE_INFINITY, 1f);
            fdrMap.put(Float.NEGATIVE_INFINITY, 0f);
        }

        for (int decoyIndex = 0; decoyIndex < decoy.size(); decoyIndex++) {
            float decoyScore = decoy.get(decoyIndex);
            if (decoyScore == prevDecoyScore)
                continue;
            else
                prevDecoyScore = decoyScore;
            if (isGreaterBetter) {
                while (targetIndex < target.size() && target.get(targetIndex) > decoyScore)
                    targetIndex++;
            } else {
                while (targetIndex < target.size() && target.get(targetIndex) < decoyScore)
                    targetIndex++;
            }

            if (targetIndex > 0) {
                float fdr;

                if (targetIndex <= decoyIndex) {
                    fdr = 1;
                } else {
                    // Compute FDR using simple formulation by Lukas Käll et al., JPR 2008
                    // https://pubmed.ncbi.nlm.nih.gov/18052118/
                    // fdr = ReversePeptideCount ÷ ForwardPeptideCount

                    // pit is "portion of incorrect target PSMs" and is always 1 (in practice)
                    fdr = Math.round(decoyIndex * pit) / (float) targetIndex;

                    // Alternative formula, from Elias and Gygi, Nat. Methods 2007
                    // https://pubmed.ncbi.nlm.nih.gov/17327847/
                    // fdr = (2 × ReversePeptideCount) ÷ (ForwardPeptideCount + ReversePeptideCount)
                    // fdr = (2 * decoyIndex) / (float)(targetIndex + decoyIndex);
                }

                if (fdr > 1)
                    fdr = 1f;

                fdrMap.put(decoyScore, fdr);
                if (fdr >= 1)
                    break;
//                System.out.println("1: " + decoyScore + ":" + fdr);
            }
        }

        if (decoy.size() == 0) {
            if (isGreaterBetter)
                fdrMap.put(Float.NEGATIVE_INFINITY, 0f);
            else
                fdrMap.put(Float.POSITIVE_INFINITY, 0f);
        }

        TreeMap<Float, Float> finalFDRMap = new TreeMap<Float, Float>();

        // Convert FDRs into q-values
        Iterator<Entry<Float, Float>> itr;
        if (isGreaterBetter)
            itr = fdrMap.entrySet().iterator();
        else
            itr = fdrMap.descendingMap().entrySet().iterator();
        float minFDR = 1;
        while (itr.hasNext()) {
            Entry<Float, Float> entry = itr.next();
            float fdr = entry.getValue();
            if (fdr > minFDR)
                fdr = minFDR;
            minFDR = fdr;
            finalFDRMap.put(entry.getKey(), fdr);
        }

//        if(isGreaterBetter)
//        {
//            finalFDRMap.put(Float.POSITIVE_INFINITY, 0f);
//            finalFDRMap.put(Float.NEGATIVE_INFINITY, 1f);
//        }
//        else
//        {
//            finalFDRMap.put(Float.POSITIVE_INFINITY, 1f);
//            finalFDRMap.put(Float.NEGATIVE_INFINITY, 0f);
//        }
        return finalFDRMap;
    }

}
