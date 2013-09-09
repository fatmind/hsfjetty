package com.taobao.hsf.jetty.component;
/*
 * $Id$
 * $HeadURL$
 *
 * ==============================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import com.taobao.hsf.jetty.JettyWebAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Uses the provided class path ONLY, rather than also supporting the adding of
 * the jars in the WEB-INF/lib directory, and the adding of the classes in the
 * WEB-INF/classes directory.
 *
 * @author 遮山
 */
public class HsfWebAppClassLoader extends WebAppClassLoader {
    private  final Logger logger = Log.getLogger(getClass().getName());
    private boolean initialized = false;

    private Map<String, Class<?>> exportedClasses = null;
    private Set<String> existClassPath = new HashSet<String>();

    public HsfWebAppClassLoader(ClassLoader parentClassLoader,JettyWebAppContext context) throws IOException {
        super(parentClassLoader,context);
        initialized = true;

        List<File> classFiles = context.getClassPathFiles();
        for (File classFile : classFiles) {
            this.addClassPath(classFile.getAbsolutePath());
        }
    }

    @Override
    protected synchronized Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        if(exportedClasses != null){
            if(!"org.apache.log4j.DailyRollingFileAppender".equalsIgnoreCase(name)){
                Class clz = exportedClasses.get(name);
                if (clz != null) {
                    return clz;
                }
            }
        }

        return super.loadClass(name, resolve);
    }

    @Override
    public void addClassPath(String classPath) throws IOException {
        /*if (initialized) {
			*//*
			 * Disable the adding of directories to the class path after
			 * initialization with the project class path. XXX Except for the
			 * addition of the WEB-INF/classes
			 *//*
            if (!classPath.endsWith("WEB-INF/classes/"))
                return;
        }*/
        if(existClassPath.contains(classPath)){
            return;
        }
//        System.out.println("**************"+classPath);
        super.addClassPath(classPath);
        existClassPath.add(classPath);
        return;
    }

    @Override
    public void addJars(Resource lib) {
        /*if (initialized) {
			*//*
			 * Disable the adding of jars (or folders of jars) to the class path
			 * after initialization with the project class path.
			 *//*
            return;
        }*/
//        System.out.println("-------------"+lib.getURL().toString());
        super.addJars(lib);
        return;
    }
    public void addJars(File libDir){
            try {
                this.addJars(Resource.newResource(libDir.getAbsolutePath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public Map<String, Class<?>> getExportedClasses() {
        return exportedClasses;
    }

    public void setExportedClasses(Map<String, Class<?>> exportedClasses) {
        this.exportedClasses = exportedClasses;
    }
/*
    @Override
    public URL getResource(String name)
    {

        Class clazz = getClassFromResourceName(name);
        if (clazz != null)
        {
            ClassLoader cl = clazz.getClassLoader();
            if (cl == null)
                return null;

            if ("org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader".startsWith(cl.getClass().getName())) {
                return null;
            }

            URL url = cl.getResource(name);
            if (url != null)
                return url;
        }


        return super.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        Class clazz = getClassFromResourceName(name);
        if (clazz != null) {
            ClassLoader cl = clazz.getClassLoader();
            if (cl == null)
                return null;

            if ("org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader".startsWith(cl.getClass().getName())) {
                return null;
            }

            InputStream in = cl.getResourceAsStream(name);
            if (in != null)
                return in;
        }
        return super.getResourceAsStream(name);
    }
    private Class<?> getClassFromResourceName(String name) {
        if ((name == null) || (!(name.endsWith(".class"))))
            return null;

        String className = name;
        if (className.startsWith("/"))
            className = className.substring(1);

        className = className.substring(0, className.length() - 6).replace('/', '.');
        Class clazz = (Class)exportedClasses.get(className);

        return clazz;
    }*/
}

