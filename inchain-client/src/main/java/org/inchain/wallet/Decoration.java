package org.inchain.wallet;

import java.net.URL;
import java.util.Iterator;

import org.inchain.wallet.controllers.DecorationController;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Decoration extends Application {
	
	private static final int DEFAULT_WIDTH = 1050;
	private static final int DEFAULT_HEIGHT = 735;
	public DecorationController decorationController;
	@FXML
	protected Pane mainContent;

    
	@Override
	public void start(Stage stage) throws Exception {
		URL location = getClass().getResource("/resources/template/decoration.fxml");
        FXMLLoader loader = new FXMLLoader(location);

        Pane main = loader.load();

        decorationController = loader.getController();
        decorationController.setStage(stage);
        
        ObservableList<Node> childrens = main.getChildren();
        Iterator<Node> it = childrens.iterator();
        while(it.hasNext()) {
        	Node node = it.next();
        	if("mainContent".equals(node.getId())) {
        		mainContent = (Pane) node;
        		break;
        	}
        }
        Scene scene = new Scene(main, Color.TRANSPARENT);
        scene.getStylesheets().add("/resources/css/wallet.css");
        scene.getStylesheets().add("/resources/css/tableView.css");
		scene.getStylesheets().add("/resources/css/decoration.css");
        
		stage.setScene(scene);

		stage.setMinHeight(DEFAULT_HEIGHT);
        stage.setMinWidth(DEFAULT_WIDTH);
        stage.setWidth(DEFAULT_WIDTH);
        stage.setHeight(DEFAULT_HEIGHT);
        stage.initStyle(StageStyle.TRANSPARENT);
	}
	

}
