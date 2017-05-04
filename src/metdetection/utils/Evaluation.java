package metdetection.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.midi.InvalidMidiDataException;

import metdetection.Main;
import metdetection.Runner;
import metdetection.beat.Beat;
import metdetection.meter.Measure;
import metdetection.parsing.DummyNoteEventParser;
import metdetection.parsing.NoteFileWriter;
import metdetection.parsing.NoteListGenerator;
import metdetection.time.TimeSignature;
import metdetection.time.TimeTracker;

/**
 * The <code>Evaluation</code> can be used to perform global evaluation on some output file.
 * 
 * @author Andrew McLeod - 9 September, 2016
 */
public class Evaluation {

	public static void main(String[] args) throws IOException, InvalidMidiDataException {
		// No args given
		if (args.length == 0) {
			argumentError("No arguments given");
		}
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i].charAt(0)) {
				// ARGS
				case '-':
					if (args[i].length() == 1) {
						argumentError("Unrecognized option: " + args[i]);
					}
					
					switch (args[i].charAt(1)) {
						// Check for time changes
						case 'c':
							i++;
							if (args.length <= i) {
								argumentError("No file given for -c option.");
							}
							
							while (i < args.length) {
								File file = new File(args[i]);
								if (!file.exists()) {
									argumentError("File " + args[i] + " not found");
								}
							
								for (File toTest : Main.getAllFilesRecursive(file)) {
									try {
										List<TimeSignature> measures = getAllMeters(toTest); 
										System.out.println(toTest + ": " + measures.size() + " " + measures);
									} catch (IOException | InvalidMidiDataException e) {
										System.err.println("Error parsing file " + toTest);
										System.err.println(e.getMessage());
									}
								}
								i++;
							}
							break;

						// Check meter
						case 'm':
							i++;
							if (args.length <= i) {
								argumentError("No file given for -m option.");
							}
							
							while (i < args.length) {
								File file = new File(args[i]);
								if (!file.exists()) {
									argumentError("File " + args[i] + " not found");
								}
							
								for (File toTest : Main.getAllFilesRecursive(file)) {
									try {
										System.out.println(toTest + ": " + getMeter(toTest));
									} catch (IOException | InvalidMidiDataException e) {
										System.err.println("Error parsing file " + toTest);
										System.err.println(e.getMessage());
									}
								}
								i++;
							}
							break;
							
						// lpcfg
						case 'l':
							evaluateLpcfg();
							break;
							
						// Notefile generation
						case 'n':
							i++;
							if (args.length <= i) {
								argumentError("No file given for -n option.");
							}
							
							File file = new File(args[i]);
							if (!file.exists()) {
								argumentError("File " + args[i] + " not found");
							}
							
							System.out.println(getNoteFileString(file));
							break;
							
						// Temperley evaluation
						case 't':
							i++;
							if (args.length <= i) {
								argumentError("No file given for -t option.");
							}
							
							file = new File(args[i]);
							if (!file.exists()) {
								argumentError("File " + args[i] + " not found");
							}
							
							evaluateTemperley(file);
							break;
							
						// Error
						default:
							argumentError("Unrecognized option: " + args[i]);
					}
					break;
					
				// Error
				default:
					argumentError("Unrecognized option: " + args[i]);
			}
		}
	}

	/**
	 * Get a List of all of the meters of the given song, excluding the initial dummy one.
	 * 
	 * @param file The file whose meters we want.
	 * @return A List of all of the meters of the given song, excluding the initial dummy one.
	 * @throws InterruptedException 
	 * @throws InvalidMidiDataException 
	 * @throws IOException 
	 */
	private static List<TimeSignature> getAllMeters(File file) throws IOException, InvalidMidiDataException {
		TimeTracker tt = new TimeTracker();
		DummyNoteEventParser d = new DummyNoteEventParser();
		Runner.parseFile(file, d, tt);
		
		return tt.getAllTimeSignatures();
	}
	
	/**
	 * Get the meter at the beginning of the given file.
	 * 
	 * @param file The file whose meter we want.
	 * @return The meter at the beginning of the given file.
	 * @throws InterruptedException 
	 * @throws InvalidMidiDataException 
	 * @throws IOException 
	 */
	private static TimeSignature getMeter(File file) throws IOException, InvalidMidiDataException {
		TimeTracker tt = new TimeTracker();
		DummyNoteEventParser d = new DummyNoteEventParser();
		Runner.parseFile(file, d, tt);
		
		return tt.getFirstTimeSignature();
	}

	/**
	 * Evaluate a Temperley file output (from std in) based on the gold standard meter parsed
	 * from the given MIDI or **kern file.
	 *  
	 * @param file The MIDI or **kern file which contains the gold standard meter.
	 * @throws InterruptedException 
	 * @throws InvalidMidiDataException 
	 * @throws IOException 
	 */
	private static void evaluateTemperley(File file) throws IOException, InvalidMidiDataException {
		System.out.println("TESTING: " + file);
		
		// Get correct info from file
		TimeTracker tt = new TimeTracker();
		NoteListGenerator nlg = new NoteListGenerator(tt);
		Runner.parseFile(file, nlg, tt);
		
		// Get correct measure structure
		Measure correctMeter = tt.getFirstTimeSignature().getMetricalMeasure();
		int correctBeatsPerMeasure = correctMeter.getBeatsPerMeasure();
		int correctSubBeatsPerBeat = correctMeter.getSubBeatsPerBeat();
		
		// Get correct sub beat length (in terms of first note length)
		int ticksPerMeasure = tt.getFirstTimeSignature().getNotes32PerBar() * ((int) tt.getPPQ() / 8);
		int ticksPerSubBeat = ticksPerMeasure / (correctBeatsPerMeasure * correctSubBeatsPerBeat);
		
		MidiNote firstNote = nlg.getNoteList().get(0);
		Beat onsetBeat = firstNote.getOnsetBeat(tt.getBeats());
		Beat offsetBeat = firstNote.getOffsetBeat(tt.getBeats());
		TimeSignature onsetTimeSignature = tt.getNodeAtTime(firstNote.getOnsetTime()).getTimeSignature();
		
		double correctSubBeatsPerFirstNote = offsetBeat.getBeat() - onsetBeat.getBeat();
		int offsetMeasure = offsetBeat.getMeasure();
		while (offsetMeasure != onsetBeat.getMeasure()) {
			correctSubBeatsPerFirstNote += onsetTimeSignature.getNotes32PerBar();
			offsetMeasure--;
		}
		
		correctSubBeatsPerFirstNote /= onsetTimeSignature.getNotes32PerBar();
		correctSubBeatsPerFirstNote *= correctBeatsPerMeasure * correctSubBeatsPerBeat;
		
		// Get correct anacrusis length, relative to the first note's position
		int offsetTicks = (int) firstNote.getOnsetTick();
		int anacrusisTicks = (tt.getAnacrusisTicks() - offsetTicks + ticksPerMeasure) % ticksPerMeasure;
		
		int correctSubBeatsPerAnacrusis = (int) Math.round(((double) anacrusisTicks) / ticksPerSubBeat);
		
		// Get info from Temperley (std in)
		Scanner input = new Scanner(System.in);
		
		// First, skip all preamble
		while (input.hasNextLine()) {
			if (input.nextLine().startsWith("Total final score =")) {
				break;
			}
		}
		
		// First note
		int firstNoteOnsetTick = 0;
		int firstNoteLength = 0;
		boolean firstNoteFound = false;
		List<Integer> potentialFirstNoteIndices = new ArrayList<Integer>();
		
		// Bar structure
		int barCount = 0;
		int ticksPerBar = 0;
		int temperleyBeatsPerBar = 0;
		int temperleySubBeatsPerBeat = 0;
		
		int anacrusisTicksTemperley = 0;
		
		int lineNum = -1;
		// Here, we are in the printed score
		while (input.hasNextLine() && (barCount < 2 || !firstNoteFound)) {
			String line = input.nextLine();
			if (line.lastIndexOf("x ") == -1) {
				continue;
			}
			lineNum++;
			
			// Bar structure
			int level = getNumLevels(line);
			if (level == 4) {
				// bar found
				barCount++;
				
				if (barCount == 1) {
					anacrusisTicksTemperley = lineNum;
					
				} else if (barCount == 2) {
					ticksPerBar = lineNum - anacrusisTicksTemperley;
				}
			}
			
			if (barCount == 1 && level >= 3) {
				// Tactus found
				temperleyBeatsPerBar++;
			}
			
			if (barCount == 1 && temperleyBeatsPerBar == 1 && level >= 2) {
				// sub beat found
				temperleySubBeatsPerBeat++;
			}
			
			// First note
			if (!firstNoteFound) {
				if (potentialFirstNoteIndices.isEmpty()) {
					int scoreIndex = line.lastIndexOf("x ");
					
					Pattern noteOnsetPattern = Pattern.compile("[0-9]");
					Matcher noteOnsetMatcher = noteOnsetPattern.matcher(line.substring(scoreIndex));
					
					while (noteOnsetMatcher.find()) {
						potentialFirstNoteIndices.add(scoreIndex + noteOnsetMatcher.start());
						firstNoteOnsetTick = lineNum;
					}
					
				} else {
					for (int noteIndex : potentialFirstNoteIndices) {
						if (line.charAt(noteIndex) != '|') {
							firstNoteFound = true;
							firstNoteLength = lineNum - firstNoteOnsetTick;
							break;
						}
					}
				}
			}
		}
		input.close();
		
		int temperleyTicksPerSubBeat = ticksPerBar / temperleyBeatsPerBar / temperleySubBeatsPerBeat;
		
		// sub beat length
		double temperleySubBeatsPerFirstNote = ((double) firstNoteLength) / temperleyTicksPerSubBeat;
		
		// anacrusis
		anacrusisTicksTemperley = (anacrusisTicksTemperley - firstNoteOnsetTick + ticksPerBar) % ticksPerBar;
		double temperleySubBeatsPerAnacrusis = ((double) anacrusisTicksTemperley) / temperleyTicksPerSubBeat;
		
		System.out.println("CORRECT: " + correctMeter + " SB=" + correctSubBeatsPerFirstNote +
				" A=" + correctSubBeatsPerAnacrusis);
		System.out.println("Temperley: " + temperleyBeatsPerBar + "," + temperleySubBeatsPerBeat +
				" SB=" + temperleySubBeatsPerFirstNote + " A=" + temperleySubBeatsPerAnacrusis);
		
		System.out.println(
				getAccuracyString(
						correctMeter,
						(int) Math.round(480.0 / correctSubBeatsPerFirstNote),
						(int) Math.round(480.0 / correctSubBeatsPerFirstNote * correctSubBeatsPerAnacrusis),
						new Measure(temperleyBeatsPerBar, temperleySubBeatsPerBeat),
						(int) Math.round(480.0 / temperleySubBeatsPerFirstNote),
						(int) Math.round(480.0 / temperleySubBeatsPerFirstNote * temperleySubBeatsPerAnacrusis)));
	}

	/**
	 * Return the number of levels on the given line of Temperley output. That is,
	 * the number of occurrences of "x " on that line.
	 * 
	 * @param line The line we are searching.
	 * @return The number of levels on the given line.
	 */
	private static int getNumLevels(String line) {
		int lastIndex = 0;
		int count = 0;
		
		while (lastIndex != -1) {
			lastIndex = line.indexOf("x ", lastIndex);
			
			if (lastIndex != -1) {
				count++;
				lastIndex += 2;
			}
		}
		
		return count;
	}
	
	/**
	 * Get the accuracy String for a metrical hypothesis.
	 *
	 * @param correctMeasure The correct measure.
	 * @param correctSubBeatLength The correct sub beat length, in ticks.
	 * @param correctAnacrusisLength The correct anacrusis length, in ticks.
	 * @param hypothesisMeasure The hypothesis measure.
	 * @param hypothesisSubBeatLength The hypothesis sub beat length, in ticks.
	 * @param hypothesisAnacrusisLength The hypothesis anacrusis length, in ticks.
	 * 
	 * @return The accuracy string for the given hypothesis.
	 */
	public static String getAccuracyString(Measure correctMeasure, int correctSubBeatLength, int correctAnacrusisLength,
			Measure hypothesisMeasure, int hypothesisSubBeatLength, int hypothesisAnacrusisLength) {
		
		if (hypothesisMeasure.equals(correctMeasure) &&
				hypothesisAnacrusisLength == correctAnacrusisLength &&
				hypothesisSubBeatLength == correctSubBeatLength) {
			return "TP = 3\nFP = 0\nFN = 0\nP = 1.0\nR = 1.0\nF1 = 1.0";
		}
		
		int truePositives = 0;
		int falsePositives = 0;
		
		// Sub beat
		int length = hypothesisSubBeatLength;
		int offset = 0;
		
		int match = getMatch(length, offset, correctMeasure, correctSubBeatLength, correctAnacrusisLength);
		if (match > 0) {
			truePositives++;
			
		} else if (match < 0) {
			falsePositives++;
		}
		
		// Beat
		length *= hypothesisMeasure.getSubBeatsPerBeat();
		offset = hypothesisAnacrusisLength % length;
		
		match = getMatch(length, offset, correctMeasure, correctSubBeatLength, correctAnacrusisLength);
		if (match > 0) {
			truePositives++;
			
		} else if (match < 0) {
			falsePositives++;
		}
		
		// Measure
		length *= hypothesisMeasure.getBeatsPerMeasure();
		offset = hypothesisAnacrusisLength;
		
		match = getMatch(length, offset, correctMeasure, correctSubBeatLength, correctAnacrusisLength);
		if (match > 0) {
			truePositives++;
			
		} else if (match < 0) {
			falsePositives++;
		}
		
		int falseNegatives = 3 - truePositives;
		
		double precision = ((double) truePositives) / (truePositives + falsePositives);
		double recall = ((double) truePositives) / (truePositives + falseNegatives);
		
		double fMeasure = 2 * precision * recall / (precision + recall);
		
		if (Double.isNaN(fMeasure)) {
			fMeasure = 0.0;
		}
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("TP = ").append(truePositives).append('\n');
		sb.append("FP = ").append(falsePositives).append('\n');
		sb.append("FN = ").append(falseNegatives).append('\n');
		sb.append("P = ").append(precision).append('\n');
		sb.append("R = ").append(recall).append('\n');
		sb.append("F1 = ").append(fMeasure);
		
		return sb.toString();
	}
	
	/**
	 * Get the match type of a grouping of the given length and offset given the correct measure,
	 * anacrusis length, and sub beat length.
	 * 
	 * @param length The length of the grouping we want to check.
	 * @param offset The offset of the grouping we want to check.
	 * @param correctMeasure The correct measure of this song.
	 * @param correctSubBeatLength The correct sub beat length.
	 * @param correctAnacrusisLength The correct anacrusis length, measured in tacti.
	 * 
	 * @return A value less than 0 if this grouping overlaps some correct tree boundary. A value
	 * greater than 0 if this grouping matches a correct tree boundary exactly. A value of 0
	 * otherwise, for example if the grouping lies under the lowest grouping, but could be grouped
	 * up into a correct grouping.
	 */
	private static int getMatch(int length, int offset, Measure correctMeasure, int correctSubBeatLength,
			int correctAnacrusisLength) {
		// Sub beat
		int correctLength = correctSubBeatLength;
		int correctOffset = correctAnacrusisLength % correctLength;
		
		if (correctLength == length) {
			return correctOffset == offset ? 1 : -1;
			
		} else if (correctLength < length) {
			if ((offset - correctOffset) % correctLength != 0 || (offset + length - correctOffset) % correctLength != 0) {
				// We don't match up with both the beginning and the end
				return -1;
			}
			
		} else {
			// correctLength > length
			if ((correctOffset - offset) % length != 0 || (correctOffset + correctLength - offset) % length != 0) {
				// We don't match up with both the beginning and the end
				return -1;
			}
		}

		// Beat
		correctLength *= correctMeasure.getSubBeatsPerBeat();
		correctOffset = correctAnacrusisLength % correctLength;
		
		if (correctLength == length) {
			return correctOffset == offset ? 1 : -1;
			
		} else if (correctLength < length) {
			if ((offset - correctOffset) % correctLength != 0 || (offset + length - correctOffset) % correctLength != 0) {
				// We don't match up with both the beginning and the end
				return -1;
			}
			
		} else {
			// correctLength > length
			if ((correctOffset - offset) % length != 0 || (correctOffset + correctLength - offset) % length != 0) {
				// We don't match up with both the beginning and the end
				return -1;
			}
		}
		
		// Measure
		correctLength *= correctMeasure.getBeatsPerMeasure();
		correctOffset = correctAnacrusisLength;
		
		if (correctLength == length) {
			return correctOffset == offset ? 1 : -1;
			
		} else if (correctLength < length) {
			if ((offset - correctOffset) % correctLength != 0 || (offset + length - correctOffset) % correctLength != 0) {
				// We don't match up with both the beginning and the end
				return -1;
			}
			
		} else {
			// correctLength > length
			if ((correctOffset - offset) % length != 0 || (correctOffset + correctLength - offset) % length != 0) {
				// We don't match up with both the beginning and the end
				return -1;
			}
		}
		
		return 0;
	}

	/**
	 * Convert the given input file out into a note file.
	 * 
	 * @param file The input file to be converted into a note file.
	 * @return The String of the note file for the given input file.
	 */
	private static String getNoteFileString(File file) {
		TimeTracker tt = new TimeTracker();
		NoteListGenerator nlg = new NoteListGenerator(tt);
			
		try {
			Runner.parseFile(file, nlg, tt);
				
		} catch (IOException | InvalidMidiDataException e) {	
			System.err.println(e.getLocalizedMessage());
		}
			
		return new NoteFileWriter(tt, nlg).toString();
	}

	/**
	 * Evaluate the input lpcfg results. That is, compute and print the global
	 * FN, FP, TP, P, R, and F1 from std in.
	 */
	private static void evaluateLpcfg() {
		Pattern tpPattern = Pattern.compile("^TP = ([0123])$");
		Pattern fpPattern = Pattern.compile("^FP = ([0123])$");
		Pattern fnPattern = Pattern.compile("^FN = ([0123])$");
		
		int truePositives = 0;
		int falsePositives = 0;
		int falseNegatives = 0;
		
		Scanner input = new Scanner(System.in);
		while (input.hasNextLine()) {
			String line = input.nextLine();
			
			Matcher tpMatcher = tpPattern.matcher(line);
			Matcher fpMatcher = fpPattern.matcher(line);
			Matcher fnMatcher = fnPattern.matcher(line);
			
			if (tpMatcher.matches()) {
				truePositives += Integer.parseInt(tpMatcher.group(1));
				
			} else if (fpMatcher.matches()) {
				falsePositives += Integer.parseInt(fpMatcher.group(1));
				
			} else if (fnMatcher.matches()) {
				falseNegatives += Integer.parseInt(fnMatcher.group(1));
			}
		}
		
		input.close();
		
		System.out.println("TP = " + truePositives);
		System.out.println("FP = " + falsePositives);
		System.out.println("FN = " + falseNegatives);
		
		double precision = ((double) truePositives) / (truePositives + falsePositives);
		double recall = ((double) truePositives) / (truePositives + falseNegatives);
		
		double fMeasure = 2 * precision * recall / (precision + recall);
		
		System.out.println("P = " + precision);
		System.out.println("R = " + recall);
		System.out.println("F1 = " + fMeasure);
	}
	
	/**
	 * Some argument error occurred. Print the message to std err and exit.
	 * 
	 * @param message The message to print to std err.
	 */
	private static void argumentError(String message) {
		StringBuilder sb = new StringBuilder(message).append('\n');
		
		sb.append("Usage: metdetection.utils.Evaluation ARGS\n");
		sb.append("ARGS:\n");
		
		sb.append("-l = Evaluate the input LPCFG output data (from std in),");
		sb.append(" and output (to std out) the overall stats (precision, recall, F1).\n");
		
		sb.append("-n FILE = Convert the given file into a note files");
		sb.append(" and print it to std out. This option can be used multiple times");
		sb.append(" with different files.\n");
		
		sb.append("-t FILE = Evaluate a Temperley formatted output file (read from std in) based");
		sb.append(" on the gold standard meter read in from the given file (MIDI or **kern).\n");
		
		sb.append("-m FILE_LIST = Get the meter at the start of each of the files in the file list.\n");
		
		sb.append("-c FILE_LIST = Get the number of meters in each of the files and list them.");
		
		System.err.println(sb);
		System.exit(1);
	}
}
