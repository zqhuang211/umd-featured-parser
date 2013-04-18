/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
/**
 *
 * @author zqhuang
 */
public final class SimpleLogFormatter extends Formatter {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        sb.append(formatMessage(record))
            .append(LINE_SEPARATOR);

//        if (record.getThrown() != null) {
//            try {
//                StringWriter sw = new StringWriter();
//                PrintWriter pw = new PrintWriter(sw);
//                record.getThrown().printStackTrace(pw);
//                pw.close();
//                sb.append(sw.toString());
//            } catch (Exception ex) {
//                // ignore
//            }
//        }

        return sb.toString();
    }
}