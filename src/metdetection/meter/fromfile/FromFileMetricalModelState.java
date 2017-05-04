package metdetection.meter.fromfile;

import java.util.List;
import java.util.TreeSet;

import metdetection.meter.MetricalModelState;
import metdetection.meter.Measure;
import metdetection.time.TimeTracker;
import metdetection.utils.MidiNote;

/**
 * A <code>FromFileHierarchyModelState</code> grabs the correct metrical structure from the MIDI file and
 * uses that to construct the proper meter.
 * 
 * @author Andrew McLeod - 2 Dec, 2015
 */
public class FromFileMetricalModelState extends MetricalModelState implements Comparable<FromFileMetricalModelState> {
	/**
	 * The Measure of this metrical structure.
	 */
	private Measure measure;
	
	/**
	 * The time of the most recent note onset we've seen so far.
	 */
	private long mostRecentTime;
	
	/**
	 * Create a new FromFileBeatHierarchyState based on the given TimeTracker.
	 * 
	 * @param tt The TimeTracker which we will grab the proper meter from.
	 */
	public FromFileMetricalModelState(TimeTracker tt) {
		mostRecentTime = 0;
		measure = tt.getFirstTimeSignature().getMetricalMeasure();
	}
	
	/**
	 * Create a new FromFileHierarchyModelState and initialize the fileds as given. This is private
	 * as it is only used to clone this object from within the {@link #handleIncoming(List)} method.
	 * 
	 * @param measure {@link #measure}
	 * @param mostRecentTime {@link #mostRecentTime}
	 */
	private FromFileMetricalModelState(Measure measure, long mostRecentTime) {
		this.measure = measure;
		this.mostRecentTime = mostRecentTime;
	}

	@Override
	public Measure getMetricalMeasure() {
		return measure;
	}

	@Override
	public TreeSet<FromFileMetricalModelState> handleIncoming(List<MidiNote> notes) {
		TreeSet<FromFileMetricalModelState> newState = new TreeSet<FromFileMetricalModelState>();
		
		mostRecentTime = notes.get(0).getOnsetTime();
		newState.add(this);
		
		return newState;
	}
	
	@Override
	public TreeSet<FromFileMetricalModelState> close() {
		TreeSet<FromFileMetricalModelState> newState = new TreeSet<FromFileMetricalModelState>();
		
		newState.add(this);
		
		return newState;
	}
	
	@Override
	public MetricalModelState deepCopy() {
		return new FromFileMetricalModelState(measure, mostRecentTime);
	}

	@Override
	public double getScore() {
		return 1.0;
	}
	
	@Override
	public int compareTo(FromFileMetricalModelState o) {
		if (o == null) {
			return -1;
		}
		
		int result = Long.compare(mostRecentTime, o.mostRecentTime);
		if (result != 0) {
			return result;
		}
		
		result = measure.compareTo(o.measure);
		if (result != 0) {
			return result;
		}
		
		if (voiceState == o.voiceState && beatState == o.beatState) {
			return 0;
		}
		return 1;
	}
}
