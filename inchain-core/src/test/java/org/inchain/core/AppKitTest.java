package org.inchain.core;

import org.inchain.UnitBaseTestCase;
import org.inchain.kits.AppKit;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AppKitTest extends UnitBaseTestCase {

	@Autowired
	private AppKit appKit;
	
	@Test
	public void testAppKit() {
		appKit.startSyn();
	}
}
