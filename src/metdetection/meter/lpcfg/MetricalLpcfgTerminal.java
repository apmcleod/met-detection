package metdetection.meter.lpcfg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import metdetection.utils.MathUtils;

/**
 * A <code>MetricalLpcfgTerminal</code> object represents a terminal symbol in the
 * rhythmic grammar. That is, any pattern of ties, notes, and rests which make
 * up an entire sub-beat in a given song's metrical structure. It is made up of a
 * List of {@link MetricalLpcfgQuantum}s.
 * 
 * @author Andrew McLeod - 24 February, 2016
 */
public class MetricalLpcfgTerminal implements MetricalLpcfgNode, Comparable<MetricalLpcfgTerminal>, Serializable {
	/**
	 * Version 1
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The pattern of quantums that make up this terminal, in fully reduced form.
	 */
	private final MetricalLpcfgQuantum[] reducedPattern;
	
	/**
	 * The pattern of quantums that make up this terminal, in unreduced form.
	 */
	private final MetricalLpcfgQuantum[] originalPattern;
	
	/**
	 * The base length of this terminal, used to normalize in {@link #getHead()}.
	 */
	private final int baseLength;
	
	/**
	 * Create a new terminal with just a single rest.
	 */
	public MetricalLpcfgTerminal() {
		this(new MetricalLpcfgQuantum[] {MetricalLpcfgQuantum.REST});
	}
	
	/**
	 * Create a new MetricalGrammarTerminal with the given pattern and {@link #baseLength} of 1. This
	 * will convert the given pattern into reduced form before saving it.
	 * 
	 * @param pattern The given pattern, in non-reduced form.
	 */
	public MetricalLpcfgTerminal(MetricalLpcfgQuantum[] pattern) {
		this(pattern, 1);
	}

	/**
	 * Create a new MetricalGrammarTerminal with the given pattern and base length. This will convert
	 * the given pattern into reduced form before saving it.
	 * 
	 * @param pattern The given pattern, in non-reduced form.
	 * @param baseLength {@link #baseLength}
	 */
	public MetricalLpcfgTerminal(MetricalLpcfgQuantum[] pattern, int baseLength) {
		originalPattern = pattern;
		reducedPattern = pattern.length == 0 ? new MetricalLpcfgQuantum[0] : generateReducedPattern(pattern);
		this.baseLength = baseLength;
	}
	
	/**
	 * Create a new MetricalGrammarTerminal, a shallow copy of the given one.
	 */
	private MetricalLpcfgTerminal(MetricalLpcfgTerminal terminal) {
		reducedPattern = terminal.reducedPattern;
		originalPattern = terminal.originalPattern;
		baseLength = terminal.baseLength;
	}

	/**
	 * Convert the given pattern into reduced form and return it. That is, divide all constituent
	 * lengths by their GCF.
	 * 
	 * @param pattern The pattern we want to reduce.
	 * @return The given pattern in fully reduced form.
	 */
	private static MetricalLpcfgQuantum[] generateReducedPattern(MetricalLpcfgQuantum[] pattern) {
		int gcf = getGCF(pattern);
		
		MetricalLpcfgQuantum[] reducedPattern = new MetricalLpcfgQuantum[pattern.length / gcf];
		int reducedPatternIndex = 0;
		
		// Are we initially in a rest?
		boolean inRest = pattern[0] == MetricalLpcfgQuantum.REST;
		int currentLength = 1;
		
		for (int i = 1; i < pattern.length; i++) {
			switch (pattern[i]) {
			case REST:
				if (inRest) {
					// Rest continues
					currentLength++;
					
				} else {
					// New rest
					int reducedLength = currentLength / gcf;
					
					// Add initial symbol (complex in case pattern begins with a TIE)
					reducedPattern[reducedPatternIndex] = reducedPatternIndex == 0 ? pattern[0] : MetricalLpcfgQuantum.ONSET;
					reducedPatternIndex++;
					
					// Add all ties
					for (int j = 1; j < reducedLength; j++) {
						reducedPattern[reducedPatternIndex++] = MetricalLpcfgQuantum.TIE;
					}
					
					inRest = true;
					currentLength = 1;
				}
				break;
				
			case ONSET:
				// New note
				int reducedLength = currentLength / gcf;

				if (inRest) {
					// Add all RESTs
					for (int j = 0; j < reducedLength; j++) {
						reducedPattern[reducedPatternIndex++] = MetricalLpcfgQuantum.REST;
					}
					
				} else {
					// Add initial symbol (complex in case pattern begins with a TIE)
					reducedPattern[reducedPatternIndex] = reducedPatternIndex == 0 ? pattern[0] : MetricalLpcfgQuantum.ONSET;
					reducedPatternIndex++;
				
					// Add all TIEs
					for (int j = 1; j < reducedLength; j++) {
						reducedPattern[reducedPatternIndex++] = MetricalLpcfgQuantum.TIE;
					}
				}
				
				currentLength = 1;
				inRest = false;
				break;
				
			case TIE:
				if (inRest) {
					System.err.println("ERROR: TIE after REST - Treating as ONSET");
					
					reducedLength = currentLength / gcf;
					
					for (int j = 0; j < reducedLength; j++) {
						reducedPattern[reducedPatternIndex++] = MetricalLpcfgQuantum.REST;
					}
					
					currentLength = 1;
					inRest = false;
					
				} else {
					// Note continues
					currentLength++;
				}
				break;
			}
		}
		
		// Handle final constituent
		int reducedLength = currentLength / gcf;

		if (inRest) {
			// Add all RESTs
			for (int j = 0; j < reducedLength; j++) {
				reducedPattern[reducedPatternIndex++] = MetricalLpcfgQuantum.REST;
			}
			
		} else {
			// Add initial symbol (complex in case pattern begins with a TIE)
			reducedPattern[reducedPatternIndex] = reducedPatternIndex == 0 ? pattern[0] : MetricalLpcfgQuantum.ONSET;
			reducedPatternIndex++;
		
			// Add all TIEs
			for (int j = 1; j < reducedLength; j++) {
				reducedPattern[reducedPatternIndex++] = MetricalLpcfgQuantum.TIE;
			}
		}
		
		return reducedPattern;
	}

	/**
	 * Get the greatest common factor of the lengths of all of the constituents in the given
	 * pattern.
	 * 
	 * @param pattern The pattern we want to reduce.
	 * @return The greatest common factor of the constituents of the given pattern.
	 */
	private static int getGCF(MetricalLpcfgQuantum[] pattern) {
		// Find constituent lengths
		List<Integer> lengths = new ArrayList<Integer>();
		lengths.add(1);
		
		int gcf = 0;
		
		// Are we initially in a rest?
		boolean inRest = pattern[0] == MetricalLpcfgQuantum.REST;
		
		for (int i = 1; i < pattern.length; i++) {
			if (gcf == 1) {
				return 1;
			}
			
			switch (pattern[i]) {
			case REST:
				if (inRest) {
					// Rest continues
					incrementLast(lengths);
					
				} else {
					// New rest
					inRest = true;
					int lastLength = lengths.get(lengths.size() - 1);
					gcf = gcf == 0 ? lastLength : MathUtils.getGCF(gcf, lastLength);
					lengths.add(1);
				}
				break;
				
			case ONSET:
				// New note
				int lastLength = lengths.get(lengths.size() - 1);
				gcf = gcf == 0 ? lastLength : MathUtils.getGCF(gcf, lastLength);
				lengths.add(1);
				inRest = false;
				break;
				
			case TIE:
				if (inRest) {
					System.err.println("ERROR: TIE after REST - Treating as ONSET");
					lastLength = lengths.get(lengths.size() - 1);
					gcf = gcf == 0 ? lastLength : MathUtils.getGCF(gcf, lastLength);
					lengths.add(1);
					inRest = false;
					
				} else {
					// Note continues
					incrementLast(lengths);
				}
				break;
			}
		}
		
		// Add last constituent (if we already did, it won't affect the gcf anyways)
		int lastLength = lengths.get(lengths.size() - 1);
		gcf = gcf == 0 ? lastLength : MathUtils.getGCF(gcf, lastLength);
		
		return gcf;
	}

	/**
	 * Utility method to increment the last value in an Integer List.
	 * 
	 * @param list The Integer List whose last value we want to increment.
	 */
	private static void incrementLast(List<Integer> list) {
		if (list.isEmpty()) {
			return;
		}
		
		list.set(list.size() - 1, list.get(list.size() - 1) + 1);
	}
	
	/**
	 * Get whether this terminal contains any notes or not.
	 * 
	 * @return True if this terminal constins no notes (is all RESTS). False otherwise.
	 */
	@Override
	public boolean isEmpty() {
		return equals(new MetricalLpcfgTerminal());
	}
	
	@Override
	public boolean startsWithRest() {
		return reducedPattern.length == 0 || reducedPattern[0] == MetricalLpcfgQuantum.REST;
	}
	
	/**
	 * Get the head of this terminal.
	 * 
	 * @return The head of this terminal.
	 */
	@Override
	public MetricalLpcfgHead getHead() {
		int maxNoteLength = 0;
		int maxNoteIndex = 0;
		
		int currentNoteLength = 0;
		int currentNoteIndex = 0;
		
		for (int i = 0; i < originalPattern.length; i++) {
			MetricalLpcfgQuantum quantum = originalPattern[i];
			if (quantum == MetricalLpcfgQuantum.ONSET || quantum == MetricalLpcfgQuantum.REST) {
				// Note ended
				if (currentNoteLength > maxNoteLength) {
					maxNoteLength = currentNoteLength;
					maxNoteIndex = currentNoteIndex;
				}
				
				currentNoteLength = 0;
				currentNoteIndex = i;
			}
			
			if (quantum == MetricalLpcfgQuantum.ONSET || quantum == MetricalLpcfgQuantum.TIE) {
				// Note continues
				currentNoteLength++;
			}
		}
		
		// Get final max as double
		if (currentNoteLength > maxNoteLength) {
			maxNoteLength = currentNoteLength;
			maxNoteIndex = currentNoteIndex;
		}
		
		double length = ((double) maxNoteLength) / originalPattern.length * baseLength;
		
		return new MetricalLpcfgHead(length, ((double) maxNoteIndex) / originalPattern.length * baseLength, originalPattern[maxNoteIndex] == MetricalLpcfgQuantum.TIE);
	}
	
	@Override
	public int getLength() {
		return baseLength;
	}
	
	/**
	 * Get the pattern of this terminal.
	 * 
	 * @return {@link #reducedPattern}
	 */
	public MetricalLpcfgQuantum[] getReducedPattern() {
		return reducedPattern;
	}
	
	/**
	 * Get the original unreduced pattern of this terminal.
	 * 
	 * @return {@link #originalPattern}
	 */
	public MetricalLpcfgQuantum[] getOriginalPattern() {
		return originalPattern;
	}
	
	@Override
	public MetricalLpcfgTerminal getTerminal() {
		return this;
	}
	
	/**
	 * Return a copy of this terminal.
	 * 
	 * @return A copy of this terminal.
	 */
	@Override
	public MetricalLpcfgTerminal deepCopy() {
		return new MetricalLpcfgTerminal(this);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof MetricalLpcfgTerminal)) {
			return false;
		}
		
		MetricalLpcfgTerminal o = (MetricalLpcfgTerminal) other;
		return Arrays.equals(reducedPattern, o.reducedPattern);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(reducedPattern);
	}
	
	/**
	 * Get the recursive String of this terminal. That is, the one that shows probabilities.
	 * 
	 * @return The recursive String of this terminal.
	 */
	public String toStringPretty(int depth, String tab) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < depth; i++) {
			sb.append(tab);
		}
		
		sb.append(toString());
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return Arrays.toString(reducedPattern);
	}

	@Override
	public int compareTo(MetricalLpcfgTerminal o) {
		if (o == null) {
			return 1;
		}
		
		int result = reducedPattern.length - o.reducedPattern.length;
		if (result != 0) {
			return result;
		}
		
		return Integer.compare(hashCode(), o.hashCode());
	}
}
