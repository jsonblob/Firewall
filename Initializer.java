import java.util.concurrent.atomic.*;

@SuppressWarnings("unchecked")
class Initializer {
    PacketGenerator pktGen;
    AccessControl ac;
    int total;
    int numSources;

    public Initializer(PacketGenerator pktGen, AccessControl ac, int total, int numSources) {
        this.pktGen = pktGen;
        this.ac = ac;
        this.total = total;
        this.numSources = numSources;
    }

    public void init() {
        LamportQueue[] queues = new LamportQueue[numSources];
        PaddedPrimitiveNonVolatile<Boolean> workersDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
        PaddedPrimitive<Boolean> workersMemFence = new PaddedPrimitive<Boolean>(false);

        for (int i = 0; i < numSources; i++) {
          queues[i] = new LamportQueue<Packet>(32);
        }

        InitializerWorker[] workersData = new InitializerWorker[numSources];
        Thread[] workers = new Thread[numSources];
        for (int i = 0; i < numSources; i++) {
          workersData[i] = new InitializerWorker(workersDone, ac, i, queues);
          workers[i] = new Thread(workersData[i]);
          workers[i].start();
        }

        Packet tmp;
        int n = 0;
        while( n < total ) {
            for( int i = 0; i < numSources; i++ ) {
                tmp = pktGen.getConfigPacket();
                boolean delivered = false;
                while( !delivered ) {
                    try {
                        queues[i].enq(tmp);
                        n++;
                        delivered = true;
                    } catch (FullException e) {;}
                }
            }
        }

        workersDone.value = true;
        workersMemFence.value = true;
        for (int i = 0; i < numSources; i++) {
          try {
            workers[i].join();
          } catch (InterruptedException ignore) {;}
        }
    }
}