package com.taobao.hsf.jetty;

import net.vidageek.mirror.dsl.Mirror;
import net.vidageek.mirror.set.dsl.SetterHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.resources.ResourcesMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.File;

/**
 * @auther: zheshan
 * Time: 13-7-15 下午7:27
 */
@Mojo(name = "test",requiresDependencyResolution = ResolutionScope.RUNTIME)
public class TestMojo extends AbstractMojo implements Contextualizable {

    /**
     * The output directory into which to copy the resources.
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}/hsf_jetty_temp/mvn_placeholder")
    private File outputDirectory;

    @Parameter(required = true, readonly = true, defaultValue = "${project}")
    protected MavenProject project;
    @Parameter(defaultValue = "${session}")
    private MavenSession session;
    private PlexusContainer plexusContainer;
    @Parameter(defaultValue = "${maven.resources.escapeString}")
    protected String escapeString;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    protected String encoding;

    @Component(role = org.apache.maven.shared.filtering.MavenResourcesFiltering.class,hint = "default")
    protected MavenResourcesFiltering mavenResourcesFiltering;

    @Override
    public void contextualize(Context context) throws ContextException {
        plexusContainer = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);

    }

    public void execute()
            throws MojoExecutionException {
        ResourcesMojo resources = new ResourcesMojo();
        resources.setOutputDirectory(outputDirectory);
        resources.setResources(project.getResources());


        resources.setFilters(project.getFilters());

        try {

            Mirror mirror = new Mirror();
            SetterHandler setter = mirror.on(resources).set();
            setter.field("buildFilters").withValue(project.getBuild().getFilters());
            setter.field("session").withValue(session);
            setter.field("escapeString").withValue(escapeString);
            setter.field("plexusContainer").withValue(plexusContainer);
            setter.field("session").withValue(session);
            setter.field("project").withValue(project);
            setter.field("encoding").withValue(encoding);
            setter.field("mavenResourcesFiltering").withValue(mavenResourcesFiltering);
            resources.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }


//        MavenResourcesFiltering filter = null;
//        try {
//            filter = (MavenResourcesFiltering) plexusContainer.lookup( MavenResourcesFiltering.class.getName(), "default");
//            System.out.println(filter+"----------------");
//        } catch (ComponentLookupException e) {
//            e.printStackTrace();
//        }
//        System.out.println(filter+"****************");

    }
}
