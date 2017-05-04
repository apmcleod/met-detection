package metdetection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;

import metdetection.beat.fromfile.FromFileBeatTrackingModelState;
import metdetection.joint.JointModel;
import metdetection.meter.Measure;
import metdetection.meter.fromfile.FromFileMetricalModelState;
import metdetection.meter.lpcfg.MetricalLpcfg;
import metdetection.meter.lpcfg.MetricalLpcfgElementNotFoundException;
import metdetection.meter.lpcfg.MetricalLpcfgGenerator;
import metdetection.meter.lpcfg.MetricalLpcfgMetricalModelState;
import metdetection.meter.lpcfg.MetricalLpcfgTree;
import metdetection.parsing.EventParser;
import metdetection.parsing.NoteListGenerator;
import metdetection.time.TimeSignature;
import metdetection.time.TimeTracker;
import metdetection.utils.Evaluation;
import metdetection.voice.fromfile.FromFileVoiceSplittingModelState;

/**
 * The <code>Main</code> class is used to interface with and run
 * the {@link MetricalLpcfgGenerator} class.
 * 
 * @author Andrew McLeod - 29 February, 2016
 */
public class Main {
	public static boolean VERBOSE = false;
	public static boolean TESTING = false;
	public static boolean LEXICALIZATION = true;
	public static boolean EXTRACT = false;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		boolean useChannel = true;
		boolean test = false;
		boolean generate = false;
		File exportModelFile = null;
		List<File> modelFiles = new ArrayList<File>();
		List<File> testFiles = new ArrayList<File>();
		List<File> anacrusisFiles = new ArrayList<File>();

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
						// Use track
						case 'T':
							useChannel = false;
							break;
							
						case 'v':
							VERBOSE = true;
							System.out.println("Verbose output");
							break;
							
						case 'l':
							LEXICALIZATION = false;
							break;
							
						case 'g':
							if (test) {
								argumentError("Cannot use -t and -g at once");
							}
							generate = true;
							if (args.length <= ++i) {
								argumentError("No File used with -g");
							}
							exportModelFile = new File(args[i]);
							break;
							
						case 't':
							if (generate) {
								argumentError("Cannot use -t and -g at once");
							}
							test = true;
							if (args.length <= ++i) {
								argumentError("No File used with -t");
							}
							File modelFile = new File(args[i]);
							if (!modelFile.exists()) {
								argumentError("Model File " + args[i] + " not found");
							}
							modelFiles.add(modelFile);
							break;
							
						case 'a':
							if (args.length <= ++i) {
								argumentError("No Anacrusis Files given after -a");
							}
							File file = new File(args[i]);
							if (!file.exists()) {
								argumentError("Anacrusis File " + args[i] + " not found");
							}
							anacrusisFiles.addAll(getAllFilesRecursive(file));
							break;
							
						case 'x':
							EXTRACT = true;
							break;
							
						// Error
						default:
							argumentError("Unrecognized option: " + args[i]);
					}
					break;
					
				// File or directory name
				default:
					File file = new File(args[i]);
					if (!file.exists()) {
						argumentError("File " + args[i] + " not found");
					}
					testFiles.addAll(getAllFilesRecursive(file));
			}
		}
		
		if (testFiles.isEmpty()) {
			argumentError("No music files given for testing");
		}
		
		// Generate grammar
		if (generate) {
			if (VERBOSE) {
				System.out.println("Generating grammar into " + exportModelFile);
			}
			
			MetricalLpcfgGenerator generator = generateGrammar(testFiles, anacrusisFiles, useChannel);
			
			MetricalLpcfg.serialize(generator.getGrammar(), exportModelFile);	
		}
		
		// Test grammar
		if (test) {
			MetricalLpcfg grammar = new MetricalLpcfg();
			for (File modelFile : modelFiles) {
				if (VERBOSE) {
					System.out.println("Reading grammar in from " + modelFile);
				}
			
				MetricalLpcfg tempGrammar = MetricalLpcfg.deserialize(modelFile);
				for (MetricalLpcfgTree tree : tempGrammar.getTrees()) {
					grammar.addTree(tree);
				}
			}
			
			if (VERBOSE) {
				System.out.println("Loaded grammar:");
				System.out.println(grammar);
				System.out.println(grammar.getProbabilityTracker());
			}
			
			for (File testFile : testFiles) {
				System.out.println("Testing " + testFile);
				
				List<File> testFileList = new ArrayList<File>(1);
				testFileList.add(testFile);
				
				TESTING = false;
				MetricalLpcfgGenerator correctGrammarGenerator = generateGrammar(testFileList, anacrusisFiles, useChannel);
				TESTING = true;
				
				try {
					testGrammar(testFile, anacrusisFiles, useChannel, grammar, correctGrammarGenerator);
				} catch (MetricalLpcfgElementNotFoundException e) {
					System.err.println("Tree extraction error - skipping file " + testFile);
					System.err.println(e.getLocalizedMessage());
				}
			}
		}
	}
	
	/**
	 * Test the given Runner with the given generated grammar.
	 * 
	 * @param testFile The MIDI File we will use to test the grammar.
	 * @param anacrusisFiles The anacrusis files for this run.
	 * @param useChannel True if to use channels as the gold standard voice in MIDI files.
	 * False for tracks.
	 * @param grammar The grammar.
	 * @param correctGrammarGenerator The correct grammar generator from this song.
	 * @throws MetricalLpcfgElementNotFoundException A tree to be extracted was not found in the grammar.
	 */
	private static void testGrammar(File testFile, List<File> anacrusisFiles, boolean useChannel, MetricalLpcfg grammar, MetricalLpcfgGenerator correctGrammarGenerator) throws MetricalLpcfgElementNotFoundException {
		TimeTracker tt = new TimeTracker();
		tt.setAnacrusis(getAnacrusisLength(testFile, anacrusisFiles));
		NoteListGenerator nlg = new NoteListGenerator(tt);
			
		// PARSE!
		EventParser ep = null;
		try {
			ep = Runner.parseFile(testFile, nlg, tt, useChannel);
				
		} catch (InvalidMidiDataException | IOException e) {
			System.err.println("Error parsing file " + testFile + ":\n" + e.getLocalizedMessage());
			
			if (VERBOSE) {
				e.printStackTrace();
			}
			
			return;
		}
		
		if (tt.getFirstTimeSignature().getNumerator() == TimeSignature.IRREGULAR_NUMERATOR) {
			System.err.println("Irregular meter detected. Skipping song " + testFile);
			return;
		}
		
		if (tt.getAllTimeSignatures().size() != 1) {
			System.err.println("Meter change detected. Skipping song " + testFile);
			return;
		}
		
		if (EXTRACT) {
			if (VERBOSE) {
				System.out.print("Extracting " + testFile + " from grammar...");
			}
			
			extractFromGrammar(grammar, correctGrammarGenerator);
			
			if (VERBOSE) {
				System.out.println("Done!");
			}
		}
			
		// RUN!
		JointModel jm;
		try {
		jm = new JointModel(
				new FromFileVoiceSplittingModelState(ep),
				new FromFileBeatTrackingModelState(tt),
				new MetricalLpcfgMetricalModelState(grammar));
		
		} catch (InvalidMidiDataException e) {
			System.err.println("Error parsing file " + testFile + ":\n" + e.getLocalizedMessage());
			
			if (VERBOSE) {
				e.printStackTrace();
			}
			
			return;
		}
		
		Runner.performInference(jm, nlg);
		
		if (EXTRACT) {
			if (VERBOSE) {
				System.out.print("Adding " + testFile + " back into grammar...");
			}
			
			addToGrammar(grammar, correctGrammarGenerator);
			
			if (VERBOSE) {
				System.out.println("Done!");
			}
		}
		
		@SuppressWarnings("unchecked")
		List<MetricalLpcfgMetricalModelState> resultsByProb = (List<MetricalLpcfgMetricalModelState>) jm.getHierarchyHypotheses();
		
		Measure correctMeasure = null;
		for (Measure measure : correctGrammarGenerator.getGrammar().getMeasures()) {
			correctMeasure = measure;
			break;
		}
		int correctSubBeatLength = correctGrammarGenerator.getTerminalLength();
		
		// Grab new anacrusis length in case it was a **kern file (with the anacrusis built in)
		int anacrusisLength = tt.getAnacrusisSubBeats() * correctSubBeatLength;
		
		if (resultsByProb.size() == 0) {
			System.out.println("TP = 0\nFP = 0\nFN = 3\nP = 0.0\nR = 0.0\nF1 = 0.0");
			
		} else {
			MetricalLpcfgMetricalModelState hypothesis = resultsByProb.get(0);
			System.out.println(Evaluation.getAccuracyString(correctMeasure, correctSubBeatLength, anacrusisLength,
					hypothesis.getMetricalMeasure(), hypothesis.getSubBeatLength(), hypothesis.getAnacrusisLength() * hypothesis.getSubBeatLength()));
		}
		
		System.out.println("CORRECT = " + correctMeasure + " length=" + correctSubBeatLength + " anacrusis=" + anacrusisLength);
		
		for (int probRank = 0; probRank < resultsByProb.size(); probRank++) {
			MetricalLpcfgMetricalModelState state = resultsByProb.get(probRank);
			String prequel = "    ";
			
			if (state.getMetricalMeasure().equals(correctMeasure) &&
					correctSubBeatLength == state.getSubBeatLength() &&
					state.getAnacrusisLength() * state.getSubBeatLength() == anacrusisLength) {
				prequel = "*** ";
			}
			
			if (VERBOSE || prequel.equals("*** ") || probRank == 0) {
				System.out.println(prequel + probRank + " === " + state);
			}
			
			if (VERBOSE) {
				System.out.println(state.getLocalGrammar());
			}
		}
	}

	/**
	 * Extract the grammar rules from the given generator from the given grammar.
	 * 
	 * @param grammar The grammar we want to extract the given rules from.
	 * @param toExtract The generator which contains the rules we want to extract.
	 * @throws MetricalLpcfgElementNotFoundException A tree to be extracted was not found.
	 */
	private static void extractFromGrammar(MetricalLpcfg grammar, MetricalLpcfgGenerator toExtract) throws MetricalLpcfgElementNotFoundException {
		for (MetricalLpcfgTree tree : toExtract.getGrammar().getTrees()) {
			if (VERBOSE) {
				System.out.println("Extracting tree:");
				System.out.println(tree);
			}
			grammar.extractTree(tree);
		}
	}
	
	/**
	 * Add the grammar rules from the given generator to the given grammar.
	 * 
	 * @param grammar The grammar we want to add the given rules to.
	 * @param toAdd The generator which contains the rules we want to add.
	 */
	private static void addToGrammar(MetricalLpcfg grammar, MetricalLpcfgGenerator toAdd) {
		for (MetricalLpcfgTree tree : toAdd.getGrammar().getTrees()) {
			if (VERBOSE) {
				System.out.println("Adding tree:");
				System.out.println(tree);
			}
			grammar.addTree(tree);
		}
	}

	/**
	 * Generate a grammar from the given files with the given Runner.
	 * 
	 * @param midiFiles The Files we want to generate a grammar from.
	 * @param anacrusisFiles The anacrusis files for the given midiFiles.
	 * @param useChannel True if to use channels as the gold standard voice in MIDI files.
	 * False for tracks.
	 */
	private static MetricalLpcfgGenerator generateGrammar(List<File> midiFiles, List<File> anacrusisFiles, boolean useChannel) {
		// We have files and are ready to run!
		MetricalLpcfgGenerator generator = new MetricalLpcfgGenerator();
		
		for (File file : midiFiles) {
			if (VERBOSE) {
				System.out.println("Parsing " + file);
			}
			
			TimeTracker tt = new TimeTracker();
			tt.setAnacrusis(getAnacrusisLength(file, anacrusisFiles));
			NoteListGenerator nlg = new NoteListGenerator(tt);
			
			// PARSE!
			EventParser ep = null;
			try {
				ep = Runner.parseFile(file, nlg, tt, useChannel);
				
			} catch (InvalidMidiDataException | IOException e) {
				System.err.println("Error parsing file " + file + ":\n" + e.getLocalizedMessage());
				
				if (VERBOSE) {
					e.printStackTrace();
				}
				
				continue;
			}
			
			if (tt.getFirstTimeSignature().getNumerator() == TimeSignature.IRREGULAR_NUMERATOR) {
				System.err.println("Irregular meter detected. Skipping song " + file);
				continue;
			}
			
			if (tt.getAllTimeSignatures().size() != 1) {
				System.err.println("Meter change detected. Skipping song " + file);
				continue;
			}
			
			// RUN!
			JointModel jm;
			try {
			jm = new JointModel(
					new FromFileVoiceSplittingModelState(ep),
					new FromFileBeatTrackingModelState(tt),
					new FromFileMetricalModelState(tt));
			
			} catch (InvalidMidiDataException e) {
				System.err.println("Error parsing file " + file + ":\n" + e.getLocalizedMessage());
				
				if (VERBOSE) {
					e.printStackTrace();
				}
				
				continue;
			}
			
			Runner.performInference(jm, nlg);
			
			// GRAMMARIZE
			generator.parseSong(jm, tt);
		}
		
		return generator;
	}
	
	/**
	 * Get the anacrusis length for the given test file given the anacrusis files.
	 * 
	 * @param testFile The file for which we want the anacrusis.
	 * @param anacrusisFiles The anacrusisFiles.
	 * @return The anacrusis length for the given test file.
	 */
	private static int getAnacrusisLength(File testFile, List<File> anacrusisFiles) {
		String anacrusisFileName = testFile.getName() + ".anacrusis";
		for (File file : anacrusisFiles) {
			if (file.getName().equals(anacrusisFileName)) {
				try {
					BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
					
					int anacrusisLength = Integer.parseInt(bufferedReader.readLine());
					bufferedReader.close();
					return anacrusisLength;
					
				} catch (FileNotFoundException e) {
				} catch (NumberFormatException e) {
				} catch (IOException e) {}
			}
		}
		
		return 0;
	}
	
	/**
	 * Get and return a List of every file beneath the given one.
	 * 
	 * @param file The head File.
	 * @return A List of every File under the given head.
	 */
	public static List<File> getAllFilesRecursive(File file) {
		List<File> files = new ArrayList<File>();
		
		if (file.isFile()) {
			files.add(file);
			
		} else if (file.isDirectory()) {
			File[] fileList = file.listFiles();
			if (fileList != null) {
				for (File subFile : fileList) {
					files.addAll(getAllFilesRecursive(subFile));
				}
			}
		}
		
		Collections.sort(files);
		return files;
	}

	/**
	 * An argument error occurred. Print the usage help info to standard error, and then exit.
	 * <p>
	 * NOTE: This method calls <code>System.exit(1)</code> and WILL NOT return.
	 * 
	 * @param message The error message to print at the beginning of the exception.
	 */
	private static void argumentError(String message) {
		System.err.println(message);
		
		StringBuilder sb = new StringBuilder("USAGE: metdetection.Main [-T] [-l] [-x] [-a AnacrusisFiles] [-g File | -t File] FILES\n");
		sb.append("-v = Verbose output\n");
		sb.append("-T = Use tracks as gold standard voices (instead of channels)\n");
		sb.append("-l = Do NOT use lexicalization\n");
		sb.append("-a AnacrusisFiles = Use the given AnacrusisFiles for FILES\n");
		sb.append("-g File = Generate a grammar and write it out to the given file\n");
		sb.append("-t File = Load a grammar in for testing from the given file. (Can use -t multiple times to combine different model files)\n");
		sb.append("-x = Temporarily extract each file currently being tested from the loaded grammar when testing\n\n");
		sb.append("NOTE: -t and -g CANNOT be used together.\n");
		sb.append("FILES = List of MIDI files or directories to be searched recursively.");
		sb.append(" These will be either tested (if used with -t) or used to generate a");
		sb.append(" grammar (if used with -g).");
		
		System.err.println(sb.toString());
		System.exit(1);
	}
}
