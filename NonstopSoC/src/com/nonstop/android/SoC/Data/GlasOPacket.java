package com.nonstop.android.SoC.Data;

public class GlasOPacket {
	char start_=0xff;
	char mode_=0x00;
	char[] data_={0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
	char end_=0xff;
	
	GlasOPacket(){
		start_ = 0xff;
		mode_=0x00;
		end_ = 0xff;
	}
	
	String makePacket(){
		return ("" + start_+mode_+data_[0]+data_[1]+data_[2]+data_[3]+data_[4]+data_[5]+data_[6]+end_);	
	}
	void setMode(char m){
		mode_ = m;
	}
	void setData1(char m){
		data_[0] = m;
	}
	void setData2(char m){
		data_[1] = m;
	}
	void setData3(char m){
		data_[2] = m;
	}
	void setData4(char m){
		data_[3] = m;
	}
	void setData5(char m){
		data_[4] = m;
	}
	void setData6(char m){
		data_[5] = m;
	}
	void setData7(char m){
		data_[6] = m;
	}
	
}
