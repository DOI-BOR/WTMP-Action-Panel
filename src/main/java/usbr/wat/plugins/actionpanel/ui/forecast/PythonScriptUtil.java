package usbr.wat.plugins.actionpanel.ui.forecast;

import com.rma.model.Project;                       // Provides Project for resolving project-relative file paths to absolute paths

import org.python.core.Py;                          // Provides Py for converting Java objects to their Jython PyObject equivalents
import org.python.core.PyObject;                    // Provides PyObject, the root type for all Jython Python objects and return values
import org.python.core.PySystemState;               // Provides PySystemState for configuring the Jython runtime (e.g. adding packages)
import org.python.util.PythonInterpreter;           // Provides PythonInterpreter for executing Python scripts and calling their functions

import java.io.File;                                // Provides File for constructing and inspecting file system paths as abstract objects
import java.io.IOException;                         // Provides IOException for handling errors when creating the scripts directory
import java.nio.file.Files;                         // Provides Files for creating directories on the file system
import java.nio.file.Path;                          // Provides Path for representing file system paths in a platform-independent way
import java.nio.file.Paths;                         // Provides Paths for constructing Path instances from string segments

import java.util.StringTokenizer;                   // Provides StringTokenizer for splitting the Java classpath into individual path entries
import java.util.logging.Level;                     // Provides Logger for JUL-based logging of initialisation timing and error messages
import java.util.logging.Logger;                    // Provides Level for specifying log severity (CONFIG, INFO) in Logger calls

/**
 * Utility class for executing Jython (Python) scripts from within the Forecast
 * Action Panel. Provides a single static entry point, runScript, for loading a
 * Python source file, locating a named function within it, and invoking that
 * function with Java arguments converted to Jython PyObjects.
 *
 * On class load a static initialiser calls initInterpreter, which:
 * 1. Creates the "forecast/scripts" directory under the current project if absent.
 * 2. Resolves the application home directory and the Jython standard library path.
 * 3. Configures the "python.path" system property so that all subsequent
 *    PythonInterpreter instances can locate the Jython runtime and project scripts.
 * 4. Registers the "hec.rss.model" package with the Jython type system.
 *
 * This class is non-instantiable; attempting to instantiate it throws AssertionError.
 *
 * @see PythonInterpreter
 * @see PyObject
 */
public final class PythonScriptUtil {
    /**
     * JUL logger scoped to this class for initialisation timing and error reporting.
     */
    private static final Logger LOGGER = Logger.getLogger(PythonScriptUtil.class.getName());

    /**
     * Private constructor that prevents instantiation of this utility class.
     * All functionality is provided via static methods.
     *
     * @throws AssertionError always, to guard against reflective instantiation
     */
    private PythonScriptUtil() {
        throw new AssertionError("Utility Class. Don't instantiate");
    }

    /*
     * Static initialiser: runs once when the class is first loaded by the JVM.
     * Delegates to initInterpreter to configure the Jython runtime environment
     * before any script execution is attempted.
     */
    static {
        initInterpreter();
    }

    /**
     * Executes a named Python function from the specified script file and returns
     * the result cast to the requested Java type. A fresh PythonInterpreter is
     * created for each invocation and closed automatically via try-with-resources.
     *
     * The script file path is resolved to an absolute path within the current
     * project before execution. Each Java argument is converted to a Jython
     * PyObject using Py.java2py before being passed to the Python function.
     *
     * @param scriptFilePath the project-relative or absolute path to the .py file
     * @param functionName   the name of the top-level function to call in the script
     * @param returnType     the Class to which the Python return value is cast
     * @param args           zero or more Java objects to pass as arguments to the function
     * @param <T>            the expected Java return type
     * @return the function's return value cast to T
     */
    public static <T> T runScript(Path scriptFilePath, String functionName, Class<T> returnType, Object... args) {
        // Open a new interpreter; try-with-resources ensures it is closed on exit
        try (PythonInterpreter pythonInterpreter = new PythonInterpreter()) {
            // Resolve the script path to an absolute path within the current project
            Path scriptAbsPath = Paths.get(Project.getCurrentProject().getAbsolutePath(scriptFilePath.toString()));

            // Load and execute the script file, making all its top-level definitions available
            pythonInterpreter.execfile(scriptAbsPath.toString());

            // Retrieve the named function object from the interpreter's global namespace
            PyObject function = pythonInterpreter.get(functionName);

            // Convert each Java argument to a Jython PyObject for the function call
            PyObject[] pyArgs = new PyObject[args.length];
            for (int i = 0; i < args.length; i++) {
                pyArgs[i] = Py.java2py(args[i]);
            }

            // Invoke the Python function and convert its return value back to a Java object
            PyObject result = function.__call__(pyArgs);
            return returnType.cast(result.__tojava__(returnType));
        }
    }

    /**
     * Creates the "forecast/scripts" directory tree under the current project if it
     * does not already exist. This ensures the Jython python.path includes a valid
     * project-local scripts directory before any interpreter is initialised.
     * Logs a CONFIG-level message if directory creation fails.
     */
    private static void createScriptsDir() {
        String scriptsDir = "forecast/scripts";

        try {
            // Resolve the relative scripts directory to an absolute path and create it
            Path absScriptDir = Paths.get(Project.getCurrentProject().getAbsolutePath(scriptsDir));
            Files.createDirectories(absScriptDir);

        } catch (IOException e) {
            LOGGER.log(Level.CONFIG, e, () -> "Failed to create " + scriptsDir + " directories");
        }
    }

    /**
     * Configures the Jython runtime environment for this JVM session. This method is
     * called exactly once from the static initialiser and performs the following steps:
     *
     * 1. Calls createScriptsDir to ensure the project scripts directory exists.
     * 2. Determines the application home directory from ApplicationProperties,
     * defaulting to the current working directory if none is set.
     * 3. Builds the "python.path" property if it has not already been set externally,
     * by searching the Java classpath for "jythonlib.jar" and constructing the
     * path to include the Jython standard library, the application scripts folder,
     * and the project-local forecast/scripts directory.
     * 4. Initialises the PythonInterpreter with the resolved properties.
     * 5. Registers the "hec.rss.model" Java package with the Jython type system.
     * 6. Logs the total time taken for initialisation.
     */
    private static void initInterpreter() {
        // Ensure the project scripts directory exists before configuring the interpreter
        createScriptsDir();

        //------------------------------------------------------//
        // make sure we have a valid application home directory //
        //------------------------------------------------------//
        String appHome = hec.lang.ApplicationProperties.getAppHome();

        // Fall back to the current working directory when no app home is configured
        if (appHome == null) {
            appHome = ".";
        }

        // Normalise to an absolute path so relative references work correctly
        appHome = (new File(appHome)).getAbsolutePath();

        // Strip a trailing separator + dot that File.getAbsolutePath may append for "."
        if (appHome.endsWith(File.separator + ".")) {
            appHome = appHome.substring(0, appHome.length() - 2);
        }

        // Record the start time so initialisation duration can be logged at the end
        long t1 = System.currentTimeMillis();

        String pythonPath = System.getProperty("python.path");

        // Only build python.path if it has not been set externally (e.g. by a launcher script)
        if (pythonPath == null) {
            // Start with the application home as the base of the Python path
            pythonPath = appHome;

            // Search the Java classpath for jythonlib.jar to locate the Jython standard library
            String classpath = System.getProperty("java.class.path");
            StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
            String token = null;
            boolean found = false;

            while (tokenizer.hasMoreTokens()) {
                token = tokenizer.nextToken();

                // Stop as soon as the jythonlib.jar entry is found in the classpath
                if (token.contains("jythonlib.jar")) {
                    found = true;
                    String msg = String.format("found jythonlib.jar in classpath%s", token);
                    LOGGER.info(() -> msg);
                    break;
                }
            }

            // Append the Jython /lib directory relative to the jar's classpath entry
            if (found) {
                token = token + "/lib";
            } else {
                // Fall back to the conventional jar/jythonlib.jar/lib path under appHome
                token = appHome + File.separator + "jar" + File.separator + "jythonlib.jar/lib";
            }

            // Ensure the path ends with a separator before appending the scripts segment
            if (!pythonPath.endsWith(File.separator)) {
                pythonPath += File.separator;
            }

            // Add the application-level scripts directory and the Jython library path
            pythonPath += "scripts" + File.pathSeparator + token;

            // Also add the project-local forecast/scripts directory to the path
            pythonPath += File.pathSeparator + Project.getCurrentProject().getAbsolutePath("forecast/scripts");

            // Persist the assembled path as a system property for this JVM session
            System.setProperty("python.path", pythonPath);
        }

        // Build a Properties object containing the resolved python.path for the interpreter
        java.util.Properties props = new java.util.Properties();
        props.setProperty("python.path", pythonPath);

        // Initialise the PythonInterpreter with both the system properties and our overrides
        PythonInterpreter.initialize(System.getProperties(), props,
                new String[]{""});

        // Register the HEC ResSim model package so Jython can access its Java types
        PySystemState.add_package("hec.rss.model");

        // Log the total time taken for interpreter initialisation
        String timeMsg = "initInterp(): creating interpreter took "  + (System.currentTimeMillis() - t1) + " ms";
        LOGGER.info(timeMsg);
    }
}
