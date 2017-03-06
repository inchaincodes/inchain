package org.inchain.wallet.controllers;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import org.inchain.wallet.utils.DailogUtil;

import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class DecorationController {
	
	private Stage stage;
	
	@FXML protected Parent dragbar;
    @FXML protected Region nEdge;
    @FXML protected Region sEdge;
    @FXML protected Region wEdge;
    @FXML protected Region eEdge;

    @FXML protected Region swEdge;
    @FXML protected Region seEdge;
    @FXML protected Region nwEdge;
    @FXML protected Region neEdge;

    @FXML protected AnchorPane root;
    @FXML protected HBox detail;
    private Rectangle2D bounds;
    private boolean isMaximized;
    @FXML public Button iconified,close;
    @FXML 
    protected Label balanceId;						//账户余额
	@FXML 
	protected Label accountAddressId;				//账户地址
    @FXML 
    protected Button copyAccountAddressId;			//复制账户地址按钮
    
    private volatile double xOffset, yOffset, xMoveOffset, yMoveOffset;
    
	private Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

	public void initialize() {
    	if (dragbar != null) {
            dragbar.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    //双击最大化
                    if(isMaximized) {
                    	setNewPosition();
                		setDetailPosition(bounds.getWidth()/3);
                    } else {
                    	markWindowPosition();
                    	stage.setX(primaryScreenBounds.getMinX());
                    	stage.setY(primaryScreenBounds.getMinY());
                    	stage.setWidth(primaryScreenBounds.getWidth());
                    	stage.setHeight(primaryScreenBounds.getHeight());
                    	setDetailPosition(primaryScreenBounds.getWidth()/3);
                    }
                    isMaximized = !isMaximized;
                    event.consume();
                }
            });
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
    	/*
    	 *上边界拉伸
    	 * */
    	nEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
    		
    		markMoveOffset(event);
    	});
    	nEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
    		
        	nEdgeDragged(event);
    	});
    	/*
    	 * 下边界拉伸
    	 * */
    	sEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
    		markMoveOffset(event);
    	});
    	sEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, event ->{

    		sEdgeDragged(event);
    	});
    	/*
    	 * 左边界拉伸
    	 * */
    	wEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, event ->{
    		markMoveOffset(event);
    	});
    	wEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, event ->{
    		wEdgeDragged(event);
    	});
    	/*
    	 * 右边界拉伸
    	 * */
    	eEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, event ->{
    		markMoveOffset(event);
    	});
    	eEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, event ->{
    		eEdgeDragged(event);
    	});
    	/*
    	 * 左上拉伸
    	 * */
    	nwEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, event ->{
    		markMoveOffset(event);
    	});
    	nwEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, event ->{
    		nEdgeDragged(event);
    		wEdgeDragged(event);
    	});
    	/*
    	 * 左下拉伸
    	 * */
    	swEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, event ->{
    		markMoveOffset(event);
    	});
    	swEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, event ->{
    		
    		sEdgeDragged(event);
    		wEdgeDragged(event);
    	});
    	/*
    	 * 右上拉伸
    	 * */
    	neEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, event ->{
    		markMoveOffset(event);
    	});
    	neEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, event ->{
    		nEdgeDragged(event);
    		eEdgeDragged(event);
    	});
    	/*
    	 * 右下拉伸
    	 * */
    	seEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, event ->{
    		markMoveOffset(event);
    	});
    	seEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, event ->{
    		sEdgeDragged(event);
    		eEdgeDragged(event);
    	});
    	/*
    	 * 最小化窗体
    	 * */
    	iconified.addEventHandler(MouseEvent.MOUSE_PRESSED, event ->{

    		stage.setIconified(true);
    	});
    	/*
    	 * 关闭窗体
    	 * */
    	close.addEventHandler(MouseEvent.MOUSE_PRESSED, event ->{
    		stage.close();
    	});
    }

	private void setDetailPosition(Double position) {
		root.setLeftAnchor(detail,position);
	}

	private void eEdgeDragged(MouseEvent event) {
		double newX = event.getSceneX() - xMoveOffset;
		double newWidth = newX+ bounds.getWidth();
		if(newWidth >= stage.getMinWidth()){
			stage.setWidth(newWidth);
			setDetailPosition(newWidth/3);
		}
	}
	private void wEdgeDragged(MouseEvent event) {
		double newX = event.getScreenX() - xMoveOffset;
		double newWidth = bounds.getMinX()- newX + bounds.getWidth();
		if (newWidth >= stage.getMinWidth()){
			stage.setWidth(newWidth);
			stage.setX(newX);
			setDetailPosition(newWidth/3);
		}
	}

	private void sEdgeDragged(MouseEvent event) {
		double newY = event.getSceneY() - yMoveOffset;
		double newHeight = newY+ bounds.getHeight();
		if (newHeight >= stage.getMinHeight()){
			stage.setHeight(newHeight);
		}
	}

	private void nEdgeDragged(MouseEvent event) {
		double newY = event.getScreenY() - yMoveOffset;
		double newHeight = bounds.getMinY()  - newY + bounds.getHeight();
		if (newHeight >= stage.getMinHeight()){
			stage.setHeight(newHeight);
			stage.setY(newY);
		}
	}

    /**
     * 记录当前窗体位置及鼠标移动偏移量
     * */
	private void markMoveOffset(MouseEvent event) {
		markWindowPosition();
		xMoveOffset = event.getSceneX();
		yMoveOffset = event.getSceneY();
	}
    
    /**
     * 设置窗口新的位置
     */
	private void setNewPosition() {
		stage.setX(bounds.getMinX());
		stage.setY(bounds.getMinY());
		stage.setWidth(bounds.getWidth());
		stage.setHeight(bounds.getHeight());
	}

	/**
	 * 标记窗口当前位置
	 */
    private void markWindowPosition() {
    	bounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }
    
	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}
	/**
	 * 复制地址到剪切板
	 */
	public void onCopy(MouseEvent e) {
		
		String address = accountAddressId.getText();
		StringSelection stsel = new StringSelection(address);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
		
		DailogUtil.showTip("复制成功", e.getScreenX(), e.getScreenY());
		e.consume();
	}
	
	public void setBalanceId(String balance) {
		balanceId.setText(balance);
	}

	public void setAccountAddressId(String accountAddress) {
		accountAddressId.setText(accountAddress);
	}
	public void setTipText(String tip){
		accountAddressId.getTooltip().setText(tip);
	}
}
