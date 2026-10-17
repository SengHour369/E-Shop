package com.example.eshop.auth.service;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.GenericApplicationContext;
import static org.assertj.core.api.Assertions.*;
class UserDetailsBeanNameTest {
 @Test void profileDetailsAndAuthenticationBeansCanCoexist(){
  try(var context=new GenericApplicationContext()){
   assertThatNoException().isThrownBy(()->new ClassPathBeanDefinitionScanner(context).scan("com.example.eshop.auth.security","com.example.eshop.auth.service.impl"));
   assertThat(context.containsBeanDefinition("adminUserDetailsService")).isTrue();
   assertThat(context.containsBeanDefinition("userDetailsService")).isTrue();
  }
 }
}
