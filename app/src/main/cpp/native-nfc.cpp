#include<jni.h>
#include<string>
#include<android/log.h>
#include<termios.h>
#include<unistd.h>
#include<sys/types.h>
#include<sys/stat.h>
#include<fcntl.h>

#define LOG_TAG "NFC Module"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

unsigned char buf[100];
int fd;
unsigned char WakeUp[] = {0x55, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0x03, 0xFD, 0xD4, 0x14, 0x01, 0x17, 0x00};
unsigned char Label[] = {0x00, 0x00, 0xFF, 0x04, 0xFC, 0xD4, 0x4A, 0x01, 0x00, 0xE1, 0x00};

int set_port_option(int fd,int nSpeed, int nBits, char nEvent, int nStop);
bool WakeUpNFC();

extern "C" JNIEXPORT jboolean JNICALL Java_cn_kinghell_embedded_NfcFragment_openNfc(JNIEnv* env,jobject /* this */) {
    	int nfcflag=0,choice;
    	fd = open("/dev/ttyS2",O_RDWR|O_NOCTTY|O_NONBLOCK);
    	if(fd<0){
    		LOGE("/dev/ttyS2文件打开失败");
    		return false;
    	}
    	LOGD("/dev/ttyS2文件打开成功");
    	int ret=set_port_option(fd,115200,8,'N', 1);//设置波特率
    	if(ret==-1)
    	    return false;
    	if(!WakeUpNFC())
            return false;
        return true;
}

extern "C" JNIEXPORT jboolean JNICALL Java_cn_kinghell_embedded_NfcFragment_closeNfc(JNIEnv* env,jobject /* this */){
    int ret=close(fd);
    if(ret==0)
        return true;
    else
        return false;
}

extern "C" JNIEXPORT jstring JNICALL Java_cn_kinghell_embedded_NfcFragment_scanNfc(JNIEnv* env,jobject /* this */){
	char uid[]="00-00-00-00";
	int writebyte =write(fd, Label, sizeof(Label));
	int readbyte=read(fd,buf,30);
	LOGD("readbyte == %d",readbyte);
	if(readbyte)
	{
		for(int i=0;i<readbyte;i++)
		{
			if(buf[i]==0x8&&buf[i+1]==0x04){
			    sprintf(uid,"%02x-%02x-%02x-%02x",buf[i+2],buf[i+3],buf[i+4],buf[i+5]);
			    LOGD("UID:%s",uid);
				break;
			}
		}
		memset(buf,0,30);
	}
	return env->NewStringUTF(uid);
}

int set_port_option(int fd,int nSpeed, int nBits, char nEvent, int nStop)
{
	struct termios newtio,oldtio;

	tcflush(fd, TCIOFLUSH);

	if  ( tcgetattr( fd,&oldtio)  !=  0)
	{
		LOGE("设置串口失败");
		return -1;
	}

	bzero( &newtio, sizeof( newtio ) );
	newtio.c_cflag  |=  CLOCAL | CREAD;
	newtio.c_cflag &= ~CSIZE;

	switch( nBits )
	{
	case 7:
		newtio.c_cflag |= CS7;
		break;
	case 8:
		newtio.c_cflag |= CS8;
		break;
	}

	switch( nEvent )
	{
	case 'O':
		newtio.c_cflag |= PARENB;
		newtio.c_cflag |= PARODD;
		newtio.c_iflag |= (INPCK | ISTRIP);
		break;
	case 'E':
		newtio.c_iflag |= (INPCK | ISTRIP);
		newtio.c_cflag |= PARENB;
		newtio.c_cflag &= ~PARODD;
		break;
	case 'N':
		newtio.c_cflag &= ~PARENB;
		break;
	}

	switch( nSpeed )
	{
	case 2400:
		cfsetispeed(&newtio, B2400);
		cfsetospeed(&newtio, B2400);
		break;
	case 4800:
		cfsetispeed(&newtio, B4800);
		cfsetospeed(&newtio, B4800);
		break;
	case 9600:
		cfsetispeed(&newtio, B9600);
		cfsetospeed(&newtio, B9600);
		break;
	case 115200:
		cfsetispeed(&newtio, B115200);
		cfsetospeed(&newtio, B115200);
		break;
	default:
		cfsetispeed(&newtio, B9600);
		cfsetospeed(&newtio, B9600);
		break;
	}
	if( nStop == 1 )
	{
		newtio.c_cflag &=  ~CSTOPB;
	}
	else if ( nStop == 2 )
	{
		newtio.c_cflag |=  CSTOPB;
	}

	newtio.c_cc[VTIME]  = 0;
	newtio.c_cc[VMIN] = 0;

	tcflush(fd,TCIOFLUSH);


	if((tcsetattr(fd,TCSANOW,&newtio))!=0)
	{
		LOGE("设置串口失败");
		return -1;
	}
	LOGD("设置串口成功");
	return 0;
}

bool WakeUpNFC()
{
	int writebyte =write(fd, WakeUp, sizeof(WakeUp));
	sleep(1);
	int readbyte=read(fd,buf,30);
	memset(buf,0,30);
	if(readbyte>0)
	    return true;
	else{
	    LOGE("NFC激活失败");
	    return false;
	}

}