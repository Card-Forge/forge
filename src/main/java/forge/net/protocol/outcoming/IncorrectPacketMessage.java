package forge.net.protocol.outcoming;

import forge.net.protocol.incoming.IncorrectPacket;


public class IncorrectPacketMessage implements IMessage {

    IncorrectPacket badPacket;
    public IncorrectPacketMessage(IncorrectPacket packet) {
        badPacket = packet;
    } 

    @Override
    public String toNetString() {
        return String.format("Wrong syntax for %s command: parameter #%d is %s", badPacket.getIntendedCode().getOpcode(), 1+badPacket.getIndex(), badPacket.getString());
    }

}
