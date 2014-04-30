interface PacketWorker extends Runnable {
  public void run();
}

class SerialPacketWorker implements PacketWorker {
  PaddedPrimitiveNonVolatile<Boolean> done;
  final PacketGenerator pktGen;
  AccessControl ac;
  Filter filter;
  final Fingerprint residue = new Fingerprint();
  long fingerprint = 0;
  long totalPackets = 0;
  public SerialPacketWorker(
    PaddedPrimitiveNonVolatile<Boolean> done, 
    PacketGenerator pktGen,
    AccessControl ac,
    Filter filter
  ) {
    this.done = done;
    this.pktGen = pktGen;
    this.ac = ac;
    this.filter = filter;
  }

  public void setConfig(Config conf) {
    ac.setSendPerm(conf.address, conf.personaNonGrata);
    ac.setAcceptPerm(conf.address, conf.addressBegin, conf.addressEnd, conf.acceptingRange);
  }
  
  public void run() {
    Packet tmp;
    while( !done.value ) {
      tmp = pktGen.getPacket();
      switch (tmp.type) {
        case ConfigPacket:
          // System.out.println("--- CONFIG ---");
          Config conf = tmp.config;
          // System.out.println(conf.address);
          // System.out.println(conf.personaNonGrata);
          // System.out.println(conf.addressBegin);
          // System.out.println(conf.addressEnd);
          // System.out.println(conf.acceptingRange);
          setConfig(conf);
          break;
        case DataPacket:
          // System.out.println("--- DATA ---");
          Header header = tmp.header;
          if (ac.getSendPerm(header.source) || !ac.getAcceptPerm(header.dest, header.source)) {
            break;
          }
          // System.out.println(ac.getSendPerm(header.source));
          // System.out.println(ac.getAcceptPerm(header.dest, header.source));
          Body body = tmp.body;
          long bucket = residue.getFingerprint(body.iterations, body.seed);
          // System.out.println(bucket);
          filter.setBucket((int) bucket);
          fingerprint += bucket;
          break;
      }
      totalPackets++;  
    }
  }  
}
