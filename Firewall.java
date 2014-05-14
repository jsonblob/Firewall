import java.util.concurrent.atomic.*;

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
    SerialAccessControl ac = new SerialAccessControl(numAddressesLog);

    int cap = Math.min(numAddressesLog, 14);
    int total = (int) Math.pow((double) (1 << cap), 1.5);

    for (int i = 0; i < total; i++) {
        Config conf = pktGen.getConfigPacket().config;
        ac.setSendPerm(conf.address, conf.personaNonGrata);
        ac.setAcceptPerm(conf.address, conf.addressBegin, conf.addressEnd, conf.acceptingRange);
    }

    System.out.println("Initialization Done");

    // Initializer ini = new Initializer(pktGen, ac, total, numSources);
    // ini.init();

    SerialFilter filter = new SerialFilter();
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
    //System.out.println(ac.R[0].list);
    final long totalCount = workerData.totalPackets;
    System.out.println("count: " + totalCount);
    System.out.println("time: " + timer.getElapsedTime());
    System.out.println(totalCount/timer.getElapsedTime() + " pkts / ms");
    // filter.printHistogram();
    // ac.printSendPerms();
  }
}

@SuppressWarnings("unchecked")
class ParallelSTMFirewall {
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

    LamportQueue[] queues = new LamportQueue[numSources];
    PaddedPrimitiveNonVolatile<Boolean> dispatcherDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> dispatcherMemFence = new PaddedPrimitive<Boolean>(false);
    PaddedPrimitiveNonVolatile<Boolean> workersDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> workersMemFence = new PaddedPrimitive<Boolean>(false);

    PaddedPrimitiveNonVolatile<Integer> inFlight = new PaddedPrimitiveNonVolatile<Integer>(0);
    PaddedPrimitive<Integer> inFlightMemFence = new PaddedPrimitive<Integer>(0);

    AtomicInteger tinFlight = new AtomicInteger(0);

    for (int i = 0; i < numSources; i++) {
      queues[i] = new LamportQueue<Packet>(32);
    }

    ParallelSTMAccessControl ac = new ParallelSTMAccessControl(numAddressesLog);

    int cap = Math.min(numAddressesLog, 14);
    int total = (int) Math.pow((double) (1 << cap), 1.5);

    for (int i = 0; i < total; i++) {
        Config conf = pktGen.getConfigPacket().config;
        ac.setSendPerm(conf.address, conf.personaNonGrata);
        ac.setAcceptPerm(conf.address, conf.addressBegin, conf.addressEnd, conf.acceptingRange);
    }

    System.out.println("Initialization Done");

    ParallelSTMFilter filter = new ParallelSTMFilter();

    Dispatcher dispatcherData = new Dispatcher(dispatcherDone, inFlight, inFlightMemFence, pktGen, numSources, queues, tinFlight);
    Thread dispatcher = new Thread(dispatcherData);

    ParallelSTMPacketWorker[] workersData = new ParallelSTMPacketWorker[numSources];
    Thread[] workers = new Thread[numSources];
    for (int i = 0; i < numSources; i++) {
      workersData[i] = new ParallelSTMPacketWorker(workersDone, inFlight, inFlightMemFence, ac, filter, i, queues, tinFlight);
      workers[i] = new Thread(workersData[i]);
      workers[i].start();
    }
    timer.startTimer();
    dispatcher.start();
    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    dispatcherDone.value = true;
    dispatcherMemFence.value = true;
    try {
      dispatcher.join();
    } catch (InterruptedException ignore) {;}
    workersDone.value = true;
    workersMemFence.value = true;
    for (int i = 0; i < numSources; i++) {
      try {
        workers[i].join();
      } catch (InterruptedException ignore) {;}
    }
    timer.stopTimer();
    long totalCount = 0;
    for (int i = 0; i < numSources; i++) {
      totalCount += workersData[i].totalPackets;
    }

    System.out.println("count: " + totalCount);
    System.out.println("time: " + timer.getElapsedTime());
    System.out.println(totalCount/timer.getElapsedTime() + " pkts / ms");
  }
}

@SuppressWarnings("unchecked")
class ParallelFirewall {
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

    LamportQueue[] queues = new LamportQueue[numSources];
    PaddedPrimitiveNonVolatile<Boolean> dispatcherDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> dispatcherMemFence = new PaddedPrimitive<Boolean>(false);
    PaddedPrimitiveNonVolatile<Boolean> workersDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> workersMemFence = new PaddedPrimitive<Boolean>(false);

    PaddedPrimitiveNonVolatile<Integer> inFlight = new PaddedPrimitiveNonVolatile<Integer>(0);
    PaddedPrimitive<Integer> inFlightMemFence = new PaddedPrimitive<Integer>(0);

    AtomicInteger tinFlight = new AtomicInteger(0);

    for (int i = 0; i < numSources; i++) {
      queues[i] = new LamportQueue<Packet>(32);
    }

    ParallelAccessControl ac = new ParallelAccessControl(numAddressesLog);

    int cap = Math.min(numAddressesLog, 12);
    int total = (int) Math.pow((double) (1 << cap), 1.5);

    for (int i = 0; i < total; i++) {
        Config conf = pktGen.getConfigPacket().config;
        ac.setSendPerm(conf.address, conf.personaNonGrata);
        ac.setAcceptPermInit(conf.address, conf.addressBegin, conf.addressEnd, conf.acceptingRange);
    }

    System.out.println("Initialization Done");

    // Initializer ini = new Initializer(pktGen, ac, total, numSources);
    // ini.init();

    ParallelFilter filter = new ParallelFilter();

    Dispatcher dispatcherData = new Dispatcher(dispatcherDone, inFlight, inFlightMemFence, pktGen, numSources, queues, tinFlight);
    Thread dispatcher = new Thread(dispatcherData);

    ParallelPacketWorker[] workersData = new ParallelPacketWorker[numSources];
    Thread[] workers = new Thread[numSources];
    for (int i = 0; i < numSources; i++) {
      workersData[i] = new ParallelPacketWorker(workersDone, inFlight, inFlightMemFence, ac, filter, i, queues, tinFlight);
      workers[i] = new Thread(workersData[i]);
      workers[i].start();
    }
    timer.startTimer();
    dispatcher.start();
    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    dispatcherDone.value = true;
    dispatcherMemFence.value = true;
    try {
      dispatcher.join();
    } catch (InterruptedException ignore) {;}
    workersDone.value = true;
    workersMemFence.value = true;
    for (int i = 0; i < numSources; i++) {
      try {
        workers[i].join();
      } catch (InterruptedException ignore) {;}
    }
    timer.stopTimer();
    long totalCount = 0;
    for (int i = 0; i < numSources; i++) {
      totalCount += workersData[i].totalPackets;
    }

    System.out.println("count: " + totalCount);
    System.out.println("time: " + timer.getElapsedTime());
    System.out.println(totalCount/timer.getElapsedTime() + " pkts / ms");
  }
}