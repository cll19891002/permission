package com.goufn.permission.shiro.realm;

import com.goufn.permission.entity.User;
import com.goufn.permission.service.PermissionService;
import com.goufn.permission.service.RoleService;
import com.goufn.permission.service.UserService;
import com.goufn.permission.utils.PasswordUtil;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

public class MyShiroRealm extends AuthorizingRealm {
    @Autowired
    private RoleService roleService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private UserService userService;

    /**
     * 对用户进行角色授权
     *
     * @param principals 用户信息
     * @return 返回用户授权信息
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        System.out.println("权限配置-->MyShiroRealm.doGetAuthorizationInfo()");
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        User user = (User)principals.getPrimaryPrincipal();
        Set<String> roles = roleService.findRoleByUserId(user.getId());
        Set<String> permissions = permissionService.findPermsByUserId(user.getId());
        authorizationInfo.setRoles(roles);
        authorizationInfo.setStringPermissions(permissions);
        return authorizationInfo;
    }

    /**
     * 对用户进行认证
     *
     * @param authenticationToken 用户凭证
     * @return 返回用户的认证信息
     * @throws AuthenticationException 用户认证异常信息
     * Realm的认证方法，自动将token传入，比较token与数据库的数据是否匹配
     * 验证逻辑是先根据用户名查询用户，
     * 如果查询到的话再将查询到的用户名和密码放到SimpleAuthenticationInfo对象中，
     * Shiro会自动根据用户输入的密码和查询到的密码进行匹配，如果匹配不上就会抛出异常，
     * 匹配上之后就会执行doGetAuthorizationInfo()进行相应的权限验证。
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        System.out.println("身份认证方法：MyShiroRealm.doGetAuthenticationInfo()");
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        //获取用户的输入的账号.
        String username = token.getUsername();
        System.out.println(token.getCredentials());
        //通过username从数据库中查找 User对象，如果找到，没找到.
        //实际项目中，这里可以根据实际情况做缓存，如果不做，Shiro自己也是有时间间隔机制，2分钟内不会重复执行该方法
        User user = userService.getUserInfo(username);
        System.out.println("----->>user="+ user);
        if(user == null){
            throw new AuthenticationException();
        }
        //认证信息里存放账号密码, getName() 是当前Realm的继承方法,通常返回当前类名 :databaseRealm
        SimpleAuthenticationInfo authenticationInfo = new SimpleAuthenticationInfo(
                user,
                user.getPassword(),
                ByteSource.Util.bytes(user.getSalt()),
                getName()
        );
        return authenticationInfo;
    }

    @Override
    public void setCredentialsMatcher(CredentialsMatcher credentialsMatcher) {
        //HashedCredentialsMatcher是shiro提供的解析盐的实现类
        HashedCredentialsMatcher matcher = new HashedCredentialsMatcher();
        matcher.setHashAlgorithmName(PasswordUtil.algorithmName);
        matcher.setHashIterations(PasswordUtil.hashIterations);
        super.setCredentialsMatcher(matcher);
    }
}
