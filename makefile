JFLAGS=-cp :.:./deuce/
JC= javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	PaddedPrimitive.java \
	RandomGenerator.java \
	PacketGenerator.java \
	Fingerprint.java \
	StopWatch.java \
	LamportQueue.java \
	RangeSkipList.java \
	AccessControl.java \
	Filter.java \
	Worker.java \
	Firewall.java \
	LamportQueue.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class