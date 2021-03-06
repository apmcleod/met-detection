package metdetection.meter.lpcfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeSet;

import metdetection.Main;
import metdetection.beat.Beat;
import metdetection.meter.MetricalModelState;
import metdetection.meter.Measure;
import metdetection.utils.MidiNote;
import metdetection.voice.Voice;

/**
 * A <code>GrammarHierarchyModelState</code> is used to generate hierarchy hypotheses
 * using a grammar.
 * 
 * @author Andrew McLeod - 29 February, 2016
 */
public class MetricalLpcfgMetricalModelState extends MetricalModelState implements Comparable<MetricalLpcfgMetricalModelState> {
	
	/**
	 * A <code>MetricalLpcfgMatch</code> is an enum indicating which type of match a
	 * {@link MetricalLpcfgMetricalModelState} has matched.
	 * 
	 * @author Andrew McLeod - 12 May, 2016
	 */
	public enum MetricalLpcfgMatch {
		/**
		 * A sub beat level match.
		 */
		SUB_BEAT,
		
		/**
		 * A beat level match.
		 */
		BEAT,
		
		/**
		 * A wrong match.
		 */
		WRONG;
	}

	/**
	 * The length of the terminals which we are looking for.
	 */
	private final int subBeatLength;
	
	/**
	 * The length of the anacrusis in this state, measured in quantums.
	 */
	private final int anacrusisLength;
	
	/**
	 * The measure type we are looking for.
	 */
	private final Measure measure;
	
	/**
	 * The grammar object we will use for probabilities.
	 */
	private final MetricalLpcfg grammar;
	
	/**
	 * The local grammar for this hierarchy model state.
	 */
	private final MetricalLpcfg localGrammar;
	
	/**
	 * The log probability of this hypothesis.
	 */
	private double logProbability;
	
	/**
	 * The measure number of the next subbeat to be shifted onto the stack.
	 */
	private int measureNum;
	
	/**
	 * The index of the first beat of the next measure.
	 */
	private int nextMeasureIndex;
	
	/**
	 * A List of whether each given voice has begun yet. This is used to throw out anacrusis measures.
	 */
	private final List<Boolean> hasBegun;
	
	/**
	 * A List of any notes which have not yet been completely added to a Tree, divided by voice.
	 */
	private final List<LinkedList<MidiNote>> unfinishedNotes;
	
	/**
	 * Notes which we have yet to check for beat and sub beat matches.
	 */
	private final Queue<MidiNote> notesToCheck;
	
	/**
	 * A List of a Queue per voice of notes to check for beat matching.
	 */
	private final List<LinkedList<MidiNote>> notesToCheckBeats;
	
	/**
	 * The number of times a sub beat match has been found.
	 */
	private int subBeatMatches;
	
	/**
	 * The number of times a beat match has been found.
	 */
	private int beatMatches;
	
	/**
	 * The number of times a mismatch has been found.
	 */
	private int wrongMatches;
	
	/**
	 * Create a new state based on the given grammar. The {@link #measure} and {@link #subBeatLength} fields
	 * will be null and 0 respectively, and appropriate settings for these will be returned by the first call
	 * to {@link #handleIncoming(List)}. This allows all possible states to be generated by a single one without
	 * having to manually create each one each time.
	 * 
	 * @param grammar {@link #grammar}
	 */
	public MetricalLpcfgMetricalModelState(MetricalLpcfg grammar) {
		subBeatLength = 0;
		anacrusisLength = 0;
		measure = null;
		this.grammar = grammar;
		
		subBeatMatches = 0;
		beatMatches = 0;
		wrongMatches = 0;
		
		localGrammar = new MetricalLpcfg();
		
		logProbability = 0.0;
		
		measureNum = 0;
		nextMeasureIndex = 0;
		
		unfinishedNotes = new ArrayList<LinkedList<MidiNote>>();
		notesToCheck = new LinkedList<MidiNote>();
		notesToCheckBeats = new ArrayList<LinkedList<MidiNote>>();
		hasBegun = new ArrayList<Boolean>();
	}
	
	/**
	 * Create a new state based on the given grammar, measure, and terminal length, and as a partial
	 * deep copy of the given state.
	 * 
	 * @param state The old state we want this new one to be a deep copy of.
	 * @param grammar {@link #grammar}
	 * @param measure {@link #measure}
	 * @param terminalLength {@link #subBeatLength}
	 * @param anacrusisLength {@link #anacrusisLength}
	 */
	private MetricalLpcfgMetricalModelState(MetricalLpcfgMetricalModelState state, MetricalLpcfg grammar, Measure measure, int terminalLength, int anacrusisLength) {
		this.subBeatLength = terminalLength;
		this.anacrusisLength = anacrusisLength;
		this.measure = measure;
		this.grammar = grammar;
		
		subBeatMatches = state.subBeatMatches;
		beatMatches = state.beatMatches;
		wrongMatches = state.wrongMatches;
		
		logProbability = state.logProbability;
		
		measureNum = state.measureNum;
		nextMeasureIndex = state.nextMeasureIndex;
		if (measure != null && nextMeasureIndex == 0) {
			if (anacrusisLength != 0) {
				nextMeasureIndex = anacrusisLength * subBeatLength;
				measureNum = -1;
				
			} else {
				nextMeasureIndex = subBeatLength * measure.getBeatsPerMeasure() * measure.getSubBeatsPerBeat();
			}
		}
		
		unfinishedNotes = new ArrayList<LinkedList<MidiNote>>();
		for (Queue<MidiNote> voice : state.unfinishedNotes) {
			unfinishedNotes.add(new LinkedList<MidiNote>(voice));
		}
		
		notesToCheckBeats = new ArrayList<LinkedList<MidiNote>>();
		for (LinkedList<MidiNote> voice : state.notesToCheckBeats) {
			notesToCheckBeats.add(new LinkedList<MidiNote>(voice));
		}
		
		hasBegun = new ArrayList<Boolean>(state.hasBegun);
		
		localGrammar = state.localGrammar.deepCopy();
		notesToCheck = new LinkedList<MidiNote>(state.notesToCheck);
		
		setVoiceState(state.voiceState);
		setBeatState(state.beatState);
	}
	
	/**
	 * Create a new state which is a deep copy of the given one (when necessary).
	 * 
	 * @param state The state whose deep copy we want.
	 */
	private MetricalLpcfgMetricalModelState(MetricalLpcfgMetricalModelState state) {
		this(state, state.grammar, state.measure, state.subBeatLength, state.anacrusisLength);
	}

	@Override
	public TreeSet<MetricalLpcfgMetricalModelState> handleIncoming(List<MidiNote> notes) {
		if (!isFullyMatched()) {
			notesToCheck.addAll(notes);
		}
		
		// Update for any new voices
		addNewVoices(notes);
		
		if (measure != null) {
			while (beatState.getBeats().size() > nextMeasureIndex) {
				parseStep();
			}
		}
		
		// Branch
		TreeSet<MetricalLpcfgMetricalModelState> newStates = new TreeSet<MetricalLpcfgMetricalModelState>();
		
		if (measure == null) {
			newStates.addAll(getAllFirstStepBranches());
			
		} else {
			if (!isFullyMatched()) {
				updateMatchType();
			}
			
			if (!isWrong()) {
				newStates.add(this);
				
			} else if (Main.VERBOSE) {
				System.out.println("Eliminating " + this);
			}
		}
		
		return newStates;
	}
	
	/**
	 * Get a List of all of the first step branches we could make out of this state. That is,
	 * given that we currently do not have a measure, assign all possible measures and terminalLengths
	 * to new copies and return them in a List.
	 * 
	 * @return A List of all possible first step branches out of this state.
	 */
	private List<MetricalLpcfgMetricalModelState> getAllFirstStepBranches() {
		List<MetricalLpcfgMetricalModelState> newStates = new ArrayList<MetricalLpcfgMetricalModelState>();
		
		long lastTime = notesToCheck.peek().getOffsetTime();
		long lastBeatTime = beatState.getBeats().get(beatState.getBeats().size() - 1).getTime();
		
		// No notes have finished yet, we must still wait
		if (lastBeatTime < lastTime) {
			newStates.add(this);
			return newStates;
		}
		
		// A note has finished, add measure hypotheses
		for (int subBeatLength = 1; subBeatLength <= 1; subBeatLength++) {
			
			for (Measure measure : grammar.getMeasures()) {
				
				int subBeatsPerMeasure = measure.getBeatsPerMeasure() * measure.getSubBeatsPerBeat();
				for (int anacrusisLength = 0; anacrusisLength < subBeatsPerMeasure; anacrusisLength++) {
					
					MetricalLpcfgMetricalModelState newState =
							new MetricalLpcfgMetricalModelState(this, grammar, measure, subBeatLength, anacrusisLength);
					newState.updateMatchType();
					
					// This hypothesis could match the first note
					if (!newState.isWrong()) {
						newStates.add(newState);
						
						if (Main.VERBOSE) {
							System.out.println("Adding " + newState);
						}
					}
				}
			}
		}
		
		return newStates;
	}
	
	@Override
	public TreeSet<MetricalLpcfgMetricalModelState> close() {
		while (!allNotesFinished()) {
			parseStep();
		}
				
		// Branch
		TreeSet<MetricalLpcfgMetricalModelState> newStates = new TreeSet<MetricalLpcfgMetricalModelState>();
		
		if (!isFullyMatched()) {
			updateMatchType();
		}
			
		if (!isWrong() && isFullyMatched()) {
			newStates.add(this);
				
		} else if (Main.VERBOSE) {
			System.out.println("Eliminating " + this);
		}
		
		return newStates;
	}
	
	/**
	 * Perform a single parse step. That is, make a tree from {@link #unfinishedNotes},
	 * add it and its probability to our model, and then rmove any finished notes.
	 */
	private void parseStep() {
		for (int voiceIndex = 0; voiceIndex < unfinishedNotes.size(); voiceIndex++) {
			List<MidiNote> voice = unfinishedNotes.get(voiceIndex);
			MetricalLpcfgTree tree = 
					MetricalLpcfgTreeFactory.makeTree(voice, beatState.getBeats(), measure, subBeatLength, anacrusisLength, measureNum);
			
			if (!tree.isEmpty()) {
				if (!hasBegun.get(voiceIndex)) {
					hasBegun.set(voiceIndex, Boolean.TRUE);
					
					if (tree.startsWithRest()) {
						continue;
					}
				}
				
				logProbability += grammar.getTreeLogProbability(tree);
				localGrammar.addTree(tree);
			}
		}
		
		removeFinishedNotes();
		nextMeasureIndex += subBeatLength * measure.getBeatsPerMeasure() * measure.getSubBeatsPerBeat();
		measureNum++;
	}
	
	/**
	 * Check if all of the voices within {@link #unfinishedNotes} are empty.
	 * 
	 * @return True if all of the voices are empty. False otherwise.
	 */
	private boolean allNotesFinished() {
		for (List<MidiNote> voice : unfinishedNotes) {
			if (!voice.isEmpty()) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Remove any notes which are entirely finished from {@link #unfinishedNotes}.
	 */
	private void removeFinishedNotes() {
		List<Beat> beats = beatState.getBeats();
		
		for (List<MidiNote> voice : unfinishedNotes) {
			for (int i = 0; i < voice.size(); i++) {
				int lastIndex = beats.lastIndexOf(voice.get(i).getOffsetBeat(beats));
				
				if ((lastIndex != beats.size() - 1 && lastIndex == nextMeasureIndex) || lastIndex < nextMeasureIndex) {
					voice.remove(i--);
				}
			}
		}
	}

	/**
	 * Add new voices into our tracking list {@link #unfinishedNotes}.
	 * 
	 * @param notes The new notes that were just added.
	 */
	private void addNewVoices(List<MidiNote> notes) {
		for (int voiceIndex = 0; voiceIndex < voiceState.getVoices().size(); voiceIndex++) {
			Voice voice = voiceState.getVoices().get(voiceIndex);
			MidiNote note = voice.getMostRecentNote();
			
			if (notes.contains(note)) {
				// This Voice's most recent note is one of the new ones
				LinkedList<MidiNote> newNotes = new LinkedList<MidiNote>();
				
				boolean newVoice = true;
				for (MidiNote voiceNote : voice.getNotes()) {
					if (!notes.contains(voiceNote)) {
						// This is not a new voice
						newVoice = false;
						
					} else {
						newNotes.add(voiceNote);
					}
				}
				
				if (newVoice) {
					unfinishedNotes.add(voiceIndex, newNotes);
					hasBegun.add(voiceIndex, Boolean.FALSE);
					
					if (!matches(MetricalLpcfgMatch.BEAT)) {
						notesToCheckBeats.add(voiceIndex, new LinkedList<MidiNote>(newNotes));
					}
					
				} else {
					unfinishedNotes.get(voiceIndex).addAll(newNotes);
					
					if (!matches(MetricalLpcfgMatch.BEAT)) {
						notesToCheckBeats.get(voiceIndex).addAll(newNotes);
					}
				}
			}
		}
	}
	
	/**
	 * Go through the {@link #notesToCheck} Queue for any notes that have finished and update
	 * the current match for any that have.
	 */
	private void updateMatchType() {
		// Check for conglomerate beat matches
		for (int voiceIndex = 0; !isWrong() && !matches(MetricalLpcfgMatch.BEAT) && voiceIndex < notesToCheckBeats.size(); voiceIndex++) {
			while (checkConglomerateBeatMatch(notesToCheckBeats.get(voiceIndex)));
			
			// Empty notesToCheckBeats if done
			if (matches(MetricalLpcfgMatch.BEAT)) {
				notesToCheckBeats.clear();
			}
		}
		
		long lastTime = beatState.getBeats().get(beatState.getBeats().size() - 1).getTime();
		
		while (!isWrong() && !isFullyMatched() && !notesToCheck.isEmpty() && notesToCheck.peek().getOffsetTime() <= lastTime) {
			updateMatchType(notesToCheck.poll());
		}
	}
	
	/**
	 * Check for a conglomerate beat match. That is, a match where some number of notes add up to a beat exactly
	 * with no leading, trailing, or interspersed rests.
	 * 
	 * @param voiceNotes The unchecked notes in the voice we want to check.
	 * @return True if we need to continue checking this voice. That is, if this model is not wrong yet, hasn't yet
	 * been matched at the beat level, and if we were able to check the beat beginning with the previous first note.
	 * False otherwise.
	 */
	private boolean checkConglomerateBeatMatch(LinkedList<MidiNote> voiceNotes) {
		if (voiceNotes.isEmpty()) {
			return false;
		}
		
		int tactiPerMeasure = beatState.getTactiPerMeasure();
		int beatLength = subBeatLength * measure.getSubBeatsPerBeat();
		
		List<Beat> beats = beatState.getBeats();
		
		Beat lastBeat = beats.get(beats.size() - 1);
		int lastBeatTactus = getTactusNormalized(tactiPerMeasure, beats.get(0), lastBeat.getMeasure(), lastBeat.getBeat());
		lastBeatTactus -= anacrusisLength * subBeatLength;
		
		if (anacrusisLength % measure.getSubBeatsPerBeat() != 0) {
			lastBeatTactus += subBeatLength * (measure.getSubBeatsPerBeat() - (anacrusisLength % measure.getSubBeatsPerBeat()));
		}
		int lastBeatNum = lastBeatTactus / beatLength;
		
		// First note
		Iterator<MidiNote> iterator = voiceNotes.iterator();
		MidiNote firstNote = iterator.next();
		iterator.remove();
		
		Beat startBeat = firstNote.getOnsetBeat(beats);
		Beat endBeat = firstNote.getOffsetBeat(beats);
		
		int startTactus = getTactusNormalized(tactiPerMeasure, beats.get(0), startBeat.getMeasure(), startBeat.getBeat());
		int endTactus = getTactusNormalized(tactiPerMeasure, beats.get(0), endBeat.getMeasure(), endBeat.getBeat());
		
		int noteLengthTacti = Math.max(1, endTactus - startTactus);
		startTactus -= anacrusisLength * subBeatLength;
		endTactus = startTactus + noteLengthTacti;
		
		// Add up possible partial beat at the start
		if (anacrusisLength % measure.getSubBeatsPerBeat() != 0) {
			startTactus += subBeatLength * (measure.getSubBeatsPerBeat() - (anacrusisLength % measure.getSubBeatsPerBeat()));
		}
		int beatOffset = startTactus % beatLength;
		int firstBeatNum = startTactus / beatLength;
		
		// First note's beat hasn't finished yet
		if (firstBeatNum == lastBeatNum) {
			voiceNotes.addFirst(firstNote);
			return false;
		}

		// First note doesn't begin on a beat
		if (beatOffset != 0) {
			return iterator.hasNext();
		}
		
		// Tracking array
		MetricalLpcfgQuantum[] quantums = new MetricalLpcfgQuantum[beatLength + 1];
		Arrays.fill(quantums, MetricalLpcfgQuantum.REST);
		
		// Add first note into tracking array
		quantums[beatOffset] = MetricalLpcfgQuantum.ONSET;
		for (int tactus = beatOffset + 1; tactus < noteLengthTacti && tactus < quantums.length; tactus++) {
			quantums[tactus] = MetricalLpcfgQuantum.TIE;
		}
		
		while (iterator.hasNext()) {
			MidiNote note = iterator.next();
			
			startBeat = note.getOnsetBeat(beats);
			endBeat = note.getOffsetBeat(beats);
			
			startTactus = getTactusNormalized(tactiPerMeasure, beats.get(0), startBeat.getMeasure(), startBeat.getBeat());
			endTactus = getTactusNormalized(tactiPerMeasure, beats.get(0), endBeat.getMeasure(), endBeat.getBeat());
			
			noteLengthTacti = Math.max(1, endTactus - startTactus);
			startTactus -= anacrusisLength * subBeatLength;
			endTactus = startTactus + noteLengthTacti;
			
			// Add up possible partial beat at the start
			if (anacrusisLength % measure.getSubBeatsPerBeat() != 0) {
				startTactus += subBeatLength * (measure.getSubBeatsPerBeat() - (anacrusisLength % measure.getSubBeatsPerBeat()));
			}
			beatOffset = startTactus % beatLength;
			int beatNum = startTactus / beatLength;
			
			if (beatNum != firstBeatNum) {
				if (beatOffset == 0) {
					quantums[beatLength] = MetricalLpcfgQuantum.ONSET;
				}
				
				break;
			}
			
			// This note was in the same beat. Remove it.
			iterator.remove();
			
			// Add note into tracking array
			quantums[beatOffset] = MetricalLpcfgQuantum.ONSET;
			for (int tactus = beatOffset + 1; tactus - beatOffset < noteLengthTacti && tactus < quantums.length; tactus++) {
				quantums[tactus] = MetricalLpcfgQuantum.TIE;
			}
		}
		
		// Some note tied over the beat boundary, no match
		if (quantums[beatLength] == MetricalLpcfgQuantum.TIE) {
			return iterator.hasNext();
		}
		
		// Get the onsets of this quantum
		List<Integer> onsets = new ArrayList<Integer>();
		for (int tactus = 0; tactus < beatLength; tactus++) {
			MetricalLpcfgQuantum quantum = quantums[tactus];
			
			// There's a REST in this quantum, no match
			if (quantum == MetricalLpcfgQuantum.REST) {
				return iterator.hasNext();
			}
			
			if (quantum == MetricalLpcfgQuantum.ONSET) {
				onsets.add(tactus);
			}
		}
		
		// Get the lengths of the notes of this quantum
		List<Integer> lengths = new ArrayList<Integer>(onsets.size());
		for (int i = 1; i < onsets.size(); i++) {
			lengths.add(onsets.get(i) - onsets.get(i - 1));
		}
		lengths.add(beatLength - onsets.get(onsets.size() - 1));
		
		// Only 1 note, no match
		if (lengths.size() == 1) {
			return iterator.hasNext();
		}
		
		for (int i = 1; i < lengths.size(); i++) {
			// Some lengths are different, match
			if (lengths.get(i) != lengths.get(i - 1)) {
				addMatch(MetricalLpcfgMatch.BEAT);
				return false;
			}
		}
		
		// All note lengths were the same, no match
		return iterator.hasNext();
	}
	
	/**
	 * Get the tactus number as a beat index given some information about the Beats.
	 * 
	 * @param tactiPerMeasure The number of tacti per measure of the Beats in the Beat List. A value
	 * of 0 will return exactly the tactus number that is passed in. 
	 * @param firstBeat The first Beat of the beat tracker.
	 * @param measure The measure number of the beat we want the tactus of.
	 * @param tactus The tactus number we want adjusted.
	 * 
	 * @return The adjusted tactus number.
	 */
	private int getTactusNormalized(int tactiPerMeasure, Beat firstBeat, int measure, int tactus) {
		if (tactiPerMeasure != 0) {
			// FromFileBeatTrackingModelState anacrusis correction (erasure)
			measure -= firstBeat.getMeasure();
			
			tactus += tactiPerMeasure * measure;
			tactus -= firstBeat.getBeat();
		}
		
		return tactus;
	}

	/**
	 * Update the current match type for the given note.
	 * 
	 * @param note The note we need to check for a match.
	 */
	private void updateMatchType(MidiNote note) {
		List<Beat> beats = beatState.getBeats();
		Beat startBeat = note.getOnsetBeat(beats);
		Beat endBeat = note.getOffsetBeat(beats);
		
		int tactiPerMeasure = beatState.getTactiPerMeasure();
		
		int startTactus = getTactusNormalized(tactiPerMeasure, beats.get(0), startBeat.getMeasure(), startBeat.getBeat());
		int endTactus = getTactusNormalized(tactiPerMeasure, beats.get(0), endBeat.getMeasure(), endBeat.getBeat());
		
		int noteLengthTacti = Math.max(1, endTactus - startTactus);
		startTactus -= anacrusisLength * subBeatLength;
		endTactus = startTactus + noteLengthTacti;
		
		int prefixStart = startTactus;
		int middleStart = startTactus;
		int postfixStart = endTactus;
		
		int prefixLength = 0;
		int middleLength = noteLengthTacti;
		int postfixLength = 0;
		
		int beatLength = subBeatLength * measure.getSubBeatsPerBeat();
		
		// Reinterpret note given matched levels
		if (matches(MetricalLpcfgMatch.SUB_BEAT) && startTactus / subBeatLength != (endTactus - 1) / subBeatLength) {
			// Interpret note as sub beats
			
			int subBeatOffset = startTactus % subBeatLength;
			int subBeatEndOffset = endTactus % subBeatLength;
			
			// Prefix
			if (subBeatOffset != 0) {
				prefixLength = subBeatLength - subBeatOffset;
			}
			
			// Middle fix
			middleStart += prefixLength;
			middleLength -= prefixLength;
			
			// Postfix
			postfixStart -= subBeatEndOffset;
			postfixLength += subBeatEndOffset;
			
			// Middle fix
			middleLength -= postfixLength;
			
		} else if (matches(MetricalLpcfgMatch.BEAT) && startTactus / beatLength != (endTactus - 1) / beatLength) {
			// Interpret note as beats
			
			// Add up possible partial beat at the start
			if (anacrusisLength % measure.getSubBeatsPerBeat() != 0) {
				int diff = subBeatLength * (measure.getSubBeatsPerBeat() - (anacrusisLength % measure.getSubBeatsPerBeat()));
				startTactus += diff;
				endTactus += diff;
			}
			int beatOffset = (startTactus + subBeatLength * (anacrusisLength % measure.getSubBeatsPerBeat())) % beatLength;
			int beatEndOffset = (endTactus + subBeatLength * (anacrusisLength % measure.getSubBeatsPerBeat())) % beatLength;
			
			// Prefix
			if (beatOffset != 0) {
				prefixLength = beatLength - beatOffset;
			}
			
			// Middle fix
			middleStart += prefixLength;
			middleLength -= prefixLength;
			
			// Postfix
			postfixStart -= beatEndOffset;
			postfixLength += beatEndOffset;
			
			// Middle fix
			middleLength -= postfixLength;
		}
		
		// Prefix checking
		if (prefixLength != 0) {
			updateMatchType(prefixStart, prefixLength);
		}
		
		// Middle checking
		if (!isFullyMatched() && !isWrong() && middleLength != 0) {
			updateMatchType(middleStart, middleLength);
		}
		
		// Postfix checking
		if (!isFullyMatched() && !isWrong() && postfixLength != 0) {
			updateMatchType(postfixStart, postfixLength);
		}
	}

	/**
	 * Update the current match for a note with the given start tactus and length.
	 * 
	 * @param startTactus The tactus at which this note begins, normalized to where 0
	 * is the beginning of the full anacrusis measure, if any exists.
	 * @param noteLengthTacti The length and tacti of the note.
	 */
	private void updateMatchType(int startTactus, int noteLengthTacti) {
		int beatLength = subBeatLength * measure.getSubBeatsPerBeat();
		int measureLength = beatLength * measure.getBeatsPerMeasure();
		
		int subBeatOffset = startTactus % subBeatLength;
		int beatOffset = startTactus % beatLength;
		int measureOffset = startTactus % measureLength;
		
		if (matches(MetricalLpcfgMatch.SUB_BEAT)) {
			// Matches sub beat (and not beat)
			
			if (noteLengthTacti < subBeatLength) {
				// Note is shorter than a sub beat
				
			} else if (noteLengthTacti == subBeatLength) {
				// Note is exactly a sub beat
				
			} else if (noteLengthTacti < beatLength) {
				// Note is between a sub beat and a beat in length
				
				// Can only happen when the beat is divided in 3, but this is 2 sub beats
				addMatch(MetricalLpcfgMatch.WRONG);
				
			} else if (noteLengthTacti == beatLength) {
				// Note is exactly a beat in length
				
				// Must match exactly
				addMatch(beatOffset == 0 ? MetricalLpcfgMatch.BEAT : MetricalLpcfgMatch.WRONG);
				
			} else {
				// Note is greater than a beat in length
				
				if (noteLengthTacti % beatLength != 0) {
					// Not some multiple of the beat length
					addMatch(MetricalLpcfgMatch.WRONG);	
				}
			}
			
		} else if (matches(MetricalLpcfgMatch.BEAT)) {
			// Matches beat (and not sub beat)
			
			if (noteLengthTacti < subBeatLength) {
				// Note is shorter than a sub beat
				
				if (subBeatLength % noteLengthTacti != 0 || subBeatOffset % noteLengthTacti != 0) {
					// Note doesn't divide sub beat evenly
					addMatch(MetricalLpcfgMatch.WRONG);
				}
				
			} else if (noteLengthTacti == subBeatLength) {
				// Note is exactly a sub beat
				
				// Must match sub beat exactly
				addMatch(subBeatOffset == 0 ? MetricalLpcfgMatch.SUB_BEAT : MetricalLpcfgMatch.WRONG);
				
			} else if (noteLengthTacti < beatLength) {
				// Note is between a sub beat and a beat in length
				
				// Wrong if not aligned with beat at onset or offset
				if (beatOffset != 0 && beatOffset + noteLengthTacti != beatLength) {
					addMatch(MetricalLpcfgMatch.WRONG);
				}
				
			} else if (noteLengthTacti == beatLength) {
				// Note is exactly a beat in length
				
			} else {
				// Note is longer than a beat in length
				
			}
			
		} else {
			// Matches neither sub beat nor beat
			
			if (noteLengthTacti < subBeatLength) {
				// Note is shorter than a sub beat
				
				if (subBeatLength % noteLengthTacti != 0 || subBeatOffset % noteLengthTacti != 0) {
					// Note doesn't divide sub beat evenly
					addMatch(MetricalLpcfgMatch.WRONG);
				}
				
			} else if (noteLengthTacti == subBeatLength) {
				// Note is exactly a sub beat
				
				// Must match sub beat exactly
				addMatch(subBeatOffset == 0 ? MetricalLpcfgMatch.SUB_BEAT : MetricalLpcfgMatch.WRONG);
				
			} else if (noteLengthTacti < beatLength) {
				// Note is between a sub beat and a beat in length
				
				// Wrong if not aligned with beat at onset or offset
				if (beatOffset != 0 && beatOffset + noteLengthTacti != beatLength) {
					addMatch(MetricalLpcfgMatch.WRONG);
				}
				
			} else if (noteLengthTacti == beatLength) {
				// Note is exactly a beat in length
				
				// Must match beat exactly
				addMatch(beatOffset == 0 ? MetricalLpcfgMatch.BEAT : MetricalLpcfgMatch.WRONG);
				
			} else {
				// Note is greater than a beat in length
				
				if (measureLength % noteLengthTacti != 0 || measureOffset % noteLengthTacti != 0 ||
						beatOffset != 0 || noteLengthTacti % beatLength != 0) {
					// Note doesn't divide measure evenly, or doesn't match a beat
					addMatch(MetricalLpcfgMatch.WRONG);
				}
			}
		}
	}
	
	/**
	 * Return whether this model has been fully matched yet. That is, whether it has been
	 * matched at both the beat level and the sub beat level.
	 * 
	 * @return True if this model has been fully matched. False otherwise.
	 */
	private boolean isFullyMatched() {
		return subBeatMatches > 0 && beatMatches > 0;
	}
	
	/**
	 * Return whether this model has been designated as wrong yet.
	 * 
	 * @return True if this model is wrong. False otherwise.
	 */
	private boolean isWrong() {
		return wrongMatches >= 5;
	}
	
	/**
	 * Add the given matchType to this model.
	 * 
	 * @param matchType The match type we want to add to this model.
	 */
	private void addMatch(MetricalLpcfgMatch matchType) {
		switch (matchType) {
			case SUB_BEAT:
				subBeatMatches++;
				break;
			
			case BEAT:
				beatMatches++;
				break;
				
			case WRONG:
				wrongMatches++;
		}
	}
	
	/**
	 * Check if this model matches the given match type.
	 * 
	 * @param matchType The match type we want to check our model against.
	 * @return True if our model matches the given match type. False otherwise.
	 */
	private boolean matches(MetricalLpcfgMatch matchType) {
		switch (matchType) {
			case SUB_BEAT:
				return subBeatMatches > 0;
				
			case BEAT:
				return beatMatches > 0;
				
			case WRONG:
				return isWrong();
		}
		
		return false;
	}
	
	/**
	 * Get the terminal length of this model.
	 * 
	 * @return {@link #subBeatLength}
	 */
	public int getSubBeatLength() {
		return subBeatLength;
	}

	/**
	 * Get the anacrusis length of this model.
	 * 
	 * @return {@link #anacrusisLength}
	 */
	public int getAnacrusisLength() {
		return anacrusisLength;
	}
	
	/**
	 * Get the local grammar of this model.
	 * 
	 * @return {@link #localGrammar}
	 */
	public MetricalLpcfg getLocalGrammar() {
		return localGrammar;
	}
	
	@Override
	public Measure getMetricalMeasure() {
		return measure;
	}

	@Override
	public MetricalLpcfgMetricalModelState deepCopy() {
		return new MetricalLpcfgMetricalModelState(this);
	}

	@Override
	public double getScore() {
		return logProbability;
	}

	@Override
	public int compareTo(MetricalLpcfgMetricalModelState o) {
		if (o == null) {
			return -1;
		}
		
		int result = Double.compare(o.getScore(), getScore());
		if (result != 0) {
			return (o.getScore() == 0.0 || getScore() == 0.0) ? -result : result;
		}
		
		result = Integer.compare(subBeatLength, o.subBeatLength);
		if (result != 0) {
			return result;
		}
		
		result = Integer.compare(anacrusisLength, o.anacrusisLength);
		if (result != 0) {
			return result;
		}
		
		if (measure != null) {
			result = measure.compareTo(o.measure);
			if (result != 0) {
				return result;
			}
		}
		
		if (voiceState == o.voiceState && beatState == o.beatState) {
			return 0;
		}
		return 1;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(measure).append(" length=").append(subBeatLength).append(" anacrusis=").append(anacrusisLength);
		sb.append(" Score=").append(logProbability);
		
		return sb.toString();
	}
}
