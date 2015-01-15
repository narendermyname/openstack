package com.naren.openstack.client.executer;

public class C implements A, B {

	public C() {
		// TODO Auto-generated constructor stub
	}

	public void get() {
		System.out.print("Hello");
		
	}

	public static void main(String...a) {
		A aa=new C();
		B b=new C();
		aa.get();
		b.get();
		
	}
}
