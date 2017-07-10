package org.inchain.core.exception;

public class CertAcountStateException extends StateException {
    public CertAcountStateException(String msg){
        super(msg);
    }

    public CertAcountStateException(){
        super("认证用户状态错误");
    }
}
