package org.inchain.wallet.entity;

import java.io.ByteArrayInputStream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class DetailValueCell extends TableCell<TransactionEntity, DetailValue> {
	
	@Override
	protected void updateItem(DetailValue detailValue, boolean empty) {
		super.updateItem(detailValue, empty);
		if (empty) {
			setGraphic(null);
			return;
		}
		VBox box = new VBox(3);
		Insets padding= new Insets(10,10,10,10);
		box.setPadding(padding);
		box.setAlignment(Pos.CENTER_LEFT);
		if(detailValue.getImg() != null) {
			ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(detailValue.getImg())));
			
			imageView.setFitWidth(30);
			imageView.setFitHeight(30);
			
			box.getChildren().add(imageView);
		}
		
		String content = detailValue.getValue();
		Label values = new Label(content);
		values.setEllipsisString(content.substring(0, content.length() > 100 ? 100 : content.length()));
		values.setWrapText(true);
		Tooltip tooltip = new Tooltip(content);
		tooltip.setFont(Font.font(14));
		tooltip.setMaxWidth(480);
		tooltip.setWrapText(true);
		values.setTooltip(tooltip);
		box.getChildren().add(values);
		
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		setGraphic(box);
	}
}
