package metdetection.parsing;

import javax.sound.midi.InvalidMidiDataException;

import metdetection.utils.MidiNote;

/**
 * A <code>DummyNoteEventParser</code> is used to save memory when we don't really care about
 * the actual notes being parsed. Since an EventParser needs some NoteEventParser,
 * we give it this one, which does nothing.
 * 
 * @author Andrew McLeod - 6 October, 2016
 */
public class DummyNoteEventParser implements NoteEventParser {

	@Override
	public MidiNote noteOn(int key, int velocity, long tick, int channel) {
		return new MidiNote(key, velocity, tick, tick, channel, channel);
	}

	@Override
	public void noteOff(int key, long tick, int channel) throws InvalidMidiDataException {}
}
