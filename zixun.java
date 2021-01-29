package com.h1code2.test;

import com.github.unidbg.Module;
import com.github.unidbg.arm.ARMEmulator;
import com.github.unidbg.linux.android.AndroidARMEmulator;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;

/**
 * 一点资讯
 */

public class TiDianZiXun extends AbstractJni {
    // ARM 虚拟机
    private final ARMEmulator emulator;
    // vm
    private final VM vm;
    //载入模块
    private final Module module;
    private final DvmClass TTEncryptUtils;
    static DalvikModule dm;

    // 初始化虚拟机
    public TiDianZiXun(String soFilePath, String classPath) throws IOException {
        //创建app进程，这里其实可以不用写，这里是我随便写的，使用app本身的进程就可以绕过检测，可以随便写
        emulator = new AndroidARMEmulator("com.hipu.yidian");
        Memory memory = emulator.getMemory();
        // 支持19和23两个sdk
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建DalvikVM 利用apk本身 可以为null
        vm = ((AndroidARMEmulator) emulator).createDalvikVM(new File("src/test/resources/so/yidianzixun.apk"));
        //关键处1 加载so 填写搜文件路径 加载路径
        dm = vm.loadLibrary(new File(soFilePath), false);
        // vm.loadLibrary(new File("unidbg-android/src/com.test/native/android/libs/armeabi-v7a/libcrypto.so"),false);
        vm.setJni(this);
        vm.setVerbose(true); // 日志
        //调用jni 调用JNI_Onload 也就是注册签名的那个
        dm.callJNI_OnLoad(emulator);
        //加载的模块
        module = dm.getModule();
        //关键处2 加载so文件的那个类,填写完整的类路径
        TTEncryptUtils = vm.resolveClass(classPath);
    }

    private String myJni(final String methodSign, Object... args) {
        Number ret = TTEncryptUtils.callStaticJniMethod(emulator, methodSign, args);
        StringObject text = vm.getObject(ret.intValue() & 0xffffffffL);
        return text.getValue();
    }

    public static void main(String[] args) throws IOException {
        String classPath = "com/yidian/news/util/sign/SignUtil";
        String soFilePath = "src/test/resources/so/libutil.so";
        // 3需要调用函数的函数签名
        String method = "signInternal(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;";
        TiDianZiXun duEncrypt = new TiDianZiXun(soFilePath, classPath);
        DvmObject context = duEncrypt.vm.resolveClass("android/content/Context").newObject(null);
        //输出方法调用结果
        String ret = duEncrypt.myJni(method, context, new StringObject(duEncrypt.vm, "yidian5.0.0.01k2uq08wb_1611830597895_117022900"));
        System.out.printf("ret:%s", ret);
        duEncrypt.destroy();
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if ("android/content/pm/Signature->hashCode()I".equals(signature)) {
            return -2081411825;  // 固定签名信息
        }
        throw new AbstractMethodError(signature);
    }

    private void destroy() throws IOException {
        //关闭虚拟机
        emulator.close();
    }
}
