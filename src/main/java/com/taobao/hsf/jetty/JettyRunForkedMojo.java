// ========================================================================
// Copyright (c) Webtide LLC
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package com.taobao.hsf.jetty;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.taobao.hsf.jetty.component.Starter;
import com.taobao.ticket.hsfjetty.Counter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jetty.util.IO;


/**
 * <p>
 * This goal is used to assemble your webapp into a war and automatically deploy it to Jetty in a forked JVM.
 * </p>
 * <p>
 * You need to define a jetty.xml file to configure connectors etc and a context xml file that sets up anything special
 * about your webapp. This plugin will fill in the:
 * <ul>
 * <li>context path
 * <li>classes
 * <li>web.xml
 * <li>root of the webapp
 * </ul>
 * Based on a combination of information that you supply and the location of files in your unassembled webapp.
 * </p>
 * <p>
 * There is a <a href="run-war-mojo.html">reference guide</a> to the configuration parameters for this plugin, and more detailed information
 * with examples in the <a href="http://docs.codehaus.org/display/JETTY/Maven+Jetty+Plugin/">Configuration Guide</a>.
 * </p>
 * <p/>
 * goal run-forked
 * requiresDependencyResolution compile+runtime
 * execute phase="test-compile"
 * description Runs Jetty in forked JVM on an unassembled webapp
 */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME
        , defaultPhase = LifecyclePhase.NONE)
public class JettyRunForkedMojo extends AbstractJettyMojo {
    public String PORT_SYSPROPERTY = "jetty.port";

    /**
     * Whether or not to include dependencies on the plugin's classpath with &lt;scope&gt;provided&lt;/scope&gt;
     * Use WITH CAUTION as you may wind up with duplicate jars/classes.
     * parameter  default-value="false"
     */

    @Parameter(defaultValue = "false")
    protected boolean useProvidedScope;


    /**
     * If true, the &lt;testOutputDirectory&gt;
     * and the dependencies of &lt;scope&gt;test&lt;scope&gt;
     * will be put first on the runtime classpath.
     * parameter alias="useTestClasspath" default-value="false"
     */

    @Parameter(defaultValue = "false", alias = "useTestClasspath")
    private boolean useTestScope;


    /**
     * Directories that contain static resources
     * for the webapp. Optional.
     * <p/>
     * parameter
     */
    @Parameter
    private File[] resourceBases;
    /**
     * If true, the webAppSourceDirectory will be first on the list of
     * resources that form the resource base for the webapp. If false,
     * it will be last.
     * <p/>
     * parameter  default-value="true"
     */
    @Parameter(defaultValue = "true")
    private boolean baseAppFirst;


    /**
     * Location of jetty xml configuration files whose contents
     * will be applied before any plugin configuration. Optional.
     * parameter
     */
    @Parameter
    private String jettyXml;


    /**
     * Location of a context xml configuration file whose contents
     * will be applied to the webapp AFTER anything in &lt;webAppConfig&gt;.Optional.
     * parameter
     */
    @Parameter
    private String contextXml;


    /**
     * parameter expression="${jetty.skip}" default-value="false"
     */
    @Parameter(defaultValue = "false", property = "jetty.skip")
    private boolean skip;


    /**
     * Arbitrary jvm args to pass to the forked process
     * parameter
     */
    @Parameter(defaultValue = "-showversion", required = false)
    private String jvmArgs;


    /**
     * parameter expression="true" default-value="true"
     */
    @Parameter(defaultValue = "true")
    private boolean waitForChild;


    /**
     * parameter default-value="50"
     */
    @Parameter(defaultValue = "50")
    private int maxStartupLines;


    private Process forkedProcess;

    private Random random;


    public class ShutdownThread extends Thread {
        public ShutdownThread() {
            super("RunForkedShutdown");
        }

        public void run() {
            if (forkedProcess != null && waitForChild) {
                forkedProcess.destroy();
            }
        }
    }

    @Override
    public void checkPomConfiguration() throws MojoExecutionException {

    }

    @Override
    public void configureScanner() throws MojoExecutionException {

    }

    @Override
    public void applyJettyXml() throws Exception {

    }

    @Override
    public void finishConfigurationBeforeStart() throws Exception {

    }

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Configuring Jetty for project: " + project.getName());
        if (skip) {
            getLog().info("Skipping Jetty start: jetty.skip==true");
            return;
        }
        PluginLog.setLog(getLog());
        Runtime.getRuntime().addShutdownHook(new ShutdownThread());
        random = new Random();
        startJettyRunner();
    }

    @Override
    public void restartWebApp(boolean reconfigureScanner) throws Exception {

    }


    public List<String> getProvidedJars() throws MojoExecutionException {
        //if we are configured to include the provided dependencies on the plugin's classpath
        //(which mimics being on jetty's classpath vs being on the webapp's classpath), we first
        //try and filter out ones that will clash with jars that are plugin dependencies, then
        //create a new classloader that we setup in the parent chain.
        if (useProvidedScope) {

            List<String> provided = new ArrayList<String>();
            for (Iterator<Artifact> iter = project.getArtifacts().iterator(); iter.hasNext(); ) {
                Artifact artifact = iter.next();
                if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) && !isPluginArtifact(artifact)) {
                    provided.add(artifact.getFile().getAbsolutePath());
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Adding provided artifact: " + artifact);
                    }
                }
            }
            return provided;

        } else
            return null;
    }

    /* ------------------------------------------------------------ */
    public File prepareConfiguration() throws MojoExecutionException {
        try {
            //work out the configuration based on what is configured in the pom
            File propsFile = new File(target, getProject().getArtifactId() + "-hsf-jetty-maven-plugin.properties");
            if (propsFile.exists())
                propsFile.delete();

            propsFile.createNewFile();
            //propsFile.deleteOnExit();

            Properties props = new Properties();

            File webXML = getAutoconfigReplacer().getWebXml("/WEB-INF/web.xml");
            //web.xml
            if (webXML != null && webXML.exists() && !webXML.isDirectory()) {
                props.put("web.xml", webXML.getAbsolutePath());
            } else {
                props.put("web.xml", getWebXml());
            }

            if (webPort > 0) {
                props.put("web.port", String.valueOf(webPort));
            } else {

            }

            //sort out the context path
            if (contextPath != null)
                props.put("context.path", contextPath);

            //sort out the tmp directory (make it if it doesn't exist)
            if (tmpDirectory != null) {
                if (!tmpDirectory.exists())
                    tmpDirectory.mkdirs();
                props.put("tmp.dir", tmpDirectory.getAbsolutePath());
            }

            //sort out base dir of webapp

            if (getWebAppSourceDirectory() != null)
                props.put("base.dir", getWebAppSourceDirectory().getAbsolutePath());

            //sort out the resource base directories of the webapp
            StringBuilder builder = new StringBuilder();
            if (baseAppFirst) {
                add((getWebAppSourceDirectory() == null ? null : getWebAppSourceDirectory().getAbsolutePath()), builder);
                if (resourceBases != null) {
                    for (File resDir : resourceBases)
                        add(resDir.getAbsolutePath(), builder);
                }
            } else {
                if (resourceBases != null) {
                    for (File resDir : resourceBases)
                        add(resDir.getAbsolutePath(), builder);
                }
                add((getWebAppSourceDirectory() == null ? null : getWebAppSourceDirectory().getAbsolutePath()), builder);
            }
            props.put("res.dirs", builder.toString());

            //web-inf classes
            List<File> classDirs = getClassesDirs();
            StringBuffer strbuff = new StringBuffer();
            for (int i = 0; i < classDirs.size(); i++) {
                File f = classDirs.get(i);
                strbuff.append(f.getAbsolutePath());
                if (i < classDirs.size() - 1)
                    strbuff.append(",");
            }

            if (getClassesDirectory() != null) {
                props.put("classes.dir", getClassesDirectory().getAbsolutePath());
            }

            if (useTestScope && getTestClassesDirectory() != null) {
                props.put("testClasses.dir", getTestClassesDirectory().getAbsolutePath());
            }

            //web-inf lib
            List<File> deps = getDependencyFiles();
            strbuff.setLength(0);
            for (int i = 0; i < deps.size(); i++) {
                File d = deps.get(i);
                strbuff.append(d.getAbsolutePath());
                if (i < deps.size() - 1)
                    strbuff.append(",");
            }
            props.put("lib.jars", strbuff.toString());

            //any overlays
            List<File> overlays = getOverlays();
            strbuff.setLength(0);
            for (int i = 0; i < overlays.size(); i++) {
                File f = overlays.get(i);
                strbuff.append(f.getAbsolutePath());
                if (i < overlays.size() - 1)
                    strbuff.append(",");
            }
            props.put("overlay.files", strbuff.toString());


            //autoconfig output
            Set<String> output = getAutoconfigReplacer().getAutoconfigIncludeFile();
            strbuff.setLength(0);
            for (String relativePath : output) {
                strbuff.append(relativePath).append(",");
            }

            props.put("autoconfig.dir", getAutoconfigTempDirectory().getAbsolutePath());
            if (strbuff.length() > 0) {
                props.put("autoconfig.output", strbuff.substring(0, strbuff.length() - 1));
            }


            if (getHsfDirectory() != null && getHsfDirectory().exists()) {
                props.put("hsf.dir", getHsfDirectory().getAbsolutePath());
            }

            props.store(new BufferedOutputStream(new FileOutputStream(propsFile)), "properties for forked webapp");
            return propsFile;
        } catch (Exception e) {
            throw new MojoExecutionException("Prepare webapp configuration", e);
        }
    }

    private void add(String string, StringBuilder builder) {
        if (string == null)
            return;
        if (builder.length() > 0)
            builder.append(",");
        builder.append(string);
    }

    private List<File> getClassesDirs() {
        List<File> classesDirs = new ArrayList<File>();

        //if using the test classes, make sure they are first
        //on the list
        if (useTestScope && (getTestClassesDirectory() != null))
            classesDirs.add(getTestClassesDirectory());

        if (getClassesDirectory() != null)
            classesDirs.add(getClassesDirectory());

        return classesDirs;
    }


    private List<File> getOverlays()
            throws MalformedURLException, IOException {
        List<File> overlays = new ArrayList<File>();
        for (Iterator<Artifact> iter = project.getArtifacts().iterator(); iter.hasNext(); ) {
            Artifact artifact = (Artifact) iter.next();

            if (artifact.getType().equals("war"))
                overlays.add(artifact.getFile());
        }

        return overlays;
    }


    private List<File> getDependencyFiles() {
        List<File> dependencyFiles = new ArrayList<File>();
        for (Iterator<Artifact> iter = getProject().getArtifacts().iterator(); iter.hasNext(); ) {
            Artifact artifact = (Artifact) iter.next();

            if (((!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) && (!Artifact.SCOPE_TEST.equals(artifact.getScope())))
                    ||
                    (useTestScope && Artifact.SCOPE_TEST.equals(artifact.getScope()))) {
                dependencyFiles.add(artifact.getFile());
                getLog().debug("Adding artifact " + artifact.getFile().getName() + " for WEB-INF/lib ");
            }
        }

        return dependencyFiles;
    }

    public boolean isPluginArtifact(Artifact artifact) {
        if (getPluginArtifacts() == null || getPluginArtifacts().isEmpty())
            return false;

        boolean isPluginArtifact = false;
        for (Iterator<Artifact> iter = getPluginArtifacts().iterator(); iter.hasNext() && !isPluginArtifact; ) {
            Artifact pluginArtifact = iter.next();
            if (getLog().isDebugEnabled()) {
                getLog().debug("Checking " + pluginArtifact);
            }
            if (pluginArtifact.getGroupId().equals(artifact.getGroupId()) && pluginArtifact.getArtifactId().equals(artifact.getArtifactId()))
                isPluginArtifact = true;
        }


        return isPluginArtifact;
    }

    private String getCurrentPluginPath() throws URISyntaxException {
        CodeSource codeSource = JettyRunForkedMojo.class.getProtectionDomain().getCodeSource();
        File jarFile = new File(codeSource.getLocation().toURI().getPath());
//        String jarDir = jarFile.getParentFile().getPath();
        return jarFile.getAbsolutePath();
    }

    private Set<Artifact> getExtraJars() throws Exception {
        Set<Artifact> extraJars = new HashSet<Artifact>();


        Set<Artifact> projectPlugins = getProject().getPluginArtifacts();

        if (projectPlugins != null) {
            Iterator itor = projectPlugins.iterator();
            while (itor.hasNext()) {
                Artifact a = (Artifact) itor.next();
                if (a.getArtifactId().equals(getPlugin().getArtifactId()) ||
                        a.getArtifactId().equals(getCurrentPluginArtifactId())) //get the hsfjetty-maven-plugin jar
                {
                    extraJars.add(a);
                }
            }
        }

        return extraJars;
    }


    /* ------------------------------------------------------------ */
    public void startJettyRunner() throws MojoExecutionException {
        try {

            List<String> cmd = makeCmd();

            String token = createToken();
            cmd.add("--token");
            cmd.add(token);

            getLog().info("");
            getLog().info("fork process cmd:" + cmd);
            getLog().info("");
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream(true);

//            builder.directory(project.getBasedir());
            builder.directory(new File(project.getBuild().getDirectory()));

            forkedProcess = builder.start();
            PluginLog.getLog().info("Forked process starting");

            if (waitForChild) {
                startPump("", forkedProcess.getInputStream());
//                startPump("STDERR",forkedProcess.getErrorStream());
                int exitcode = forkedProcess.waitFor();
                PluginLog.getLog().info("Forked execution exit: " + exitcode);
            } else {
                //we're not going to be reading the stderr as we're not waiting for the child to finish
                forkedProcess.getErrorStream().close();

                //wait for the child to be ready before terminating.
                //child indicates it has finished starting by printing on stdout the token passed to it
                try {
                    LineNumberReader reader = new LineNumberReader(new InputStreamReader(forkedProcess.getInputStream()));
                    String line = "";
                    int attempts = maxStartupLines; //max lines we'll read trying to get token
                    while (attempts > 0 && line != null) {
                        --attempts;
                        line = reader.readLine();
                        if (line != null && line.startsWith(token))
                            break;
                    }

                    reader.close();

                    if (line != null && line.trim().equals(token))
                        PluginLog.getLog().info("Forked process started.");
                    else {
                        String err = (line == null ? "" : (line.startsWith(token) ? line.substring(token.length()) : line));
                        PluginLog.getLog().info("Forked process startup errors" + (!"".equals(err) ? ", received: " + err : ""));
                    }
                } catch (Exception e) {
                    throw new MojoExecutionException("Problem determining if forked process is ready: " + e.getMessage());
                }
            }

        } catch (InterruptedException ex) {
            if (forkedProcess != null && waitForChild)
                forkedProcess.destroy();

            throw new MojoExecutionException("Failed to start Jetty within time limit");
        } catch (Exception ex) {
            if (forkedProcess != null && waitForChild)
                forkedProcess.destroy();

            throw new MojoExecutionException("Failed to create Jetty process", ex);
        }
    }

    private List<String> makeCmd() throws Exception {
        File props = prepareConfiguration();

        List<String> cmd = new ArrayList<String>();
        cmd.add(getJavaBin());
//        cmd.add("java");
//        cmd.add("-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000");
        cmd.add("-Xdebug");
        cmd.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8787");
        File hotcodeConfigFile = new File(getProject().getBasedir(), "workspace.xml");
        if (hotcodeConfigFile.exists()) {
            File hotCode = new File(getHsfJettyTempDirectory(), "hotcode.jar");
            if (hotCode.exists()) {
//                cmd.add("-javaagent:d:/env/hotcode/hotcode.jar");
                cmd.add("-javaagent:" + hotCode.getAbsolutePath());
            } else {
                hotCode = downloadHotCode();
                if (hotCode != null && hotCode.exists()) {
                    cmd.add("-javaagent:" + hotCode.getAbsolutePath());
                }
            }
            cmd.add("-Dhotcode.confFile=" + hotcodeConfigFile.getAbsolutePath());
        }
        cmd.add("-noverify");

        if (jvmArgs != null) {
            String[] args = jvmArgs.split(" ");
            for (int i = 0; args != null && i < args.length; i++) {
                if (args[i] != null && !"".equals(args[i]))
                    cmd.add(args[i].trim());
            }
        }

        String classPath = getClassPath();
        if (classPath != null && classPath.length() > 0) {
//            cmd.add("-cp");
            cmd.add("-classpath");
            cmd.add(classPath);
        }

        cmd.add(Starter.class.getCanonicalName());

        if (stopPort > 0 && stopKey != null) {
            cmd.add("--stop-port");
            cmd.add(Integer.toString(stopPort));
            cmd.add("--stop-key");
            cmd.add(stopKey);
        }
        if (jettyXml != null) {
            cmd.add("--jetty-xml");
            cmd.add(jettyXml);
        }

        if (contextXml != null) {
            cmd.add("--context-xml");
            cmd.add(contextXml);
        }

        cmd.add("--props");
        cmd.add(props.getAbsolutePath());
        return cmd;
    }

    private File downloadHotCode() {
        HttpURLConnection urlConnection = null;
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            URL url = new URL("http://hotcode.goldendoc.org/download/hotcode.jar");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
//            urlConnection.setRequestProperty("Accept-Language", "zh-CN");
//            urlConnection.setRequestProperty("Referer", url.toString());
//            urlConnection.setRequestProperty("Charset", "UTF-8");
//            urlConnection.setRequestProperty("Range", "bytes=" + begin + "-" + end);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.connect();
            inputStream = urlConnection.getInputStream();
            String saveFilePath = getHsfJettyTempDirectory().getAbsolutePath() + File.separator + "hotcode.jar";

            // opens an output stream to save into file
            outputStream = new FileOutputStream(saveFilePath);
            int bytesRead = -1;
            byte[] buffer = new byte[2046];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return new File(saveFilePath);
        } catch (Exception e) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }


    public String getHsfJars() {
        File hsfJarDir = new File(getHsfDirectory(), "lib");
        String[] jarPaths = hsfJarDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (jarPaths == null || jarPaths.length == 0)
            return "";
        StringBuilder classPath = new StringBuilder();

        for (String jarPath : jarPaths) {
            if (classPath.length() > 0) {
                classPath.append(File.pathSeparator);
            }
            classPath.append(hsfJarDir + File.separator + jarPath);
        }
        return classPath.toString();
    }

    public String getClassPath() throws Exception {
        StringBuilder classPath = new StringBuilder();
        for (Object obj : getPluginArtifacts()) {
            Artifact artifact = (Artifact) obj;
            //knock out the antx-autoconfig from the classpath because of the logback version confliction
            if ((artifact.getArtifactId().contains("antx-autoconfig")
                    || artifact.getArtifactId().contains("maven-autoconfig")
                    || artifact.getArtifactId().contains("autoconfig-maven")
                    && artifact.getGroupId().contains("com.alibaba"))) {
                getLog().info("ingored plugin dependency in the cp:" + artifact.toString());
                continue;
            }
            if ("jar".equals(artifact.getType())) {
                if (classPath.length() > 0) {
                    classPath.append(File.pathSeparator);
                }
                classPath.append(artifact.getFile().getAbsolutePath());

            }
        }

        //Any jars that we need from the plugin environment (like the ones containing Starter class)
        /*
        Set<Artifact> extraJars = getExtraJars();
        for (Artifact a:extraJars)
        { 
            classPath.append(File.pathSeparator);
            classPath.append(a.getFile().getAbsolutePath()); a.getFile == null bug?
        }
        */
        String pluginPath = getCurrentPluginPath();
        if (pluginPath != null) {
            classPath.append(File.pathSeparator);
            classPath.append(pluginPath);
        }


        //Any jars that we need from the project's dependencies because we're useProvided
        List<String> providedJars = getProvidedJars();
        if (providedJars != null && !providedJars.isEmpty()) {
            for (String jar : providedJars) {
                classPath.append(File.pathSeparator);
                classPath.append(jar);
                if (getLog().isDebugEnabled()) getLog().debug("Adding provided jar: " + jar);
            }
        }
        String hsfJars = getHsfJars();
        classPath.append(File.pathSeparator).append(hsfJars);

        //web project dependency lib
        /*List<File> deps = getDependencyFiles();
        for (File dep : deps) {
            classPath.append(File.pathSeparator);
            classPath.append(dep.getAbsolutePath());
        }*/
        return classPath.toString();
    }

    private String getJavaBin() {
        String javaexes[] = new String[]
                {"java", "java.exe"};

//        File javaHomeDir = new File(System.getProperty("java.home"));
        String javaHomeDir = System.getenv("JAVA_HOME");
        for (String javaexe : javaexes) {
            File javabin = new File(javaHomeDir, fileSeparators("bin/" + javaexe));
            if (javabin.exists() && javabin.isFile()) {
                return javabin.getAbsolutePath();
            }
        }

        return "java";
    }

    public static String fileSeparators(String path) {
        StringBuilder ret = new StringBuilder();
        for (char c : path.toCharArray()) {
            if ((c == '/') || (c == '\\')) {
                ret.append(File.separatorChar);
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    public static String pathSeparators(String path) {
        StringBuilder ret = new StringBuilder();
        for (char c : path.toCharArray()) {
            if ((c == ',') || (c == ':')) {
                ret.append(File.pathSeparatorChar);
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }


    private String createToken() {
        return Long.toString(random.nextLong() ^ System.currentTimeMillis(), 36).toUpperCase();
    }


    private void startPump(String mode, InputStream inputStream) {
        ConsoleStreamer pump = new ConsoleStreamer(mode, inputStream);
        Thread thread = new Thread(pump, "ConsoleStreamer/" + mode);
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * Simple streamer for the console output from a Process
     */
    private static class ConsoleStreamer implements Runnable {
        private String mode;
        private BufferedReader reader;

        public ConsoleStreamer(String mode, InputStream is) {
            this.mode = mode;
            this.reader = new BufferedReader(new InputStreamReader(is));
        }



        public void run() {
            boolean print1st = true;
            Counter counter = new Counter();
            int count = 0;
            try{
                count = counter.getCountAndInc();
            }catch (Exception e){
                                /* ignore */
            }
            String line;
            try {
                while ((line = reader.readLine()) != (null)) {
                    if(print1st){
                        if(count > 0){
                            if(line.startsWith("------")){
                                print1st = false;
                                System.out.println("hsfjetty-maven-plugin has been used " + count + " times");
                            }
                        }
                    }
                    System.out.println(mode + line);
                }
            } catch (IOException ignore) {
                /* ignore */
            } finally {
                IO.close(reader);
            }
        }
//        public static void main(String[] args){
//            ConsoleStreamer console = new ConsoleStreamer("",System.in);
//            try {
//                int count = console.useCounter();
//                System.out.println(count);
//            } catch (Exception e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
    }
}
