/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * 一个可以延迟或者周期性调度任务的线程池。
 *
 * schedule方法根据变量不同的延迟创建一个任务，并且返回可以取消或者查看执行状态的
 * 任务对象。scheduleAtFixedRate跟scheduleWithFixedDelay方法可以在任务取消
 * 之前周期性的执行任务。
 *
 * 使用Executor.execute(Runnable)方法跟ExecutorService.submit方法提交的任务
 * 按照延迟时间为0进行调度。schedule方法延迟时间设置如果小于等于0，就会被当成立即
 * 执行。
 *
 * 所有的schedule方法都是以相对延迟时间跟周期作为参数，而不是绝对时间或者日期。
 * 把以java.util.Date表示的绝对时间转化成schedule方法要求是这种形式是非常
 * 简单的。比如，想要在未来一个特定的时间执行一个任务，你可以使用
 * chedule(task,date.getTime() - System.currentTimeMillis(),TimeUnit.MILLISECONDS)}
 * 这种方式。但是要注意，相对延迟的过期不一定与当前日期一致,在这种情况下任务会根据网络时间同步协议、
 * 时钟漂移或其他因素启动。
 *
 * Executors为ScheduledExecutorService的实现提供了同样的工厂方法。
 *
 * 示例：
 *
 * 下面这个类有一个方法启动一个ScheduledExecutorService，每10秒输出一次beep，
 * 持续1小时
 *
 * import static java.util.concurrent.TimeUnit.*;
 * class BeeperControl {
 *   private final ScheduledExecutorService scheduler =
 *     Executors.newScheduledThreadPool(1);
 *
 *   public void beepForAnHour() {
 *     final Runnable beeper = new Runnable() {
 *       public void run() { System.out.println("beep"); }
 *     };
 *
 *     //启动任务
 *     final ScheduledFuture<?> beeperHandle =
 *       scheduler.scheduleAtFixedRate(beeper, 10, 10, SECONDS);
 *
 *     //取消任务
 *     scheduler.schedule(new Runnable() {
 *       public void run() { beeperHandle.cancel(true); }
 *     }, 60 * 60, SECONDS);
 *   }
 * }}
 *
 *
 *
 * An {@link ExecutorService} that can schedule commands to run after a given
 * delay, or to execute periodically.
 *
 * <p>The {@code schedule} methods create tasks with various delays
 * and return a task object that can be used to cancel or check
 * execution. The {@code scheduleAtFixedRate} and
 * {@code scheduleWithFixedDelay} methods create and execute tasks
 * that run periodically until cancelled.
 *
 * <p>Commands submitted using the {@link Executor#execute(Runnable)}
 * and {@link ExecutorService} {@code submit} methods are scheduled
 * with a requested delay of zero. Zero and negative delays (but not
 * periods) are also allowed in {@code schedule} methods, and are
 * treated as requests for immediate execution.
 *
 * <p>All {@code schedule} methods accept <em>relative</em> delays and
 * periods as arguments, not absolute times or dates. It is a simple
 * matter to transform an absolute time represented as a {@link
 * java.util.Date} to the required form. For example, to schedule at
 * a certain future {@code date}, you can use: {@code schedule(task,
 * date.getTime() - System.currentTimeMillis(),
 * TimeUnit.MILLISECONDS)}. Beware however that expiration of a
 * relative delay need not coincide with the current {@code Date} at
 * which the task is enabled due to network time synchronization
 * protocols, clock drift, or other factors.
 *
 * <p>The {@link Executors} class provides convenient factory methods for
 * the ScheduledExecutorService implementations provided in this package.
 *
 * <h3>Usage Example</h3>
 *
 * Here is a class with a method that sets up a ScheduledExecutorService
 * to beep every ten seconds for an hour:
 *
 *  <pre> {@code
 * import static java.util.concurrent.TimeUnit.*;
 * class BeeperControl {
 *   private final ScheduledExecutorService scheduler =
 *     Executors.newScheduledThreadPool(1);
 *
 *   public void beepForAnHour() {
 *     final Runnable beeper = new Runnable() {
 *       public void run() { System.out.println("beep"); }
 *     };
 *     final ScheduledFuture<?> beeperHandle =
 *       scheduler.scheduleAtFixedRate(beeper, 10, 10, SECONDS);
 *     scheduler.schedule(new Runnable() {
 *       public void run() { beeperHandle.cancel(true); }
 *     }, 60 * 60, SECONDS);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ScheduledExecutorService extends ExecutorService {

    /**
     * 达到执行延迟时间以后，创建并执行一次任务。
     *
     * Creates and executes a one-shot action that becomes enabled
     * after the given delay.
     * @param command 用于执行的任务
     * @param delay 从现在起延迟的时间
     * @param unit 延迟时间的单位
     * @return 返回一个ScheduledFuture对象，表示等待执行的任务。该对象在任务
     *         完成以后，get()方法会返回null。
     *
     * @param command the task to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @return a ScheduledFuture representing pending completion of
     *         the task and whose {@code get()} method will return
     *         {@code null} upon completion
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if command is null
     */
    public ScheduledFuture<?> schedule(Runnable command,
                                       long delay, TimeUnit unit);

    /**
     * 达到执行延迟时间以后，创建并执行ScheduledFuture。
     *
     * Creates and executes a ScheduledFuture that becomes enabled after the
     * given delay.
     *
     * @return 返回一个ScheduledFuture对象，可以获取任务执行结果或者取消任务。
     *
     * @param callable the function to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @param <V> the type of the callable's result
     * @return a ScheduledFuture that can be used to extract result or cancel
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if callable is null
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay, TimeUnit unit);

    /**
     * 在达到初始延迟时间以后，创建一个周期性的任务，按照给定的周期时间进行周期性
     * 的调度执行。也就是说任务会在initialDelay，initialDelay+period，
     * initialDelay + 2 * period以此类推的时间点执行。
     * 如果任何一次任务的执行抛出了异常，那么后续的执行将会停止。否则的话，任务只有在
     * 被取消或者调度线程池被关闭的情况下才会终止。如果该任务的任何一次执行超过了它
     * 的周期时间，那么后续的执行会推迟，但绝对不会重叠执行。
     *
     * Creates and executes a periodic action that becomes enabled first
     * after the given initial delay, and subsequently with the given
     * period; that is executions will commence after
     * {@code initialDelay} then {@code initialDelay+period}, then
     * {@code initialDelay + 2 * period}, and so on.
     * If any execution of the task
     * encounters an exception, subsequent executions are suppressed.
     * Otherwise, the task will only terminate via cancellation or
     * termination of the executor.  If any execution of this task
     * takes longer than its period, then subsequent executions
     * may start late, but will not concurrently execute.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit the time unit of the initialDelay and period parameters
     * @return a ScheduledFuture representing pending completion of
     *         the task, and whose {@code get()} method will throw an
     *         exception upon cancellation
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if period less than or equal to zero
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit);

    /**
     * 在达到初始延迟时间后创建并执行任务，后续会在上一次任务执行结束以后等待
     * 指定延迟时间后进行周期性执行。如果任何一次任务的执行抛出了异常，
     * 那么后续的执行将会停止。否则的话，任务只有在被取消或者调度线程池被关闭的情况下才会终止。
     *
     * Creates and executes a periodic action that becomes enabled first
     * after the given initial delay, and subsequently with the
     * given delay between the termination of one execution and the
     * commencement of the next.  If any execution of the task
     * encounters an exception, subsequent executions are suppressed.
     * Otherwise, the task will only terminate via cancellation or
     * termination of the executor.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one
     * execution and the commencement of the next
     * @param unit the time unit of the initialDelay and delay parameters
     * @return a ScheduledFuture representing pending completion of
     *         the task, and whose {@code get()} method will throw an
     *         exception upon cancellation
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if delay less than or equal to zero
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit);

}
