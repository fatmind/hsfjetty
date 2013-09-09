package com.taobao.hsf.jetty;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Display help information on hsfjetty-maven-plugin.<br/>
 * Call
 * 
 * <pre>
 * mvn hsfjetty:help -Ddetail=true -Dgoal=&lt;goal-name&gt;
 * </pre>
 * 
 * to display parameter details.
 * 
 */

//@plexus.component role="org.apache.maven.shared.filtering.MavenResourcesFiltering"
//                           role-hint="default"
//@Component(role = com.taobao.hsf.jetty.HelpMojo.class,hint = "help")
@Mojo(name="help",requiresProject=false)
public class HelpMojo extends AbstractMojo {
    /**
     * If <code>true</code>, display all settable properties for each goal.
     * 
     */
    @Parameter(defaultValue="false",alias="detail")
    private boolean detail;

    /**
     * The name of the goal for which to show help. If unspecified, all goals will be displayed.
     * 
     */
    @Parameter(alias="goal")
    private java.lang.String goal;

    /**
     * The maximum length of a display line, should be positive.
     * 
     */
    @Parameter(alias="lineLength",defaultValue="80")
    private int lineLength;

    /**
     * The number of spaces per indentation level, should be positive.
     * 
     */
    @Parameter(alias="indentSize",defaultValue="2")
    private int indentSize;

    /** {@inheritDoc} */
    public void execute() throws MojoExecutionException {
        if (lineLength <= 0) {
            getLog().warn("The parameter 'lineLength' should be positive, using '80' as default.");
            lineLength = 80;
        }
        if (indentSize <= 0) {
            getLog().warn("The parameter 'indentSize' should be positive, using '2' as default.");
            indentSize = 2;
        }

        StringBuffer sb = new StringBuffer();

        append(sb, "org.mortbay.jetty:jetty-maven-plugin:7.3.1.v20110307", 0);
        append(sb, "", 0);

        append(sb, "Jetty :: Jetty Maven Plugin", 0);
        append(sb, "Jetty integrations and distributions", 1);
        append(sb, "", 0);

        if (goal == null || goal.length() <= 0) {
            append(sb, "This plugin has 6 goals:", 0);
            append(sb, "", 0);
        }

        if (goal == null || goal.length() <= 0 || "deploy-war".equals(goal)) {
            append(sb, "jetty:deploy-war", 0);
            append(sb,
                    "This goal is used to run Jetty with a pre-assembled war.\n\nIt accepts exactly the same options as the run-war goal. However, it doesn\'t assume that the current artifact is a webapp and doesn\'t try to assemble it into a war before its execution. So using it makes sense only when used in conjunction with the webApp configuration parameter pointing to a pre-built WAR.\n\nThis goal is useful e.g. for launching a web app in Jetty as a target for unit-tested HTTP client components.\n",
                    1);
            append(sb, "", 0);
            if (detail) {
                append(sb, "Available parameters:", 1);
                append(sb, "", 0);

                append(sb, "connectors", 2);
                append(sb,
                        "List of connectors to use. If none are configured then the default is a single SelectChannelConnector at port 8080. You can override this default port number by using the system property jetty.port on the command line, eg: mvn -Djetty.port=9999 jetty:run",
                        3);
                append(sb, "", 0);

                append(sb, "contextHandlers", 2);
                append(sb, "List of other contexts to set up. Optional.", 3);
                append(sb, "", 0);

                append(sb, "daemon (Default: false)", 2);
                append(sb,
                        "Determines whether or not the server blocks when started. The default behavior (daemon = false) will cause the server to pause other processes while it continues to handle web requests. This is useful when starting the server with the intent to work with it interactively.\n\nOften, it is desirable to let the server start and continue running subsequent processes in an automated build environment. This can be facilitated by setting daemon to true.\n",
                        3);
                append(sb, "", 0);

                append(sb, "jettyConfig", 2);
                append(sb,
                        "Location of a jetty xml configuration file whose contents will be applied before any plugin configuration. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "loginServices", 2);
                append(sb, "List of security realms to set up. Optional.", 3);
                append(sb, "", 0);

                append(sb, "reload (Default: automatic)", 2);
                append(sb,
                        "reload can be set to either \'automatic\' or \'manual\' if \'manual\' then the context can be reloaded by a linefeed in the console if \'automatic\' then traditional reloading on changed files is enabled.",
                        3);
                append(sb, "", 0);

                append(sb, "requestLog", 2);
                append(sb, "A RequestLog implementation to use for the webapp at runtime. Optional.", 3);
                append(sb, "", 0);

                append(sb, "scanIntervalSeconds (Default: 0)", 2);
                append(sb,
                        "The interval in seconds to scan the webapp for changes and restart the context if necessary. Ignored if reload is enabled. Disabled by default.",
                        3);
                append(sb, "", 0);

                append(sb, "skip (Default: false)", 2);
                append(sb, "(no description available)", 3);
                append(sb, "", 0);

                append(sb, "stopKey", 2);
                append(sb,
                        "Key to provide when stopping jetty on executing java -DSTOP.KEY=<stopKey> -DSTOP.PORT=<stopPort> -jar start.jar --stop",
                        3);
                append(sb, "", 0);

                append(sb, "stopPort", 2);
                append(sb,
                        "Port to listen to stop jetty on executing -DSTOP.PORT=<stopPort> -DSTOP.KEY=<stopKey> -jar start.jar --stop",
                        3);
                append(sb, "", 0);

                append(sb, "systemProperties", 2);
                append(sb,
                        "System properties to set before execution. Note that these properties will NOT override System properties that have been set on the command line or by the JVM. They WILL override System properties that have been set via systemPropertiesFile. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "systemPropertiesFile", 2);
                append(sb,
                        "File containing system properties to be set before execution Note that these properties will NOT override System properties that have been set on the command line, by the JVM, or directly in the POM via systemProperties. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "webApp", 2);
                append(sb, "The location of the war file.", 3);
                append(sb, "", 0);

                append(sb, "webAppConfig", 2);
                append(sb, "The \'virtual\' webapp created by the plugin", 3);
                append(sb, "", 0);

                append(sb, "webAppXml", 2);
                append(sb,
                        "Location of a context xml configuration file whose contents will be applied to the webapp AFTER anything in <webAppConfig>.Optional.",
                        3);
                append(sb, "", 0);
            }
        }

        if (goal == null || goal.length() <= 0 || "help".equals(goal)) {
            append(sb, "jetty:help", 0);
            append(sb,
                    "Display help information on jetty-maven-plugin.\nCall\n\u00a0\u00a0mvn\u00a0jetty:help\u00a0-Ddetail=true\u00a0-Dgoal=<goal-name>\nto display parameter details.",
                    1);
            append(sb, "", 0);
            if (detail) {
                append(sb, "Available parameters:", 1);
                append(sb, "", 0);

                append(sb, "detail (Default: false)", 2);
                append(sb, "If true, display all settable properties for each goal.", 3);
                append(sb, "", 0);

                append(sb, "goal", 2);
                append(sb, "The name of the goal for which to show help. If unspecified, all goals will be displayed.",
                        3);
                append(sb, "", 0);

                append(sb, "indentSize (Default: 2)", 2);
                append(sb, "The number of spaces per indentation level, should be positive.", 3);
                append(sb, "", 0);

                append(sb, "lineLength (Default: 80)", 2);
                append(sb, "The maximum length of a display line, should be positive.", 3);
                append(sb, "", 0);
            }
        }

        if (goal == null || goal.length() <= 0 || "run".equals(goal)) {
            append(sb, "jetty:run", 0);
            append(sb,
                    "This goal is used in-situ on a Maven project without first requiring that the project is assembled into a war, saving time during the development cycle. The plugin forks a parallel lifecycle to ensure that the \'compile\' phase has been completed before invoking Jetty. This means that you do not need to explicity execute a \'mvn compile\' first. It also means that a \'mvn clean jetty:run\' will ensure that a full fresh compile is done before invoking Jetty.\n\nOnce invoked, the plugin can be configured to run continuously, scanning for changes in the project and automatically performing a hot redeploy when necessary. This allows the developer to concentrate on coding changes to the project using their IDE of choice and have those changes immediately and transparently reflected in the running web container, eliminating development time that is wasted on rebuilding, reassembling and redeploying.\n\nYou may also specify the location of a jetty.xml file whose contents will be applied before any plugin configuration. This can be used, for example, to deploy a static webapp that is not part of your maven build.\n\nThere is a reference guide to the configuration parameters for this plugin, and more detailed information with examples in the Configuration Guide.\n",
                    1);
            append(sb, "", 0);
            if (detail) {
                append(sb, "Available parameters:", 1);
                append(sb, "", 0);

                append(sb, "classesDirectory", 2);
                append(sb, "The directory containing generated classes.", 3);
                append(sb, "", 0);

                append(sb, "connectors", 2);
                append(sb,
                        "List of connectors to use. If none are configured then the default is a single SelectChannelConnector at port 8080. You can override this default port number by using the system property jetty.port on the command line, eg: mvn -Djetty.port=9999 jetty:run",
                        3);
                append(sb, "", 0);

                append(sb, "contextHandlers", 2);
                append(sb, "List of other contexts to set up. Optional.", 3);
                append(sb, "", 0);

                append(sb, "daemon (Default: false)", 2);
                append(sb,
                        "Determines whether or not the server blocks when started. The default behavior (daemon = false) will cause the server to pause other processes while it continues to handle web requests. This is useful when starting the server with the intent to work with it interactively.\n\nOften, it is desirable to let the server start and continue running subsequent processes in an automated build environment. This can be facilitated by setting daemon to true.\n",
                        3);
                append(sb, "", 0);

                append(sb, "jettyConfig", 2);
                append(sb,
                        "Location of a jetty xml configuration file whose contents will be applied before any plugin configuration. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "loginServices", 2);
                append(sb, "List of security realms to set up. Optional.", 3);
                append(sb, "", 0);

                append(sb, "reload (Default: automatic)", 2);
                append(sb,
                        "reload can be set to either \'automatic\' or \'manual\' if \'manual\' then the context can be reloaded by a linefeed in the console if \'automatic\' then traditional reloading on changed files is enabled.",
                        3);
                append(sb, "", 0);

                append(sb, "requestLog", 2);
                append(sb, "A RequestLog implementation to use for the webapp at runtime. Optional.", 3);
                append(sb, "", 0);

                append(sb, "scanIntervalSeconds (Default: 0)", 2);
                append(sb,
                        "The interval in seconds to scan the webapp for changes and restart the context if necessary. Ignored if reload is enabled. Disabled by default.",
                        3);
                append(sb, "", 0);

                append(sb, "scanTargetPatterns", 2);
                append(sb,
                        "List of directories with ant-style <include> and <exclude> patterns for extra targets to periodically scan for changes. Can be used instead of, or in conjunction with <scanTargets>.Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "scanTargets", 2);
                append(sb, "List of files or directories to additionally periodically scan for changes. Optional.", 3);
                append(sb, "", 0);

                append(sb, "skip (Default: false)", 2);
                append(sb, "(no description available)", 3);
                append(sb, "", 0);

                append(sb, "stopKey", 2);
                append(sb,
                        "Key to provide when stopping jetty on executing java -DSTOP.KEY=<stopKey> -DSTOP.PORT=<stopPort> -jar start.jar --stop",
                        3);
                append(sb, "", 0);

                append(sb, "stopPort", 2);
                append(sb,
                        "Port to listen to stop jetty on executing -DSTOP.PORT=<stopPort> -DSTOP.KEY=<stopKey> -jar start.jar --stop",
                        3);
                append(sb, "", 0);

                append(sb, "systemProperties", 2);
                append(sb,
                        "System properties to set before execution. Note that these properties will NOT override System properties that have been set on the command line or by the JVM. They WILL override System properties that have been set via systemPropertiesFile. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "systemPropertiesFile", 2);
                append(sb,
                        "File containing system properties to be set before execution Note that these properties will NOT override System properties that have been set on the command line, by the JVM, or directly in the POM via systemProperties. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "testClassesDirectory", 2);
                append(sb, "The directory containing generated test classes.", 3);
                append(sb, "", 0);

                append(sb, "useTestClasspath (Default: false)", 2);
                append(sb,
                        "If true, the <testOutputDirectory> and the dependencies of <scope>test<scope> will be put first on the runtime classpath.",
                        3);
                append(sb, "", 0);

                append(sb, "webAppConfig", 2);
                append(sb, "The \'virtual\' webapp created by the plugin", 3);
                append(sb, "", 0);

                append(sb, "webAppSourceDirectory", 2);
                append(sb, "Root directory for all html/jsp etc files", 3);
                append(sb, "", 0);

                append(sb, "webAppXml", 2);
                append(sb,
                        "Location of a context xml configuration file whose contents will be applied to the webapp AFTER anything in <webAppConfig>.Optional.",
                        3);
                append(sb, "", 0);
            }
        }

        if (goal == null || goal.length() <= 0 || "run-exploded".equals(goal)) {
            append(sb, "jetty:run-exploded", 0);
            append(sb,
                    "This goal is used to assemble your webapp into an exploded war and automatically deploy it to Jetty.\n\nOnce invoked, the plugin can be configured to run continuously, scanning for changes in the pom.xml and to WEB-INF/web.xml, WEB-INF/classes or WEB-INF/lib and hot redeploy when a change is detected.\n\nYou may also specify the location of a jetty.xml file whose contents will be applied before any plugin configuration. This can be used, for example, to deploy a static webapp that is not part of your maven build.\n\nThere is a reference guide to the configuration parameters for this plugin, and more detailed information with examples in the Configuration Guide.\n",
                    1);
            append(sb, "", 0);
            if (detail) {
                append(sb, "Available parameters:", 1);
                append(sb, "", 0);

                append(sb, "connectors", 2);
                append(sb,
                        "List of connectors to use. If none are configured then the default is a single SelectChannelConnector at port 8080. You can override this default port number by using the system property jetty.port on the command line, eg: mvn -Djetty.port=9999 jetty:run",
                        3);
                append(sb, "", 0);

                append(sb, "contextHandlers", 2);
                append(sb, "List of other contexts to set up. Optional.", 3);
                append(sb, "", 0);

                append(sb, "daemon (Default: false)", 2);
                append(sb,
                        "Determines whether or not the server blocks when started. The default behavior (daemon = false) will cause the server to pause other processes while it continues to handle web requests. This is useful when starting the server with the intent to work with it interactively.\n\nOften, it is desirable to let the server start and continue running subsequent processes in an automated build environment. This can be facilitated by setting daemon to true.\n",
                        3);
                append(sb, "", 0);

                append(sb, "jettyConfig", 2);
                append(sb,
                        "Location of a jetty xml configuration file whose contents will be applied before any plugin configuration. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "loginServices", 2);
                append(sb, "List of security realms to set up. Optional.", 3);
                append(sb, "", 0);

                append(sb, "reload (Default: automatic)", 2);
                append(sb,
                        "reload can be set to either \'automatic\' or \'manual\' if \'manual\' then the context can be reloaded by a linefeed in the console if \'automatic\' then traditional reloading on changed files is enabled.",
                        3);
                append(sb, "", 0);

                append(sb, "requestLog", 2);
                append(sb, "A RequestLog implementation to use for the webapp at runtime. Optional.", 3);
                append(sb, "", 0);

                append(sb, "scanIntervalSeconds (Default: 0)", 2);
                append(sb,
                        "The interval in seconds to scan the webapp for changes and restart the context if necessary. Ignored if reload is enabled. Disabled by default.",
                        3);
                append(sb, "", 0);

                append(sb, "skip (Default: false)", 2);
                append(sb, "(no description available)", 3);
                append(sb, "", 0);

                append(sb, "stopKey", 2);
                append(sb,
                        "Key to provide when stopping jetty on executing java -DSTOP.KEY=<stopKey> -DSTOP.PORT=<stopPort> -jar start.jar --stop",
                        3);
                append(sb, "", 0);

                append(sb, "stopPort", 2);
                append(sb,
                        "Port to listen to stop jetty on executing -DSTOP.PORT=<stopPort> -DSTOP.KEY=<stopKey> -jar start.jar --stop",
                        3);
                append(sb, "", 0);

                append(sb, "systemProperties", 2);
                append(sb,
                        "System properties to set before execution. Note that these properties will NOT override System properties that have been set on the command line or by the JVM. They WILL override System properties that have been set via systemPropertiesFile. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "systemPropertiesFile", 2);
                append(sb,
                        "File containing system properties to be set before execution Note that these properties will NOT override System properties that have been set on the command line, by the JVM, or directly in the POM via systemProperties. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "webApp", 2);
                append(sb, "The location of the war file.", 3);
                append(sb, "", 0);

                append(sb, "webAppConfig", 2);
                append(sb, "The \'virtual\' webapp created by the plugin", 3);
                append(sb, "", 0);

                append(sb, "webAppXml", 2);
                append(sb,
                        "Location of a context xml configuration file whose contents will be applied to the webapp AFTER anything in <webAppConfig>.Optional.",
                        3);
                append(sb, "", 0);
            }
        }

        if (goal == null || goal.length() <= 0 || "run-war".equals(goal)) {
            append(sb, "jetty:run-war", 0);
            append(sb,
                    "This goal is used to assemble your webapp into a war and automatically deploy it to Jetty.\n\nOnce invoked, the plugin can be configured to run continuously, scanning for changes in the project and to the war file and automatically performing a hot redeploy when necessary.\n\nYou may also specify the location of a jetty.xml file whose contents will be applied before any plugin configuration. This can be used, for example, to deploy a static webapp that is not part of your maven build.\n\nThere is a reference guide to the configuration parameters for this plugin, and more detailed information with examples in the Configuration Guide.\n",
                    1);
            append(sb, "", 0);
            if (detail) {
                append(sb, "Available parameters:", 1);
                append(sb, "", 0);

                append(sb, "connectors", 2);
                append(sb,
                        "List of connectors to use. If none are configured then the default is a single SelectChannelConnector at port 8080. You can override this default port number by using the system property jetty.port on the command line, eg: mvn -Djetty.port=9999 jetty:run",
                        3);
                append(sb, "", 0);

                append(sb, "contextHandlers", 2);
                append(sb, "List of other contexts to set up. Optional.", 3);
                append(sb, "", 0);

                append(sb, "daemon (Default: false)", 2);
                append(sb,
                        "Determines whether or not the server blocks when started. The default behavior (daemon = false) will cause the server to pause other processes while it continues to handle web requests. This is useful when starting the server with the intent to work with it interactively.\n\nOften, it is desirable to let the server start and continue running subsequent processes in an automated build environment. This can be facilitated by setting daemon to true.\n",
                        3);
                append(sb, "", 0);

                append(sb, "jettyConfig", 2);
                append(sb,
                        "Location of a jetty xml configuration file whose contents will be applied before any plugin configuration. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "loginServices", 2);
                append(sb, "List of security realms to set up. Optional.", 3);
                append(sb, "", 0);

                append(sb, "reload (Default: automatic)", 2);
                append(sb,
                        "reload can be set to either \'automatic\' or \'manual\' if \'manual\' then the context can be reloaded by a linefeed in the console if \'automatic\' then traditional reloading on changed files is enabled.",
                        3);
                append(sb, "", 0);

                append(sb, "requestLog", 2);
                append(sb, "A RequestLog implementation to use for the webapp at runtime. Optional.", 3);
                append(sb, "", 0);

                append(sb, "scanIntervalSeconds (Default: 0)", 2);
                append(sb,
                        "The interval in seconds to scan the webapp for changes and restart the context if necessary. Ignored if reload is enabled. Disabled by default.",
                        3);
                append(sb, "", 0);

                append(sb, "skip (Default: false)", 2);
                append(sb, "(no description available)", 3);
                append(sb, "", 0);

                append(sb, "stopKey", 2);
                append(sb,
                        "Key to provide when stopping jetty on executing java -DSTOP.KEY=<stopKey> -DSTOP.PORT=<stopPort> -jar start.jar --stop",
                        3);
                append(sb, "", 0);

                append(sb, "stopPort", 2);
                append(sb,
                        "Port to listen to stop jetty on executing -DSTOP.PORT=<stopPort> -DSTOP.KEY=<stopKey> -jar start.jar --stop",
                        3);
                append(sb, "", 0);

                append(sb, "systemProperties", 2);
                append(sb,
                        "System properties to set before execution. Note that these properties will NOT override System properties that have been set on the command line or by the JVM. They WILL override System properties that have been set via systemPropertiesFile. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "systemPropertiesFile", 2);
                append(sb,
                        "File containing system properties to be set before execution Note that these properties will NOT override System properties that have been set on the command line, by the JVM, or directly in the POM via systemProperties. Optional.",
                        3);
                append(sb, "", 0);

                append(sb, "webApp", 2);
                append(sb, "The location of the war file.", 3);
                append(sb, "", 0);

                append(sb, "webAppConfig", 2);
                append(sb, "The \'virtual\' webapp created by the plugin", 3);
                append(sb, "", 0);

                append(sb, "webAppXml", 2);
                append(sb,
                        "Location of a context xml configuration file whose contents will be applied to the webapp AFTER anything in <webAppConfig>.Optional.",
                        3);
                append(sb, "", 0);
            }
        }

        if (goal == null || goal.length() <= 0 || "stop".equals(goal)) {
            append(sb, "jetty:stop", 0);
            append(sb,
                    "JettyStopMojo - stops a running instance of jetty. The ff are required: -DstopKey=someKey -DstopPort=somePort",
                    1);
            append(sb, "", 0);
            if (detail) {
                append(sb, "Available parameters:", 1);
                append(sb, "", 0);

                append(sb, "stopKey", 2);
                append(sb,
                        "Key to provide when stopping jetty on executing java -DSTOP.KEY=<stopKey> -DSTOP.PORT=<stopPort> -jar start.jar --stop",
                        3);
                append(sb, "", 0);

                append(sb, "stopPort", 2);
                append(sb, "Port to listen to stop jetty on sending stop command", 3);
                append(sb, "", 0);
            }
        }

        if (getLog().isInfoEnabled()) {
            getLog().info(sb.toString());
        }
    }

    /**
     * <p>
     * Repeat a String <code>n</code> times to form a new string.
     * </p>
     * 
     * @param str String to repeat
     * @param repeat number of times to repeat str
     * @return String with repeated String
     * @throws NegativeArraySizeException if <code>repeat < 0</code>
     * @throws NullPointerException if str is <code>null</code>
     */
    private static String repeat(String str, int repeat) {
        StringBuffer buffer = new StringBuffer(repeat * str.length());

        for (int i = 0; i < repeat; i++) {
            buffer.append(str);
        }

        return buffer.toString();
    }

    /**
     * Append a description to the buffer by respecting the indentSize and lineLength parameters. <b>Note</b>: The last
     * character is always a new line.
     * 
     * @param sb The buffer to append the description, not <code>null</code>.
     * @param description The description, not <code>null</code>.
     * @param indent The base indentation level of each line, must not be negative.
     */
    private void append(StringBuffer sb, String description, int indent) {
        for (Iterator it = toLines(description, indent, indentSize, lineLength).iterator(); it.hasNext();) {
            sb.append(it.next().toString()).append('\n');
        }
    }

    /**
     * Splits the specified text into lines of convenient display length.
     * 
     * @param text The text to split into lines, must not be <code>null</code>.
     * @param indent The base indentation level of each line, must not be negative.
     * @param indentSize The size of each indentation, must not be negative.
     * @param lineLength The length of the line, must not be negative.
     * @return The sequence of display lines, never <code>null</code>.
     * @throws NegativeArraySizeException if <code>indent < 0</code>
     */
    private static List toLines(String text, int indent, int indentSize, int lineLength) {
        List lines = new ArrayList();

        String ind = repeat("\t", indent);
        String[] plainLines = text.split("(\r\n)|(\r)|(\n)");
        for (int i = 0; i < plainLines.length; i++) {
            toLines(lines, ind + plainLines[i], indentSize, lineLength);
        }

        return lines;
    }

    /**
     * Adds the specified line to the output sequence, performing line wrapping if necessary.
     * 
     * @param lines The sequence of display lines, must not be <code>null</code>.
     * @param line The line to add, must not be <code>null</code>.
     * @param indentSize The size of each indentation, must not be negative.
     * @param lineLength The length of the line, must not be negative.
     */
    private static void toLines(List lines, String line, int indentSize, int lineLength) {
        int lineIndent = getIndentLevel(line);
        StringBuffer buf = new StringBuffer(256);
        String[] tokens = line.split(" +");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (i > 0) {
                if (buf.length() + token.length() >= lineLength) {
                    lines.add(buf.toString());
                    buf.setLength(0);
                    buf.append(repeat(" ", lineIndent * indentSize));
                } else {
                    buf.append(' ');
                }
            }
            for (int j = 0; j < token.length(); j++) {
                char c = token.charAt(j);
                if (c == '\t') {
                    buf.append(repeat(" ", indentSize - buf.length() % indentSize));
                } else if (c == '\u00A0') {
                    buf.append(' ');
                } else {
                    buf.append(c);
                }
            }
        }
        lines.add(buf.toString());
    }

    /**
     * Gets the indentation level of the specified line.
     * 
     * @param line The line whose indentation level should be retrieved, must not be <code>null</code>.
     * @return The indentation level of the line.
     */
    private static int getIndentLevel(String line) {
        int level = 0;
        for (int i = 0; i < line.length() && line.charAt(i) == '\t'; i++) {
            level++;
        }
        for (int i = level + 1; i <= level + 4 && i < line.length(); i++) {
            if (line.charAt(i) == '\t') {
                level++;
                break;
            }
        }
        return level;
    }
}
