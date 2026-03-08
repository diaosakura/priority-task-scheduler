package com.sakura.scheduler.task;

/**
 * 任务状态流转枚举 (状态机节点)
 */
public enum TaskStatus {
    WAITING(0, "等待中/延迟中"),
    RUNNING(1, "执行中"),
    SUCCESS(2, "执行成功"),
    FAILED(-1, "执行失败");

    private final int code;
    private final String desc;

    TaskStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}