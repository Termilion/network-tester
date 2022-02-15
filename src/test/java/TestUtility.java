import general.Utility.TransmissionPayload;

public class TestUtility {
    public static void main(String[] args) {
        testEncodingDecodingTransmissionPayload();
    }

    private static void testEncodingDecodingTransmissionPayload() {
        System.out.println("Testing encoding and decoding");
        int id = 123456789;
        long time = 33441188;
        int round = 10;
        boolean firstPacketOfRound = true;
        boolean lastPacketOfRound = false;

        byte[] buffer = new byte[1000];

        TransmissionPayload in = new TransmissionPayload(id, time, round, firstPacketOfRound, lastPacketOfRound);
        try {
            buffer = in.encode(buffer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        TransmissionPayload out = null;
        try {
            out = TransmissionPayload.decode(buffer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (in != out) {
            System.out.println("in != out");
            System.out.printf("in id: %d\n", in.getId());
            System.out.printf("in time: %d\n", in.getTime());
            System.out.printf("in round: %d\n", in.getRound());
            System.out.printf("in firstPacket: %b\n", in.isFirstPacketOfRound());
            System.out.printf("in lastPacket: %b\n", in.isLastPacketOfRound());
            System.out.printf("out id: %d\n", out.getId());
            System.out.printf("out time: %d\n", out.getTime());
            System.out.printf("out round: %d\n", out.getRound());
            System.out.printf("out firstPacket: %b\n", out.isFirstPacketOfRound());
            System.out.printf("out lastPacket: %b\n", out.isLastPacketOfRound());
            System.exit(1);
        }

        System.out.println("Test successful");
    }
}
