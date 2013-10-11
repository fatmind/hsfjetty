package com.taobao.hsf.jetty.component;

import com.alibaba.antx.config.ConfigRuntimeImpl;
import com.alibaba.antx.config.descriptor.ConfigGenerate;
import com.alibaba.antx.config.entry.ConfigEntry;
import com.alibaba.antx.config.generator.ConfigGenerator;
import com.alibaba.antx.util.CharsetUtil;
import com.alibaba.citrus.logconfig.LogConfigurator;
import com.taobao.hsf.jetty.PluginLog;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @auther: zheshan
 * Time: 13-6-6 上午9:53
 */
public class AutoconfigReplacer implements PlaceholderReplacer {
    private static MavenProject project;
    private static boolean replaced = false;
    private static File autoconfigFile;
    private static File webAppSourceDirectory;
    private static Log logger;
    private static File buildDirectory;//${project.build.directory}
    private static File baseDirectory;//${basedir}
    private static File autoconfigTempDirectory;//defaultValue = "${project.build.directory}/hsf_jetty_temp/antx_autoconfig"
    private static Resource antxPropertiesFile;
    private static Set<String> excludeFiles = new HashSet<String>();//exclude files of autoconfig template
    private static Set<String> includeFiles = new HashSet<String>();//include files of autoconfig destination
    private static Map<String, String> replacedFiles = new HashMap<String, String>();// key : dest , value: template
    private static File webXml;

    public File getAutoconfigTempDirectory() {
        return autoconfigTempDirectory;
    }

    public Set<String> getAutoconfigExcludeFile() {
        return excludeFiles;
    }

    public Set<String> getAutoconfigIncludeFile() {
        return includeFiles;
    }

    public AutoconfigReplacer(MavenProject project, File webAppSourceDirectory, File autoconfigFile,File autoconfigTempDirectory, Log logger) {
        if (!replaced) {
            AutoconfigReplacer.project = project;
            AutoconfigReplacer.webAppSourceDirectory = webAppSourceDirectory;
            mkdirs(webAppSourceDirectory);
            AutoconfigReplacer.autoconfigFile = autoconfigFile;
            AutoconfigReplacer.logger = logger;
            AutoconfigReplacer.buildDirectory = new File(project.getBuild().getOutputDirectory());
            mkdirs(buildDirectory);
            AutoconfigReplacer.baseDirectory = new File(project.getBuild().getDirectory());
//            AutoconfigReplacer.autoconfigTempDirectory = new File(buildDirectory + File.separator + "hsf_jetty_temp/antx_autoconfig");
            AutoconfigReplacer.autoconfigTempDirectory = autoconfigTempDirectory;
                    mkdirs(autoconfigTempDirectory);
            replace();
        }
    }

    /*    public AutoconfigReplacer(String baseDirectoryPath,String buildDirectoryPath,File webAppSourceDirectory, File autoconfigFile, Log logger) {
            if (!replaced) {
                AutoconfigReplacer.webAppSourceDirectory = webAppSourceDirectory;
                mkdirs(webAppSourceDirectory);
                AutoconfigReplacer.autoconfigFile = autoconfigFile;
                AutoconfigReplacer.logger = logger;
                AutoconfigReplacer.buildDirectory = new File(buildDirectoryPath);
                mkdirs(buildDirectory);
                AutoconfigReplacer.baseDirectory = new File(baseDirectoryPath);
                AutoconfigReplacer.autoconfigTempDirectory = new File (buildDirectory+File.separator+"hsf_jetty_temp/antx_autoconfig");
                mkdirs(autoconfigTempDirectory);
                replace();
            }
        }*/
    private void mkdirs(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private File getAutoconfigFile() {
        String[] autoconfigPath = new String[]{autoconfigFile.getAbsolutePath(), webAppSourceDirectory + File.separator + "META-INF/autoconfig/auto-config.xml"
                , webAppSourceDirectory + File.separator + "META-INF/autoconf/auto-config.xml"};
        for (String path : autoconfigPath) {
            autoconfigFile = new File(path);
            if (autoconfigFile == null || !autoconfigFile.exists()) {
                logger.error(autoconfigFile.getAbsolutePath() + ", autoconfig file not exists!");
            } else {
                logger.info(autoconfigFile.getAbsolutePath() + ", autoconfig file exists!");
                return autoconfigFile;
            }
        }
        return null;
    }

    public void replace() {
        File autoconfig = getAutoconfigFile();
        boolean existsAutoconfigFile = (autoconfig != null && autoconfigFile.exists());
//        String logCharset = "utf-8";//GBK
        String logCharset = null;//GBK
//        if (logCharset == null||logCharset.length() == 0) {
//            logCharset = CharsetUtil.detectedSystemCharset();
//        }
//        logger.info("-------------------------------------------------");
//        logger.info("Detected system charset encoding: " + logCharset);
//        logger.info("If your can't read the following text, specify correct one like this: ");
//        logger.info("");
//        logger.info("  mvn -Dautoconfig.charset=yourcharset");
//        logger.info("");

        LogConfigurator.getConfigurator().configureDefault(false, logCharset);

        ConfigRuntimeImpl antxRuntime = new ConfigRuntimeImpl(System.in, System.out, System.err, logCharset);
//        ConfigRuntimeImpl antxRuntime = new WebInfClassesConfigRuntimeImpl(System.in, System.out, System.err, logCharset);

        //ori dependecy jars
        File[] oriJars = getRuntimeJars().toArray(new File[0]);

        String[] oriPath = new String[oriJars.length + (existsAutoconfigFile ? 1 : 0)];
//        logger.info(oriJars.length+"-"+oriPath.length+"-"+(existsAutoconfigFile ? 2 : 0)+"-"+existsAutoconfigFile);
        for (int i = 0; i < oriJars.length; i++) {
//            logger.info(oriJars[i].getAbsolutePath());
            oriPath[i] = oriJars[i].getAbsolutePath();
        }
        antxRuntime.setDests(oriPath);
//        logger.info("dest size:" + oriPath.length);
        if (existsAutoconfigFile) {//scan current project webapp
            oriPath[oriPath.length - 1] = webAppSourceDirectory.getAbsolutePath();
            String[] outputs = new String[oriPath.length];
            if (outputs.length > 0) {
                outputs[outputs.length - 1] = autoconfigTempDirectory.getAbsolutePath();
                logger.info("根据:" + autoconfig.getAbsolutePath() + "生成配置文件,放置目录:" + autoconfigTempDirectory.getAbsolutePath());
                antxRuntime.setOutputs(outputs);
            }
//            logger.info("output size:" + outputs.length);
        }

        if (getAntxPropertiesFile().exists())
            antxRuntime.setUserPropertiesFile(getAntxPropertiesFile().getAbsolutePath(), null);

//        antxRuntime.setInteractiveMode();

        if (!replaced) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(
                    ConfigRuntimeImpl.class.getClassLoader());
            try {

            replaced = antxRuntime.start();
            logger.info("antx-autoconfig run over: " + (replaced ? "success" : "failure"));
            logger.info("dest size:" + antxRuntime.getDestFiles().length + ",output size:" + antxRuntime.getOutputFiles().length);
            }catch (Exception e){
                logger.error("antx-autoconfig failure!"+e.getMessage());
            }
            finally {
                Thread.currentThread().setContextClassLoader(loader);
            }

        }
        outputAutoconfig(antxRuntime);
    }


    private void outputAutoconfig(ConfigRuntimeImpl antxRuntime) {

        if (replaced) {
            List<ConfigEntry> configEntries = antxRuntime.scan(false);
            /*
            String[] fieldNames = new String[]{"generateTemplateFiles",
                    "generateTemplateFilesIncludingMetaInfos","generateDestFiles"};
            for (String fieldName : fieldNames) {
            getLog().info(fieldName+"-------------------------");
            */
            for (ConfigEntry configEntry : configEntries) {
                ConfigGenerator generator = configEntry.getGenerator();
                try {
                    Field templateField = ConfigGenerator.class.getDeclaredField("generateTemplateFilesIncludingMetaInfos");
                    templateField.setAccessible(true);
                    Map<String, List<ConfigGenerate>> templates = (Map<String, List<ConfigGenerate>>) templateField.get(generator);
                    for (String template : templates.keySet()) {
                        List<ConfigGenerate> generates = templates.get(template);
                        logger.info("template:" + template + ":");
//                        getWebAppSourceDirectory().getAbsolutePath()
                        excludeFiles.add(template);

                        for (ConfigGenerate generate : generates) {
                            logger.info(generate + "-" + generate.getTemplateBase());
                            includeFiles.add(generate.getDestfile());
//                            replacedFiles.put(template,generate.getDestfile());
                        }
//                        autoconfigReplaced.put(template,templates.get(template));
                    }

                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
//                ConfigResource resource = configEntry.getConfigEntryResource();
//                getLog().info(configEntry.getName() + "," + configEntry.getOutputFile() + "," + resource);
            }
//            }

        }
    }

    public File getAntxPropertiesFile() {
//        @Parameter(defaultValue = "${basedir}/antx.properties", required = false)
        File properties = new File(baseDirectory, "antx.properties");
        return properties;
    }

    private List<File> getRuntimeJars() {
        List<File> dependencyFiles = new ArrayList<File>();
        List<Artifact> runtimeAritifacts = project.getRuntimeArtifacts();
        for (Artifact runtimeAritifact : runtimeAritifacts) {
            dependencyFiles.add(runtimeAritifact.getFile());
        }


        /*for (Iterator<Artifact> iter = getProject().getArtifacts().iterator(); iter.hasNext(); ) {
            Artifact artifact = (Artifact) iter.next();
            // Include runtime and compile time libraries, and possibly test libs too
            if (artifact.getType().equals("war")) {
                try {
                    Resource r = Resource.newResource("jar:" + artifact.getFile().toURI().toURL().toString() + "!/");
                    overlays.add(r);
                    getExtraScanTargets().add(artifact.getFile());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            if (((!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) && (!Artifact.SCOPE_TEST.equals(artifact.getScope())))
                    ||
                    (useTestClasspath && Artifact.SCOPE_TEST.equals(artifact.getScope()))) {
                dependencyFiles.add(artifact.getFile());
                getLog().debug("Adding artifact " + artifact.getFile().getName() + " for WEB-INF/lib ");
            }
        }
*/
        return dependencyFiles;
    }

    public File getWebXml(String webXmlPath) {
        if (webXmlPath == null)
            return null;

        {

            String relativePath = null;

            if (webXmlPath.startsWith(URIUtil.SLASH)) {
                relativePath = webXmlPath.substring(1);
            } else {
                relativePath = webXmlPath;
            }

//        path.startsWith(defaultWebAppSourceDirectory.getAbsolutePath())
            if (includeFiles.contains(relativePath)) {
                String resourcePath = autoconfigTempDirectory.getAbsolutePath() + File.separator + relativePath;
                return new File(resourcePath);

            }
            return null;
        }
    }

}
