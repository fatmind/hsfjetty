package com.taobao.hsf.jetty.component;

//import com.taobao.hsf.container.HSFContainer;
import com.taobao.hsf.container.HSFContainer;
import com.taobao.hsf.jetty.JettyServer;
import com.taobao.hsf.jetty.JettyWebAppContext;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;

import javax.management.MBeanServer;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

//import com.taobao.hsf.jetty.PluginLog;
//import org.apache.maven.plugin.logging.Log;

public class Starter
{ 
    public static final String PORT_SYSPROPERTY = "jetty.port";
    private static final Logger LOG = Log.getLogger(Starter.class.toString());

//    private static final Log LOG =  PluginLog.getLog();

    private List<File> jettyXmls; // list of jetty.xml config files to apply - Mandatory
    private File contextXml; //name of context xml file to configure the webapp - Mandatory

    private JettyServer server;
    private JettyWebAppContext webApp;
    
    private int stopPort=0;
    private String stopKey=null;
    private Properties props;
    private String token;

    
    
    public void configureJetty () throws Exception
    {
        LOG.debug("Starting Jetty Server ...");

        this.server = new JettyServer();

        //apply any configs from jetty.xml files first 
        applyJettyXml ();

        // if the user hasn't configured a connector in the jetty.xml
        //then use a default
        Connector[] connectors = this.server.getConnectors();
        if (connectors == null|| connectors.length == 0) {
            String port = (String) props.get("web.port");
            if (port != null) {
                connectors = new Connector[]{this.server.createDefaultConnector(port)};
            } else {
                //if a SystemProperty -Djetty.port=<portnum> has been supplied, use that as the default port
                connectors = new Connector[]{this.server.createDefaultConnector(System.getProperty(PORT_SYSPROPERTY, null))};
            }
            this.server.setConnectors(connectors);
        }

        //check that everything got configured, and if not, make the handlers
        HandlerCollection handlers = (HandlerCollection) server.getChildHandlerByClass(HandlerCollection.class);
        if (handlers == null)
        {
            handlers = new HandlerCollection();
            server.setHandler(handlers);
        }

        //check if contexts already configured, create if not
        this.server.configureHandlers();


        //configure webapp from properties file describing unassembled webapp
        configureWebApp();

        //set up the webapp from the context xml file provided
        //NOTE: just like jetty:run mojo this means that the context file can
        //potentially override settings made in the pom. Ideally, we'd like
        //the pom to override the context xml file, but as the other mojos all
        //configure a WebAppContext in the pom (the <webApp> element), it is 
        //already configured by the time the context xml file is applied.
        if (contextXml != null)
        {

            XmlConfiguration xmlConfiguration = new XmlConfiguration(Resource.newResource(contextXml.getAbsolutePath()).getURL());
            xmlConfiguration.getIdMap().put("Server",server);
            xmlConfiguration.configure(webApp);
        }


        /*ajp protocol
            Ajp13SocketConnector ajp = new Ajp13SocketConnector();
            ajp.setPort(8009);
            server.addConnector(ajp);
            this.server.addConnector(ajp);
        */
        this.server.addWebApplication(webApp);

        //jetty mbean
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanContainer mBeanContainer = new MBeanContainer(mBeanServer);
        server.getContainer().addEventListener(mBeanContainer);
        mBeanContainer.start();




        System.err.println("STOP PORT="+stopPort+", STOP KEY="+stopKey);
        if(stopPort>0 && stopKey!=null)
        {
//            ShutdownMonitor monitor = ShutdownMonitor.getInstance();
//            monitor.setPort(stopPort);
//            monitor.setKey(stopKey);
//            monitor.setExitVm(true);
        }
    }
    
    
    public void configureWebApp ()
    throws Exception
    {
        if (props == null)
            return;

        String autoconfigDirPath = null;
        String str = (String)props.get("autoconfig.dir");
        if (str != null)
            autoconfigDirPath = str;
        str = (String)props.get("autoconfig.output");
        Set<String> autoconfigOutputSet = new HashSet<String>(0);
        if(str != null)
        {
            String[] autoconfigOutput = str.split(",");
            autoconfigOutputSet = new HashSet<String>(Arrays.asList(autoconfigOutput));
        }

        webApp = new JettyWebAppContext(autoconfigOutputSet,new File(autoconfigDirPath));
        //apply a properties file that defines the things that we configure in the jetty:run plugin:
        // - the context path
        str = (String)props.get("context.path");
        if (str != null)
        {
            webApp.setContextPath((str.startsWith("/") ? str : "/"+ str));
        }
        
        // - web.xml
        str = (String)props.get("web.xml");
        if (str != null)
            webApp.setDescriptor(str);
        
        // - the tmp directory
        str = (String)props.getProperty("tmp.dir");
        if (str != null)
            webApp.setTempDirectory(new File(str.trim()));
        
        // - the base directory
        str = (String)props.getProperty("base.dir");
        if (str != null && !"".equals(str.trim()))
            webApp.setWar(str);

        // - the multiple comma separated resource dirs
        str = (String)props.getProperty("res.dirs");
        if (str != null && !"".equals(str.trim()))
        {
            ResourceCollection resources = new ResourceCollection(str);
            webApp.setBaseResource(resources);
        }
        
        // - overlays
        str = (String)props.getProperty("overlay.files");
        if (str != null && !"".equals(str.trim()))
        {
            List<Resource> overlays = new ArrayList<Resource>();
            String[] names = str.split(",");
            for (int j=0; names != null && j < names.length; j++)

//                overlays.add(Resource.newResource("jar:"+Resource.toURL(new File(names[j].trim())).toString()+"!/"));
                overlays.add(Resource.newResource("jar:" + Resource.newResource(names[j].trim()).getURL() + "!/"));
            webApp.setOverlays(overlays);
        }

        // - the equivalent of web-inf classes
        str = (String)props.getProperty("classes.dir");
        if (str != null && !"".equals(str.trim()))
        {
            webApp.setWebInfClasses(Arrays.asList(new File(str)));
        }

        
//        str = (String)props.getProperty("testClasses.dir");
//        if (str != null && !"".equals(str.trim()))
//        {
//            webApp.setTestClasses(new File(str));
//        }


        // - the equivalent of web-inf lib
        str = (String)props.getProperty("lib.jars");
        if (str != null && !"".equals(str.trim()))
        {
            List<File> jars = new ArrayList<File>();
            String[] names = str.split(",");
            for (int j=0; names != null && j < names.length; j++)
                jars.add(new File(names[j].trim()));
            webApp.setWebInfLib(jars);
        }

        WebAppClassLoader hsfWebAppClassLoader = new HsfWebAppClassLoader(HsfWebAppClassLoader.class.getClassLoader(),webApp);
        webApp.setClassLoader(hsfWebAppClassLoader);

        str = (String)props.getProperty("hsf.dir");
        if (str != null && !"".equals(str.trim()))
        {
            this.configureHsf(str);
        }
        
    }
    private void configureHsf(String hsfDir) throws IOException {
//        String hsfDir = "D:\\env\\hsf\\1.4.9.6\\taobao-hsf.sar";


        try {

            if (hsfDir != null && hsfDir.length() != 0) {
            ClassLoader hsfWebAppClassLoader = webApp.getClassLoader();
//              WebAppClassLoader hsfWebAppClassLoader = new HsfWebAppClassLoader(HsfWebAppClassLoader.class.getClassLoader(),webApp);
//              webApp.setClassLoader(hsfWebAppClassLoader);
//            File[] hsfLibs = getHSFLibs(hsfDir);
//            ((HsfWebAppClassLoader)hsfWebAppClassLoader).addJars(new File(hsfDir, "lib"));

              HSFContainer.setThirdContainerClassLoader(hsfWebAppClassLoader);

//            FrameworkProperties.setProperty("org.osgi.framework.system.packages.extra","org.springframework.*");
//            FrameworkProperties.setProperty("org.osgi.framework.bootdelegation","org.springframework.*");
//            FrameworkProperties.setProperty("osgi.parentClassLoader","fwk");

                HSFContainer.start(new String[]{hsfDir});
                Map<String, Class<?>> exportedClasses = HSFContainer.getExportedClasses();

//            Class hsfContainerClass = hsfWebAppClassLoader.loadClass("com.taobao.hsf.container.HSFContainer");
//            hsfContainerClass.getMethod("setThirdContainerClassLoader", new Class[] { ClassLoader.class }).invoke(null, new Object[] {webAppClassLoader});
//            webApp.setClassLoader(hsfWebAppClassLoader);

//            hsfContainerClass.getMethod("start", String[].class).invoke(null, new Object[]{new String[]{hsfDir}});
//            Map<String, Class<?>> exportedClasses = (Map)hsfContainerClass.getMethod("getExportedClasses", (Class[])null).invoke(null, (Object[])null);

                ((HsfWebAppClassLoader)hsfWebAppClassLoader).setExportedClasses(exportedClasses);

                ClassLoader ctl = Thread.currentThread().getContextClassLoader();
                try {
                    final Class clz = exportedClasses.get("com.taobao.config.client.SubscriberRegistration");

                    if(clz != null){
                        Thread.currentThread().setContextClassLoader(clz.getClassLoader());
                        Constructor c = clz.getConstructor(String.class,String.class);
                        Object obj = c.newInstance("HSF_Jetty","hsf_jetty");
                        Method m = clz.getMethod("setCacheable", Boolean.TYPE);
                        if(m != null){
                            m.invoke(obj, Boolean.TRUE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }finally{
                    Thread.currentThread().setContextClassLoader(ctl);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void getConfiguration (String[] args)
    throws Exception
    {
        for (int i=0; i<args.length; i++)
        {
            //--stop-port
            if ("--stop-port".equals(args[i]))
                stopPort = Integer.parseInt(args[++i]);

            //--stop-key
            if ("--stop-key".equals(args[i]))
                stopKey = args[++i];

            //--jettyXml
            if ("--jetty-xml".equals(args[i]))
            {
                jettyXmls = new ArrayList<File>();
                String[] names = args[++i].split(",");
                for (int j=0; names!= null && j < names.length; j++)
                {
                    jettyXmls.add(new File(names[j].trim()));
                }  
            }

            //--context-xml
            if ("--context-xml".equals(args[i]))
            {
                contextXml = new File(args[++i]);
            }

            //--props
            if ("--props".equals(args[i]))
            {
                File f = new File(args[++i].trim());
                props = new Properties();
                props.load(new FileInputStream(f));
            }
            
            //--token
            if ("--token".equals(args[i]))
            {
                token = args[++i].trim();
            }
        }
    }


    public void run() throws Exception
    {
        LOG.info("Started Jetty Server");
        server.start();  
    }

    
    public void join () throws Exception
    {
        server.join();
    }
    
    
    public void communicateStartupResult (Exception e)
    {
        if (token != null)
        {
            if (e==null)
                System.out.println(token);
            else
                System.out.println(token+"\t"+e.getMessage());
        }
    }
    
    
    public void applyJettyXml() throws Exception
    {
        if (jettyXmls == null)
            return;
        
        for ( File xmlFile : jettyXmls )
        {
            LOG.info( "Configuring Jetty from xml configuration file = " + xmlFile.getCanonicalPath() );

//            XmlConfiguration xmlConfiguration = new XmlConfiguration(Resource.toURL(xmlFile));
            XmlConfiguration xmlConfiguration = new XmlConfiguration(Resource.newResource(xmlFile.getAbsolutePath()).getURL());
            xmlConfiguration.configure(this.server);
        }
    }




    protected void prependHandler (Handler handler, HandlerCollection handlers)
    {
        if (handler == null || handlers == null)
            return;

        Handler[] existing = handlers.getChildHandlers();
        Handler[] children = new Handler[existing.length + 1];
        children[0] = handler;
        System.arraycopy(existing, 0, children, 1, existing.length);
        handlers.setHandlers(children);
    }
    
    
    public static final void main(String[] args)
    {
       Starter starter = null;
       try
       {

           System.out.println();
           System.out.println("----------------------listening at 8787 for remote debug---------------------");
           System.out.println();
           if (args == null)
               System.exit(1);

           TimeUnit.SECONDS.sleep(3L);
           starter = new Starter();
           starter.getConfiguration(args);
           starter.configureJetty();
           starter.run();
           starter.communicateStartupResult(null);
           starter.join();
       }
       catch (Exception e)
       {
           if(starter != null)
            starter.communicateStartupResult(e);
           e.printStackTrace();
           System.exit(1);
       }

    }


    public File[] getHSFLibs(String hsfDir){
        File hsfLibDir = new File(hsfDir,"lib");
        return hsfLibDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".jar");
            }
        });
    }
}
