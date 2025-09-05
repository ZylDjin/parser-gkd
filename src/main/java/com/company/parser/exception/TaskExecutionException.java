package com.company.parser.exception;

/**
 * Исключение при ошибках выполнения задач
 */
public class TaskExecutionException extends ParserException {

    private String taskId;
    private String taskName;

    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskExecutionException(String taskId, String taskName, String message) {
        super(String.format("Task '%s' [%s] failed: %s", taskName, taskId, message));
        this.taskId = taskId;
        this.taskName = taskName;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }
}
