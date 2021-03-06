package org.hswebframework.web.authorization.basic.web;

import org.hswebframework.web.authorization.listener.AuthorizationListener;
import org.hswebframework.web.authorization.listener.event.AuthorizationSuccessEvent;
import org.hswebframework.web.authorization.token.UserToken;
import org.hswebframework.web.authorization.token.UserTokenHolder;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhouhao
 */
public class UserOnSignIn implements AuthorizationListener<AuthorizationSuccessEvent>
        ,ApplicationListener<AuthorizationSuccessEvent>{

    private String defaultTokenType = "sessionId";

    private UserTokenManager userTokenManager;

    private List<UserTokenGenerator> userTokenGenerators = new ArrayList<>();

    public UserOnSignIn(UserTokenManager userTokenManager) {
        this.userTokenManager = userTokenManager;
    }

    public void setDefaultTokenType(String defaultTokenType) {
        this.defaultTokenType = defaultTokenType;
    }

    @Autowired(required = false)
    public void setUserTokenGenerators(List<UserTokenGenerator> userTokenGenerators) {
        this.userTokenGenerators = userTokenGenerators;
    }

    @Override
    public void on(AuthorizationSuccessEvent event) {
       onApplicationEvent(event);
    }

    @Override
    public void onApplicationEvent(AuthorizationSuccessEvent event) {
        UserToken token = UserTokenHolder.currentToken();
        String tokenType = (String) event.getParameter("token_type").orElse(defaultTokenType);

        if (token != null) {
            //先退出已登陆的用户
            userTokenManager.signOutByToken(token.getToken());
        }
        //创建token
        GeneratedToken newToken = userTokenGenerators.stream()
                .filter(generator -> generator.getSupportTokenType().equals(tokenType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(tokenType))
                .generate(event.getAuthentication());
        //登入
        userTokenManager.signIn(newToken.getToken(), newToken.getType(), event.getAuthentication().getUser().getId(), newToken.getTimeout());

        //响应结果
        event.getResult().putAll(newToken.getResponse());

    }
}
