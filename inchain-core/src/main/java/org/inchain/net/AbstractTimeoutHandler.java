package org.inchain.net;

import java.util.Timer;
import java.util.TimerTask;

public abstract class AbstractTimeoutHandler {
	
    private TimerTask timeoutTask;
    private long timeoutMillis = 0;
    private boolean timeoutEnabled = true;

    private static final Timer timeoutTimer = new Timer("AbstractTimeoutHandler timeouts", true);

    public synchronized final void setTimeoutEnabled(boolean timeoutEnabled) {
        this.timeoutEnabled = timeoutEnabled;
        resetTimeout();
    }

    public synchronized final void setSocketTimeout(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        resetTimeout();
    }

    protected synchronized void resetTimeout() {
        if (timeoutTask != null)
            timeoutTask.cancel();
        if (timeoutMillis == 0 || !timeoutEnabled)
            return;
        timeoutTask = new TimerTask() {
            @Override
            public void run() {
                timeoutOccurred();
            }
        };
        timeoutTimer.schedule(timeoutTask, timeoutMillis);
    }

    protected abstract void timeoutOccurred();
}
