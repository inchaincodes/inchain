package org.inchain.wallet.controllers;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

public class StartupFailedController extends DailogController{

	public Button defineId;
	
	public void initialize() {
		defineId.setOnMouseClicked(new EventHandler<Event>() {
			public void handle(Event event) {
				System.exit(0);
			}
		});
	}
}
