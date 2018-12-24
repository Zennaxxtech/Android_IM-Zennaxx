package com.irmsimapp.Model.ForgotPassword;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ForgotPassword{

	@SerializedName("Status")
	private String status;

	@SerializedName("Msg")
	private String msg;

	@SerializedName("Data")
	private List<Object> data;

	public void setStatus(String status){
		this.status = status;
	}

	public String getStatus(){
		return status;
	}

	public void setMsg(String msg){
		this.msg = msg;
	}

	public String getMsg(){
		return msg;
	}

	public void setData(List<Object> data){
		this.data = data;
	}

	public List<Object> getData(){
		return data;
	}

	@Override
 	public String toString(){
		return 
			"ForgotPassword{" + 
			"status = '" + status + '\'' + 
			",msg = '" + msg + '\'' + 
			",data = '" + data + '\'' + 
			"}";
		}
}