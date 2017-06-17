package com.khronodragon.bluestone.handlers;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class RejectedExecHandlerImpl implements RejectedExecutionHandler {
    private String poolName;

    public RejectedExecHandlerImpl(String name) {
        poolName = name;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.out.println("[ThreadPool: " + poolName + "] Runnable rejected: " + r.toString());
    }
}