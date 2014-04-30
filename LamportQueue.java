import org.deuce.Atomic;

class FullException extends Exception {
    public FullException() {
        super();
    }
}

class EmptyException extends Exception {
    public EmptyException() {
        super();
    }
}

@SuppressWarnings("unchecked")
class LamportQueue<T> {
  volatile int head = 0, tail = 0;
  T[] items;
  public LamportQueue(int capacity) {
    items = (T[])new Object[capacity];
    head = 0; tail = 0;
  }

  @Atomic
  public void enq(T x) throws FullException {
    if (tail - head == items.length)
     throw new FullException();
    items[tail % items.length] = x;
    tail++;
  }

  @Atomic
  public T deq() throws EmptyException {
    if (tail - head == 0)
      throw new EmptyException();
    T x = items[head % items.length];
    head++;
    return x;
  }
  public boolean empty() {
    return tail - head == 0;
  }
}