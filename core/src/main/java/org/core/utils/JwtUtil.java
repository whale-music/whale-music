package org.core.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import org.core.config.JwtConfig;

import java.util.Date;

public class JwtUtil {
    
    private JwtUtil() {
    }
    
    /**
     * 生成jwt字符串，根据设置时间过期  JWT(json web token)
     *
     * @param userId                                                              根据用户ID生成token
     * @param info,Map的value只能存放值的类型为：Map，List，Boolean，Integer，Long，Double，String and Date
     * @return 返回token
     */
    public static String sign(String userId, String info) {
        Date date = new Date(System.currentTimeMillis() + JwtConfig.EXPIRE_TIME);
        Algorithm algorithm = Algorithm.HMAC256(JwtConfig.SEED_KEY);
        return JWT.create()
                  // 将userId保存到token里面
                  .withAudience(userId)
                  // 存放自定义数据
                  .withClaim("info", info)
                  // 根据设定的时间过期
                  .withExpiresAt(date)
                  // token的密钥
                  .sign(algorithm);
    }
    
    /**
     * 根据token获取userId
     *
     * @param token token值
     * @return 返回User ID
     */
    public static String getUserId(String token) {
        try {
            return JWT.decode(token).getAudience().get(0);
        } catch (JWTDecodeException e) {
            return null;
        }
    }
    
    /**
     * 根据token获取自定义数据info
     *
     * @param token token
     * @return 返回自定义数据
     */
    public static String getInfo(String token) {
        try {
            return JWT.decode(token).getClaim("info").asString();
        } catch (JWTDecodeException e) {
            return null;
        }
    }
    
    /**
     * 校验token
     *
     * @param token token 数据
     */
    public static void checkSign(String token) {
        Algorithm algorithm = Algorithm.HMAC256(JwtConfig.SEED_KEY);
        JWTVerifier verifier = JWT.require(algorithm)
                                  //.withClaim("username, username)
                                  .build();
        verifier.verify(token);
    }
}

