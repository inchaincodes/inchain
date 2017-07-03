package org.inchain.transaction;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.inchain.account.Account;
import org.inchain.account.AccountBody;
import org.inchain.core.AccountKeyValue;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.utils.Hex;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MakeCertAccountRegisterTransaction {
	
	public static void main(String[] args) throws Exception {
		
		String[] xmls = new String[] { "classpath:/applicationContext-testnet.xml", "classpath:/applicationContext.xml" };

		ClassPathXmlApplicationContext springContext = new ClassPathXmlApplicationContext(xmls);
		
		springContext.start();
		
		AppKit appKit = springContext.getBean(AppKit.class);
		appKit.startSyn();
		
		Thread.sleep(10000l);
		
		AccountKit accountKit = springContext.getBean(AccountKit.class);
		
		try {
			
			AccountKeyValue[] values = {
					new AccountKeyValue("name", "名称", "韩锋"),
					//new AccountKeyValue("address", "单位", "清华大学高等研究院"),
					new AccountKeyValue("descript", "描述", "清华大学iCenter导师\n清华大学量子物理学博士生\n亚洲DACA区块链协会秘书长"),
			};
			AccountBody body = new AccountBody(values);
			System.out.println(Hex.encode(body.serialize()));
			Account account = accountKit.createNewCertAccount("ssssss0", "ssssss1", body, "inchain123");
			System.out.println("base58 : " + account.getAddress().getBase58());
			System.out.println("hash160: " + Hex.encode(account.getAddress().getHash160()));
			System.out.println("mgtx is : " + account.getAccountTransaction().getHash());
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			springContext.close();
			System.exit(0);
		}
	}
	
	public static void main1(String[] args) throws IOException {
		InputStream is = new FileInputStream("D:/1.jpg");  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        int b = 0;  
        while((b = is.read())!=-1){  
            baos.write(b);  
        }  
        System.out.println(Hex.encode(baos.toByteArray()));
        baos.close();
	}
}
