package com.nonstop.android.SoC.Data;


import com.nonstop.android.SoC.Util;

public class GlasOPacket {
	char[] data_={0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

	char start_=(char)0xff;
	char end_=(char)0xff;

	
	GlasOPacket(){
		start_ = (char)0xff;
		end_ = (char)0xff;
	}
	
	String makePacket(){ 
		String temp = "" + (start_)+(data_[0])+(data_[1])+(data_[2])+(data_[3])+(data_[4])+(data_[5])+(data_[6])+(data_[7])+(end_);	
		return temp;
	}
	void setData1(int i){
		data_[0] = (char)i;//(char)(i & 0xFF);
	}
	void setData2(int i){
		data_[1] = (char)i;//(char)(i & 0xFF);
	}
	void setData3(int i){
		data_[2] = (char)i;//(char)(i & 0xFF);
	}
	void setData4(int i){
		data_[3] = (char)i;//(char)(i & 0xFF);
	}
	void setData5(int i){
		data_[4] = (char)i;//(char)(i & 0xFF);
	}
	void setData6(int i){
		data_[5] = (char)i;//(char)(i & 0xFF);
	}
	void setData7(int i){
		data_[6] = (char)i;//(char)(i & 0xFF);
	}
	void setData8(int i){
		data_[7] = (char)i;//(char)(i & 0xFF);
	}
	
}
