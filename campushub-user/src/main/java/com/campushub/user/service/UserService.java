package com.campushub.user.service;

import com.campushub.user.dto.UpdatePasswordRequest;
import com.campushub.user.dto.UpdateSignatureRequest;
import com.campushub.user.vo.UserInfoVO;

/** 用户信息业务接口。 */
public interface UserService {

    /** 查询当前登录用户的个人信息。 */
    UserInfoVO getCurrentUserInfo();

    /** 修改当前用户的个性签名（允许传空串清空签名）。 */
    void updateSignature(UpdateSignatureRequest request);

    /**
     * 修改当前用户密码：校验原密码后写新密文，并踢出全部登录设备。
     *
     * <p>改密后踢出所有设备是安全惯例：若旧密码已泄露，改密后旧会话不能继续有效。
     */
    void updatePassword(UpdatePasswordRequest request);
}
