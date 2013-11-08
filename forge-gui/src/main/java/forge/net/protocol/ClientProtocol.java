package forge.net.protocol;


/** The protocol is in charge of serialization of internal classes into whatever remote client understands. 
 * Descendants may use different techniques to serialize data, be it Gson, ProtoBuf or common Java serialization */
public interface ClientProtocol<TIncoming, TOutComing> {
    TIncoming decodePacket(String data);
    String encodePacket(TOutComing packet);
}