package metdetection.meter;

import java.util.List;
import java.util.TreeSet;

import metdetection.beat.BeatTrackingModelState;
import metdetection.generic.MidiModelState;
import metdetection.utils.MidiNote;
import metdetection.voice.VoiceSplittingModelState;

/**
 * A <code>MetricalModelState</code> is used in meter detection, and
 * contains some representation of a song's meter.
 * 
 * @author Andrew McLeod - 8 Sept, 2015
 */
public abstract class MetricalModelState extends MidiModelState {
	/**
	 * The VoiceSplittingModelState which these hierarchies will be based on.
	 */
	protected VoiceSplittingModelState voiceState;
	
	/**
	 * The BeatTrackingModelState which we are modeling jointly with.
	 */
	protected BeatTrackingModelState beatState;
	
	/**
	 * Gets the Measure which is contained by this state currently.
	 * 
	 * @return The measure contained by this state.
	 */
	public abstract Measure getMetricalMeasure();
	
	@Override
	public abstract TreeSet<? extends MetricalModelState> handleIncoming(List<MidiNote> notes);
	
	@Override
	public abstract TreeSet<? extends MetricalModelState> close();
	
	/**
	 * Set the BeatTrackingModelState which this HierarchyModelState is to be based on.
	 * 
	 * @param beatState {@link MetricalModelState#beatState}
	 */
	public void setBeatState(BeatTrackingModelState beatState) {
		this.beatState = beatState;
	}
	
	/**
	 * Get the VoiceSplittingModelState which this hierarchy is based on.
	 * 
	 * @return {@link #voiceState}
	 */
	public VoiceSplittingModelState getVoiceState() {
		return voiceState;
	}
	
	/**
	 * Get the BeatTrackingModelState which this hierarchy is based on.
	 * 
	 * @return {@link #beatState}
	 */
	public BeatTrackingModelState getBeatState() {
		return beatState;
	}
	
	/**
	 * Set the VoiceSplittingModelState which this HierarchyModelState is to be based on.
	 * 
	 * @param voiceState {@link MetricalModelState#voiceState}
	 */
	public void setVoiceState(VoiceSplittingModelState voiceState) {
		this.voiceState = voiceState;
	}
	
	/**
	 * Create a deep copy of this HierarchyModelState.
	 * 
	 * @return A deep copy of this HierarchyModelState.
	 */
	public abstract MetricalModelState deepCopy();
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		sb.append(getMetricalMeasure()).append(']');
		return sb.toString();
	}
}
