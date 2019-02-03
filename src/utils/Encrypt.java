package utils;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Encrypt {

    public static String encode (String mess){
        return HexBin.encode(Base64.getEncoder().encode(mess.getBytes()));
    }

    public static String decode (String mess){
        return new String(Base64.getDecoder().decode(HexBin.decode(mess)), StandardCharsets.UTF_8);
    }

    public static String toHex(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes(/*YOUR_CHARSET?*/)));
    }

}
