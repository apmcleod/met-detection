package metdetection.beat;

import java.util.List;
import java.util.TreeSet;

import metdetection.generic.MidiModelState;
import metdetection.utils.MidiNote;
import metdetection.voice.VoiceSplittingModelState;

/**
 * A <code>BeatTrackingModelState</code> is a {@link MidiModelState} which contains
 * a List of {@link Beat}s which has been created from the incoming MIDI data. In
 * order to get these beats, the {@link #getBeats()} method should be called.
 * 
 * @author Andrew McLeod - 7 Sept, 2015
 */
public abstract class BeatTrackingModelState extends MidiModelState {
	/**
	 * The VoiceSplittingModelState which these hierarchies will be based on.
	 */
	protected VoiceSplittingModelState voiceState;
	
	/**
	 * Set the VoiceSplittingModelState which this JointBeatTrackingModelState is to be
	 * based on.
	 * 
	 * @param voiceState {@link BeatTrackingModelState#voiceState}
	 */
	public void setVoiceState(VoiceSplittingModelState voiceState) {
		this.voiceState = voiceState;
	}

	/**
	 * Get the VoiceSplittingModelState which this beat tracker is based on.
	 * 
	 * @return {@link #voiceState}
	 */
	public VoiceSplittingModelState getVoiceState() {
		return voiceState;
	}
	
	/**
	 * Gets the Beats which are contained by this state currently.
	 * 
	 * @return A List of the Beats contained by this State.
	 */
	public abstract List<Beat> getBeats();
	
	/**
	 * Get the number of tacti which are present in each measure of the Beats of this state.
	 * This is needed because for a {@link metdetection.beat.fromfile.FromFileBeatTrackingModelState},
	 * the measures are incremented correctly, but for any other BeatTrackingModelState, this is not the case.
	 * 
	 * @return The number of tacti per measure for a {@link metdetection.beat.fromfile.FromFileBeatTrackingModelState},
	 * or 0 for any other model.
	 */
	public int getTactiPerMeasure() {
		return 0;
	}
	
	/**
	 * Create a deep copy of this BeatTrackingModelState.
	 * 
	 * @return A deep copy of this BeatTrackingModelState.
	 */
	public abstract BeatTrackingModelState deepCopy();
	
	@Override
	public abstract TreeSet<? extends BeatTrackingModelState> handleIncoming(List<MidiNote> notes);
	
	@Override
	public abstract TreeSet<? extends BeatTrackingModelState> close();
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		
		for (Beat beat : getBeats()) {
			sb.append(beat).append(',');
		}
		
		sb.setCharAt(sb.length() - 1, ']');
		
		sb.append(' ').append(getScore());
		return sb.toString();
	}
}
