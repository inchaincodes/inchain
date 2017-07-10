package org.inchain.core.exception;

public class ProductStateException extends StateException{
    public ProductStateException(String msg){
        super(msg);
    }

    public ProductStateException(){
        super("产品状态错误");
    }
}
