package usbr.wat.plugins.actionpanel.ui.forecast;

import com.fasterxml.jackson.databind.MappingIterator;                  // Provides MappingIterator for lazily iterating over deserialized CSV rows one at a time
import com.fasterxml.jackson.databind.module.SimpleModule;              // Provides SimpleModule for registering custom deserializers with the Jackson mapper
import com.fasterxml.jackson.dataformat.csv.CsvMapper;                  // Provides CsvMapper as the Jackson entry point for reading CSV-formatted data
import com.fasterxml.jackson.dataformat.csv.CsvSchema;                  // Provides CsvSchema for configuring CSV parsing options such as header detection

import java.io.IOException;                                             // Provides IOException for signaling file read or deserialization failures to callers
import java.nio.file.Files;                                             // Provides Files for opening a buffered reader on a file-system path
import java.nio.file.Path;                                              // Provides Path for representing the file-system location of the CSV file to read

import java.util.ArrayList;                                             // Provides ArrayList as the resizable-array implementation used to collect deserialized rows
import java.util.List;                                                  // Provides the List interface for the ordered collection of deserialized objects returned to callers

/**
 * A non-instantiable utility class for reading CSV files and deserializing their rows
 * into typed Java objects using the Jackson CSV library.
 *
 * {@code CsvReader} provides a single static method, {@link #readCsv(Path, Class)}, that
 * opens a CSV file, infers the column-to-field mapping from the header row, and returns
 * a list of deserialized objects of the requested type.
 *
 * A {@link TrimDeserializer} is registered for {@link String} fields so that leading and
 * trailing whitespace is automatically stripped from all string values read from the file.
 *
 * This class cannot be instantiated; any attempt to do so throws an
 * {@link AssertionError}.
 */
public final class CsvReader {
    /**
     * Private constructor that prevents instantiation of this utility class.
     *
     * @throws AssertionError always, to guard against reflective instantiation attempts
     */
    private CsvReader() {
        throw new AssertionError("Utility class. Don't instantiate");
    }

    /**
     * Reads a CSV file from the given path and deserializes each data row into an
     * object of the specified type, returning all rows as an ordered list.
     *
     * The first row of the file is treated as a header row and used to map column names
     * to the fields of {@code valueType}. A {@link TrimDeserializer} is applied to all
     * {@link String} fields so that whitespace surrounding values is silently removed
     * before binding.
     *
     * The underlying file reader is opened inside a try-with-resources block and is
     * guaranteed to be closed when iteration completes or if an exception is thrown.
     *
     * @param <T>         the type of object each CSV row is deserialized into
     * @param csvFilePath the {@link Path} to the CSV file to read; must exist and be readable
     * @param valueType   the {@link Class} of the target type {@code T}; must be a
     *                    Jackson-compatible POJO with fields matching the CSV column headers
     * @return a new {@link List} containing one deserialized {@code T} instance per
     * data row in the file; empty if the file contains only a header row
     * @throws IOException if the file cannot be opened, read, or if a row cannot be deserialized into the specified type
     */
    public static <T> List<T> readCsv(Path csvFilePath, Class<?> valueType) throws IOException {
        // Create the Jackson CSV mapper that will handle reading and deserialization
        CsvMapper csvMapper = new CsvMapper();

        // Configure the schema to use the first row as column headers for field mapping
        CsvSchema csvSchema = csvMapper.schema().withHeader();

        // Register the TrimDeserializer so all String fields have whitespace stripped automatically
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new TrimDeserializer());
        csvMapper.registerModule(module);

        List<T> retVal = new ArrayList<>();

        // Open the file and iterate over deserialized rows; the iterator is auto-closed on exit
        try (MappingIterator<T> iterator = csvMapper.readerFor(valueType)
                .with(csvSchema)
                .readValues(Files.newBufferedReader(csvFilePath))) {
            while (iterator.hasNext()) {
                // Deserialize the next CSV row into an instance of the target type
                T object = iterator.next();
                retVal.add(object);
            }
        }

        return retVal;
    }
}
