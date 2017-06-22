package org.inchain.wallet.utils;

import org.inchain.wallet.Context;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 弹出提示层
 * @author cj
 *
 */
public class TipsWindows {

	private Stage dialogStage;
	private Stage primaryStage;
	private boolean complete;
	
	private Label text;
	
	/**
	 * 提示层
	 * @param stage
	 * @param msg  
	 */
	public TipsWindows(Stage stage,String msg) {
		
		if(stage == null) {
			stage = Context.getMainStage();
		}
		
		int width = (int) stage.getWidth();
		int height = (int) stage.getHeight();

		dialogStage = new Stage(StageStyle.TRANSPARENT);
		primaryStage = stage;
		dialogStage.initOwner(primaryStage);

		dialogStage.initModality(Modality.WINDOW_MODAL);
		dialogStage.setWidth(width);
		dialogStage.setHeight(height);
		
		dialogStage.setX(stage.getX());
		dialogStage.setY(stage.getY());
		dialogStage.setResizable(false);
		BorderPane borderPaneLayout = new BorderPane();
		borderPaneLayout.getStyleClass().add("root");
		
		text = new Label(msg);
		text.maxWidth(80);
		text.setPadding(new Insets(0,50,0,60));
		text.setWrapText(true);
		
		Image image = new Image("/images/loading.gif");
		ImageView loading = new ImageView(image);
		VBox vBox = new VBox();
		vBox.setSpacing(40);
		vBox.getChildren().addAll(loading,text);
		vBox.setAlignment(Pos.CENTER);
		
		borderPaneLayout.setCenter(vBox);
		Scene scene = new Scene(borderPaneLayout);
		scene.getStylesheets().add("/resources/css/tipWindows.css");
		scene.setFill(null);
		
		dialogStage.setScene(scene);
	}
	
	public void show() {
		dialogStage.show();
	}

	public boolean isComplete() {
		return complete;
	}
	public void settStatus(boolean complete) {
		this.complete = complete;
	}
	
	public void close() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				dialogStage.close();
			}
		});
	}
}
