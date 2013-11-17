package com.taobao.hsf.jetty.component;

import com.taobao.hsf.jetty.PluginLog;
import com.taobao.ticket.hsfjetty.Counter;
import org.eclipse.jetty.util.IO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * close the jvm process
 */
public class ShutdownMonitor extends Thread{
    private String stopKey;
    private Process forkedProcess;
    public ShutdownMonitor(String stopKey,Process forkedProcess) {
        super("RunForkedShutdownMonitor");
        this.stopKey = stopKey;
        this.forkedProcess = forkedProcess;
    }


    public void run() {
        if (forkedProcess != null){
            forkedProcess.destroy();
            PluginLog.getLog().info("Forked process is stopped.................");
        }
    }

}