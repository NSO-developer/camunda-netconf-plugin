package com.cisco.adt.data.controllers.exceptions;

public class ValidationFailException extends Exception {
	public ValidationFailException() {
		super();
	}

	public ValidationFailException(String msg) {
		super(msg);
	}
}
