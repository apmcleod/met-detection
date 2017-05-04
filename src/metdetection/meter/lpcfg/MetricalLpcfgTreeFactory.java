package metdetection.meter.lpcfg;

import java.util.Arrays;
import java.util.List;

import metdetection.beat.Beat;
import metdetection.meter.Measure;
import metdetection.meter.lpcfg.MetricalLpcfgNonterminal.MetricalLpcfgLevel;
import metdetection.utils.MidiNote;

/**
 * A <code>MetricalLpcfgTreeFactory</code> is a class whose static methods aid in the
 * creation of {@link MetricalLpcfgTree}s. It cannot be instantiated.
 * 
 * @author Andrew McLeod - 25 April, 2016
 */
public class MetricalLpcfgTreeFactory {
	/**
	 * Private constructor to ensure that no factory is instantiated.
	 */
	private MetricalLpcfgTreeFactory() {}
	
	/**
	 * Make a new tree based on a List of MidiNotes.
	 * 
	 * @param notes A List of the notes which lie within the tree we want.
	 * @param beats A List of ALL of the beats of the current song.
	 * @param measure The measure type for the tree we will make.
	 * @param subBeatLength The sub beat length of the tree we will make.
	 * @param anacrusisLengthSubBeats The anacrusis length of the current song, measured in sub beats.
	 * @param measureNum The measure number of the tree we want.
	 * 
	 * @return A tree of the given measure type, containing the given notes.
	 */
	public static MetricalLpcfgTree makeTree(List<MidiNote> notes, List<Beat> beats, Measure measure, int subBeatLength, int anacrusisLengthSubBeats, int measureNum) {
		int beatsPerMeasure = measure.getBeatsPerMeasure();
		int subBeatsPerBeat = measure.getSubBeatsPerBeat();
		
		int measureLength = subBeatLength * beatsPerMeasure * subBeatsPerBeat;
		int anacrusisLength = subBeatLength * anacrusisLengthSubBeats;
		
		MetricalLpcfgQuantum[] quantums = new MetricalLpcfgQuantum[measureLength];
		Arrays.fill(quantums, MetricalLpcfgQuantum.REST);
		
		int firstBeatIndex = measureLength * measureNum + anacrusisLength; 
		int lastBeatIndex = firstBeatIndex + measureLength;
		
		for (MidiNote note : notes) {
			addNote(note, quantums, beats, firstBeatIndex, lastBeatIndex);
		}
		
		return makeTree(quantums, beatsPerMeasure, subBeatsPerBeat);
	}

	/**
	 * Add the given note into the given quantums array. The quantums parameter here is changed
	 * as a result of this call.
	 * 
	 * @param note The note we want to add into our quantums array.
	 * @param quantums The quantums array for tracking the current tree's quantums. This array may be
	 * changed as a result of this call.
	 * @param beats A List of ALL of the beats in the current song.
	 * @param firstBeatIndex The index of the beat which represents the first quantum in the quantum array.
	 * @param lastBeatIndex The index of the beat after the last quantum in the quantum array.
	 */
	private static void addNote(MidiNote note, MetricalLpcfgQuantum[] quantums, List<Beat> beats, int firstBeatIndex, int lastBeatIndex) {
		Beat onsetBeat = note.getOnsetBeat(beats);
		Beat offsetBeat = note.getOffsetBeat(beats);
		
		// Iterate to onset beat
		int beatIndex = 0;
		while (!beats.get(beatIndex).equals(onsetBeat)) {
			beatIndex++;
		}
		
		// Add onset
		if (beatIndex >= firstBeatIndex && beatIndex < lastBeatIndex) {
			addQuantum(MetricalLpcfgQuantum.ONSET, quantums, beatIndex - firstBeatIndex);
		}
		
		// Add ties
		beatIndex++;
		while (beatIndex < lastBeatIndex && beatIndex < beats.size() && !beats.get(beatIndex).equals(offsetBeat)) {
			if (beatIndex >= firstBeatIndex) {
				addQuantum(MetricalLpcfgQuantum.TIE, quantums, beatIndex - firstBeatIndex);
			}
			
			beatIndex++;
		}
	}

	/**
	 * Add the given quatnum into the given index of the given quantums array, if the type overrides that index's
	 * current value. That is, if the current value is not already an ONSET. The quantums array may be changed
	 * as a result of this call.
	 * 
	 * @param quantum The quantum we want to add to the quantums array.
	 * @param quantums The quantums array. This array may be changed as a result of this call.
	 * @param index The index at which we want to try to insert the given quantum.
	 */
	private static void addQuantum(MetricalLpcfgQuantum quantum, MetricalLpcfgQuantum[] quantums, int index) {
		// Update value if not ONSET. (We don't want a TIE to overwrite an ONSET)
		if (quantums[index] != MetricalLpcfgQuantum.ONSET) {
			quantums[index] = quantum;
		}
	}

	/**
	 * Make and return a tree from the given quantums with the given structure.
	 * 
	 * @param quantums The quantums which will be contained by this tree, unreduced.
	 * @param beatsPerMeasure The beats per measure which should be in this tree.
	 * @param subBeatsPerBeat The sub beats per beat which should be in this tree.
	 * 
	 * @return A tree generated from the given quantums and measure structure.
	 */
	public static MetricalLpcfgTree makeTree(MetricalLpcfgQuantum[] quantums, int beatsPerMeasure, int subBeatsPerBeat) {
		MetricalLpcfgMeasure measure = new MetricalLpcfgMeasure(beatsPerMeasure, subBeatsPerBeat);
		
		int beatLength = quantums.length / beatsPerMeasure;
		
		// Create beat quantum arrays
		MetricalLpcfgQuantum[][] beatQuantums = new MetricalLpcfgQuantum[beatsPerMeasure][];
		for (int beat = 0; beat < beatsPerMeasure; beat++) {
			beatQuantums[beat] = Arrays.copyOfRange(quantums, beatLength * beat, beatLength * (beat + 1));
		}
		
		// Create beat nodes
		for (MetricalLpcfgQuantum[] beatQuantum : beatQuantums) {
			measure.addChild(makeBeatNonterminal(beatQuantum, subBeatsPerBeat));
		}
		measure.fixChildrenTypes();
		
		return new MetricalLpcfgTree(measure);
	}
	
	/**
	 * Make and return a non-terminal representing a beat.
	 * 
	 * @param quantums The quantums which lie in this non-terminal.
	 * @param subBeatsPerBeat The number of sub beats which lie in this non-terminal.
	 * @return The non-terminal representing the given quantums.
	 */
	private static MetricalLpcfgNonterminal makeBeatNonterminal(MetricalLpcfgQuantum[] quantums, int subBeatsPerBeat) {
		MetricalLpcfgNonterminal beatNonterminal = new MetricalLpcfgNonterminal(MetricalLpcfgLevel.BEAT);
		
		MetricalLpcfgTerminal beatTerminal = new MetricalLpcfgTerminal(quantums, subBeatsPerBeat);
		if (beatTerminal.getReducedPattern().length == 1) {
			beatNonterminal.addChild(beatTerminal);
			
		} else {
			// Need to split into sub beats
			int subBeatLength = quantums.length / subBeatsPerBeat;
			
			// Create sub beat quantum arrays
			MetricalLpcfgQuantum[][] subBeatQuantums = new MetricalLpcfgQuantum[subBeatsPerBeat][];
			for (int subBeat = 0; subBeat < subBeatsPerBeat; subBeat++) {
				subBeatQuantums[subBeat] = Arrays.copyOfRange(quantums, subBeatLength * subBeat, subBeatLength * (subBeat + 1));
			}
			
			// Create sub beat nodes
			for (MetricalLpcfgQuantum[] subBeatQuantum : subBeatQuantums) {
				beatNonterminal.addChild(makeSubBeatNonterminal(subBeatQuantum));
			}
			beatNonterminal.fixChildrenTypes();
		}
		
		return beatNonterminal;
	}
	
	/**
	 * Make and return a non-terminal representing a sub beat.
	 * 
	 * @param quantums The quantums which lie in this non-terminal.
	 * @return The non-terminal representing the given quantums.
	 */
	private static MetricalLpcfgNonterminal makeSubBeatNonterminal(MetricalLpcfgQuantum[] quantums) {
		MetricalLpcfgNonterminal subBeatNonterminal = new MetricalLpcfgNonterminal(MetricalLpcfgLevel.SUB_BEAT);
		
		subBeatNonterminal.addChild(new MetricalLpcfgTerminal(quantums));
		
		return subBeatNonterminal;
	}
}
