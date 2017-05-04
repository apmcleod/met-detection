package metdetection.joint;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import metdetection.beat.BeatTrackingModelState;
import metdetection.generic.MidiModelState;
import metdetection.meter.MetricalModelState;
import metdetection.utils.MidiNote;
import metdetection.voice.VoiceSplittingModelState;

/**
 * A <code>JointModelState</code> is the state used to model and infer one of each type of model
 * simultaneously as a set, one step at a time.
 * 
 * @author Andrew McLeod - 8 Sept, 2015
 */
public class JointModelState extends MidiModelState implements Comparable<JointModelState> {
	
	/**
	 * The VoiceSplittingModel to use in our joint model.
	 */
	private VoiceSplittingModelState voiceState;
	
	/**
	 * The BeatTrackingModel to use in our joint model.
	 */
	private BeatTrackingModelState beatState;
	
	/**
	 * The HierarchyModel to use in our joint model.
	 */
	private MetricalModelState hierarchyState;
	
	/**
	 * Create a new JointModelState with the given constituent states.
	 * 
	 * @param voice {@link #voiceState}
	 * @param beat {@link #beatState}
	 * @param hierarchy {@link #hierarchyState}
	 */
	public JointModelState(VoiceSplittingModelState voice, BeatTrackingModelState beat, MetricalModelState hierarchy) {
		voiceState = voice;
		
		beatState = beat;
		beatState.setVoiceState(voiceState);
		
		hierarchyState = hierarchy;
		hierarchyState.setVoiceState(voiceState);
		hierarchyState.setBeatState(beatState);
	}

	/**
	 * Create a new ModelState on the given HierarchyModelState. The {@link #voiceState} and the
	 * {@link #beatState} will be loaded from the given HierarchyModelState.
	 * 
	 * @param h {@link #hierarchyState}
	 */
	public JointModelState(MetricalModelState h) {
		voiceState = h.getVoiceState();
		beatState = h.getBeatState();
		hierarchyState = h;
	}

	@Override
	public double getScore() {
		return voiceState.getScore() + beatState.getScore() + hierarchyState.getScore();
	}

	@Override
	public TreeSet<JointModelState> handleIncoming(List<MidiNote> notes) {
		TreeSet<JointModelState> newStates = new TreeSet<JointModelState>();
		
		// Voice states
		List<VoiceSplittingModelState> newVoiceStates = new ArrayList<VoiceSplittingModelState>(voiceState.handleIncoming(notes));
		
		// Beat states
		List<BeatTrackingModelState> newBeatStates = new ArrayList<BeatTrackingModelState>();
		for (VoiceSplittingModelState voiceState : newVoiceStates) {
			BeatTrackingModelState beatStateCopy = beatState.deepCopy();
			beatStateCopy.setVoiceState(voiceState);
			newBeatStates.addAll(beatStateCopy.handleIncoming(notes));
		}
		
		// Hierarchy states
		List<MetricalModelState> newHierarchyStates = new ArrayList<MetricalModelState>();
		for (BeatTrackingModelState beatState : newBeatStates) {
			MetricalModelState hierarchyStateCopy = hierarchyState.deepCopy();
			hierarchyStateCopy.setVoiceState(beatState.getVoiceState());
			hierarchyStateCopy.setBeatState(beatState);
			newHierarchyStates.addAll(hierarchyStateCopy.handleIncoming(notes));
		}
			
		// Joint states
		for (MetricalModelState newHierarchyState : newHierarchyStates) {
			newStates.add(new JointModelState(newHierarchyState));
		}
				
		return newStates;
	}
	
	@Override
	public TreeSet<JointModelState> close() {
		TreeSet<JointModelState> newStates = new TreeSet<JointModelState>();
		
		// Voice states
		List<VoiceSplittingModelState> newVoiceStates = new ArrayList<VoiceSplittingModelState>(voiceState.close());
		
		// Beat states
		List<BeatTrackingModelState> newBeatStates = new ArrayList<BeatTrackingModelState>();
		for (VoiceSplittingModelState voiceState : newVoiceStates) {
			BeatTrackingModelState beatStateCopy = beatState.deepCopy();
			beatStateCopy.setVoiceState(voiceState);
			newBeatStates.addAll(beatStateCopy.close());
		}
		
		// Hierarchy states
		List<MetricalModelState> newHierarchyStates = new ArrayList<MetricalModelState>();
		for (BeatTrackingModelState beatState : newBeatStates) {
			MetricalModelState hierarchyStateCopy = hierarchyState.deepCopy();
			hierarchyStateCopy.setVoiceState(beatState.getVoiceState());
			hierarchyStateCopy.setBeatState(beatState);
			newHierarchyStates.addAll(hierarchyStateCopy.close());
		}
			
		// Joint states
		for (MetricalModelState newHierarchyState : newHierarchyStates) {
			newStates.add(new JointModelState(newHierarchyState));
		}
				
		return newStates;
	}
	
	/**
	 * Get the VoiceSplittingModelState currently in this JointModelState.
	 * 
	 * @return {@link #voiceState}
	 */
	public VoiceSplittingModelState getVoiceState() {
		return voiceState;
	}
	
	/**
	 * Get the BeatTrackingModelState currently in this JointModelState.
	 * 
	 * @return {@link #beatState}
	 */
	public BeatTrackingModelState getBeatState() {
		return beatState;
	}
	
	/**
	 * Get the HierarchyModelState currently in this JointModelState.
	 * 
	 * @return {@link #hierarchyState}
	 */
	public MetricalModelState getHierarchyState() {
		return hierarchyState;
	}

	@Override
	public int compareTo(JointModelState o) {
		if (o == null) {
			return 1;
		}
		
		// Larger scores first
		int result = Double.compare(o.getScore(), getScore());
		if (result != 0) {
			return (o.getScore() == 0.0 || getScore() == 0.0) ? -result : result;
		}
		
		result = Double.compare(voiceState.getScore(), o.voiceState.getScore());
		if (result != 0) {
			return result;
		}
		
		result = Double.compare(beatState.getScore(), o.beatState.getScore());
		if (result != 0) {
			return result;
		}
		
		result = Double.compare(hierarchyState.getScore(), o.hierarchyState.getScore());
		if (result != 0) {
			return result;
		}
		
		if (voiceState == o.voiceState && beatState == o.beatState && hierarchyState == o.hierarchyState) {
			return 0;
		}
		
		return 1;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		
		sb.append(voiceState).append(";");
		sb.append(beatState).append(";");
		sb.append(hierarchyState).append("}");
		
		sb.append('=').append(getScore());
		
		return sb.toString();
	}
}
