@SuppressWarnings("unchecked")
class Dispatcher implements Runnable {
  PaddedPrimitiveNonVolatile<Boolean> done;
  final PacketSource pkt;
  long totalPackets = 0;
  final int numSources;
  final boolean uniformBool;
  LamportQueue[] queues;
  public Dispatcher(
    PaddedPrimitiveNonVolatile<Boolean> done, 
    PacketSource pkt,
    boolean uniformBool,
    int numSources,
    LamportQueue[] queues) {
    this.done = done;
    this.pkt = pkt;
    this.uniformBool = uniformBool;
    this.numSources = numSources;
    this.queues = queues;
  }
  public void run() {
    Packet tmp;
    while( !done.value ) {
      for( int i = 0; i < numSources; i++ ) {
        if( uniformBool )
          tmp = pkt.getUniformPacket(i);
        else
          tmp = pkt.getExponentialPacket(i);
        try {
          queues[i].enq(tmp);
          totalPackets++;
        } catch (FullException e) {;}
      }
    }
  }
}