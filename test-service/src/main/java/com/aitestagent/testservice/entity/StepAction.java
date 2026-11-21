package com.aitestagent.testservice.entity;

/**
 * Supported Playwright test step actions.
 */
public enum StepAction {
    NAVIGATE,
    FILL,
    CLICK,
    WAIT,
    ASSERT_VISIBLE,
    ASSERT_TEXT;

    public static StepAction fromString(String actionStr) {
        if (actionStr == null || actionStr.isBlank()) {
            return WAIT;
        }
        try {
            return StepAction.valueOf(actionStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return WAIT;
        }
    }
}
