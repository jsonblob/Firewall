class SerialFirewall {
  public static void main(String[] args) {
    final int numMilliseconds = Integer.parseInt(args[0]);
    final int numSources = Integer.parseInt(args[1]);  
    final int numAddressesLog = Integer.parseInt(args[2]);
    final int numTrainsLog = Integer.parseInt(args[3]);
    final double meanTrainSize = Double.parseDouble(args[4]);
    final double meanTrainsPerComm = Double.parseDouble(args[5]);
    final int meanWindow = Integer.parseInt(args[6]);
    final int meanCommsPerAddress = Integer.parseInt(args[7]);
    final int meanWork = Integer.parseInt(args[8]);
    final double configFraction = Double.parseDouble(args[9]);
    final double pngFraction = Double.parseDouble(args[10]);
    final double acceptingFraction = Double.parseDouble(args[11]);
    
    StopWatch timer = new StopWatch();
    PacketGenerator pktGen = new PacketGenerator(
        numAddressesLog,
        numTrainsLog,
        meanTrainSize,
        meanTrainsPerComm,
        meanWindow,
        meanCommsPerAddress,
        meanWork,
        configFraction,
        pngFraction,
        acceptingFraction
    );
    AccessControl ac = new AccessControl(numAddressesLog);

    int total = (int) Math.pow((double) (1 << numAddressesLog), 1.5);
    for (int i = 0; i < total; i++) {
        Config conf = pktGen.getConfigPacket().config;
        ac.setSendPerm(conf.address, conf.personaNonGrata);
        ac.setAcceptPerm(conf.address, conf.addressBegin, conf.addressEnd, conf.acceptingRange);
    }

    Filter filter = new Filter();
    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
        
    SerialPacketWorker workerData = new SerialPacketWorker(done, pktGen, ac, filter);
    Thread workerThread = new Thread(workerData);
    
    workerThread.start();
    timer.startTimer();
    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    done.value = true;
    memFence.value = true;  // memFence is a 'volatile' forcing a memory fence
    try {                   // which means that done.value is visible to the workers
      workerThread.join();
    } catch (InterruptedException ignore) {;}      
    timer.stopTimer();
    final long totalCount = workerData.totalPackets;
    System.out.println("count: " + totalCount);
    System.out.println("time: " + timer.getElapsedTime());
    System.out.println(totalCount/timer.getElapsedTime() + " pkts / ms");
    filter.printHistogram();
    ac.printSendPerms();
  }
}

// @SuppressWarnings("unchecked")
// class SerialQueueFirewall {
//   public static void main(String[] args) {
//     final int numMilliseconds = Integer.parseInt(args[0]);   
//     final int numSources = Integer.parseInt(args[1]);
//     final long mean = Long.parseLong(args[2]);
//     final boolean uniformFlag = Boolean.parseBoolean(args[3]);
//     final int queueDepth = Integer.parseInt(args[4]);
//     final short experimentNumber = Short.parseShort(args[5]);
   
//     StopWatch timer = new StopWatch();
//     PacketSource pkt = new PacketSource(mean, numSources, experimentNumber);
//     PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
//     PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);

//     LamportQueue[] queues = new LamportQueue[numSources];
//     for (int i = 0; i < numSources; i++) {
//       queues[i] = new LamportQueue<Packet>(queueDepth);
//     }

//     SerialQueuePacketWorker workerData = new SerialQueuePacketWorker(done, pkt, uniformFlag, numSources, queues);

//     Thread workerThread = new Thread(workerData);
    
//     workerThread.start();
//     timer.startTimer();
//     try {
//       Thread.sleep(numMilliseconds);
//     } catch (InterruptedException ignore) {;}
//     done.value = true;
//     memFence.value = true;  // memFence is a 'volatile' forcing a memory fence
//     try {                   // which means that done.value is visible to the workers
//       workerThread.join();
//     } catch (InterruptedException ignore) {;}      
//     timer.stopTimer();
//     final long totalCount = workerData.totalPackets;
//     System.out.println("count: " + totalCount);
//     System.out.println("time: " + timer.getElapsedTime());
//     System.out.println(totalCount/timer.getElapsedTime() + " pkts / ms");
//   }
// }

// @SuppressWarnings("unchecked")
// class ParallelFirewall {
//   public static void main(String[] args) {
//     final int numMilliseconds = Integer.parseInt(args[0]);     
//     final int numSources = Integer.parseInt(args[1]);
//     final long mean = Long.parseLong(args[2]);
//     final boolean uniformFlag = Boolean.parseBoolean(args[3]);
//     final int queueDepth = Integer.parseInt(args[4]);
//     final short experimentNumber = Short.parseShort(args[5]);

//     StopWatch timer = new StopWatch();
//     PacketSource pkt = new PacketSource(mean, numSources, experimentNumber);
    
//     LamportQueue[] queues = new LamportQueue[numSources];
//     PaddedPrimitiveNonVolatile<Boolean> dispatcherDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
//     PaddedPrimitive<Boolean> dispatcherMemFence = new PaddedPrimitive<Boolean>(false);
//     PaddedPrimitiveNonVolatile<Boolean> workersDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
//     PaddedPrimitive<Boolean> workersMemFence = new PaddedPrimitive<Boolean>(false);

//     for (int i = 0; i < numSources; i++) {
//       queues[i] = new LamportQueue<Packet>(queueDepth);
//     }

//     Dispatcher dispatcherData = new Dispatcher(dispatcherDone, pkt, uniformFlag, numSources, queues);
//     Thread dispatcher = new Thread(dispatcherData);

//     ParallelPacketWorker[] workersData = new ParallelPacketWorker[numSources];
//     Thread[] workers = new Thread[numSources];
//     for (int i = 0; i < numSources; i++) {
//       workersData[i] = new ParallelPacketWorker(workersDone, queues[i]);
//       workers[i] = new Thread(workersData[i]);
//       workers[i].start();
//     }
//     timer.startTimer();
//     dispatcher.start();
//     try {
//       Thread.sleep(numMilliseconds);
//     } catch (InterruptedException ignore) {;}
//     dispatcherDone.value = true;
//     dispatcherMemFence.value = true;
//     try {
//       dispatcher.join();
//     } catch (InterruptedException ignore) {;}
//     workersDone.value = true;
//     workersMemFence.value = true;
//     for (int i = 0; i < numSources; i++) {
//       try {
//         workers[i].join();
//       } catch (InterruptedException ignore) {;}
//     }
//     timer.stopTimer();
//     long totalCount = 0;
//     for (int i = 0; i < numSources; i++) {
//       totalCount += workersData[i].totalPackets;
//     }
//     System.out.println("count: " + totalCount);
//     System.out.println("time: " + timer.getElapsedTime());
//     System.out.println(totalCount/timer.getElapsedTime() + " pkts / ms");
//   }
// }
