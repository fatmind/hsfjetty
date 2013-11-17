package com.taobao.hsf.jetty.component;

import com.taobao.ticket.hsfjetty.Counter;
import org.eclipse.jetty.util.IO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Simple streamer for the console output from a Process
 */
public class ConsoleStreamer implements Runnable {
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
        try {
            count = counter.getCountAndInc();
        } catch (Exception e) {
                                /* ignore */
        }
        String line;
        try {
            while ((line = reader.readLine()) != (null)) {
                if (print1st) {
                    if (count > 0) {
                        if (line.startsWith("------")) {
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