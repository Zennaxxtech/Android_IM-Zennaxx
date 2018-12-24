package com.irmsimapp.Model;

import com.google.gson.annotations.SerializedName;

public class CheckUserOnOpenFire {

	@SerializedName("name")
	private String name;

	@SerializedName("email")
	private String email;

	@SerializedName("properties")
	private Object properties;

	@SerializedName("username")
	private String username;

	public void setName(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	public void setEmail(String email){
		this.email = email;
	}

	public String getEmail(){
		return email;
	}

	public void setProperties(Object properties){
		this.properties = properties;
	}

	public Object getProperties(){
		return properties;
	}

	public void setUsername(String username){
		this.username = username;
	}

	public String getUsername(){
		return username;
	}

	@Override
 	public String toString(){
		return 
			"CheckUserOnOpenFire{" +
			"name = '" + name + '\'' + 
			",email = '" + email + '\'' + 
			",properties = '" + properties + '\'' + 
			",username = '" + username + '\'' + 
			"}";
		}
}