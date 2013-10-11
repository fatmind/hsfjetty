package com.taobao.hsf.jetty.component;

import com.alibaba.antx.config.ConfigResource;
import com.alibaba.antx.config.ConfigRuntimeImpl;
import com.alibaba.antx.config.entry.ConfigEntry;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * handle the autoconfig template config as the "<generate template="WEB-INF/classes/file2.xml" />" <br />
 * if the file2.xml exists in the dir ${webapp-context}/WEB-INF/classes, do as normal.
 * if the file2.xml in the dir ${webapp}/src/main/resources, redirect the path.
 *
 * @auther: zheshan
 * Time: 13-9-12 上午9:27
 */
public class WebInfClassesConfigRuntimeImpl extends ConfigRuntimeImpl{

    public WebInfClassesConfigRuntimeImpl(InputStream in, PrintStream out, PrintStream err, String logCharset) {
        super(in,out,err,logCharset);
    }

    @Override
    public List scan(boolean includeEmptyEntries) {
        File[] destFiles = getDestFiles();
        File[] outputFiles = getOutputFiles();
        List entries = new ArrayList(destFiles.length);

        for (int i = 0; i < destFiles.length; i++) {
            File destFile = destFiles[i];
            File outputFile = outputFiles[i];

            ConfigEntry entry = getConfigEntryFactory().create(new ConfigResource(destFile), outputFile, getType());

            entry.scan();

            if (includeEmptyEntries || !entry.isEmpty()) {
                entries.add(entry);
            }
        }

        return entries;
    }
}
