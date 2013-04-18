/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author zqhuang
 */
public class GlobalLogger {

    public static final Logger logger = Logger.getLogger(GlobalLogger.class.getName());
    
    public static void setLevel(Level level) {
        logger.setLevel(level);
    }

    public static void log(Level level, String message) {
        logger.log(level, message);
    }
    
    public static void init() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        consoleHandler.setFormatter(new SimpleLogFormatter());
        logger.addHandler(consoleHandler);
        logger.setUseParentHandlers(false);
    }
}
