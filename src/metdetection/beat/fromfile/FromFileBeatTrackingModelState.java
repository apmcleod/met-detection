package metdetection.beat.fromfile;

import java.util.List;
import java.util.TreeSet;

import metdetection.beat.Beat;
import metdetection.beat.BeatTrackingModelState;
import metdetection.time.TimeSignature;
import metdetection.time.TimeTracker;
import metdetection.utils.MidiNote;

/**
 * A <code>FromFileBeatTrackingModelState</code> generates a list of {@link Beat}s, the one
 * directly generated from the underlying MIDI. This is generated from the {@link TimeTracker} passed in
 * through the constructor.
 * 
 * @author Andrew McLeod - 3 Sept, 2015
 */
public class FromFileBeatTrackingModelState extends BeatTrackingModelState implements Comparable<FromFileBeatTrackingModelState> {

	/**
	 * A List of the Beats generated from the given TimeTracker.
	 */
	private final List<Beat> beats;
	
	/**
	 * The index of the current Beat in our Beats list.
	 */
	private int mostRecentIndex;
	
	/**
	 * The most recent time for which we have seen a note offset so far, initially 0.
	 */
	private long mostRecentTime;
	
	/**
	 * The TimeTracker for this song.
	 */
	private TimeTracker timeTracker;
	
	/**
	 * Creates a new object, generating the {@link #beats} list directly from the given
	 * TimeTracker.
	 * 
	 * @param timeTracker The TimeTracker which will generate {@link #beats}.
	 */
	public FromFileBeatTrackingModelState(TimeTracker timeTracker) {
		beats = timeTracker.getBeats();
		this.timeTracker = timeTracker;
		mostRecentTime = 0;
		mostRecentIndex = 0;
	}

	/**
	 * Create a new state, a copy of the given one.
	 * 
	 * @param state The state whose copy we want.
	 */
	private FromFileBeatTrackingModelState(FromFileBeatTrackingModelState state) {
		voiceState = state.voiceState;
		beats = state.beats;
		timeTracker = state.timeTracker;
		mostRecentTime = state.mostRecentTime;
		mostRecentIndex = state.mostRecentIndex;
	}
	
	@Override
	public int getTactiPerMeasure() {
		TimeSignature timeSignature = mostRecentTime == 0 ? timeTracker.getFirstTimeSignature()
				: timeTracker.getNodeAtTime(mostRecentTime).getTimeSignature();
		
		return timeSignature.getNotes32PerBar();
	}

	@Override
	public List<Beat> getBeats() {
		// Get Beats only up to those we are supposed to have seen so far
		for (; mostRecentIndex < beats.size(); mostRecentIndex++) {
			if (beats.get(mostRecentIndex).getTime() > mostRecentTime) {
				return beats.subList(0, mostRecentIndex);
			}
		}
		
		return beats;
	}

	@Override
	public double getScore() {
		return 0.0;
	}

	@Override
	public TreeSet<FromFileBeatTrackingModelState> handleIncoming(List<MidiNote> notes) {
		TreeSet<FromFileBeatTrackingModelState> newState = new TreeSet<FromFileBeatTrackingModelState>();
		
		mostRecentTime = notes.get(0).getOnsetTime();
		newState.add(this);
		
		return newState;
	}
	
	@Override
	public TreeSet<FromFileBeatTrackingModelState> close() {
		TreeSet<FromFileBeatTrackingModelState> newState = new TreeSet<FromFileBeatTrackingModelState>();
		
		mostRecentIndex = beats.size();
		mostRecentTime = beats.get(beats.size() - 1).getTime();
		newState.add(this);
		
		return newState;
	}
	
	@Override
	public BeatTrackingModelState deepCopy() {
		return new FromFileBeatTrackingModelState(this);
	}

	@Override
	public int compareTo(FromFileBeatTrackingModelState o) {
		if (o == null) {
			return -1;
		}
		
		int result = Double.compare(o.getScore() + o.voiceState.getScore(), getScore() + voiceState.getScore());
		if (result != 0) {
			return result;
		}
		
		result = beats.size() - o.beats.size();
		if (result != 0) {
			return result;
		}
		
		for (int i = 0; i < beats.size(); i++) {
			result = beats.get(i).compareTo(o.beats.get(i));
			if (result != 0) {
				return result;
			}
		}
		
		if (voiceState == o.voiceState) {
			return 0;
		}
		return 1;
	}
}
