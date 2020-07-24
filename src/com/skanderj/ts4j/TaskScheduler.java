package com.skanderj.ts4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class TaskScheduler {
	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(0);
	private static final Map<String, ScheduledFuture<?>> scheduledFutures = new HashMap<>();
	private static final Map<String, Task> tasks = new HashMap<>();

	private TaskScheduler() {
	}

	public static boolean scheduleTask(final String identifier, final Task task) {
		ScheduledFuture<?> future = null;
		if (task.getPeriod().value != Task.NO_REPEATS) {
			switch (task.type()) {
			case FIXED_DELAY:
				future = TaskScheduler.executor.scheduleWithFixedDelay(task.asRunnable(), task.getInitialDelay().value, task.getInitialDelay().unit.convert(task.getPeriod().value, task.getPeriod().unit), task.getInitialDelay().unit);
				break;
			case FIXED_RATE:
				future = TaskScheduler.executor.scheduleAtFixedRate(task.asRunnable(), task.getInitialDelay().value, task.getInitialDelay().unit.convert(task.getPeriod().value, task.getPeriod().unit), task.getInitialDelay().unit);
				break;
			}
		} else {
			future = TaskScheduler.executor.schedule(task.asRunnable(), task.getInitialDelay().value, task.getInitialDelay().unit);
		}
		if (future != null) {
			TaskScheduler.scheduledFutures.put(identifier, future);
			TaskScheduler.tasks.put(identifier, task);
			return true;
		} else {
			return false;
		}
	}

	public static boolean cancelTask(final String identifier, final boolean finish) {
		final ScheduledFuture<?> future = TaskScheduler.scheduledFutures.get(identifier);
		if (future != null) {
			future.cancel(finish);
			return true;
		} else {
			return false;
		}
	}

	public static int getRepeatsCounter(final String identifier) {
		return TaskScheduler.tasks.get(identifier) == null ? -1 : TaskScheduler.tasks.get(identifier).getRepeatsCounter();
	}

	public static void main(final String[] args) {
		TaskScheduler.scheduleTask("time-every-5-sec", new Task(0, 5, TimeUnit.SECONDS) {
			@Override
			public TaskType type() {
				return TaskType.FIXED_DELAY;
			}

			@Override
			public void execute() {
				System.out.println("Task #1: " + new SimpleDateFormat("hh:mm:ss").format(new Date()));
			}
		});
		TaskScheduler.scheduleTask("time-every-3-sec", new Task(0, 3, TimeUnit.SECONDS) {
			@Override
			public TaskType type() {
				return TaskType.FIXED_DELAY;
			}

			@Override
			public void execute() {
				System.out.println("Task #2: " + new SimpleDateFormat("hh:mm:ss").format(new Date()));
			}
		});
		TaskScheduler.scheduleTask("time-every-1-sec", new Task(0, 1, TimeUnit.SECONDS) {
			@Override
			public TaskType type() {
				return TaskType.FIXED_DELAY;
			}

			@Override
			public void execute() {
				System.out.println("Task #3: " + new SimpleDateFormat("hh:mm:ss").format(new Date()));
			}
		});
		TaskScheduler.scheduleTask("counters-checker", new Task(0, 500, TimeUnit.MILLISECONDS) {
			@Override
			public TaskType type() {
				return TaskType.FIXED_DELAY;
			}

			@Override
			public void execute() {
				boolean cancel = true;
				if (TaskScheduler.getRepeatsCounter(("time-every-5-sec")) >= 2) {
					TaskScheduler.cancelTask("time-every-5-sec", true);
				} else {
					cancel = false;
				}
				if (TaskScheduler.getRepeatsCounter(("time-every-3-sec")) >= 4) {
					TaskScheduler.cancelTask("time-every-3-sec", true);
				} else {
					cancel = false;
				}
				if (TaskScheduler.getRepeatsCounter(("time-every-1-sec")) >= 10) {
					TaskScheduler.cancelTask("time-every-1-sec", true);
				} else {
					cancel = false;
				}
				if (cancel) {
					TaskScheduler.cancelTask("counters-checker", true);
				}
			}
		});
	}
}
