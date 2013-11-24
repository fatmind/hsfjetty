//========================================================================
//$Id: JettyRunMojo.java 6311 2011-01-06 21:30:04Z jesse $
//Copyright 2000-2009 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package com.taobao.hsf.jetty;

import com.taobao.hsf.jetty.component.AntxReplacer;
import com.taobao.hsf.jetty.component.PlaceholderReplacer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * <p>
 * This goal is used in-situ on a Maven project without first requiring that the project
 * is assembled into a war, saving time during the development cycle.
 * The plugin forks a parallel lifecycle to ensure that the "compile" phase has been completed before invoking Jetty. This means
 * that you do not need to explicity execute a "mvn compile" first. It also means that a "mvn clean jetty:run" will ensure that
 * a full fresh compile is done before invoking Jetty.
 * </p>
 * <p>
 * Once invoked, the plugin can be configured to run continuously, scanning for changes in the project and automatically performing a
 * hot redeploy when necessary. This allows the developer to concentrate on coding changes to the project using their IDE of choice and have those changes
 * immediately and transparently reflected in the running web container, eliminating development time that is wasted on rebuilding, reassembling and redeploying.
 * </p>
 * <p>
 * You may also specify the location of a jetty.xml file whose contents will be applied before any plugin configuration.
 * This can be used, for example, to deploy a static webapp that is not part of your maven build.
 * </p>
 * <p>
 * There is a <a href="run-mojo.html">reference guide</a> to the configuration parameters for this plugin, and more detailed information
 * with examples in the <a href="http://docs.codehaus.org/display/JETTY/Maven+Jetty+Plugin">Configuration Guide</a>.
 * </p>
 * <p/>
 * <p/>
 * goal autoconfig
 * requiresDependencyResolution runtime
 * description Runs autoconfig directly from a maven project
 */

@Mojo(name = "autoconfig", requiresDependencyResolution = ResolutionScope.RUNTIME
        , requiresProject = true)
public class JettyAutoconfigMojo extends AbstractMojo {

    /**
     * The maven project.
     * <p/>
     * parameter expression="${executedProject}"
     * required
     * readonly
     */
    @Parameter(required = true, readonly = true, defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(defaultValue = "${basedir/target_hsf")
    protected File hsfDirectory;

    /**
     * The directory containing generated classes.
     * <p/>
     * parameter expression="${project.build.outputDirectory}"
     * required
     */
    @Parameter(alias = "classesDirectory", required = true, defaultValue = "${project.build.outputDirectory}")
    private File classesDirectory;


    /**
     * The directory containing generated test classes.
     * <p/>
     * parameter expression="${project.build.testOutputDirectory}"
     * required
     */
    @Parameter(required = true, defaultValue = "${project.build.testOutputDirectory}")
    private File testClassesDirectory;

    /**
     * Root directory for all html/jsp etc files
     * <p/>
     * parameter expression="${maven.war.src}"
     */
    @Parameter(defaultValue = "${basedir}/src/main/webapp")
    private File webAppSourceDirectory;

    /**
     * autoconfig.xml 默认位置:<br />
     * ${basedir}/src/main/webapp/META-INF/autoconfig/auto-config.xml <br />
     * ${basedir}/src/main/webapp/META-INF/autoconf/auto-config.xml
     */
    @Parameter(defaultValue = "${basedir}/src/main/webapp/META-INF/autoconfig/auto-config.xml")
    private File autoconfigFile;

    /**
     * antx.properties default path:<br />
     * ${basedir}/antx.properties <br/>
     * ${user.dir}/antx.properties <br />
     */
    @Parameter(defaultValue = "${basedir}/antx.properties", required = false)
    private File antxPropertiesFile;

    @Parameter(defaultValue = "${project.build.directory}/hsf_jetty_temp/antx_autoconfig")
    private File autoconfigTempDirectory;

    @Parameter(defaultValue = "${project.build.directory}/hsf_jetty_temp/mvn_placeholder")
    private File placehoderTempDirectory;


    public File getClassesDirectory() {
        return this.classesDirectory;
    }

    public File getWebAppSourceDirectory() {
        return this.webAppSourceDirectory;
    }

    /**
     * get the antx auto-config xml configuration
     *
     * @return
     */
    public File getAutoconfigFile() {
        return autoconfigFile;
    }

    public File getAntxPropertiesFile() {
        return antxPropertiesFile;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        autoconfigReplace();
    }

    @Override
    public org.apache.maven.plugin.logging.Log getLog() {
        return super.getLog();
    }

    /**
     * run antx-autoconfig to replace the placeholder
     */
    public void autoconfigReplace() {
        PlaceholderReplacer replacer = new AntxReplacer(true,getProject(),getWebAppSourceDirectory(),getAutoconfigFile(),getAutoconfigTempDirectory(),getLog());
        replacer.replace();
    }


    public File getAutoconfigTempDirectory() {
        if (!autoconfigTempDirectory.exists())
            autoconfigTempDirectory.mkdirs();
        return autoconfigTempDirectory;
    }

    public File getPlacehoderTempDirectory() {
        if (!placehoderTempDirectory.exists())
            placehoderTempDirectory.mkdirs();
        return placehoderTempDirectory;

    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

}
