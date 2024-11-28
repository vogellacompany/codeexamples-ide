package com.vogella.contribute.parts;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class ContextAccessExample {
	@Inject
	public ContextAccessExample(@Named("activeEditor") Object o) {
		System.out.println(o);
	}
}
