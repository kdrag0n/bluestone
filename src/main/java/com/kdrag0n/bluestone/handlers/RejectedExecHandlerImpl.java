package com.kdrag0n.bluestone.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class RejectedExecHandlerImpl implements RejectedExecutionHandler {
    private final Logger logger;

    public RejectedExecHandlerImpl(String name) {
        logger = LoggerFactory.getLogger("Pool " + name);
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        logger.error("Runnable rejected: {}", r);
    }
}