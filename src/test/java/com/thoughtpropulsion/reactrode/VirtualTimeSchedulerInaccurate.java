package com.thoughtpropulsion.reactrode;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.test.scheduler.VirtualTimeScheduler;
import reactor.util.annotation.Nullable;

public class VirtualTimeSchedulerInaccurate extends VirtualTimeScheduler {

  private final Random random;
  private long standardDeviationNanos;
  private volatile boolean shutdown;

  public static VirtualTimeScheduler create(
      final Random random,
      final long standardDeviation,
      final TimeUnit timeUnit) {
    return new VirtualTimeSchedulerInaccurate(random, timeUnit.toNanos(standardDeviation));
  }

  private VirtualTimeSchedulerInaccurate(
      final Random random,
      final long standardDeviationNanos) {
    this.random = random;
    this.standardDeviationNanos = standardDeviationNanos;
  }

  @Override
  public Disposable schedule(
      final Runnable task) {

    return schedule(task, 0, TimeUnit.MILLISECONDS);
  }

  @Override
  public Disposable schedule(
      final Runnable task,
      final long delayArg,
      final TimeUnit unit) {

    return super.schedule(task, skewDelay(delayArg, unit), TimeUnit.NANOSECONDS);
  }

  @Override
  public Disposable schedulePeriodically(
      final Runnable task,
      final long initialDelay,
      final long period,
      final TimeUnit unit) {

    /*
     We haven't done the work (yet) to make the periodic schedule inaccurate.
     That scheduling is done via VirtualTimeScheduler.PeriodicTask which does
     the calculation of the next run time after the initial run.
     */
//    throw new NotImplementedException();

    /*
     This implementation is provisional. It isn't as accurate (over time) as the
     one in VirtualTimeScheduler.PeriodicTask. This one calculates the next time
     to run based on the current time which means it accumulates error with every step.
     */
    final long periodInNanoseconds = unit.toNanos(period);

    /*
     FIXME:
     In VirtualTimeScheduler this is:
     long nowNanoseconds = nanoTime
     By calling now() we're adding deferredNanoTime
     */
    final long firstNowNanoseconds = now(TimeUnit.NANOSECONDS);

    final long firstStartInNanoseconds = firstNowNanoseconds + unit.toNanos(initialDelay);

    PeriodicTask periodicTask = new PeriodicTask(
        firstStartInNanoseconds,
        task,
        firstNowNanoseconds,
        periodInNanoseconds);

    /*
     !! The point of reproducing this class from VirtualTimeScheduler
        is that we are able to call our own schedule() method here.
        Ours introduces inaccuracy
     */
    replace(periodicTask, schedule(periodicTask, initialDelay, unit));
    return periodicTask;

  }

  private long skewDelay(final long delayArg, final TimeUnit unit) {
    final long skewedDelay = gaussianLong(delayArg);

    System.out.println(String.format(
        "scheduler delaying requested:actual %d:%d %s",
        delayArg, skewedDelay, unit));

    return skewedDelay;
  }

  private long gaussianLong(final long x) {
    return (long) (random.nextGaussian() * standardDeviationNanos) + x;
  }

  final class PeriodicTask extends AtomicReference<Disposable> implements Runnable,
      Disposable {

    final Runnable decoratedRun;
    final long     periodInNanoseconds;
    long count;
    long lastNowNanoseconds;
    long startInNanoseconds;

    PeriodicTask(long firstStartInNanoseconds,
                 Runnable decoratedRun,
                 long firstNowNanoseconds,
                 long periodInNanoseconds) {
      this.decoratedRun = decoratedRun;
      this.periodInNanoseconds = periodInNanoseconds;
      lastNowNanoseconds = firstNowNanoseconds;
      startInNanoseconds = firstStartInNanoseconds;
      lazySet(EMPTY);
    }

    @Override
    public void run() {
      decoratedRun.run();

      if (get() != CANCELLED && !shutdown) {

        long nextTick;

        /*
         FIXME:
         In VirtualTimeScheduler this is:
         long nowNanoseconds = nanoTime
         By calling now() we're adding deferredNanoTime
         */
        long nowNanoseconds = now(TimeUnit.NANOSECONDS);

        // If the clock moved in a direction quite a bit, rebase the repetition period
        if (nowNanoseconds + CLOCK_DRIFT_TOLERANCE_NANOSECONDS < lastNowNanoseconds || nowNanoseconds >= lastNowNanoseconds + periodInNanoseconds + CLOCK_DRIFT_TOLERANCE_NANOSECONDS) {
          nextTick = nowNanoseconds + periodInNanoseconds;
          /*
           * Shift the start point back by the drift as if the whole thing
           * started count periods ago.
           */
          startInNanoseconds = nextTick - (periodInNanoseconds * (++count));
        }
        else {
          nextTick = startInNanoseconds + (++count * periodInNanoseconds);
        }
        lastNowNanoseconds = nowNanoseconds;

        long delay = nextTick - nowNanoseconds;

        /*
         !! The point of reproducing this class from VirtualTimeScheduler
            is that we are able to call our own schedule() method here.
            Ours introduces inaccuracy
         */
        replace(this, schedule(this, delay, TimeUnit.NANOSECONDS));
      }
    }

    @Override
    public void dispose() {
      getAndSet(CANCELLED).dispose();
    }
  }

  static final long CLOCK_DRIFT_TOLERANCE_NANOSECONDS;

  static {
    CLOCK_DRIFT_TOLERANCE_NANOSECONDS = TimeUnit.MINUTES.toNanos(Long.getLong(
        "reactor.scheduler.drift-tolerance",
        15));
  }

  static final Disposable CANCELLED = Disposables.disposed();
  static final Disposable EMPTY = Disposables.never();

  static boolean replace(AtomicReference<Disposable> ref, @Nullable Disposable c) {
    for (; ; ) {
      Disposable current = ref.get();
      if (current == CANCELLED) {
        if (c != null) {
          c.dispose();
        }
        return false;
      }
      if (ref.compareAndSet(current, c)) {
        return true;
      }
    }
  }

}
