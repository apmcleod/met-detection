# Meter Detection

This is the code and data from my 2017 SMC paper. If you use it, please cite it:

```
@inproceedings{McLeod:17a,
  title={Meter Detection in Symbolic Music Using a Lexicalized {PCFG}},
  author={McLeod, Andrew and Steedman, Mark},
  booktitle={Proceedings of the 14th Sound and Music Computing Conference},
  pages={373--379},
  year={2017}
}
```

Use this code to reproduce the results from the paper. For an updated model, see [met-align](https://github.com/apmcleod/met-align), which is also able to perform meter alignment on live performance and unquantized MIDI. (You can still use it with the `-BFromFile` flag to align the results to a 32nd note tatum like in this package, but met-align contains a few other updates as well (see the referenced papers there).

## Project Overview
The goal of this project is to have a Java program which is able to detect the meter of
a piece from a MIDI file.

NOTE: In order to work well, the notes in the input MIDI files should be split into monophonic voices per MIDI channel. If they are not, the recommended way to do so is to use my [voice-splitting](https://github.com/apmcleod/voice-splitting) package, and run like this:

`$ java -cp bin voicesplitting.voice.hmm.HmmVoiceSplittingModelTester -w voice FILES`
(also add `-l` if the files are live performance).

This will create new files with the extension ".voice" added to the end, with voice separation performed. Use these new .voice files as input for meter alignment.

Note that some MIDI files separate voices by channel, while some
do so by track. This project uses channel by default, but this can be changed by using the
`-T` flag when running from the command line. MIDI files written out by this software will have
voices divided by both channel and track.

## License
You are free to use this software as is allowed by the [MIT License](https://github.com/apmcleod/met-detection/blob/master/License).
I only ask that you please cite it as my work where appropriate, including
the paper on my [website](http://homepages.inf.ed.ac.uk/s1331854/software.html#MeterDetection)
which has been accepted to be presented at the 2017 Sound and Music Computing Conference.

## Documentation
This document contains some basic code examples and a general overview of how to use
the classes in this project. All specific documentation for the code found in this
project can be found in the [Javadocs](https://apmcleod.github.io/met-detection/doc). 

## Installing
The java files can all be compiled into class files in a bin directory using the Makefile
included with the project with the following command: `$ make`.

## Running
Once the class files are installed in the bin directory, the project can be run. To run the
project, use the command `$ java -cp bin metdetection.Main`. The proper arguments for
running are as follows:

`$ java -cp metdetection.Main [-T] [-l] [-x] [-v] [-a AnacrusisFiles] [-g File | -t File] FILES`

ARGS:
 * `-T` = Use tracks as the gold standard voice separation, rather than channels.
 * `-l` = Do NOT use lexicalisation (i.e., use the PCFG, rather than the LPCFG).
 * `-x` = Temporarily extract each file currently being tested from the loaded grammar when testing
 * `-v` =  Use verbose printing.
 * `-a AnacrusisFiles` = Use the given AnacrusisFiles (directories are searched recursively)
                           to get gold standard meters. (See [Anacrusis Files](#anacrusis-files)).
 * `-g File` = Generate a grammar from the given FILES and write it out to the given File.
 * `-t File` = Load a grammar from the given File for testing the FILES.
 
FILES should be a list of 1 or more MIDI or kern files or directories containing only MIDI or kern
files. Any directory entered will be searched recursively for files.

### Anacrusis Files
Anacrusis files must be used because a MIDI file on its own does not have a way of storing the phase
of the meter (that is, the anacrusis length). By default, this program will assume that the anacrusis
length is 0 (i.e., that time 0 in the MIDI file is the beginning of the first bar of the piece).

If that is not the case, it is necessary to pass the program an anacrusis file for the corresponding MIDI
file. If the MIDI file is called `file.mid`, the anacrusis file must be called `file.mid.anacrusis`.

The file must contain a single line with a single number: the number of MIDI ticks which lie before the
first full bar in the corresponding MIDI file.

These anacrusis files must be used in BOTH training and testing.

### Output
For each FILE in FILES, the program generates the following output (in non-verbose mode):

```
Testing FILE.mid
TP = 2
FP = 1
FN = 1
P = 0.6666666666666666
R = 0.6666666666666666
F1 = 0.6666666666666666
CORRECT = M_4,2 length=4 anacrusis=0
    0 === M_4,2 length=4 anacrusis=4 Score=-1139.6134383900494
*** 2 === M_4,2 length=4 anacrusis=0 Score=-1183.5854709435646
```

Where TP, FP, FN, P, R, and F1 are true positive, false positive, false negative, precision, recall,
and F1 scores given the metric described in our paper, CORRECT is the correct result, and below is
the top resulting metrical hypothesis, followed by the correct guess, if it was not the top one.
See [Evaluation](#evaluation) for more information about calculating the overall F1 for a corpus.

(*** is printed before the correct guess). The first number is the rank of a hypothesis, followed
by the metrical structure. Length is the number of 32nd notes in a single sub-beat, and anacrusis
is the anacrusis length, measured in sub-beats. Score is the log probability of the given hypothesis. 

### Evaluation
The java file src/utils/Evaluation.java contains a main method with many utilities to help in evaluation.
Usage:

`java -cp bin metdetection.utils.Evaluation ARGS`

ARGS:
 * `-l` = Evaluate the input LPCFG output data (from std in, in the format described in [Output](#output)),
          and output (to std out) the overall stats (precision, recall, F1).
 * `-n FILE` = Convert the given file into a note file and print it to std out. This option can be used
                multiple times with different files. This is usefule since Temperley's model runs on note
                files.
 * `-t FILE` = Evaluate a Temperley formatted output file (read from std in) based on the gold standard
                meter read in from the given file (MIDI or kern).
 * `-m FILE_LIST` = Get the meter at the start of each of the files in the file list.
 * `-c FILE_LIST` = Get the number of meters in each of the files and list them.

### Troubleshooting
Most Exceptions that occur while running this program should print a useful error message out to
stderr. In general, there are 2 main types of errors:
 * I/O Errors: These indicate that some I/O error occured while trying to read a MIDI file. These
   could indicate that the program does not have read permission on the given file, or that the
   file does not exist. Check for typos in the file names.
 * MIDI errors: These indicate that the MIDI files given are not in a format that can be read
   by Java. There is currently no way to solve this issue besides looking for new MIDI files.

If the results you are getting seem very low, try using the `-T` flag. Some MIDI files
divide notes into voices by track while others do it by channel. Also, check that you are
giving the model the correct anacrusis files (see [Anacrusis Files](#anacrusis-files)).

## Contact
Please let me know if you are interested in using my work. If you run into any problems installing it,
using it, extending it, or you'd like to see me add any additional features, please let me know either by
email or by submitting an issue on github. Any and all questions are always welcome.

There is a paper which has been accepted to be presented at the 2017 Sound and Music Computing Conference available on
my [website](https://apmcleod.github.io/publications.html) with further documentation.
Please cite this if you use my code or the paper.

Thanks, and enjoy!  
Andrew McLeod  
