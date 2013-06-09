package com.nonstop.android.SoC;

public class Util {
	 // 문자열을 헥사 스트링으로 변환하는 메서드
	  public static String stringToHex(String s) {
	    String result = "";

	    for (int i = 0; i < s.length(); i++) {
	      result += String.format("%02X ", (int) s.charAt(i));
	    }

	    return result;
	  }


	  // 헥사 접두사 "0x" 붙이는 버전
	  public static String stringToHex0x(String s) {
	    String result = "";

	    for (int i = 0; i < s.length(); i++) {
	      result += String.format("0x%02X ", (int) s.charAt(i));
	    }

	    return result;
	  }
	  public static byte[] int2byte( int i ) 
	  {
	             byte[] dest = new byte[4];
	             dest[3] = (byte)(i & 0xff);
	             dest[2] = (byte)((i>>8) & 0xff);
	             dest[1] = (byte)((i>>16) & 0xff);
	             dest[0] = (byte)((i>>24) & 0xff);
	             return dest;
	     }
}
