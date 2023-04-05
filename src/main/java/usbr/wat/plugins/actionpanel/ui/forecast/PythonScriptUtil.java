package usbr.wat.plugins.actionpanel.ui.forecast;

import com.rma.model.Project;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import usbr.wat.plugins.actionpanel.model.ActionComputable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PythonScriptUtil
{
    private static final Logger LOGGER = Logger.getLogger(PythonScriptUtil.class.getName());
    private static final PythonInterpreter PYTHON_INTERPRETER = new PythonInterpreter();
    private PythonScriptUtil()
    {
        throw new AssertionError("Utility Class. Don't instantiate");
    }

    static
    {
        initInterpreter();
    }

    public static void runScript(Path scriptFilePath, String functionName, Object... args)
    {
        Path scriptAbsPath = Paths.get(Project.getCurrentProject().getAbsolutePath(scriptFilePath.toString()));
        Path parentDir = scriptAbsPath.getParent();
        PYTHON_INTERPRETER.exec("import sys");
        PYTHON_INTERPRETER.exec(String.format("sys.path.append('%s')", parentDir.toString()));
        PYTHON_INTERPRETER.execfile(scriptAbsPath.toString());
        PyObject function = PYTHON_INTERPRETER.get(functionName);
        PyObject[] pyArgs = new PyObject[args.length];
        for(int i=0; i < args.length; i++)
        {
            pyArgs[i] = Py.java2py(args[i]);
        }
        PyObject result = function.__call__(pyArgs);
        System.out.println(result.toString());
    }

    private static void initInterpreter()
    {
        //------------------------------------------------------//
        // make sure we have a valid application home directory //
        //------------------------------------------------------//
        String appHome = hec.lang.ApplicationProperties.getAppHome();
        if (appHome == null) appHome = ".";
        try {
            appHome = (new File(appHome)).getAbsolutePath();
            if (appHome.endsWith(File.separator+".")) {
                appHome = appHome.substring(0, appHome.length() - 2);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.CONFIG, "Error determining app home directory", e);
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
                    Logger.getLogger(ActionComputable.class.getName()).info("found jythonlib.jar in classpath"+token);
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
            if (!pythonPath.endsWith(File.separator)) pythonPath += File.separator;
            pythonPath += "scripts"+File.pathSeparator+token;

            System.setProperty("python.path", pythonPath);
        }
        java.util.Properties props = new java.util.Properties();
        props.setProperty("python.path", pythonPath);

        PythonInterpreter.initialize(System.getProperties(), props,
                new String[] {""});
        PySystemState sys = Py.getSystemState();
        sys.add_package("hec.rss.model");
        {
            LOGGER.info("initInterp(): creating interpreter took "
                    +(System.currentTimeMillis()-t1)+" ms");
        }
    }
}
