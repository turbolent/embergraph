package it.unimi.dsi.compression;

import it.unimi.dsi.bits.BitVector;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanIterator;
import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import it.unimi.dsi.io.InputBitStream;
import it.unimi.dsi.io.OutputBitStream;
import java.io.IOException;
import java.util.Random;
import junit.framework.TestCase;

public abstract class CodecTestCase extends TestCase {

  public void test() {}

  protected void checkPrefixCodec(PrefixCodec codec, Random r) throws IOException {
    int[] symbol = new int[100];
    BooleanArrayList bits = new BooleanArrayList();
    for (int i = 0; i < symbol.length; i++) symbol[i] = r.nextInt(codec.size());
    for (int i3 : symbol) {
      BitVector word = codec.codeWords()[i3];
      for (Boolean aBoolean : word) {
        bits.add(aBoolean);
      }
    }

    BooleanIterator booleanIterator = bits.iterator();
    Decoder decoder = codec.decoder();
    for (int i2 : symbol) {
      assertEquals(decoder.decode(booleanIterator), i2);
    }

    FastByteArrayOutputStream fbaos = new FastByteArrayOutputStream();
    OutputBitStream obs = new OutputBitStream(fbaos, 0);
    obs.write(bits.iterator());
    obs.flush();
    InputBitStream ibs = new InputBitStream(fbaos.array);

    for (int i1 : symbol) {
      assertEquals(decoder.decode(ibs), i1);
    }
  }

  protected void checkLengths(int[] frequency, int[] codeLength, BitVector[] codeWord) {
    for (int i = 0; i < frequency.length; i++)
      assertEquals(Integer.toString(i), codeLength[i], codeWord[i].size());
  }
}
