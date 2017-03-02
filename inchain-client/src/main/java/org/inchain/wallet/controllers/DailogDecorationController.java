package org.inchain.wallet.controllers;

import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class DailogDecorationController {

	private Stage stage;
	public Button close;
	public Parent dragbar;
	public Pane dailogContent;
	public Label title;

	private volatile double xOffset, yOffset;

	private Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
	
	public void initialize(){
		if (dragbar != null) {
			
            dragbar.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            	if (event.isPrimaryButtonDown()) {
                	xOffset = event.getSceneX();
                	yOffset = event.getSceneY();
                    event.consume();
                }
            });
            dragbar.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            	double newX = event.getScreenX() - xOffset;
            	double newY = event.getScreenY() - yOffset;
            	stage.setX(newX);
            	if (newY < 0) {
            		stage.setY(0);
        		} else if(newY > primaryScreenBounds.getHeight() - 20){
    				stage.setY(primaryScreenBounds.getHeight() - 20);
    			} else {
    				stage.setY(newY);
    			}
            });
        }
		close.addEventHandler(MouseEvent.MOUSE_PRESSED, event ->{
			if(stage != null) {
				stage.close();
			}
		});
	}
    
	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public Pane getDailogContent() {
		return dailogContent;
	}

	public void setDailogContent(Pane dailogContent) {
		this.dailogContent = dailogContent;
	}

	public String getTitle() {
		return title.getText().toString();
	}

	public void setTitle(String title) {
		this.title.setText(title);
	}

}
