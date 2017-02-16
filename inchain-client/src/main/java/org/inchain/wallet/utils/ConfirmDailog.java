package org.inchain.wallet.utils;

import org.inchain.listener.Listener;
import org.inchain.wallet.entity.Point;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 弹出框
 * @author ln
 *
 */
public class ConfirmDailog {

    private Stage dialogStage;
    private Stage primaryStage;
    
    private Listener confirmListener;
    private Listener cancelListener;
    
    public ConfirmDailog(Stage stage) {
    	this(stage, "确认继续该操作吗？");
    }

    public ConfirmDailog(Stage stage, String message) {

    	int width = 280;
    	int height = 160;
    	
    	Point point = DailogUtil.getDailogPoint(width, height);

        dialogStage = new Stage();
        primaryStage = stage;

        dialogStage.initOwner(primaryStage);

        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setWidth(width);
        dialogStage.setHeight(height);
        dialogStage.setX(point.getX());
        dialogStage.setY(point.getY());
        dialogStage.setResizable(false);

        BorderPane borderPaneLayout = new BorderPane();
        borderPaneLayout.getStyleClass().add("root");

        Button confirm = new Button("确认");
        Button cancel = new Button("取消");

        confirm.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                dialogStage.close();
                if(confirmListener != null) {
                	confirmListener.onComplete();
                }
            }
        });

        cancel.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                dialogStage.close();
                if(cancelListener != null) {
                	cancelListener.onComplete();
                }
            }
        });

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().addAll(confirm,cancel);
        hBox.setAlignment(Pos.CENTER);

        VBox vBox = new VBox();
        Label text = new Label(message);
        text.maxWidth(200);
        text.setWrapText(true);
        
        vBox.setSpacing(40);
        vBox.getChildren().addAll(text,hBox);
        vBox.setAlignment(Pos.CENTER);

        borderPaneLayout.setCenter(vBox);

        Scene scene = new Scene(borderPaneLayout);
        dialogStage.setScene(scene);

    }

    public void show() {
        dialogStage.show();
    }
    
    public void setListener(Listener confirmListener) {
		this.confirmListener = confirmListener;
	}
    
    public void setListener(Listener confirmListener, Listener cancelListener) {
    	this.confirmListener = confirmListener;
    	this.cancelListener = cancelListener;
    }
}