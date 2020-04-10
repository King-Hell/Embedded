#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#define LOG_TAG "TEST Module"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool flag=false;

extern "C"  JNIEXPORT jboolean JNICALL Java_cn_kinghell_embedded_tools_UserChecker_startLED(JNIEnv* env,jobject){
    int fd=open("/dev/ledtest",O_RDWR | O_SYNC);
    if(fd==-1){
       LOGE("LED驱动打开失败");
       return false;
    }
    if(!flag){
        ioctl(fd,1,0);
        ioctl(fd,1,1);
        ioctl(fd,1,2);
        ioctl(fd,1,3);

        flag=true;
    }else{
            ioctl(fd,0,0);
            ioctl(fd,0,1);
            ioctl(fd,0,2);
            ioctl(fd,0,3);
            flag=false;
    }
    close(fd);

    return true;
}


