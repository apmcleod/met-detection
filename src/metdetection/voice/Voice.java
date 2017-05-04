package metdetection.voice;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import metdetection.utils.MidiNote;

/**
 * A <code>Voice</code> is a node in the LinkedList representing a
 * voice. Each node has only a previous pointer and a {@link MidiNote}.
 * Only a previous pointer is needed because we allow for Voices to split and clone themselves,
 * keeping the beginning of their note sequences identical. This allows us to have multiple
 * LinkedLists of notes without needing multiple full List objects. Rather, they all point
 * back to their common prefix LinkedLists.
 * 
 * @author Andrew McLeod - 6 April, 2015
 */
public class Voice implements Comparable<Voice> {
	/**
	 * The Voice ending at second to last note in this voice.
	 */
	private final Voice previous;
	
	/**
	 * The most recent note of this voice.
	 */
	private final MidiNote mostRecentNote;
	
	/**
	 * Create a new Voice with the given previous voice.
	 * 
	 * @param note {@link #mostRecentNote}
	 * @param prev {@link #previous}
	 */
	public Voice(MidiNote note, Voice prev) {
		previous = prev;
		mostRecentNote = note;
	}
	
	/**
	 * Create a new Voice.
	 * 
	 * @param note {@link #mostRecentNote}
	 */
	public Voice(MidiNote note) {
		this(note, null);
	}

	/**
	 * Get the number of notes we've correctly grouped into this voice, based on the most common voice in the voice.
	 * 
	 * @return The number of notes we've assigned into this voice correctly.
	 */
	public int getNumNotesCorrect() {
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		
		for (Voice noteNode = this; noteNode != null; noteNode = noteNode.previous) {
			int channel = noteNode.mostRecentNote.getCorrectVoice();
			if (!counts.containsKey(channel)) {
				counts.put(channel, 0);
			}
				
			counts.put(channel, counts.get(channel) + 1);
		}
				
		int maxCount = -1;
		for (int count : counts.values()) {
			maxCount = Math.max(maxCount, count);
		}
		
		return maxCount;
	}
	
	/**
	 * Get the number of links in this Voice which are correct. That is, the number of times
	 * that two consecutive notes belong to the same midi channel.
	 * 
	 * @param goldStandard The gold standard voices for this song.
	 * @return The number of times that two consecutive notes belong to the same midi channel.
	 */
	public int getNumLinksCorrect(List<List<MidiNote>> goldStandard) {
		int count = 0;
		int index = -1;
		
		for (Voice node = this; node.previous != null; node = node.previous) {
			MidiNote guessedPrev = node.previous.mostRecentNote;
			MidiNote note = node.mostRecentNote;
			
			if (note.getCorrectVoice() == guessedPrev.getCorrectVoice()) {
				int channel = note.getCorrectVoice();
				if (index == -1) {
					// No valid index - refind
					index = goldStandard.get(channel).indexOf(note);
				}
				
				if (index != 0 && goldStandard.get(channel).get(--index).equals(guessedPrev)) {
					// Match!
					count++;
					
				} else {
					// No match - invalidate index
					index = -1;
				}
			} else {
				// Different track - invalidate index
				index = -1;
			}
		}
		
		return count;
	}
	
	/**
	 * Get the number of notes in the linked list with this node as its tail.
	 * 
	 * @return The number of notes.
	 */
	public int getNumNotes() {
		if (previous == null) {
			return 1;
		}
		
		return 1 + previous.getNumNotes();
	}

	/**
	 * Get the List of notes which this node is the tail of, in chronological order.
	 * 
	 * @return A List of notes in chronological order, ending with this one.
	 */
	public List<MidiNote> getNotes() {
		List<MidiNote> list = previous == null ? new LinkedList<MidiNote>() : previous.getNotes();
		
		list.add(mostRecentNote);
		
		return list;
	}
	
	/**
	 * Get the most recent note in this voice.
	 * 
	 * @return {@link #mostRecentNote}
	 */
	public MidiNote getMostRecentNote() {
		return mostRecentNote;
	}
	
	/**
	 * Get the voice ending at the previous note in this voice.
	 * 
	 * @return {@link #previous}
	 */
	public Voice getPrevious() {
		return previous;
	}
	
	@Override
	public String toString() {
		return getNotes().toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Voice)) {
			return false;
		}
		
		return compareTo((Voice) o) == 0;
	}

	@Override
	public int compareTo(Voice o) {
		if (o == null) {
			return -1;
		}
		
		int result = mostRecentNote.compareTo(o.mostRecentNote);
		if (result != 0) {
			return result;
		}
		
		if (previous == o.previous) {
			return 0;
		}
		
		if (previous == null) {
			return 1;
		}
		
		return previous.compareTo(o.previous);
	}
}
