package com.thoughtpropulsion.reactrode;

import static com.thoughtpropulsion.reactrode.Functional.returning;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.vavr.Tuple2;
import io.vavr.control.Option;

/**
 * A ring-buffer for {@link GameStateWithBackpressure}
 *
 * An instance of this class may be accessed by only one thread at a time, but may be accessed
 * by more than one thread over its lifetime.
 *
 * @param <T>
 */
public class RingBufferSequential<T> {

  private final int capacity;

  private final AtomicReference<T>[] buffer;

  /*
   These offsets are in logical coordinates, i.e. coordinates meaningful to the client.

   Constraints:
   1. tail >= head
   2. empty when head == tail
   */
  private final AtomicInteger head; // the occupied position for next get
  private final AtomicInteger tail; // the empty position for next put

  public RingBufferSequential(final int capacity) {
    this(capacity,0);
  }

  @SuppressWarnings("unchecked")
  public RingBufferSequential(final int capacity, final int initialIndex) {

    if (capacity < 1)
      throw new IllegalArgumentException(String.format("capacity (%d) must be greater than 0", capacity));

    this.capacity = capacity;

    buffer = (AtomicReference<T>[])new AtomicReference[capacity];
    for(int i = 0; i < capacity; ++i)
      buffer[i] = new AtomicReference();

    head = new AtomicInteger(initialIndex);
    tail = new AtomicInteger(initialIndex);

    assert isEmpty();
  }

  public boolean offer(final T x) {
    if (isFull())
      return false;
    else {
      /*
       This is the only situation where this class can overflow an index
       (i.e. tail or head) since head <= tail always.
       */
      if (tail.get() == Integer.MAX_VALUE)
        throw new RuntimeException(String.format(
            "put() failed because tail index %d is too large to be incremented", tail.get()));
      writeBounded(x);
      tail.incrementAndGet();
      return true;
    }
  }

  /**
   * One key feature of this ring buffer, over a queue, is that it provides O(1) indexed
   * access.
   */
  public Option<T> peek(final int index) {
    if (head.get() <= index && index < tail.get())
      return Option.of(readBounded(index));
    else
      return Option.none();
  }

  public Tuple2<Integer,Option<T>> peek() {
    final int oldHead = head.get();
    // can't use static method Tuple.of() here because...compiler
    return new Tuple2<>(oldHead, peek(oldHead));
  }

  public Tuple2<Integer,Option<T>> poll() {
    final int oldHead = head.get();
    // can't use static method Tuple.of() here because...compiler
    if (isEmpty())
      return new Tuple2<>(oldHead,Option.none());
    else
      return returning(new Tuple2<>(oldHead,Option.of(readBounded(oldHead))),
          _ignored -> head.incrementAndGet()); // side-effect: advance head
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RingBufferSequential{");
    sb.append("capacity=").append(capacity);
    sb.append(", head=").append(head.get());
    sb.append(", tail=").append(tail.get());
    sb.append('}');
    return sb.toString();
  }

  private void writeBounded(final T x) {
    buffer[bound(tail.get())].set(x);
  }

  private T readBounded(final int index) {
    return buffer[bound(index)].get();
  }

  private boolean isEmpty() {
    return head.get() == tail.get();
  }

  private boolean isFull() {
    return tail.get() - head.get() == capacity;
  }

  /**
   * Convert logical coordinates to physical {@code buffer} offset.
   *
   * @param index
   * @return
   */
  private int bound(final int index) {
    return Math.floorMod(index,capacity);
  }

}
