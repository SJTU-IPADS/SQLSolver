package sqlsolver.common.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.nio.file.StandardOpenOption.*;

public interface IOSupport {

  interface IO<T> {
    T doIO() throws IOException;
  }

  static void checkFileExists(Path path) {
    if (!Files.exists(path)) throw new IllegalArgumentException("no such file: " + path);
  }

  static void appendTo(Path path, Consumer<PrintWriter> writer) {
    try (final var out = new PrintWriter(Files.newOutputStream(path, APPEND, WRITE, CREATE))) {
      writer.accept(out);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }

  static void writeTo(Path path, Consumer<PrintWriter> writer) {
    try (final var out = new PrintWriter(Files.newOutputStream(path, WRITE, CREATE))) {
      writer.accept(out);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }

  static void printWithLock(Path path, Consumer<PrintWriter> writer) {
    try (final var os = new FileOutputStream(path.toFile(), true);
        final var out = new PrintWriter(os);
        final var lock = os.getChannel().lock(); ) {
      writer.accept(out);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }

  static <T> T runIO(IO<T> io) {
    try {
      return io.doIO();
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }

  static <T> Supplier<T> io(IO<T> io) {
    return () -> runIO(io);
  }

  static PrintWriter newPrintWriter(Path path) throws IOException {
    return new PrintWriter(Files.newOutputStream(path));
  }
}
