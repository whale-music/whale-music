package org.oss.service.impl.alist.model.login.res;

public class DataRes {
	private String token;
	
	public String getToken() {
		return token;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	@Override
	public String toString() {
		return
				"DataRes{" +
						"token = '" + token + '\'' +
						"}";
	}
}
