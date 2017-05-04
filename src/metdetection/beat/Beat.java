package metdetection.beat;

/**
 * A <code>Beat</code> represents a single MIDI beat. It stores information about the onset
 * time and tick number of the beat, and the beat number (in 32nd notes). Beats are Comparable
 * and their natural ordering uses only {@link #beat}, not any absolute timing information.
 * 
 * @author Andrew McLeod - 3 March, 2014
 */
public class Beat implements Comparable<Beat> {
	/**
	 * The beat number on which this Beat lies (measured in 32nd notes)
	 */
	private final int beat;
	
	/**
	 * The measure number on which this Beat lies.
	 */
	private final int measure;
	
	/**
	 * The time in microseconds at which this Beat lies.
	 */
	private final long time;
	
	/**
	 * The tick at which this Beat lies.
	 */
	private final long tick;
	
	/**
	 * Create a new default Beat, at time, tick, and measure 0 and beat 0.
	 */
	public Beat() {
		this(0, 0, 0, 0);
	}
	
	/**
	 * Standard constructor for all fields.
	 * 
	 * @param measure {@link #measure}
	 * @param beat {@link #beat}
	 * @param time {@link #time}
	 * @param tick {@link #tick}
	 */
	public Beat(int measure, int beat, long time, long tick) {
		this.measure = measure;
		this.beat = beat;
		this.time = time;
		this.tick = tick;
	}
	
	/**
	 * Return a shallow copy of this Beat. That is, the newly created Beat will have identical
	 * {@link #beat}, {@link #time}, and {@link #tick} values.
	 * 
	 * @return A shallow copy of this Beat. 
	 */
	public Beat shallowCopy() {
		return new Beat(measure, beat, time, tick);
	}
	
	/**
	 * Get this Beat's measure number.
	 * 
	 * @return {@link #measure}
	 */
	public int getMeasure() {
		return measure;
	}
	
	/**
	 * Get this Beat's beat number.
	 * 
	 * @return {@link #beat}
	 */
	public int getBeat() {
		return beat;
	}
	
	/**
	 * Get this Beat's time.
	 * 
	 * @return {@link #time}
	 */
	public long getTime() {
		return time;
	}
	
	/**
	 * Get this Beat's tick.
	 * 
	 * @return {@link #tick}
	 */
	public long getTick() {
		return tick;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("(");
		
		sb.append(measure).append('.');
		sb.append(beat).append(',');
		sb.append(time).append(')');
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Beat)) {
			return false;
		}
		
		Beat beat = (Beat) other;
		
		return beat.beat == this.beat && beat.measure == measure && beat.tick == tick && beat.time == time;
	}
	
	@Override
	public int hashCode() {
		return Integer.valueOf(getBeat() * 50 + getMeasure()).hashCode();
	}

	@Override
	public int compareTo(Beat o) {
		if (o == null) {
			return -1;
		}
		
		int value = Integer.compare(measure, o.measure);
		if (value != 0) {
			return value;
		}
		
		return Integer.compare(beat, o.beat);
	}
}
