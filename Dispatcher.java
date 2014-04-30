import java.util.concurrent.atomic.*;

@SuppressWarnings("unchecked")
class Dispatcher implements Runnable {
  PaddedPrimitiveNonVolatile<Boolean> done;
  PaddedPrimitiveNonVolatile<Integer> inFlight;
  PaddedPrimitive<Integer> inFlightMemFence;
  final PacketGenerator pktGen;
  long totalPackets = 0;
  final int numSources;
  LamportQueue[] queues;
  AtomicInteger tinFlight;
  public Dispatcher(
    PaddedPrimitiveNonVolatile<Boolean> done,
    PaddedPrimitiveNonVolatile<Integer> inFlight,
    PaddedPrimitive<Integer> inFlightMemFence,
    PacketGenerator pktGen,
    int numSources,
    LamportQueue[] queues,
    AtomicInteger tinFlight
  ) {
    this.done = done;
    this.inFlight = inFlight;
    this.inFlightMemFence = inFlightMemFence;
    this.pktGen = pktGen;
    this.numSources = numSources;
    this.queues = queues;
    this.tinFlight = tinFlight;
  }
  public void run() {
    Packet tmp;
    while( !done.value ) {
      for( int i = 0; i < numSources; i++ ) {
        tmp = pktGen.getPacket();
        boolean delivered = false;
        while( !delivered && !done.value ) {
          try {
            while (tinFlight.get() >= 256 && !done.value) {;}
            tinFlight.getAndIncrement();
            inFlight.value++;
            inFlightMemFence.value++;
            queues[i].enq(tmp);
            totalPackets++;
            delivered = true;
          } catch (FullException e) {;}
        }
      }
    }
  }
}