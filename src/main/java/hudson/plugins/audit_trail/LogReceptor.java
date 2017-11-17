package hudson.plugins.audit_trail;

import org.graylog2.syslog4j.Syslog;
import org.graylog2.syslog4j.SyslogConfigIF;
import org.graylog2.syslog4j.SyslogIF;
import org.graylog2.syslog4j.SyslogMessageIF;
import org.graylog2.syslog4j.impl.message.processor.structured.StructuredSyslogMessageProcessor;
import org.graylog2.syslog4j.impl.message.structured.StructuredSyslogMessage;

import java.io.IOException;
import java.util.Map;

abstract class LogReceptor {

    protected SyslogIF syslog;
    protected StructuredSyslogMessageProcessor processor = new StructuredSyslogMessageProcessor();

    protected void init(SyslogConfigIF conf, String procId, String syslogInstanceName, String defaultMsgHostName,
                        String defaultAppName, int defaultFacility, String syslogServerHostName, int port) {

        if (!Syslog.exists(syslogInstanceName)) {
            conf.setHost(syslogServerHostName);
            conf.setPort(port);
            conf.setFacility(defaultFacility);
            conf.setLocalName(defaultMsgHostName);
            conf.setUseStructuredData(true);
            conf.setUseStructuredData(true);
            conf.setSendLocalName(true);
            conf.setIncludeIdentInMessageModifier(false);

            syslog = Syslog.createInstance(syslogInstanceName, conf);
        } else {
            syslog = Syslog.getInstance(syslogInstanceName);
        }

        processor.setApplicationName(defaultAppName);
        processor.setProcessId(procId);
        syslog.setMessageProcessor(processor);
    }

    protected void process(Severity severity, SyslogMessageIF message) throws IOException {
        switch (severity) {
            case DEBUG:
                syslog.debug(message);
                break;
            case INFO:
                syslog.info(message);
                break;
            case NOTICE:
                syslog.notice(message);
                break;
            case WARN:
                syslog.warn(message);
                break;
            case ERROR:
                syslog.error(message);
                break;
            case CRITICAL:
                syslog.critical(message);
                break;
            case ALERT:
                syslog.alert(message);
                break;
            case EMERGENCY:
                syslog.emergency(message);
                break;
        }
    }

    public void process(String messageID, Severity severity, Map<String, Map<String, String>> props, String textMessage)
            throws IOException {
        StructuredSyslogMessage message = new StructuredSyslogMessage(messageID, processor.getProcessId(), props,
                textMessage);
        process(severity, message);
    }

    public void flush() {
        syslog.flush();
    }

    public enum Severity {
        DEBUG,
        INFO,
        NOTICE,
        WARN,
        ERROR,
        CRITICAL,
        ALERT,
        EMERGENCY
    }
}
