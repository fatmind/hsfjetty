//========================================================================
//$Id: JettyWebAppContext.java 6281 2010-12-01 18:43:57Z gregw $
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

import com.taobao.hsf.jetty.component.AutoconfigReplacer;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

/**
 * JettyWebAppContext
 * 
 * Extends the WebAppContext to specialize for the maven environment.
 * We pass in the list of files that should form the classpath for
 * the webapp when executing in the plugin, and any jetty-env.xml file
 * that may have been configured.
 *
 */
public class JettyWebAppContext extends WebAppContext
{
    private static final String WEB_INF_CLASSES_PREFIX = "/WEB-INF/classes";
    private static final String WEB_INF_LIB_PREFIX = "/WEB-INF/lib";
//    private final AutoconfigReplacer replacer;
    private final Set<String> autoconfigOutput;
    private final File autoconfigTempDirectory;
    private File defaultWebAppSourceDirectory;

    private List<File> classpathFiles;
    private String jettyEnvXml;
    private MavenWebInfConfiguration webInfConfig = new MavenWebInfConfiguration();
    private WebXmlConfiguration webXmlConfig = new WebXmlConfiguration();
    private MetaInfConfiguration metaInfConfig = new MetaInfConfiguration();
    private FragmentConfiguration fragConfig = new FragmentConfiguration();
    private EnvConfiguration envConfig =  new EnvConfiguration();
    private AnnotationConfiguration annotationConfig = new AnnotationConfiguration();
    private PlusConfiguration plusConfig = new PlusConfiguration();
    private JettyWebXmlConfiguration jettyWebConfig =  new JettyWebXmlConfiguration();
    private TagLibConfiguration tagConfig = new TagLibConfiguration();
    private Configuration[] configs;
    private List<Resource> overlays;
    private boolean unpackOverlays;
    private List<File> webInfClasses = new ArrayList<File>();
    private List<File> webInfJars = new ArrayList<File>();
    private Map<String, File> webInfJarMap = new HashMap<String, File>();
    private  final Logger logger = Log.getLogger(getClass().getName());

    @Deprecated
    public JettyWebAppContext() {
        super();
        configs = new Configuration[]{webInfConfig, webXmlConfig,  metaInfConfig,  fragConfig, envConfig, plusConfig, annotationConfig, jettyWebConfig, tagConfig };
        this.autoconfigOutput = new HashSet<String>(0);
        this.autoconfigTempDirectory = null;
        this.setParentLoaderPriority(true);
    }

    public JettyWebAppContext(AutoconfigReplacer replacer) {
        super();
        configs = new Configuration[]{webInfConfig, webXmlConfig,  metaInfConfig,  fragConfig, envConfig, plusConfig, annotationConfig, jettyWebConfig, tagConfig };
        this.autoconfigOutput = replacer.getAutoconfigIncludeFile();
        this.autoconfigTempDirectory = replacer.getAutoconfigTempDirectory();
        this.setParentLoaderPriority(true);
    }

    public JettyWebAppContext(Set<String> autoconfigOutput,File autoconfigTempDirectory) {
        super();
        configs = new Configuration[]{webInfConfig, webXmlConfig,  metaInfConfig,  fragConfig, envConfig, plusConfig, annotationConfig, jettyWebConfig, tagConfig };
        this.autoconfigOutput = autoconfigOutput;
        this.autoconfigTempDirectory = autoconfigTempDirectory;
        this.setParentLoaderPriority(true);
    }
    public boolean getUnpackOverlays()
    {
        return unpackOverlays;
    }

    public void setUnpackOverlays(boolean unpackOverlays)
    {
        this.unpackOverlays = unpackOverlays;
    }
    
    public void setClassPathFiles(List<File> classpathFiles)
    {
        this.classpathFiles = classpathFiles;
    }

    /**
     * return class files and jar files
     * @return
     */

    public List<File> getClassPathFiles()
    {
        if(this.classpathFiles == null||this.classpathFiles.size() == 0){
            Set<File> classesFiles = new LinkedHashSet<File>();
            if(getWebInfClasses() != null){
                classesFiles.addAll(getWebInfClasses());
            }
            if(getWebInfLib() != null){
                classesFiles.addAll(getWebInfLib());
            }
            this.classpathFiles = new ArrayList<File>(classesFiles.size());
            this.classpathFiles.addAll(classesFiles);
        }
        return this.classpathFiles;
    }

    
    public void setOverlays (List<Resource> overlays)
    {
        this.overlays = overlays;
    }
    
    public List<Resource> getOverlays ()
    {
        return this.overlays;
    }
    
    public void setJettyEnvXml (String jettyEnvXml)
    {
        this.jettyEnvXml = jettyEnvXml;
    }
    
    public String getJettyEnvXml()
    {
        return this.jettyEnvXml;
    }

   
    public void setWebInfClasses(List<File> dirs)
    {
        webInfClasses.addAll(dirs);
    }

    public List<File> getWebInfClasses()
    {
        return webInfClasses;
    }

    public void setWebInfLib (List<File> jars)
    {
        webInfJars.addAll(jars);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * This method is provided as a conveniance for jetty maven plugin configuration 
     * @param resourceBases Array of resources strings to set as a {@link ResourceCollection}. Each resource string may be a comma separated list of resources
     * @see Resource
     */
    public void setResourceBases(String[] resourceBases)
    {
        List<String> resources = new ArrayList<String>();
        for (String rl:resourceBases)
        {
            String[] rs = rl.split(" *, *");
            for (String r:rs)
                resources.add(r);
        }
        
        setBaseResource(new ResourceCollection(resources.toArray(new String[resources.size()])));
    }

    public List<File> getWebInfLib()
    {
        return webInfJars;
    }

    public void doStart () throws Exception
    {
        setConfigurations(configs);

        // Initialize map containing all jars in /WEB-INF/lib
        webInfJarMap.clear();
        for (File file : webInfJars)
        {
            // Return all jar files from class path
            String fileName = file.getName();
            if (fileName.endsWith(".jar"))
                webInfJarMap.put(fileName, file);
        }

        if (this.jettyEnvXml != null)
            envConfig.setJettyEnvXml(new File(this.jettyEnvXml).toURL());
        setShutdown(false);
        super.doStart();
    }
     
    public void doStop () throws Exception
    { 
        setShutdown(true);
        //just wait a little while to ensure no requests are still being processed
        Thread.currentThread().sleep(500L);
        super.doStop();
    }
    public Resource getAutoconfigResource(String uriContext) throws IOException {

        String relativePath = null;
//        replacer.getAutoconfigTempDirectory()

        if (uriContext.startsWith(URIUtil.SLASH)){
            relativePath = uriContext.substring(1);
        }else{relativePath = uriContext;}

//        path.startsWith(defaultWebAppSourceDirectory.getAbsolutePath())
        if(autoconfigOutput.contains(relativePath)){
            String resourcePath = autoconfigTempDirectory.getAbsolutePath() + File.separator + relativePath;
            if (logger.isDebugEnabled())
                logger.debug(uriContext +" autoconfig replaced resource:"+resourcePath);
            return Resource.newResource(resourcePath);

        }
        return null;
    }
    @Override
    public Resource getResource(String uriInContext) throws MalformedURLException
    {
        Resource resource = null;
        // Try to get regular resource
//        replacer.getAutoconfigTempDirectory().getAbsolutePath()
        try{
        resource = getAutoconfigResource(uriInContext);
        if(resource == null)
            resource = super.getResource(uriInContext);
        }catch (Exception e){
            logger.warn("find autoconfig resource error! " + uriInContext, e);
        }
        // If no regular resource exists check for access to /WEB-INF/lib or /WEB-INF/classes
        if ((resource == null || !resource.exists()) && uriInContext != null && webInfClasses != null)
        {
            String uri = URIUtil.canonicalPath(uriInContext);

            try
            {
                // Replace /WEB-INF/classes with real classes directory
                if (uri.startsWith(WEB_INF_CLASSES_PREFIX))
                {
                    Resource res = null;
                    int i=0;
                    while (res == null && (i < webInfClasses.size()))
                    {
                        String newPath = uri.replace(WEB_INF_CLASSES_PREFIX, webInfClasses.get(i).getPath());
                        res = Resource.newResource(newPath);
                        if (!res.exists())
                        {
                            res = null; 
                            i++;
                        }
                    }
                    return res;
                }
                // Return the real jar file for all accesses to
                // /WEB-INF/lib/*.jar
                else if (uri.startsWith(WEB_INF_LIB_PREFIX))
                {
                    String jarName = uri.replace(WEB_INF_LIB_PREFIX, "");
                    if (jarName.startsWith("/") || jarName.startsWith("\\")) 
                        jarName = jarName.substring(1);
                    if (jarName.length()==0) 
                        return null;
                    File jarFile = webInfJarMap.get(jarName);
                    if (jarFile != null)
                        return Resource.newResource(jarFile.getPath());

                    return null;
                }
            }
            catch (MalformedURLException e)
            {
                throw e;
            }
            catch (IOException e)
            {
                Log.ignore(e);
            }
        }
        return resource;
    }

    @Override
    public Set<String> getResourcePaths(String path)
    {
        // Try to get regular resource paths
        Set<String> paths = super.getResourcePaths(path);

        // If no paths are returned check for virtual paths /WEB-INF/classes and /WEB-INF/lib
        if (paths.isEmpty() && path != null)
        {
            path = URIUtil.canonicalPath(path);
            if (path.startsWith(WEB_INF_LIB_PREFIX))
            {
                paths = new TreeSet<String>();
                for (String fileName : webInfJarMap.keySet())
                {
                    // Return all jar files from class path
                    paths.add(WEB_INF_LIB_PREFIX + "/" + fileName);
                }
            }
            else if (path.startsWith(WEB_INF_CLASSES_PREFIX))
            {
                int i=0;
               
                while (paths.isEmpty() && (i < webInfClasses.size()))
                {
                    String newPath = path.replace(WEB_INF_CLASSES_PREFIX, webInfClasses.get(i).getPath());
                    paths = super.getResourcePaths(newPath);
                    i++;
                }
            }
        }
        return paths;
    }
}
