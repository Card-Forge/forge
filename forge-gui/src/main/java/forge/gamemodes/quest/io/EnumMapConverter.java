package forge.gamemodes.quest.io;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.EnumMap;
import java.util.Map;

public class EnumMapConverter<K extends Enum<K>, V> implements Converter {

  private final Class<K> keyType;

  public EnumMapConverter(Class<K> keyType) {
    this.keyType = keyType;
  }

  @Override
  public boolean canConvert(Class type) {
    return EnumMap.class.isAssignableFrom(type);
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    EnumMap<K, V> map = (EnumMap<K, V>) source;
    for (Map.Entry<K, V> entry : map.entrySet()) {
      writer.startNode(entry.getKey().name());
      context.convertAnother(entry.getValue());
      writer.endNode();
    }
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    EnumMap<K, V> map = new EnumMap<>(keyType);
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      K key = Enum.valueOf(keyType, reader.getNodeName());
      V value = (V) context.convertAnother(null, Object.class);
      map.put(key, value);
      reader.moveUp();
    }
    return map;
  }


}