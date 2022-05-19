//
// Created by parrado on 2/07/20.
// Module to receive file descriptor of shared memory through local socket
//

#include "sharedmem.h"

#include <string>
#include <jni.h>
#include <android/log.h>


#include <fcntl.h>
#include <sys/mman.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <string.h>

#include <iostream>
#include <sys/types.h>        /* See NOTES */
#include <sys/socket.h>
#include <sys/un.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <linux/ashmem.h>

#define LOGD(...) do { printf(__VA_ARGS__); printf("\n"); } while(0)
#define LOGE(...) do { printf(__VA_ARGS__); printf("\n"); } while(0)
#define LOGW(...) do { printf(__VA_ARGS__); printf("\n"); } while(0)

#define SOCK_PATH "test.socket"

//Size of shared memory block
#define NSHARED (2048)

#define SETTINGS_OFFSET (NSHARED/2)

#define SAMPLING_RATE  (SETTINGS_OFFSET+0)
#define INPUT_SOURCE     (SETTINGS_OFFSET+1)
#define SINE_FREQUENCY     (SETTINGS_OFFSET+2)
#define NOISE_FLAG (SETTINGS_OFFSET+3)
#define NOISE_POWER (SETTINGS_OFFSET+4)



//Pointer to mmapped shared memory block
double *buffer;

//Global file descriptor
int fd1=-1;

/**
 * Sends file descriptor using given socket
 *
 * @param socket to be used for fd recepion
 * @return received file descriptor; -1 if failed
 *
 * @note socket should be (AF_UNIX, SOCK_DGRAM)
 */
int sendfd(char *path,int fd) {
    int server_socket,client_socket,  rc;
    socklen_t len=0;
    int bytes_rec = 0;
    struct sockaddr_un server_sockaddr, peer_sock;


    char dummy = '$';
    struct msghdr msg;
    struct iovec iov;

    char cmsgbuf[CMSG_SPACE(sizeof(int))];



    memset(&server_sockaddr,'x', sizeof(struct sockaddr_un));


    /****************************************/
    /* Create a UNIX domain datagram socket */
    /****************************************/
    server_socket = socket(AF_UNIX, SOCK_STREAM ,0);
    if (server_socket == -1){
        __android_log_print(ANDROID_LOG_INFO, "SimDSP","SOCKET ERROR = %d", errno);
        return -1;
    }

    /***************************************/
    /* Set up the UNIX sockaddr structure  */
    /* by using AF_UNIX for the family and */
    /* giving it a filepath to bind to.    */
    /*                                     */
    /* Unlink the file so the bind will    */
    /* succeed, then bind to that file.    */
    /***************************************/

    server_sockaddr.sun_family = AF_UNIX;

    server_sockaddr.sun_path[0] = '\0';
    strncpy( server_sockaddr.sun_path+1, path, strlen(path));
    len = offsetof(struct sockaddr_un, sun_path) + 1 + strlen(path);



    rc=bind(server_socket, (struct sockaddr *) &server_sockaddr, len);
    if(rc==-1)
    {
        __android_log_print(ANDROID_LOG_INFO, "SimDSP","BIND ERROR = %s", strerror(errno));
        return -1;
    }
    // Listen
    rc=listen(server_socket, 1);
    if(rc==-1)
    {
        __android_log_print(ANDROID_LOG_INFO, "SimDSP","LISTEN ERROR = %s", strerror(errno));
        return -1;
    }
    client_socket = accept(server_socket, (struct sockaddr *) &server_sockaddr, &len);

    // std::cout<<"Client connected "<<len<<std::endl;




    iov.iov_base = &dummy;
    iov.iov_len = sizeof(dummy);

    msg.msg_name = NULL;
    msg.msg_namelen = 0;
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_flags = 0;
    msg.msg_control = cmsgbuf;
    msg.msg_controllen = CMSG_LEN(sizeof(int));

    struct cmsghdr* cmsg = CMSG_FIRSTHDR(&msg);
    cmsg->cmsg_level = SOL_SOCKET;
    cmsg->cmsg_type = SCM_RIGHTS;
    cmsg->cmsg_len = CMSG_LEN(sizeof(int));

    *(int*) CMSG_DATA(cmsg) = fd;



    /***************************************/
    /* Copy the data to be sent to the     */
    /* buffer and send it to the server.   */
    /***************************************/

    //  std::cout<<"sending data: "<<len<<std::endl;
    int ret = sendmsg(client_socket, &msg, 0);

    if(ret==-1)
    {

        __android_log_print(ANDROID_LOG_INFO, "SimDSP","LISTEN ERROR");
        return -1;
    }

    /*****************************/
    /* Close the socket and exit */
    /*****************************/
    close(client_socket);
    close(server_socket);


    return ret;





}







//Function to send FileDescriptor to SimDSP sketch

extern "C"
JNIEXPORT jint
JNICALL
Java_com_example_simdsp_MainActivity_writeFileDescriptor( JNIEnv* env,
        jobject thiz ){

        int ret;

        char filename[] = "/dev/ashmem";
        fd1 = open(filename, O_RDWR);
       // __android_log_print(ANDROID_LOG_INFO, "SimDSP", "File descriptor created: %d\n", fd1);
        if (fd1 != -1) {


            ioctl(fd1, ASHMEM_SET_NAME, "memory");
            ioctl(fd1, ASHMEM_SET_SIZE, NSHARED * sizeof(double));

            buffer = (double *) mmap(NULL, NSHARED * sizeof(double), PROT_READ | PROT_WRITE,
                                     MAP_SHARED, fd1, 0);


         //   __android_log_print(ANDROID_LOG_INFO, "SimDSP", "File descriptor before1: %d\n", fd1);
            //  std::cout<<"Writing "<<NSHARED<<" random numbers into shared memory"<<std::endl;
            ret=sendfd(SOCK_PATH, fd1);

          //  __android_log_print(ANDROID_LOG_INFO, "SimDSP", "File descriptor after1: %d\n", fd1);

            //munmap(buffer,NSHARED*sizeof(double));
            //close(fd);

            return ret;
        } else
            return -1;

    //  __android_log_print(ANDROID_LOG_INFO, "SimDSP", "File descriptor: %d\n",fd1);


}

//Function to properly close FileDescriptor

extern "C"
JNIEXPORT void
JNICALL
Java_com_example_simdsp_MainActivity_closeFileDescriptor( JNIEnv* env,
                                                          jobject thiz ){



        //munmap(buffer,NSHARED*sizeof(double));
        close(fd1);


    //  __android_log_print(ANDROID_LOG_INFO, "SimDSP", "File descriptor: %d\n",fd1);


}

//Function to read the input signal memory block

extern "C"
JNIEXPORT jdoubleArray
JNICALL
Java_com_example_simdsp_MainActivity_readInputSignal( JNIEnv* env,
                                                         jobject thiz ){

    jdoubleArray inputSignal;

    inputSignal= env->NewDoubleArray((NSHARED)/4);

    if (inputSignal == NULL) {
        return NULL; /* out of memory error thrown */
    }
    int i;
    // move from the temp structure to the java structure
    env->SetDoubleArrayRegion( inputSignal, 0, (NSHARED)/4, buffer);
    return inputSignal;




}

//Function to read the output signal memory block

extern "C"
JNIEXPORT jdoubleArray
JNICALL
Java_com_example_simdsp_MainActivity_readOutputSignal( JNIEnv* env,
                                                      jobject thiz ){

    jdoubleArray outputSignal;

    outputSignal= env->NewDoubleArray((NSHARED)/4);

    if (outputSignal == NULL) {
        return NULL; /* out of memory error thrown */
    }
    int i;
    // move from the temp structure to the java structure
    env->SetDoubleArrayRegion( outputSignal, 0, (NSHARED)/4, buffer+(NSHARED)/4);
    return outputSignal;




}

extern "C"
JNIEXPORT jdouble
JNICALL
Java_com_example_simdsp_MainActivity_readSamplingRate( JNIEnv* env,
                                                       jobject thiz ){




    return buffer[SAMPLING_RATE];




}



extern "C"
JNIEXPORT void
JNICALL
Java_com_example_simdsp_MainActivity_writeInputSource( JNIEnv* env,
                                                       jobject thiz ,jdouble src){


    buffer[INPUT_SOURCE]=src;



}


extern "C"
JNIEXPORT void
JNICALL
Java_com_example_simdsp_MainActivity_writeSineFrequency( JNIEnv* env,
                                                       jobject thiz ,jdouble freq){


    buffer[SINE_FREQUENCY]=freq;



}


extern "C"
JNIEXPORT void
JNICALL
Java_com_example_simdsp_MainActivity_writeNoiseFlag( JNIEnv* env,
                                                       jobject thiz ,jdouble flag){


    buffer[NOISE_FLAG]=flag;



}


extern "C"
JNIEXPORT void
JNICALL
Java_com_example_simdsp_MainActivity_writeNoisePower( JNIEnv* env,
                                                     jobject thiz ,jdouble nPower){


    buffer[NOISE_POWER]=nPower;



}