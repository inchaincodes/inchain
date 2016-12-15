package org.inchain;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * spring容器管理器，获取注入的实体类
 * @author ln
 *
 */
@Service
public class SpringContextUtils implements ApplicationContextAware{
    
   public static ApplicationContext context;

   @Override
   public void setApplicationContext(ApplicationContext context)
           throws BeansException {
       SpringContextUtils.context = context;
   }
   
   public static <T> T getBean(Class<T> clazz){
       return context.getBean(clazz);
   }
   
   public static <T> T getBean(String id, Class<T> clazz){
	   return context.getBean(id, clazz);
   }
    
   public static ApplicationContext getContext(){
       return context;
   }
}