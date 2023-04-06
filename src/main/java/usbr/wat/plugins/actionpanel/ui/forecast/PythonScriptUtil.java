package usbr.wat.plugins.actionpanel.ui.forecast;

import com.rma.model.Project;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PythonScriptUtil
{
    private static final Logger LOGGER = Logger.getLogger(PythonScriptUtil.class.getName());
    private PythonScriptUtil()
    {
        throw new AssertionError("Utility Class. Don't instantiate");
    }

    static
    {
        initInterpreter();
    }

    public static <T> T runScript(Path scriptFilePath, String functionName, Class<T> returnType, Object... args)
    {
        try (PythonInterpreter pythonInterpreter = new PythonInterpreter())
        {
            Path scriptAbsPath = Paths.get(Project.getCurrentProject().getAbsolutePath(scriptFilePath.toString()));
            Path parentDir = scriptAbsPath.getParent();
            pythonInterpreter.exec("import sys");
            pythonInterpreter.exec("sys.path.append('')");
            pythonInterpreter.exec(String.format("sys.path.append('%s')", parentDir.toString()));
            pythonInterpreter.execfile(scriptAbsPath.toString());
            PyObject function = pythonInterpreter.get(functionName);
            PyObject[] pyArgs = new PyObject[args.length];
            for(int i=0; i < args.length; i++)
            {
                pyArgs[i] = Py.java2py(args[i]);
            }
            PyObject result = function.__call__(pyArgs);
            return returnType.cast(result.__tojava__(returnType));
        }
    }

    private static void createScriptsDir()
    {
        String scriptsDir = "forecast/scripts";
        try
        {
            Path absScriptDir = Paths.get(Project.getCurrentProject().getAbsolutePath(scriptsDir));
            Files.createDirectories(absScriptDir);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.CONFIG, e, () -> "Failed to create " + scriptsDir + " directories");
        }
    }

    private static void initInterpreter()
    {
        createScriptsDir();
        //------------------------------------------------------//
        // make sure we have a valid application home directory //
        //------------------------------------------------------//
        String appHome = hec.lang.ApplicationProperties.getAppHome();
        if (appHome == null)
        {
            appHome = ".";
        }
        appHome = (new File(appHome)).getAbsolutePath();
        if (appHome.endsWith(File.separator+"."))
        {
            appHome = appHome.substring(0, appHome.length() - 2);
        }

        long t1 = System.currentTimeMillis();
        String pythonPath = System.getProperty("python.path");
        if (pythonPath == null)
        {
            pythonPath = appHome;
            String classpath = System.getProperty("java.class.path");
            StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
            String token = null;
            boolean found = false;
            while ( tokenizer.hasMoreTokens())
            {
                token = tokenizer.nextToken();
                if (token.contains("jythonlib.jar"))
                {
                    found = true;
                    String msg = String.format("found jythonlib.jar in classpath%s", token);
                    LOGGER.info(() -> msg);
                    break;
                }
            }
            if(found)
            {
                token = token+"/lib";
            }
            else
            {
                token = appHome+File.separator+"jar"+File.separator+"jythonlib.jar/lib";
            }
            if (!pythonPath.endsWith(File.separator))
            {
                pythonPath += File.separator;
            }
            pythonPath += "scripts" + File.pathSeparator +token;
            pythonPath += File.pathSeparator + Project.getCurrentProject().getAbsolutePath("forecast/scripts");
            System.setProperty("python.path", pythonPath);
        }
        java.util.Properties props = new java.util.Properties();
        props.setProperty("python.path", pythonPath);

        PythonInterpreter.initialize(System.getProperties(), props,
                new String[] {""});
        PySystemState.add_package("hec.rss.model");
        String timeMsg = "initInterp(): creating interpreter took "
                +(System.currentTimeMillis()-t1)+" ms";
        LOGGER.info(timeMsg);
    }
}
