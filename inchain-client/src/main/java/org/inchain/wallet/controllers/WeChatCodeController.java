package org.inchain.wallet.controllers;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.inchain.account.Account;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.utils.Utils;
import org.inchain.wallet.utils.QRcodeUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;


/**
 * 解密钱包
 * @author ln
 *
 */
public class WeChatCodeController extends DailogController {

	public ImageView codeImage;


	public void initialize() {
		Account account = null;
		try {
			AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
			account = accountKit.getDefaultAccount();
			String privateKey = account.getEcKey().getPrivateKeyAsHex();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			QRcodeUtil.genQrcodeToStream(privateKey, outputStream, 220, 220);
			InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
			Image image = new Image(inputStream);
			codeImage.setImage(image);
		}finally {
			if(account != null) {
				account.resetKey();
			}
		}
	}
	
	/*
	 * 取消
	 */
	private void resetAndclose() {
		close();
	}

}
