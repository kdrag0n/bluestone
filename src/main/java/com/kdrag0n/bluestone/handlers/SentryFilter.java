package com.kdrag0n.bluestone.handlers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class SentryFilter extends Filter<ILoggingEvent> {
    public static boolean denyAll = false;

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (denyAll)
            return FilterReply.DENY;

        if (event.getThrowableProxy().getClassName().contains("ErrorResponseException")) {
            return FilterReply.DENY;
        } else {
            return FilterReply.NEUTRAL;
        }
    }
}
