package metdetection.voice.fromfile;

import java.util.List;
import java.util.TreeSet;

import javax.sound.midi.InvalidMidiDataException;

import metdetection.parsing.EventParser;
import metdetection.parsing.MidiEventParser;
import metdetection.utils.MidiNote;
import metdetection.voice.VoiceSplittingModel;
import metdetection.voice.VoiceSplittingModelState;

/**
 * A <code>FromFileVoiceSplittingModel</code> generates only a single {@link VoiceSplittingModelState}, the one
 * directly generated from the underlying MIDI using the {@link MidiEventParser#getGoldStandardVoices()} method.
 * 
 * @author Andrew McLeod - 2 Dec, 2015
 */
public class FromFileVoiceSplittingModel extends VoiceSplittingModel {
	/**
	 * The one state of this model, generated directly from the MIDI data.
	 */
	private FromFileVoiceSplittingModelState state;
	
	/**
	 * Generate the state of this model from the given EventParser's {@link EventParser#getGoldStandardVoices()}
	 * method.
	 * 
	 * @param ep The EventParser we will get gold standard voices from.
	 * @throws InvalidMidiDataException 
	 */
	public FromFileVoiceSplittingModel(EventParser ep) throws InvalidMidiDataException {
		state = new FromFileVoiceSplittingModelState(ep);
	}

	@Override
	public TreeSet<FromFileVoiceSplittingModelState> getHypotheses() {
		TreeSet<FromFileVoiceSplittingModelState> stateSet = new TreeSet<FromFileVoiceSplittingModelState>();
		stateSet.add(state);
		return stateSet;
	}

	@Override
	public void handleIncoming(List<MidiNote> notes) {
		state = state.handleIncoming(notes).first();
	}
	
	@Override
	public void close() {
		state = state.close().first();
	}
}
