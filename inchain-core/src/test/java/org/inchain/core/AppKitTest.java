package org.inchain.core;

import org.inchain.BaseTestCase;
import org.inchain.kits.AppKit;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AppKitTest extends BaseTestCase {

	@Autowired
	private AppKit appKit;
	
	@Test
	public void testAppKit() {
		appKit.startSyn();
	}
}
