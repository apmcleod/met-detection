package metdetection.parsing;

import javax.sound.midi.InvalidMidiDataException;

import metdetection.time.TimeTracker;
import metdetection.utils.MidiNote;

/**
 * A <code>QuantisationMeter</code> is able to measure the quantisation level of a given piece.
 * It parses note on events, and measures each note as it occurs, thus saving the memory
 * space of actually having to load the notes in to memory.
 * 
 * @author Andrew McLeod - 6 October, 2016
 */
public class QuantisationMeter implements NoteEventParser {
	
	/**
	 * The time tracker to use to measure quantisation.
	 */
	private TimeTracker timeTracker;
	
	/**
	 * The total quantisation error of the notes we've seen so far.
	 */
	private double quantisationErrorTotal;
	
	/**
	 * The number of notes we've measured so far.
	 */
	private int numMeasuredNotes;
	
	/**
	 * The number of divisions per quarter note to use when measuring.
	 */
	private int divisions;
	
	/**
	 * Create a new quantisation meter with the given TimeTracker and number of divisions
	 * per quarter note.
	 * 
	 * @param tt {@link #timeTracker}
	 * @param divisions {@link #divisions}
	 */
	public QuantisationMeter (TimeTracker tt, int divisions) {
		timeTracker = tt;
		quantisationErrorTotal = 0.0;
		numMeasuredNotes = 0;
		this.divisions = divisions;
	}
	
	@Override
	public MidiNote noteOn(int key, int velocity, long tick, int channel) {
		long time = timeTracker.getTimeAtTick(tick);
		
		MidiNote note = new MidiNote(key, velocity, time, tick, channel, -1);
		
		quantisationErrorTotal += timeTracker.getQuantizationError(note, divisions);
		numMeasuredNotes++;
		
		return note;
	}

	@Override
	public void noteOff(int key, long tick, int channel) throws InvalidMidiDataException {
		// No implementation needed - quantisation only depends on onsets.
	}

	/**
	 * Get the average quantisation error out of the notes we've seen so far.
	 * 
	 * @return The average quantisation error over all of the notes we've seen so far.
	 */
	public double getQuantisationError() {
		return quantisationErrorTotal / numMeasuredNotes;
	}
}
