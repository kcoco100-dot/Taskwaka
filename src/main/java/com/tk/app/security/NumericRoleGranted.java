package com.tk.app.security;

public class NumericRoleGranted {
	public final static int INACTIVE = 0; // 비회원
	public final static int USER = 1;	// 일반회원
	public final static int ADMIN = 90;	// 관리자
	
	public static int getUserLevel(String authority) {
		try {
			switch (authority) {
			case "USER" : return USER;
			case "ADMIN" : return ADMIN;
			}
		} catch (Exception e) {
		}
		
		return 0;
	}
}
