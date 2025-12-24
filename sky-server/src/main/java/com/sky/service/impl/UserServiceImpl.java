package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    //定义常量：微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    //注入
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登陆
     * @param userLoginDTO
     * @return
     */
    public User wxLogin(UserLoginDTO userLoginDTO) {
        ///调用封装好的私有化方法getOpenid来获取openid
        String openid = getOpenid(userLoginDTO.getCode());
        //判断openid是否为空
        if (openid==null){
            //若为空，表示登录失败
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //若不为空，则判断是否为新用户（是否在用户表中）
        User user = userMapper.getByOpenid(openid);
        //若是新用户，则自动完成注册,即往user表中插入新user
        if (user==null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //返回用户对象
        return user;
    }

    private String getOpenid(String code) {
        //调用微信接口服务，获取当前微信用户的openid
        //先将code2Session的请求参数封装到map中
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        //返回值为json数据包，其中包含openid
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        //解析json,获取openid
        JSONObject jsonObject =JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
