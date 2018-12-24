package com.irmsimapp.Model.GroupUsers;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;


public class GroupUser implements Serializable {

	@SerializedName("Status")
	@Expose
	private String status;

	@SerializedName("Msg")
	@Expose
	private String msg;

	@SerializedName("Data")
	@Expose
	private List<DataItem> data;

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

	public void setData(List<DataItem> data){
		this.data = data;
	}

	public List<DataItem> getData(){
		return data;
	}

	@Override
 	public String toString(){
		return 
			"GroupUser{" + 
			"status = '" + status + '\'' + 
			",msg = '" + msg + '\'' + 
			",data = '" + data + '\'' + 
			"}";
		}
}