package sqlsolver.api;

import sqlsolver.api.entry.Verification;
import sqlsolver.common.utils.Args;
import sqlsolver.superopt.logic.LogicSupport;
import sqlsolver.superopt.logic.VerificationResult;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Entry {
  public static void main(String[] argStrings) {
    /*
     * There should be at least three arguments:
     * -sql1 indicates the first sql file.
     * -sql2 indicates the second sql file.
     * -schema indicates the schema file.
     * [-print] indicates whether print the result to standard output stream.
     * [-output] indicates where to store the verification result.
     * [-help] indicates that show all the arguments.
     * Each SQL statement in the SQL file should be on a separate line.
     * The SQL statements on the same lines in two SQL files
     * are the two SQL statements that need to be verified for equivalence.
     */
    final Args args = Args.parse(argStrings, 0);
    final String firstQueryPathString = args.getOptional( "sql1", String.class, null);
    final String secondQueryPathString = args.getOptional("sql2", String.class, null);
    final String schemaPathString = args.getOptional("schema", String.class, null);

    final Boolean print = args.getOptional("print", Boolean.class, false);
    final Boolean help = args.getOptional("help", Boolean.class, false);
    final String outputPathString = args.getOptional("output", String.class, null);

    if (help) {
      StringBuilder sb = new StringBuilder("");
      sb.append("java -jar sqlsolver.jar [-help] -sql1=<path/to/query1> -sql2=<path/to/query2>\n" +
                "                        -schema=<path/to/schema> [-print] [-output=<path/to/output>]\n\n");
      sb.append("options:\n");
      sb.append("  -help                    show this help message and exit.\n");
      sb.append("  -sql1=<path/to/query1>   the first sql file.\n");
      sb.append("  -sql2=<path/to/query2>   the second sql file.\n");
      sb.append("  -schema=<path/to/schema> the schema file.\n");
      sb.append("  -print                   print the result to standard output stream.\n");
      sb.append("  -output=<path/to/output> the file that store the verification result.\n");
      System.out.println(sb);
      return;
    }

    if (firstQueryPathString == null) {
      System.err.println("missing first sql file: -sql1=<path/to/query1>");
      return;
    }

    if (secondQueryPathString == null) {
      System.err.println("missing second sql file: -sql2=<path/to/query2>");
      return;
    }

    if (schemaPathString == null) {
      System.err.println("missing schema file: -schema=<path/to/schema>");
      return;
    }


    final Path firstQueryPath = Paths.get(firstQueryPathString);
    final Path secondQueryPath = Paths.get(secondQueryPathString);
    final Path schemaPath = Paths.get(schemaPathString);
    List<VerificationResult> results;

    // verify the SQLs
    try {
      results = Verification.verify(Files.readAllLines(firstQueryPath),
              Files.readAllLines(secondQueryPath),
              Files.readString(schemaPath));
    } catch (IOException e) {
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return;
    }

    // print it throw system.out.println
    if (print) {
      System.out.println(results);
    }

    // output result into the target file
    if (outputPathString != null) {
      try (FileWriter fileWriter = new FileWriter(outputPathString)) {
        for (VerificationResult result : results)
          fileWriter.append(result.toString() + "\n");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}