package metdetection.parsing;

import metdetection.time.TimeSignature;
import metdetection.time.TimeTracker;
import metdetection.utils.MidiNote;

/**
 * A <code>NoteFileWriter</code> is used to generate note files, as used by
 * Temperley's model. These are files of the format:
 * <p>
 * Note 0 1000 60
 * </p>
 * Where that line would represent a middle C which starts at time 0 and ends at
 * time 1000 (measured in milliseconds). The resulting output will be shifted
 * such that any anacrusis is now built into the file. That is, the first measure
 * is extended with rests so that it becomes a full measure.
 * <p>
 * It does not actually write this out to a File. Rather, it returns the String
 * via it's toString method. Thus, it can be used to either write to a File or to
 * print out to std out, like so:
 * <p>
 * <code>
 * System.out.println(new NoteFileWriter(tt, nlg));
 * </code>
 * 
 * @author Andrew McLeod - 10 September, 2016
 */
public class NoteFileWriter {
	/**
	 * The offset to add to each note, in milliseconds, to adjust for the anacrusis.
	 */
	private int offsetLength;
	
	/**
	 * The NoteListGenerator which contains the notes we want to write out.
	 */
	private NoteListGenerator nlg;
	
	/**
	 * Create a new NoteFileWriter with the given TimeTracker and NoteListGenerator.
	 * 
	 * @param tt The TimeTracker to use to get the correct anacrusis {@link #offsetLength}.
	 * @param nlg The NoteListGenerator containing the notes we want to write out.
	 */
	public NoteFileWriter(TimeTracker tt, NoteListGenerator nlg) {
		offsetLength = 0;
		if (tt.getAnacrusisTicks() != 0 && tt.getFirstTimeSignature().getNumerator() != TimeSignature.IRREGULAR_NUMERATOR) {
			offsetLength = tt.getFirstTimeSignature().getNotes32PerBar() * tt.getFirstTimeSignature().getNotes32PerBar() * ((int) tt.getPPQ() / 8);
			offsetLength -= tt.getAnacrusisTicks();
			
			// Convert to time and milliseconds
			offsetLength *= tt.getNodeAtTime(0L).getTimePerTick(tt.getPPQ()) / 1000;
		}
		
		this.nlg = nlg;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (MidiNote note : nlg.getNoteList()) {
			long onTime = note.getOnsetTime() / 1000 + offsetLength;
			long offTime = note.getOffsetTime() / 1000 + offsetLength;
			
			sb.append("Note ");
			sb.append(onTime).append(' ');
			sb.append(offTime).append(' ');
			sb.append(note.getPitch()).append('\n');
		}
		
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}
}
