package com.atm;

/** Counts failed logins for one login screen and reports when access must be denied. */
public class LoginAttemptTracker {
    /** Default number of attempts before lock-out. */
    public static final int MAX_ATTEMPTS = 3;

    private final int maxAttempts;
    private int failures;

    public LoginAttemptTracker() {
        this(MAX_ATTEMPTS);
    }

    public LoginAttemptTracker(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
    }

    /** Registers one failed attempt. */
    public void recordFailure() {
        if (failures < maxAttempts) {
            failures++;
        }
    }

    public int getFailures() { return failures; }
    public int getMaxAttempts() { return maxAttempts; }
    public int remaining() { return maxAttempts - failures; }
    public boolean isLocked() { return failures >= maxAttempts; }
}
