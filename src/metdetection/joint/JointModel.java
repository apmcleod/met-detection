package metdetection.joint;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import metdetection.Main;
import metdetection.beat.BeatTrackingModelState;
import metdetection.generic.MidiModel;
import metdetection.meter.MetricalModelState;
import metdetection.utils.MidiNote;
import metdetection.voice.VoiceSplittingModelState;

/**
 * A <code>JointModel</code> is used to model and infer one of each type of model simultaneously as
 * a set, one step at a time.
 * 
 * @author Andrew McLeod - 8 Sept, 2015
 */
public class JointModel extends MidiModel {
	
	/**
	 * The hypothesis states at the current stage.
	 */
	private TreeSet<JointModelState> hypothesisStates;
	
	/**
	 * Create a new JointModel based on a state with the given constituent states.
	 * 
	 * @param voice The voice splitting state to use.
	 * @param beat The beat tracking state to use.
	 * @param hierarchy The hierarchy detection state to use.
	 */
	public JointModel(VoiceSplittingModelState voice, BeatTrackingModelState beat, MetricalModelState hierarchy) {
		hypothesisStates = new TreeSet<JointModelState>();
		hypothesisStates.add(new JointModelState(voice, beat, hierarchy));
	}

	@Override
	public void handleIncoming(List<MidiNote> notes) {
		TreeSet<JointModelState> newStates = new TreeSet<JointModelState>();
		
		for (JointModelState jms : hypothesisStates) {
			newStates.addAll(jms.handleIncoming(notes));
		}
		
		if (Main.VERBOSE || (Main.VERBOSE && Main.TESTING)) {
			System.out.println(notes + ": ");
			for (JointModelState jms : newStates) {
				System.out.println(jms.getVoiceState());
				System.out.println(jms.getBeatState());
				System.out.println(jms.getHierarchyState());
			}
			System.out.println();
		}
		
		hypothesisStates = newStates;
	}
	
	@Override
	public void close() {
		TreeSet<JointModelState> newStates = new TreeSet<JointModelState>();
		
		for (JointModelState jms : hypothesisStates) {
			newStates.addAll(jms.close());
		}
		
		if (Main.VERBOSE || (Main.VERBOSE && Main.TESTING)) {
			System.out.println("CLOSE: ");
			for (JointModelState jms : newStates) {
				System.out.println(jms.getVoiceState());
				System.out.println(jms.getBeatState());
				System.out.println(jms.getHierarchyState());
			}
			System.out.println();
		}
		
		hypothesisStates = newStates;
	}

	@Override
	public TreeSet<JointModelState> getHypotheses() {
		return hypothesisStates;
	}
	
	/**
	 * Get an ordered List of the {@link VoiceSplittingModelState}s which are currently the top hypotheses
	 * for this joint model. These may not be sorted in order by their own scores, but they are given in
	 * order of the underlying {@link JointModelState}'s scores.
	 * 
	 * @return An ordered List of the {@link VoiceSplittingModelState}s which are currently the top hypotheses
	 * for this joint model.
	 */
	public List<? extends VoiceSplittingModelState> getVoiceHypotheses() {
		List<VoiceSplittingModelState> voiceHypotheses = new ArrayList<VoiceSplittingModelState>(hypothesisStates.size());
		
		for (JointModelState jointState : hypothesisStates) {
			voiceHypotheses.add(jointState.getVoiceState());
		}
		
		return voiceHypotheses;
	}
	
	/**
	 * Get an ordered List of the {@link BeatTrackingModelState}s which are currently the top hypotheses
	 * for this joint model. These may not be sorted in order by their own scores, but they are given in
	 * order of the underlying {@link JointModelState}'s scores.
	 * 
	 * @return An ordered List of the {@link BeatTrackingModelState}s which are currently the top hypotheses
	 * for this joint model.
	 */
	public List<? extends BeatTrackingModelState> getBeatHypotheses() {
		List<BeatTrackingModelState> beatHypotheses = new ArrayList<BeatTrackingModelState>(hypothesisStates.size());
		
		for (JointModelState jointState : hypothesisStates) {
			beatHypotheses.add(jointState.getBeatState());
		}
		
		return beatHypotheses;
	}
	
	/**
	 * Get an ordered List of the {@link MetricalModelState}s which are currently the top hypotheses
	 * for this joint model. These may not be sorted in order by their own scores, but they are given in
	 * order of the underlying {@link JointModelState}'s scores.
	 * 
	 * @return An ordered List of the {@link MetricalModelState}s which are currently the top hypotheses
	 * for this joint model.
	 */
	public List<? extends MetricalModelState> getHierarchyHypotheses() {
		List<MetricalModelState> hierarchyHypotheses = new ArrayList<MetricalModelState>(hypothesisStates.size());
		
		for (JointModelState jointState : hypothesisStates) {
			hierarchyHypotheses.add(jointState.getHierarchyState());
		}
		
		return hierarchyHypotheses;
	}
}
