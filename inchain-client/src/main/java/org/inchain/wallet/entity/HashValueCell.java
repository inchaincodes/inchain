package org.inchain.wallet.entity;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.inchain.utils.Base58;
import org.inchain.wallet.utils.DailogUtil;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayInputStream;

public class HashValueCell extends TableCell<TransactionEntity, DetailValue> {
	
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
		
		final String content = detailValue.getValue();
		Label values = new Label(content);
		values.setEllipsisString(content);
		Tooltip tooltip = new Tooltip(content);
		tooltip.setFont(Font.font(14));
		tooltip.setMaxWidth(480);
		tooltip.setWrapText(true);
		values.setTooltip(tooltip);
		values.setWrapText(true);
		box.getChildren().add(values);
	
		if(content != null) {
			values.setCursor(Cursor.HAND);
			values.setOnMouseMoved(new EventHandler<Event>() {
				@Override
				public void handle(Event event) {
					values.setUnderline(true);
				}
			});
			values.setOnMouseExited(new EventHandler<Event>() {

				@Override
				public void handle(Event event) {
					values.setUnderline(false);
				}
			});
			values.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					StringSelection stsel = new StringSelection(content);
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
					
					DailogUtil.showTip("复制成功", e.getScreenX(), e.getScreenY());
					e.consume();
				}
			});
		}
		
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		setGraphic(box);
	}

	/*
	 * 判断内容详情里面是否有地址
	 */
	private String getAddress(String content) {
		
		String address = null;
		
		int count = 0;
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);
			boolean exist = false;
			for (char ch : Base58.ALPHABET) {
				if(c == ch) {
					exist = true;
					break;
				}
			}
			if(exist) {
				count++;
				if(count >= 34) {
					address = content.substring(i - count + 1, i + 1);
				}
			} else {
				count = 0;
			}
		}
		return address;
	}
}
